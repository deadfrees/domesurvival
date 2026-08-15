package com.wasted.domesurvival.forge.transport.energy;

import cofh.lib.api.block.IWrenchable;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.pipe.PipeWrenchConnectionService;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class EnergyPipeBlock extends BaseEntityBlock implements IWrenchable {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final ResourceLocation MACHINE_WRENCH_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "machine_wrench");

    private static final VoxelShape CORE = box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    private static final VoxelShape ARM_NORTH = box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 5.0D);
    private static final VoxelShape ARM_SOUTH = box(5.0D, 5.0D, 11.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape ARM_WEST = box(0.0D, 5.0D, 5.0D, 5.0D, 11.0D, 11.0D);
    private static final VoxelShape ARM_EAST = box(11.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
    private static final VoxelShape ARM_DOWN = box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);
    private static final VoxelShape ARM_UP = box(5.0D, 11.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    private final EnergyPipeTier tier;

    public EnergyPipeBlock(EnergyPipeTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    public EnergyPipeTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return refreshConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), canConnect(level, currentPos, direction));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;

        BlockState refreshed = refreshConnections(level, pos, state);
        if (!refreshed.equals(state)) {
            level.setBlock(pos, refreshed, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Thermal WrenchItem calls IWrenchable.wrenchBlock from onItemUseFirst.
     * This is the primary non-sneaking configuration path.
     */
    @Override
    public void wrenchBlock(Level level, BlockPos pos, BlockState state,
                            HitResult target, Player player) {
        if (!(target instanceof BlockHitResult hit)) return;
        if (level.isClientSide) return;

        configureSide(level, pos, player, hit.getDirection(), false);
    }

    @Override
    public boolean canWrench(Level level, BlockPos pos, BlockState state, Player player) {
        return true;
    }

    /**
     * Thermal's wrench deliberately skips IWrenchable while sneaking.
     * The block interaction is therefore used only for reverse cycling.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        ResourceLocation heldId = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (!MACHINE_WRENCH_ID.equals(heldId)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Direction pipeSide = PipeWrenchConnectionService.findSameFamilySide(level, pos, hit);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && pipeSide != null) {
                PipeWrenchConnectionService.toggle(serverLevel, pos, pipeSide, player);
            } else {
                configureSide(level, pos, player, hit.getDirection(), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void configureSide(Level level, BlockPos pos, Player player,
                                      Direction side, boolean reverse) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof EnergyPipeBlockEntity pipe)) return;

        EnergyPipeSideMode mode = pipe.cycleSideMode(side, reverse);

        Component sideName = Component.translatable(
                "message.domesurvival.energy_pipe.side." + side.getName()
        );
        Component modeName = Component.translatable(mode.translationKey());

        player.displayClientMessage(
                Component.translatable(
                        "message.domesurvival.energy_pipe.side_mode",
                        sideName,
                        modeName
                ),
                true
        );
    }

    public static BlockState refreshConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockState result = state;
        for (Direction direction : Direction.values()) {
            result = result.setValue(propertyFor(direction), canConnect(level, pos, direction));
        }
        return result;
    }

    private static boolean canConnect(LevelAccessor level, BlockPos pipePos, Direction direction) {
        BlockEntity ownEntity = level.getBlockEntity(pipePos);
        if (ownEntity instanceof EnergyPipeBlockEntity ownPipe
                && !ownPipe.getSideMode(direction).isConnectionEnabled()) {
            return false;
        }

        BlockPos neighborPos = pipePos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        if (neighborState.getBlock() instanceof EnergyPipeBlock) {
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
            return !(neighborEntity instanceof EnergyPipeBlockEntity neighborPipe)
                    || neighborPipe.getSideMode(direction.getOpposite()).isConnectionEnabled();
        }

        BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
        if (neighborEntity == null) return false;

        return neighborEntity
                .getCapability(ForgeCapabilities.ENERGY, direction.getOpposite())
                .isPresent();
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, ARM_DOWN);
        if (state.getValue(UP)) shape = Shapes.or(shape, ARM_UP);
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.ENERGY_PIPE.get(),
                EnergyPipeBlockEntity::serverTick
        );
    }
}
