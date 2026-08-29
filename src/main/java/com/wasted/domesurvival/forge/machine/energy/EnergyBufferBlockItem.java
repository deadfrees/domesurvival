package com.wasted.domesurvival.forge.machine.energy;

import com.wasted.domesurvival.forge.enchantment.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * BlockItem shared by the three survival energy-buffer tiers.
 * It is deliberately independent from Thermal/CoFH enchanting code.
 */
public final class EnergyBufferBlockItem extends BlockItem {
    private static final int ENCHANTMENT_VALUE = 10;

    public EnergyBufferBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public int getEnchantmentValue() {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1 && !stack.isEnchanted();
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == ModEnchantments.CAPACITY.get();
    }

    /**
     * Vanilla calls this after the BlockEntity exists. The super call first
     * restores any preserved BlockEntityTag; the mixin makes sure Capacity is
     * applied before stored Energy is read from that tag. Then a freshly
     * enchanted item can supply its enchantment level directly.
     */
    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player,
                                                  ItemStack stack, BlockState state) {
        boolean changed = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (level.isClientSide) return changed;

        int levelFromItem = EnergyBufferCapacity.getLevel(stack);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CapacityEnchantedEnergyBuffer capacityBuffer) {
            int before = capacityBuffer.getCapacityEnchantLevel();
            capacityBuffer.setCapacityEnchantLevel(levelFromItem);
            return changed || before != capacityBuffer.getCapacityEnchantLevel();
        }
        return changed;
    }
}
