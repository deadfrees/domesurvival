package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Constant-time equipment lookup.
 *
 * No inventory iteration: mask = HEAD, portable tank = CHEST.
 */
public final class OxygenEquipment {
    private OxygenEquipment() {
    }

    public static boolean hasMask(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK.get());
    }

    public static TankView tank(ServerPlayer player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(stack.getItem() instanceof OxygenTankItem tankItem)) {
            return null;
        }
        return new TankView(
                stack,
                tankItem,
                tankItem.getOxygen(stack),
                tankItem.capacity()
        );
    }

    public static boolean tankEquipmentReady(ServerPlayer player, TankView tank) {
        return hasMask(player) && tank != null;
    }

    public record TankView(
            ItemStack stack,
            OxygenTankItem item,
            int oxygen,
            int capacity
    ) {
        public void setOxygen(int newOxygen) {
            item.setOxygen(stack, newOxygen);
        }
    }
}
