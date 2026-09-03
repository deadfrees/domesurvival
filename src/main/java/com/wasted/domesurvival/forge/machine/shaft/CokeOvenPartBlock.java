package com.wasted.domesurvival.forge.machine.shaft;

import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** Invisible linked cells of the coke oven; only the two side port cells render an overlay. */
public final class CokeOvenPartBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty LOCAL_X = IntegerProperty.create("local_x", 0, 2);
    public static final IntegerProperty LOCAL_Y = IntegerProperty.create("local_y", 0, 2);
    public static final IntegerProperty LOCAL_Z = IntegerProperty.create("local_z", 0, 2);

    public CokeOvenPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LOCAL_X, 1)
                .setValue(LOCAL_Y, 0)
                .setValue(LOCAL_Z, 1));
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, LOCAL_X, LOCAL_Y, LOCAL_Z);
    }

    public static boolean isInputPort(BlockState state) {
        return state.getValue(LOCAL_X) == 0 && state.getValue(LOCAL_Y) == 1 && state.getValue(LOCAL_Z) == 1;
    }

    public static boolean isOutputPort(BlockState state) {
        return state.getValue(LOCAL_X) == 2 && state.getValue(LOCAL_Y) == 1 && state.getValue(LOCAL_Z) == 1;
    }

    public static Direction portSide(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (isInputPort(state)) return facing.getCounterClockWise();
        if (isOutputPort(state)) return facing.getClockWise();
        return Direction.UP;
    }

    public static BlockPos controllerPosition(BlockPos partPos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int x = state.getValue(LOCAL_X) - 1;
        int y = state.getValue(LOCAL_Y);
        int z = state.getValue(LOCAL_Z) - 1;
        return partPos.relative(facing.getClockWise(), -x).relative(facing, z).below(y);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos controllerPos = controllerPosition(pos, state);
            BlockEntity blockEntity = level.getBlockEntity(controllerPos);
            if (blockEntity instanceof CokeOvenBlockEntity oven) NetworkHooks.openScreen(serverPlayer, oven, controllerPos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CokeOvenPartBlockEntity(pos, state);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
