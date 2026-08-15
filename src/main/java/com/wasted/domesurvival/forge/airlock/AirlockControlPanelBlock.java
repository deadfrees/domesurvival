package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class AirlockControlPanelBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape NORTH = Block.box(1.15, 1.0, 10.15, 14.85, 15.35, 15.88);
    private static final VoxelShape SOUTH = Block.box(1.15, 1.0, 0.12, 14.85, 15.35, 5.85);
    private static final VoxelShape WEST  = Block.box(10.15, 1.0, 1.15, 15.88, 15.35, 14.85);
    private static final VoxelShape EAST  = Block.box(0.12, 1.0, 1.15, 5.85, 15.35, 14.85);

    public AirlockControlPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirlockControlPanelBlockEntity(pos, state);
    }

    public AirlockControlPanelBlockEntity ensureBlockEntity(Level level, BlockPos pos, BlockState state) {
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing instanceof AirlockControlPanelBlockEntity panel) {
            return panel;
        }

        AirlockControlPanelBlockEntity created = new AirlockControlPanelBlockEntity(pos, state);
        level.setBlockEntity(created);
        return created;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (!clickedFace.getAxis().isHorizontal()) {
            return null;
        }

        BlockState state = defaultBlockState()
                .setValue(FACING, clickedFace)
                .setValue(ACTIVE, false);

        return state.canSurvive(context.getLevel(), context.getClickedPos())
                ? state
                : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(AirlockPanelRegistry.AIRLOCK_BINDING_KEY.get())) {
            // PASS lets AirlockBindingKeyItem.useOn handle binding/clearing.
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            AirlockControlPanelBlockEntity panel = ensureBlockEntity(serverLevel, pos, state);
            if (panel.hasBinding()) {
                handleLinkedPanelUse(serverLevel, panel, player);
            } else {
                // Until the starter dome is migrated to V54/V55 gates, preserve
                // its existing coordinate-based panel behavior for unbound panels.
                AirlockService.handleMountedPanelUse(serverLevel, pos, state, player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void handleLinkedPanelUse(ServerLevel level,
                                             AirlockControlPanelBlockEntity panel,
                                             Player player) {
        if (panel.linkedGateDimension() == null || panel.linkedGatePos() == null) {
            player.displayClientMessage(
                    Component.translatable("message.domesurvival.airlock_panel.not_bound")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        if (!level.dimension().equals(panel.linkedGateDimension())) {
            player.displayClientMessage(
                    Component.translatable("message.domesurvival.airlock_panel.wrong_dimension")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        BlockPos gateMasterPos = panel.linkedGatePos();
        BlockState gateState = level.getBlockState(gateMasterPos);
        if (!(gateState.getBlock() instanceof AirlockGateBlock gate)
                || !gate.isValidMaster(level, gateMasterPos)) {
            AirlockPanelLinkIndex.setPanelActive(level, panel.getBlockPos(), false);
            player.displayClientMessage(
                    Component.translatable("message.domesurvival.airlock_panel.gate_missing")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        AirlockGateBlock.ToggleResult result = gate.requestToggle(level, gateMasterPos);
        Component message = switch (result) {
            case OPENING -> Component.translatable("message.domesurvival.airlock_panel.opening")
                    .withStyle(ChatFormatting.GREEN);
            case CLOSING -> Component.translatable("message.domesurvival.airlock_panel.closing")
                    .withStyle(ChatFormatting.GREEN);
            case MOVING -> Component.translatable("message.domesurvival.airlock_panel.moving")
                    .withStyle(ChatFormatting.YELLOW);
            case INTERLOCK_BLOCKED -> Component.translatable(
                            "message.domesurvival.airlock_panel.interlock_blocked"
                    )
                    .withStyle(ChatFormatting.RED);
            case INTERLOCK_INVALID -> Component.translatable(
                            "message.domesurvival.airlock_panel.interlock_invalid"
                    )
                    .withStyle(ChatFormatting.RED);
            case INVALID -> Component.translatable("message.domesurvival.airlock_panel.gate_missing")
                    .withStyle(ChatFormatting.RED);
        };
        player.displayClientMessage(message, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> Shapes.block();
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }
}
