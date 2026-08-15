package com.wasted.domesurvival.forge.block;

import com.wasted.domesurvival.forge.machine.copper.CopperFurnaceBlockEntity;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Early-game copper furnace with IC2 Iron Furnace-style speed and fuel economy. */
public final class CopperFurnaceBlock extends AbstractFurnaceBlock implements cofh.lib.api.block.IDismantleable {
    public CopperFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperFurnaceBlockEntity(pos, state);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopperFurnaceBlockEntity furnace) {
            player.openMenu(furnace);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                type,
                ModBlockEntities.COPPER_FURNACE.get(),
                CopperFurnaceBlockEntity::serverTick
        );
    }

    /**
     * CoFH/Thermal dismantle clone.
     * Thermal's own WrenchItem performs the actual dismantle; this method only
     * tells the standard clone-stack path how to preserve this machine's
     * BlockEntity data in the returned BlockItem.
     */
    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.phys.HitResult target,
            net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player) {
        net.minecraft.world.item.ItemStack stack = super.getCloneItemStack(level, pos, state);
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!stack.isEmpty() && blockEntity != null) {
            blockEntity.saveToItem(stack);
        }
        return stack;
    }
}
