package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SieveMeshItem extends Item {
    private final Tier tier;

    public SieveMeshItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.domesurvival.sieve_mesh.durability",
                stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.sieve_mesh." + tier.name().toLowerCase())
                .withStyle(tier == Tier.FIBER ? ChatFormatting.WHITE
                        : tier == Tier.COPPER ? ChatFormatting.GOLD : ChatFormatting.AQUA));
    }

    public enum Tier {
        FIBER(0xD8C7A0),
        COPPER(0xC87548),
        STEEL(0x91A3AE);

        private final int color;

        Tier(int color) {
            this.color = color;
        }

        public int color() {
            return color;
        }
    }
}
