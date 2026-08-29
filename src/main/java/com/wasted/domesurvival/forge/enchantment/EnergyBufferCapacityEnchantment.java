package com.wasted.domesurvival.forge.enchantment;

import com.wasted.domesurvival.forge.block.ModBlocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class EnergyBufferCapacityEnchantment extends Enchantment {
    public EnergyBufferCapacityEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.BREAKABLE, EquipmentSlot.values());
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return isSupportedEnergyBuffer(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return isSupportedEnergyBuffer(stack);
    }

    private static boolean isSupportedEnergyBuffer(ItemStack stack) {
        return stack.is(ModBlocks.ENERGY_BUFFER.get().asItem())
                || stack.is(ModBlocks.ENERGY_BUFFER_TITAN.get().asItem())
                || stack.is(ModBlocks.ENERGY_BUFFER_ADAMANTIUM.get().asItem());
    }
}
