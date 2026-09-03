package com.wasted.domesurvival.forge.machine.energy;

import com.wasted.domesurvival.forge.enchantment.ModEnchantments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

/**
 * Shared, dependency-free capacity rules for enchantable energy buffers.
 */
public final class EnergyBufferCapacity {
    public static final int MAX_LEVEL = 4;
    public static final String NBT_LEVEL = "CapacityEnchantLevel";

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

    public static int applyMultiplier(int baseCapacity, int level) {
        return apply(baseCapacity, level);
    }

    public static int getLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        int itemLevel = clampLevel(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CAPACITY.get(), stack));
        if (itemLevel > 0) return itemLevel;

        CompoundTag root = stack.getTag();
        if (root != null && root.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            return readLevel(root.getCompound("BlockEntityTag"));
        }
        return 0;
    }

    public static int readLevel(CompoundTag tag) {
        if (tag == null || !tag.contains(NBT_LEVEL, Tag.TAG_ANY_NUMERIC)) return 0;
        return clampLevel(tag.getInt(NBT_LEVEL));
    }

    public static void writeLevel(CompoundTag tag, int level) {
        if (tag == null) return;
        int safeLevel = clampLevel(level);
        if (safeLevel > 0) {
            tag.putInt(NBT_LEVEL, safeLevel);
        } else {
            tag.remove(NBT_LEVEL);
        }
    }

    public static void applyToItem(ItemStack stack, int level) {
        if (stack == null || stack.isEmpty()) return;

        int safeLevel = clampLevel(level);
        if (safeLevel <= 0) return;

        Enchantment capacity = ModEnchantments.CAPACITY.get();
        if (EnchantmentHelper.getItemEnchantmentLevel(capacity, stack) >= safeLevel) return;

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        enchantments.put(capacity, safeLevel);
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }
}
