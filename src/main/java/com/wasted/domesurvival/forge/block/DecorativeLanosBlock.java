package com.wasted.domesurvival.forge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Static decorative vehicle block used by both corrected Lanos models.
 *
 * <p>The supplied final models are exactly 7 blocks long, 3 blocks wide and
 * 2.5 blocks high. They intentionally render outside the anchor block.
 * Collision stays disabled; the outline follows the full vehicle volume so
 * selection/removal remains convenient.</p>
 */
public final class DecorativeLanosBlock extends HorizontalDirectionalBlock {
    // North/south model bounds: X [-16, 32], Y [0, 40], Z [-48, 64].
    private static final VoxelShape NORTH_SOUTH_SHAPE =
            box(-16.0D, 0.0D, -48.0D, 32.0D, 40.0D, 64.0D);
    private static final VoxelShape EAST_WEST_SHAPE =
            box(-48.0D, 0.0D, -16.0D, 64.0D, 40.0D, 32.0D);

    public DecorativeLanosBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X
                ? EAST_WEST_SHAPE
                : NORTH_SOUTH_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
