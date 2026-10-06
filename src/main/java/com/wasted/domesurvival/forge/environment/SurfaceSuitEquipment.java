package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.forge.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * O(1) server-side check for the four-piece surface suit.
 *
 * Life-support curios are intentionally not part of this check: the suit blocks weather
 * damage, while OxygenService independently requires the oxygen mask + tank for breathing.
 */
public final class SurfaceSuitEquipment {
    private SurfaceSuitEquipment() {
    }

    public static boolean hasFullSuit(ServerPlayer player) {
        return hasLegacySurfaceSuit(player) || hasProtectedNeosteelSuit(player);
    }

    private static boolean hasLegacySurfaceSuit(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SURFACE_SUIT_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SURFACE_SUIT_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SURFACE_SUIT_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SURFACE_SUIT_BOOTS.get());
    }

    /**
     * Neosteel does not inherit surface-suit protection by itself. The exact existing
     * surface-hazard immunity is unlocked only when every worn Neosteel piece has its
     * own Surface Protection Module installed. FE level is intentionally irrelevant:
     * the module is a dedicated environmental shell, not an energy shield.
     */
    private static boolean hasProtectedNeosteelSuit(ServerPlayer player) {
        return isProtectedNeosteel(player.getItemBySlot(EquipmentSlot.HEAD), net.minecraft.world.item.ArmorItem.Type.HELMET)
                && isProtectedNeosteel(player.getItemBySlot(EquipmentSlot.CHEST), net.minecraft.world.item.ArmorItem.Type.CHESTPLATE)
                && isProtectedNeosteel(player.getItemBySlot(EquipmentSlot.LEGS), net.minecraft.world.item.ArmorItem.Type.LEGGINGS)
                && isProtectedNeosteel(player.getItemBySlot(EquipmentSlot.FEET), net.minecraft.world.item.ArmorItem.Type.BOOTS);
    }

    private static boolean isProtectedNeosteel(net.minecraft.world.item.ItemStack stack,
                                                net.minecraft.world.item.ArmorItem.Type expectedType) {
        return stack.getItem() instanceof com.wasted.domesurvival.forge.item.NeosteelArmorItem armor
                && armor.armorType() == expectedType
                && com.wasted.domesurvival.forge.item.NeosteelArmorItem.hasSurfaceProtectionModule(stack);
    }
}
