package com.wasted.domesurvival.forge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible linked cells covering the full 3x7x2.5 Lanos model. */
public final class LanosHitboxPartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty LOCAL_X = IntegerProperty.create("local_x", 0, 2);
    public static final IntegerProperty LOCAL_Y = IntegerProperty.create("local_y", 0, 2);
    public static final IntegerProperty LOCAL_Z = IntegerProperty.create("local_z", 0, 6);

    public LanosHitboxPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LOCAL_X, 1)
                .setValue(LOCAL_Y, 0)
                .setValue(LOCAL_Z, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LOCAL_X, LOCAL_Y, LOCAL_Z);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public static BlockPos controllerPosition(BlockPos partPos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int x = state.getValue(LOCAL_X) - 1;
        int y = state.getValue(LOCAL_Y);
        int z = state.getValue(LOCAL_Z) - 3;
        return partPos.relative(facing.getClockWise(), -x).relative(facing, -z).below(y);
    }

    private static BlockPos resolveController(Level level, BlockPos partPos, BlockState state) {
        BlockPos encoded = controllerPosition(partPos, state);
        if (isLanos(level.getBlockState(encoded))) return encoded;

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -2; y <= 0; y++) {
            for (int z = -4; z <= 4; z++) {
                for (int x = -4; x <= 4; x++) {
                    cursor.set(partPos.getX() + x, partPos.getY() + y, partPos.getZ() + z);
                    if (!isLanos(level.getBlockState(cursor))) continue;
                    double distance = cursor.distSqr(partPos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static boolean isLanos(BlockState state) {
        return state.is(ModBlocks.LANOS_DECORATIVE.get())
                || state.is(ModBlocks.LANOS_ABANDONED.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LOCAL_Y) == 2
                ? box(0, 0, 0, 16, 8, 16)
                : box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LOCAL_Y) == 2
                ? box(0, 0, 0, 16, 8, 16)
                : box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos controller = resolveController(level, pos, state);
        if (controller == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            return DecorativeLanosBlock.openTrunk(level, controller, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos controller = resolveController(level, pos, state);
            if (controller == null) {
                super.playerWillDestroy(level, pos, state, player);
                return;
            }
            BlockState controllerState = level.getBlockState(controller);
            if (controllerState.is(ModBlocks.LANOS_DECORATIVE.get())
                    || controllerState.is(ModBlocks.LANOS_ABANDONED.get())) {
                level.destroyBlock(controller, !player.isCreative(), player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
