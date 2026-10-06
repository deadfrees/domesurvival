package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Portable 100 kFE Neosteel energy cell and armor crafting component. */
public final class EnergyCellItem extends Item {
    public static final int CAPACITY = 100_000;
    public static final int TRANSFER = 4_096;

    public EnergyCellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new StackEnergyProvider(stack, CAPACITY, TRANSFER, TRANSFER);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int energy = energy(stack);
        return Math.round(13.0F * energy / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x59D9FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int energy = energy(stack);
        tooltip.add(Component.translatable("tooltip.domesurvival.energy_item.charge", energy, CAPACITY)
                .withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.energy_cell.portable")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static int energy(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        return storage == null ? 0 : storage.getEnergyStored();
    }
}
