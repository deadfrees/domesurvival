package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.pipe.PipeWrenchConnectionService;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lightweight oxygen conduit. The pipe itself has no BlockEntity and stores no oxygen;
 * oxygen consumers discover extract-capable endpoints through connected pipe blocks.
 * This avoids one ticking BlockEntity per pipe segment.
 */
public final class OxygenPipeBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final ResourceLocation MACHINE_WRENCH_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "machine_wrench");

    private final OxygenPipeTier tier;
    private final VoxelShape[] cachedShapes = new VoxelShape[64];

    public OxygenPipeBlock(Properties properties, OxygenPipeTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
        buildShapeCache();
    }

    public int getTransferRate() {
        return tier.transferRate();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_pipe.medium")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_pipe.rate", getTransferRate())
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return refreshConnections(level, pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(property(direction), canConnect(level, pos, direction));
    }

    public static BlockState refreshConnections(LevelAccessor level, BlockPos pipePos, BlockState state) {
        BlockState result = state;
        for (Direction direction : Direction.values()) {
            result = result.setValue(property(direction), canConnect(level, pipePos, direction));
        }
        return result;
    }

    private static boolean canConnect(LevelAccessor level, BlockPos pipePos, Direction directionFromPipe) {
        if (OxygenPipeConnectionData.isDisconnected(level, pipePos, directionFromPipe)) return false;

        BlockPos neighborPos = pipePos.relative(directionFromPipe);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof OxygenPipeBlock) {
            return !OxygenPipeConnectionData.isDisconnected(
                    level, neighborPos, directionFromPipe.getOpposite());
        }

        BlockEntity blockEntity = level.getBlockEntity(neighborPos);
        if (blockEntity == null) {
            return false;
        }

        // Ask the neighbor on the face that points back toward this pipe. This makes
        // configurable machine faces visually disconnect when the oxygen capability is disabled.
        return blockEntity.getCapability(ModCapabilities.OXYGEN, directionFromPipe.getOpposite()).isPresent();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!MACHINE_WRENCH_ID.equals(ForgeRegistries.ITEMS.getKey(held.getItem()))) {
            return InteractionResult.PASS;
        }
        Direction pipeSide = PipeWrenchConnectionService.findSameFamilySide(level, pos, hit);
        if (pipeSide == null) return InteractionResult.PASS;
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            PipeWrenchConnectionService.toggle(serverLevel, pos, pipeSide, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            OxygenPipeConnectionData.get(serverLevel).clear(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return cachedShapes[toMask(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return cachedShapes[toMask(state)];
    }

    private void buildShapeCache() {
        double min = tier.min();
        double max = tier.max();
        VoxelShape core = Block.box(min, min, min, max, max, max);
        VoxelShape north = Block.box(min, min, 0.0D, max, max, 8.0D);
        VoxelShape east = Block.box(8.0D, min, min, 16.0D, max, max);
        VoxelShape south = Block.box(min, min, 8.0D, max, max, 16.0D);
        VoxelShape west = Block.box(0.0D, min, min, 8.0D, max, max);
        VoxelShape up = Block.box(min, 8.0D, min, max, 16.0D, max);
        VoxelShape down = Block.box(min, 0.0D, min, max, 8.0D, max);

        for (int mask = 0; mask < cachedShapes.length; mask++) {
            VoxelShape shape = core;
            if ((mask & 1) != 0) shape = Shapes.or(shape, north);
            if ((mask & 2) != 0) shape = Shapes.or(shape, east);
            if ((mask & 4) != 0) shape = Shapes.or(shape, south);
            if ((mask & 8) != 0) shape = Shapes.or(shape, west);
            if ((mask & 16) != 0) shape = Shapes.or(shape, up);
            if ((mask & 32) != 0) shape = Shapes.or(shape, down);
            cachedShapes[mask] = shape.optimize();
        }
    }

    private static int toMask(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 1;
        if (state.getValue(EAST)) mask |= 2;
        if (state.getValue(SOUTH)) mask |= 4;
        if (state.getValue(WEST)) mask |= 8;
        if (state.getValue(UP)) mask |= 16;
        if (state.getValue(DOWN)) mask |= 32;
        return mask;
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }
}
