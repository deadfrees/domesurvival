package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Inventory/JEI tooltip for the Energy Block family.
 *
 * This deliberately replaces the old in-world crosshair overlay. The current
 * value is read from BlockEntityTag when an item stack carries preserved block
 * data; otherwise a normal block item starts at 0 FE. The creative tier is
 * always displayed as infinite.
 */
@Mod.EventBusSubscriber(modid = "domesurvival", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnergyBufferItemTooltip {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private EnergyBufferItemTooltip() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        Block block = blockItem.getBlock();
        if (block == ModBlocks.ENERGY_BUFFER_CREATIVE.get()) {
            event.getToolTip().add(
                    Component.translatable("gui.domesurvival.energy_hover.infinite")
                            .withStyle(ChatFormatting.GRAY)
            );
            return;
        }

        int capacity;
        if (block == ModBlocks.ENERGY_BUFFER.get()) {
            capacity = EnergyBufferBlockEntity.ENERGY_CAPACITY;
        } else if (block == ModBlocks.ENERGY_BUFFER_TITAN.get()) {
            capacity = TitanEnergyBufferBlockEntity.ENERGY_CAPACITY;
        } else if (block == ModBlocks.ENERGY_BUFFER_ADAMANTIUM.get()) {
            capacity = AdamantiumEnergyBufferBlockEntity.ENERGY_CAPACITY;
        } else {
            return;
        }

        int stored = readStoredEnergy(stack, capacity);
        event.getToolTip().add(
                Component.translatable(
                                "gui.domesurvival.energy_hover",
                                formatEnergy(stored),
                                formatEnergy(capacity)
                        )
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    private static int readStoredEnergy(ItemStack stack, int capacity) {
        CompoundTag root = stack.getTag();
        if (root == null) return 0;

        // Standard BlockItem location used when block entity data is preserved.
        if (root.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag blockEntityTag = root.getCompound("BlockEntityTag");
            if (blockEntityTag.contains("Energy", Tag.TAG_INT)) {
                return clampEnergy(blockEntityTag.getInt("Energy"), capacity);
            }
        }

        // Also support a direct Energy tag for forward compatibility with a
        // dedicated energy BlockItem, should one be introduced later.
        if (root.contains("Energy", Tag.TAG_INT)) {
            return clampEnergy(root.getInt("Energy"), capacity);
        }

        return 0;
    }

    private static int clampEnergy(int stored, int capacity) {
        return Math.max(0, Math.min(stored, capacity));
    }

    private static String formatEnergy(int value) {
        return NUMBER_FORMAT.format(Math.max(0, value)).replace(',', ' ');
    }
}
