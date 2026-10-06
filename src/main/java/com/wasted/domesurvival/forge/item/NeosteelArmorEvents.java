package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Server-side FE functions for the four Neosteel armor pieces. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NeosteelArmorEvents {
    private static final int FE_PER_PREVENTED_FALL_DAMAGE = 1_800;
    private static final float MAX_FALL_REDUCTION = 0.30F;

    private static final int FE_PER_PREVENTED_PROJECTILE_DAMAGE = 1_600;
    private static final float PROJECTILE_REDUCTION = 0.12F;

    private static final int FE_PER_PREVENTED_CHEST_DAMAGE = 2_200;
    private static final float CHEST_REDUCTION = 0.10F;

    private static final int LEGGINGS_SPRINT_FE_PER_SECOND = 800;
    private static final UUID LEGGINGS_SPEED_UUID =
            UUID.fromString("9f84a34f-1cff-4cf4-8ae8-991268b69c50");
    private static final AttributeModifier LEGGINGS_SPEED = new AttributeModifier(
            LEGGINGS_SPEED_UUID,
            "Neosteel servo sprint assist",
            0.08D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
    );

    private NeosteelArmorEvents() { }

    /** Helmet: projectile deflection. Chestplate: powered combat/explosion shield. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F) return;

        float amount = event.getAmount();
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            ItemStack helmet = event.getEntity().getItemBySlot(EquipmentSlot.HEAD);
            if (isPiece(helmet, ArmorItem.Type.HELMET)) {
                amount = reduceDamageWithEnergy(
                        helmet, amount, PROJECTILE_REDUCTION, FE_PER_PREVENTED_PROJECTILE_DAMAGE);
            }
        }

        // Restrict the chest shield to combat/explosions. Surface hazards call generic/onFire
        // without an attacker, so the environmental module remains the only route to suit-like immunity.
        boolean combatOrExplosion = !event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && (event.getSource().getEntity() != null
                || event.getSource().is(DamageTypeTags.IS_EXPLOSION));
        if (combatOrExplosion && amount > 0.0F) {
            ItemStack chest = event.getEntity().getItemBySlot(EquipmentSlot.CHEST);
            if (isPiece(chest, ArmorItem.Type.CHESTPLATE)) {
                amount = reduceDamageWithEnergy(
                        chest, amount, CHEST_REDUCTION, FE_PER_PREVENTED_CHEST_DAMAGE);
            }
        }

        event.setAmount(Math.max(0.0F, amount));
    }

    /** Leggings: 8% servo sprint assist while powered, paid once per second. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        boolean poweredLeggings = isPiece(legs, ArmorItem.Type.LEGGINGS)
                && NeosteelArmorItem.getEnergy(legs) > 0;
        boolean shouldAssist = poweredLeggings && player.isSprinting();

        AttributeModifier current = speed.getModifier(LEGGINGS_SPEED_UUID);
        if (shouldAssist) {
            if (current == null) speed.addTransientModifier(LEGGINGS_SPEED);

            if (player.tickCount % 20 == 0) {
                int paid = NeosteelArmorItem.consumeEnergy(
                        legs, LEGGINGS_SPRINT_FE_PER_SECOND, false);
                if (paid < LEGGINGS_SPRINT_FE_PER_SECOND) {
                    speed.removeModifier(LEGGINGS_SPEED_UUID);
                }
            }
        } else if (current != null) {
            speed.removeModifier(LEGGINGS_SPEED_UUID);
        }
    }

    /** Boots: fall dampers, retained from V1. */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().level().isClientSide) return;

        ItemStack boots = event.getEntity().getItemBySlot(EquipmentSlot.FEET);
        if (!isPiece(boots, ArmorItem.Type.BOOTS)) return;

        float originalDamage = Math.max(0.0F,
                (event.getDistance() - 3.0F) * event.getDamageMultiplier());
        if (originalDamage <= 0.0F) return;

        int desiredReduction = Math.max(1, (int) Math.ceil(originalDamage * MAX_FALL_REDUCTION));
        int desiredEnergy = desiredReduction * FE_PER_PREVENTED_FALL_DAMAGE;
        int availableEnergy = NeosteelArmorItem.consumeEnergy(boots, desiredEnergy, true);
        int affordableReduction = Math.min(
                desiredReduction, availableEnergy / FE_PER_PREVENTED_FALL_DAMAGE);
        if (affordableReduction <= 0) return;

        int consumed = NeosteelArmorItem.consumeEnergy(
                boots,
                affordableReduction * FE_PER_PREVENTED_FALL_DAMAGE,
                false
        );
        int actualReduction = consumed / FE_PER_PREVENTED_FALL_DAMAGE;
        if (actualReduction <= 0) return;

        float remainingDamage = Math.max(0.0F, originalDamage - actualReduction);
        float factor = remainingDamage / originalDamage;
        event.setDamageMultiplier(event.getDamageMultiplier() * factor);
    }

    private static boolean isPiece(ItemStack stack, ArmorItem.Type expected) {
        return stack.getItem() instanceof NeosteelArmorItem armor
                && armor.armorType() == expected;
    }

    private static float reduceDamageWithEnergy(ItemStack stack, float incoming,
                                                float fraction, int fePerDamagePoint) {
        if (incoming <= 0.0F || fraction <= 0.0F || fePerDamagePoint <= 0) return incoming;

        float wantedReduction = incoming * Math.min(1.0F, fraction);
        int wantedEnergy = Math.max(1, (int) Math.ceil(wantedReduction * fePerDamagePoint));
        int available = NeosteelArmorItem.consumeEnergy(stack, wantedEnergy, true);
        if (available <= 0) return incoming;

        int consumed = NeosteelArmorItem.consumeEnergy(stack, Math.min(wantedEnergy, available), false);
        float actualReduction = Math.min(wantedReduction, consumed / (float) fePerDamagePoint);
        return Math.max(0.0F, incoming - actualReduction);
    }
}
