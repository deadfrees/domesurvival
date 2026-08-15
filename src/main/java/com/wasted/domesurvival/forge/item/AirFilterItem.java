package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Former industrial purifier cartridge, repurposed for air filtration. */
public final class AirFilterItem extends Item {
    private final int nominalCycles;

    public AirFilterItem(int nominalCycles, Properties properties) {
        super(properties);
        this.nominalCycles = Math.max(1, nominalCycles);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int max = Math.max(1, stack.getMaxDamage());
        int used = Math.max(0, Math.min(max, stack.getDamageValue()));
        int remaining = Math.max(0, max - used);

        tooltip.add(Component.translatable("tooltip.domesurvival.air_filter.purpose").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.domesurvival.air_filter.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Ресурс: " + remaining + " / " + max).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Отработано: " + used + " / " + max).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
