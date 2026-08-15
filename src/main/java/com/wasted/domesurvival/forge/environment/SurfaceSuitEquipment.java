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
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SURFACE_SUIT_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SURFACE_SUIT_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SURFACE_SUIT_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SURFACE_SUIT_BOOTS.get());
    }
}
