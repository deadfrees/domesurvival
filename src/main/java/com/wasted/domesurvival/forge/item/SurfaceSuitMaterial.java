package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * First-pass surface suit material.
 *
 * Weather resistance is handled by SurfaceSuitEquipment/SurfaceHazardService rather than
 * armor points. Vanilla combat protection is intentionally modest so the suit does not
 * replace dedicated combat armor in the modpack.
 */
public final class SurfaceSuitMaterial implements ArmorMaterial {
    public static final SurfaceSuitMaterial INSTANCE = new SurfaceSuitMaterial();

    private SurfaceSuitMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 220;
            case CHESTPLATE -> 320;
            case LEGGINGS -> 300;
            case BOOTS -> 260;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 1;
            case CHESTPLATE -> 3;
            case LEGGINGS -> 2;
            case BOOTS -> 1;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 9;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return DomeSurvival.MOD_ID + ":surface_suit";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
