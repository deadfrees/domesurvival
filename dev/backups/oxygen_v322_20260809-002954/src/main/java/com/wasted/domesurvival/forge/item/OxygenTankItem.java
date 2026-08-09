package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Chest-slot portable oxygen module.
 *
 * Oxygen is persisted directly in the ItemStack NBT, so every physical tank
 * carries its own independent resource state and survives logout/restart normally.
 */
public final class OxygenTankItem extends ArmorItem {
    private static final String OXYGEN_KEY = "domesurvival_oxygen";

    private final int capacity;

    public OxygenTankItem(int capacity, Properties properties) {
        super(OxygenEquipmentMaterial.INSTANCE, Type.CHESTPLATE, properties);
        this.capacity = Math.max(1, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public int getOxygen(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(OXYGEN_KEY)) {
            return capacity;
        }
        return Math.max(0, Math.min(capacity, tag.getInt(OXYGEN_KEY)));
    }

    public void setOxygen(ItemStack stack, int oxygen) {
        stack.getOrCreateTag().putInt(
                OXYGEN_KEY,
                Math.max(0, Math.min(capacity, oxygen))
        );
    }

    public boolean consume(ItemStack stack, int amount) {
        int current = getOxygen(stack);
        if (current <= 0 || amount <= 0) {
            return false;
        }
        setOxygen(stack, current - amount);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getOxygen(stack) < capacity;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getOxygen(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55D8FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int oxygen = getOxygen(stack);
        tooltip.add(Component.translatable(
                        "tooltip.domesurvival.oxygen_tank.amount",
                        oxygen,
                        capacity
                ).withStyle(oxygen > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable(
                        "tooltip.domesurvival.oxygen_tank.time",
                        oxygen
                ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_tank.requires_mask")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
