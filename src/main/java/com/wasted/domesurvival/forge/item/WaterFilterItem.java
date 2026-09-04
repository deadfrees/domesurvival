package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.machine.filter.FilterRegenerationBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Damageable purifier cartridge with its own processing speed and FE/t requirement.
 * Total energy per purification cycle is intentionally kept close to 3000 FE on all tiers.
 */
public final class WaterFilterItem extends Item {
    private final int processTicks;
    private final int energyPerTick;

    public WaterFilterItem(int processTicks, int energyPerTick, Properties properties) {
        super(properties);
        this.processTicks = Math.max(1, processTicks);
        this.energyPerTick = Math.max(1, energyPerTick);
    }

    public int processTicks() {
        return processTicks;
    }

    public int energyPerTick() {
        return energyPerTick;
    }

    public int cyclesRemaining(ItemStack stack) {
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int regenerations = FilterRegenerationBlockEntity.getRegenerationCycles(stack);
        tooltip.add(Component.translatable(
                "tooltip.domesurvival.water_filter.cycles", cyclesRemaining(stack), stack.getMaxDamage()
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.domesurvival.water_filter.process_ticks", processTicks
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.domesurvival.water_filter.energy", energyPerTick
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Регенерации: " + regenerations + " / "
                + FilterRegenerationBlockEntity.MAX_REGENERATION_CYCLES).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
