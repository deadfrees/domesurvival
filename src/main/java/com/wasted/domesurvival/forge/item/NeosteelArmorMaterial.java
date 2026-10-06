package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Passive fallback shell for Neosteel armor.
 *
 * <p>Charged armor points are supplied by NeosteelArmorItem's ItemStack-sensitive
 * Forge attribute hook. These deliberately weak values are what remains at 0 FE.</p>
 */
public final class NeosteelArmorMaterial implements ArmorMaterial {
    public static final NeosteelArmorMaterial INSTANCE = new NeosteelArmorMaterial();

    private static final ResourceLocation REPAIR_TAG =
            ResourceLocation.fromNamespaceAndPath("forge", "ingots/neosteel");

    private NeosteelArmorMaterial() { }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        // Kept non-zero for vanilla compatibility; NeosteelArmorItem#damageItem intercepts all wear.
        return switch (type) {
            case HELMET -> 363;
            case CHESTPLATE -> 528;
            case LEGGINGS -> 495;
            case BOOTS -> 429;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return NeosteelArmorItem.passiveDefense(type);
    }

    @Override public int getEnchantmentValue() { return 10; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ItemTags.create(REPAIR_TAG)); }
    @Override public String getName() { return DomeSurvival.MOD_ID + ":neosteel"; }
    @Override public float getToughness() { return 0.0F; }
    @Override public float getKnockbackResistance() { return 0.0F; }
}
