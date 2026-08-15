package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Oxygen mask equipped through a dedicated Curios slot.
 *
 * The mask is life-support equipment only. It deliberately does not occupy the vanilla
 * HEAD armor slot, leaving that slot available for the surface suit helmet.
 */
public final class OxygenMaskItem extends Item implements ICurioItem {
    public static final String CURIO_SLOT = "oxygen_mask";

    public OxygenMaskItem(Properties properties) {
        super(properties);
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
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_mask.requires_tank")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_mask.no_protection")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
