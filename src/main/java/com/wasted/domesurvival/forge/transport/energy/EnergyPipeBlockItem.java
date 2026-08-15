package com.wasted.domesurvival.forge.transport.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class EnergyPipeBlockItem extends BlockItem {
    private final int transferPerTick;

    public EnergyPipeBlockItem(net.minecraft.world.level.block.Block block,
                               Properties properties,
                               int transferPerTick) {
        super(block, properties);
        this.transferPerTick = transferPerTick;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String formatted = NumberFormat.getIntegerInstance(Locale.US)
                .format(transferPerTick)
                .replace(',', ' ');

        tooltip.add(Component.translatable(
                "item.domesurvival.energy_pipe.transfer",
                formatted
        ).withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable(
                "item.domesurvival.energy_pipe.configure"
        ).withStyle(ChatFormatting.DARK_GRAY));
    }
}
