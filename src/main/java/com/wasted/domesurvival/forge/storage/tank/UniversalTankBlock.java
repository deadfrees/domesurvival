package com.wasted.domesurvival.forge.storage.tank;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One V63 reservoir cell.
 *
 * <p>Every cell is independently usable at 4,000 mB. Adjacent compatible cells
 * share one storage immediately; a complete rectangular structure with at least
 * two dimensions >= 3 switches to the seamless BER model.</p>
 */
public final class UniversalTankBlock extends BaseEntityBlock implements cofh.lib.api.block.IDismantleable {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public UniversalTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(FORMED, false)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FORMED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // V63.1 renders both standalone cells and formed structures through the
        // same BER geometry, so the single-cell world model matches the multiblock.
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UniversalTankBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UniversalTankBlockEntity tank) {
                NetworkHooks.openScreen(serverPlayer, tank, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            Player player = placer instanceof Player p ? p : null;
            UniversalTankStructure.tryMergeAround(serverLevel, pos, player);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof UniversalTankBlockEntity tank) {
            tank.markMasterOxygenPortCacheDirty();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            UniversalTankStructure.beforeRemove(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Thermal's WrenchItem uses the IDismantleable clone-stack path.
     * Only Shift+RMB with DomeSurvival's Engineer's Wrench receives portable
     * BlockEntity data. Normal mining / pick-block keeps returning a plain cell.
     */
    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            net.minecraft.world.phys.HitResult target,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            Player player
    ) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);

        if (!stack.isEmpty() && isEngineerWrenchDismantle(player)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UniversalTankBlockEntity tank) {
                tank.prepareEngineerWrenchClone(stack);
            }
        }

        return stack;
    }

    private static boolean isEngineerWrenchDismantle(@Nullable Player player) {
        if (player == null || !player.isShiftKeyDown()) return false;
        return isEngineerWrench(player.getMainHandItem())
                || isEngineerWrench(player.getOffhandItem());
    }

    private static boolean isEngineerWrench(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null
                && DomeSurvival.MOD_ID.equals(id.getNamespace())
                && "machine_wrench".equals(id.getPath());
    }

    /**
     * Normal player mining is handled explicitly by UniversalTankEvents.
     * Returning an empty list here prevents the vanilla loot pass from creating
     * a second cell after the event has already spawned the guaranteed drop.
     *
     * Engineer-wrench dismantling remains a separate clone-stack path and can
     * preserve portable contents.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

}
