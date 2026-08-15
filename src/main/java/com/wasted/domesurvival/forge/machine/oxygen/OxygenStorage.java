package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.capability.IOxygenStorage;

/** Small allocation-free oxygen buffer for machine block entities. */
public final class OxygenStorage implements IOxygenStorage {
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;
    private int oxygen;

    public OxygenStorage(int capacity, int maxReceive, int maxExtract) {
        this.capacity = Math.max(0, capacity);
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
    }

    @Override
    public int receiveOxygen(int maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0) return 0;
        int accepted = Math.min(this.maxReceive, Math.min(maxReceive, capacity - oxygen));
        if (!simulate) oxygen += accepted;
        return accepted;
    }

    @Override
    public int extractOxygen(int maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) return 0;
        int extracted = Math.min(this.maxExtract, Math.min(maxExtract, oxygen));
        if (!simulate) oxygen -= extracted;
        return extracted;
    }

    public int addInternal(int amount) {
        if (amount <= 0) return 0;
        int accepted = Math.min(amount, capacity - oxygen);
        oxygen += accepted;
        return accepted;
    }

    public int removeInternal(int amount) {
        if (amount <= 0) return 0;
        int removed = Math.min(amount, oxygen);
        oxygen -= removed;
        return removed;
    }

    public void setStoredInternal(int amount) {
        oxygen = Math.max(0, Math.min(capacity, amount));
    }

    @Override public int getOxygenStored() { return oxygen; }
    @Override public int getMaxOxygenStored() { return capacity; }
    @Override public boolean canReceive() { return maxReceive > 0; }
    @Override public boolean canExtract() { return maxExtract > 0; }
}
