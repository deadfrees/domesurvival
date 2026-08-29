package com.wasted.domesurvival.forge.machine.energy;

import com.wasted.domesurvival.forge.enchantment.ModEnchantments;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

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
}
