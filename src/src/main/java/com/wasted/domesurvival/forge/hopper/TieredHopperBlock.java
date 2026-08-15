package com.wasted.domesurvival.forge.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class TieredHopperBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    private static final VoxelShape BOWL =
            Shapes.or(
                    box(0.0D, 10.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                    box(2.0D, 5.0D, 2.0D, 14.0D, 10.0D, 14.0D)
            );

    private static final VoxelShape DOWN =
            Shapes.or(BOWL, box(6.0D, 0.0D, 6.0D, 10.0D, 5.0D, 10.0D));
    private static final VoxelShape NORTH =
            Shapes.or(BOWL, box(6.0D, 5.0D, 0.0D, 10.0D, 9.0D, 5.0D));
    private static final VoxelShape SOUTH =
            Shapes.or(BOWL, box(6.0D, 5.0D, 11.0D, 10.0D, 9.0D, 16.0D));
    private static final VoxelShape WEST =
            Shapes.or(BOWL, box(0.0D, 5.0D, 6.0D, 5.0D, 9.0D, 10.0D));
    private static final VoxelShape EAST =
            Shapes.or(BOWL, box(11.0D, 5.0D, 6.0D, 16.0D, 9.0D, 10.0D));

    private final HopperTier tier;

    public TieredHopperBlock(HopperTier tier, Properties properties) {
        super(properties);
        this.tier = tier;

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.DOWN)
                        .setValue(ENABLED, true)
        );
    }

    public HopperTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace().getOpposite();
        Direction facing = direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction;

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        boolean shouldEnable = !level.hasNeighborSignal(pos);
        if (shouldEnable != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, shouldEnable), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, provider, pos);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof net.minecraft.world.Container container) {
                Containers.dropContents(level, pos, container);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> DOWN;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredHopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;

        return createTickerHelper(
                type,
                HopperRegistryEvents.HOPPER_BLOCK_ENTITY.get(),
                TieredHopperBlockEntity::serverTick
        );
    }
}
