package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Functional life-support equipment, not protective armor.
 * Protection is intentionally zero; environmental protection belongs to a later suit system.
 */
public final class OxygenEquipmentMaterial implements ArmorMaterial {
    public static final OxygenEquipmentMaterial INSTANCE = new OxygenEquipmentMaterial();

    private OxygenEquipmentMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 320;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return DomeSurvival.MOD_ID + ":oxygen_equipment";
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
