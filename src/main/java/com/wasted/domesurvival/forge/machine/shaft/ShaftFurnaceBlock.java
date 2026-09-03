package com.wasted.domesurvival.forge.machine.shaft;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** Compact shaft furnace using the authored facing/lit block model. */
public final class ShaftFurnaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int RADIUS = 1;
    public static final int HEIGHT = 2;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    public ShaftFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ShaftFurnaceBlockEntity furnace) {
                NetworkHooks.openScreen(serverPlayer, furnace, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static BlockPos partPosition(BlockPos controller, Direction facing, int x, int y) {
        return controller.relative(facing.getClockWise(), x).above(y);
    }

    public static boolean isStructureComplete(Level level, BlockPos controller, BlockState controllerState) {
        return controllerState.getBlock() instanceof ShaftFurnaceBlock;
    }

    /** Removes invisible cells left by the retired 3x2x1 implementation. */
    public static void clearStructure(Level level, BlockPos controller, BlockState controllerState) {
        Direction facing = controllerState.getValue(FACING);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                if (x == 0 && y == 0) continue;
                BlockPos partPos = partPosition(controller, facing, x, y);
                if (level.getBlockState(partPos).is(ModBlocks.SHAFT_FURNACE_PART.get())) {
                    level.removeBlock(partPos, false);
                }
            }
        }
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, ModBlockEntities.SHAFT_FURNACE.get(),
                ShaftFurnaceBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ShaftFurnaceBlockEntity furnace) {
                for (int slot = 0; slot < furnace.getInventory().getSlots(); slot++) {
                    ItemStack stack = furnace.getInventory().getStackInSlot(slot);
                    if (!stack.isEmpty()) popResource(level, pos, stack.copy());
                }
            }
            clearStructure(level, pos, oldState);
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
