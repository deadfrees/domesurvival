package com.wasted.domesurvival.forge.machine.energy;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/** Shared one-slot FE charger logic for all DomeSurvival energy buffers. */
public final class EnergyItemCharging {
    private EnergyItemCharging() { }

    public static ItemStackHandler createInventory(Runnable changed) {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot == 0 && isChargeable(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                changed.run();
            }
        };
    }

    public static boolean isChargeable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        IEnergyStorage energy = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        return energy != null
                && energy.canReceive()
                && energy.getEnergyStored() < energy.getMaxEnergyStored();
    }

    public static int chargeFromBuffer(ItemStackHandler inventory, MachineEnergyStorage source, int budget) {
        if (budget <= 0 || source.getEnergyStored() <= 0) return 0;
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return 0;

        IEnergyStorage target = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (target == null || !target.canReceive()) return 0;

        int available = source.extractEnergy(budget, true);
        if (available <= 0) return 0;
        int accepted = Math.min(available, target.receiveEnergy(available, true));
        if (accepted <= 0) return 0;

        int extracted = source.extractEnergy(accepted, false);
        if (extracted <= 0) return 0;
        int actual = Math.min(extracted, target.receiveEnergy(extracted, false));
        if (actual < extracted) {
            source.addEnergyInternal(extracted - actual);
        }
        return actual;
    }

    public static int chargeCreative(ItemStackHandler inventory, int budget) {
        if (budget <= 0) return 0;
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return 0;

        IEnergyStorage target = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (target == null || !target.canReceive()) return 0;
        int accepted = target.receiveEnergy(budget, true);
        return accepted <= 0 ? 0 : target.receiveEnergy(accepted, false);
    }
}
