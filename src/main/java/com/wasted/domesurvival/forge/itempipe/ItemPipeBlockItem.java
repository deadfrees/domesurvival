package com.wasted.domesurvival.forge.itempipe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ItemPipeBlockItem extends BlockItem {
    private final ItemPipeTier tier;
    private final boolean filtering;

    public ItemPipeBlockItem(Block block, Properties properties, ItemPipeTier tier, boolean filtering) {
        super(block, properties);
        this.tier = tier;
        this.filtering = filtering;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (filtering) {
            tooltip.add(Component.translatable("item.domesurvival.item_pipe.filter_capacity", ItemPipeBlockEntity.FILTER_SLOTS)
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.domesurvival.item_pipe.filter_rule")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.translatable(
                "item.domesurvival.item_pipe.transfer",
                tier.itemsPerCycle(), tier.cooldownTicks()
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.domesurvival.item_pipe.connector_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
