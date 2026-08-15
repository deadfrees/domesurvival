package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One piece of the four-piece surface protection suit.
 *
 * Uses the user-provided radiation-suit texture in its original green palette.
 * The unused Faithful64 watermark area is transparent, and the helmet face UVs
 * are cut out so the separate Curios oxygen mask can render without overlap.
 */
public final class SurfaceSuitItem extends ArmorItem {
    private static final String LAYER_1 =
            DomeSurvival.MOD_ID + ":textures/models/armor/surface_suit_layer_1.png";
    private static final String LAYER_2 =
            DomeSurvival.MOD_ID + ":textures/models/armor/surface_suit_layer_2.png";

    public SurfaceSuitItem(Type type, Properties properties) {
        super(SurfaceSuitMaterial.INSTANCE, type, properties);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return slot == EquipmentSlot.LEGS ? LAYER_2 : LAYER_1;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.domesurvival.surface_suit.weather_protection")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.domesurvival.surface_suit.oxygen_separate")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
