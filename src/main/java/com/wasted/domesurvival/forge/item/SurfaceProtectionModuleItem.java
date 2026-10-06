package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Pure environmental-protection upgrade for one Neosteel armor piece. */
public final class SurfaceProtectionModuleItem extends Item {
    public SurfaceProtectionModuleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.domesurvival.surface_protection_module.install")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.domesurvival.surface_protection_module.full_set")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.surface_protection_module.environment_only")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
