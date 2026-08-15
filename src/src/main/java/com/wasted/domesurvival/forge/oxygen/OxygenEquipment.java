package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenMaskItem;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Optional;

/**
 * Focused equipment lookup for the oxygen system.
 *
 * Both life-support items live in Curios:
 * - mask = dedicated "oxygen_mask" slot;
 * - portable tank = shared preset "back" slot.
 *
 * Legacy vanilla armor slots are checked only as migration fallbacks for existing worlds.
 */
public final class OxygenEquipment {
    private OxygenEquipment() {
    }

    public static boolean hasMask(ServerPlayer player) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player).resolve();
        if (curios.isPresent() && hasMaskInCurios(curios.get())) {
            return true;
        }

        // Existing saves may still contain the pre-Curios mask in vanilla HEAD until login
        // migration succeeds.
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK.get());
    }

    public static TankView tank(ServerPlayer player) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player).resolve();
        if (curios.isPresent()) {
            TankView equipped = tankFromBack(curios.get());
            if (equipped != null) {
                return equipped;
            }
        }

        // Existing saves may still contain the old chest-slot tank until login migration succeeds.
        return view(player.getItemBySlot(EquipmentSlot.CHEST));
    }

    /**
     * Moves the old vanilla HEAD oxygen mask into the dedicated Curios mask slot.
     * Safe to call repeatedly.
     */
    public static boolean migrateLegacyHeadMask(ServerPlayer player) {
        ItemStack legacy = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!legacy.is(ModItems.OXYGEN_MASK.get())) {
            return false;
        }

        Optional<ICuriosItemHandler> curiosOptional = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosOptional.isEmpty()) {
            return false;
        }

        ICuriosItemHandler curios = curiosOptional.get();
        Optional<ICurioStacksHandler> maskOptional =
                curios.getStacksHandler(OxygenMaskItem.CURIO_SLOT);
        if (maskOptional.isEmpty()) {
            return false;
        }

        ICurioStacksHandler mask = maskOptional.get();
        int slots = mask.getSlots();
        for (int index = 0; index < slots; index++) {
            if (!mask.getStacks().getStackInSlot(index).isEmpty()) {
                continue;
            }

            curios.setEquippedCurio(OxygenMaskItem.CURIO_SLOT, index, legacy.copy());
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            return true;
        }

        return false;
    }

    /**
     * Moves the old V3.2 chest-slot tank into the first free Curios back slot.
     * Safe to call repeatedly.
     */
    public static boolean migrateLegacyChestTank(ServerPlayer player) {
        ItemStack legacy = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(legacy.getItem() instanceof OxygenTankItem)) {
            return false;
        }

        Optional<ICuriosItemHandler> curiosOptional = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosOptional.isEmpty()) {
            return false;
        }

        ICuriosItemHandler curios = curiosOptional.get();
        Optional<ICurioStacksHandler> backOptional =
                curios.getStacksHandler(OxygenTankItem.CURIO_SLOT);
        if (backOptional.isEmpty()) {
            return false;
        }

        ICurioStacksHandler back = backOptional.get();
        int slots = back.getSlots();
        for (int index = 0; index < slots; index++) {
            if (!back.getStacks().getStackInSlot(index).isEmpty()) {
                continue;
            }

            curios.setEquippedCurio(OxygenTankItem.CURIO_SLOT, index, legacy.copy());
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            return true;
        }

        return false;
    }

    public static boolean tankEquipmentReady(ServerPlayer player, TankView tank) {
        return tank != null && hasMask(player);
    }

    private static boolean hasMaskInCurios(ICuriosItemHandler curios) {
        Optional<ICurioStacksHandler> maskOptional =
                curios.getStacksHandler(OxygenMaskItem.CURIO_SLOT);
        if (maskOptional.isEmpty()) {
            return false;
        }

        ICurioStacksHandler mask = maskOptional.get();
        int slots = mask.getSlots();
        for (int index = 0; index < slots; index++) {
            if (mask.getStacks().getStackInSlot(index).is(ModItems.OXYGEN_MASK.get())) {
                return true;
            }
        }
        return false;
    }

    private static TankView tankFromBack(ICuriosItemHandler curios) {
        Optional<ICurioStacksHandler> backOptional =
                curios.getStacksHandler(OxygenTankItem.CURIO_SLOT);
        if (backOptional.isEmpty()) {
            return null;
        }

        ICurioStacksHandler back = backOptional.get();
        int slots = back.getSlots();
        for (int index = 0; index < slots; index++) {
            TankView tank = view(back.getStacks().getStackInSlot(index));
            if (tank != null) {
                return tank;
            }
        }
        return null;
    }

    private static TankView view(ItemStack stack) {
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
