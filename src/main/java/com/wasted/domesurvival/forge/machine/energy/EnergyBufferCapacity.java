package com.wasted.domesurvival.forge.machine.energy;

import com.wasted.domesurvival.forge.enchantment.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Shared, dependency-free capacity rules for enchantable energy buffers.
 */
public final class EnergyBufferCapacity {
    public static final int MAX_LEVEL = 4;

    private EnergyBufferCapacity() {
    }

    public static int clampLevel(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    /**
     * Capacity progression: 100%, 150%, 200%, 250%, 300% for levels 0..4.
     * A long intermediate avoids overflow while the public FE API remains int-based.
     */
    public static int apply(int baseCapacity, int level) {
        int safeBase = Math.max(0, baseCapacity);
        int safeLevel = clampLevel(level);
        long capacity = (long) safeBase * (2L + safeLevel) / 2L;
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    public static int getLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return clampLevel(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CAPACITY.get(), stack));
    }

    public static void applyToItem(ItemStack stack, int level) {
        if (stack == null || stack.isEmpty()) return;
        int safeLevel = clampLevel(level);
        if (safeLevel > 0) {
            stack.enchant(ModEnchantments.CAPACITY.get(), safeLevel);
        }
    }
}
