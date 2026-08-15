package com.wasted.domesurvival.forge.hopper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TieredHopperBlockItem extends BlockItem {
    private final HopperTier tier;

    public TieredHopperBlockItem(TieredHopperBlock block, Properties properties, HopperTier tier) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(
                Component.translatable(
                        "item.domesurvival.tiered_hopper.capacity",
                        tier.slots()
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}
