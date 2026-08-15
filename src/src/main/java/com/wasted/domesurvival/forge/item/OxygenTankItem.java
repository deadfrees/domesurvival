package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Portable oxygen tank equipped through Curios in the shared "back" slot.
 *
 * Oxygen is still stored directly on the ItemStack NBT, so moving the tank between
 * inventory and the back slot preserves its current oxygen amount.
 */
public final class OxygenTankItem extends Item implements ICurioItem {
    public static final String CURIO_SLOT = "back";

    private static final String OXYGEN_KEY = "domesurvival_oxygen";
    private final int capacity;

    public OxygenTankItem(int capacity, Properties properties) {
        super(properties);
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
        stack.getOrCreateTag().putInt(OXYGEN_KEY, Math.max(0, Math.min(capacity, oxygen)));
    }

    public boolean consume(ItemStack stack, int amount) {
        int current = getOxygen(stack);
        if (current <= 0 || amount <= 0) {
            return false;
        }
        setOxygen(stack, current - amount);
        return true;
    }

    /**
     * Crafted tanks start empty so upgrading a tank cannot generate free oxygen.
     * Legacy/creative stacks without the oxygen NBT key still keep the old full-tank fallback.
     */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (!stack.getOrCreateTag().contains(OXYGEN_KEY)) {
            setOxygen(stack, 0);
        }
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return CURIO_SLOT.equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return CURIO_SLOT.equals(slotContext.identifier());
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
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_tank.amount", oxygen, capacity)
                .withStyle(oxygen > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_tank.time", oxygen)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_tank.requires_mask")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
