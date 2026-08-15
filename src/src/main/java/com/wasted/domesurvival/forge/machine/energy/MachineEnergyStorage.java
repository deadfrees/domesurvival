package com.wasted.domesurvival.forge.machine.energy;

import net.minecraftforge.energy.EnergyStorage;

/**
 * Small extension used by DomeSurvival machines for controlled internal generation
 * and NBT restoration while keeping normal Forge Energy receive/extract limits.
 */
public final class MachineEnergyStorage extends EnergyStorage {
    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public int addEnergyInternal(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int accepted = Math.min(capacity - energy, amount);
        energy += accepted;
        return accepted;
    }

    public int removeEnergyInternal(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int removed = Math.min(energy, amount);
        energy -= removed;
        return removed;
    }

    public void setEnergyStoredInternal(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }
}
