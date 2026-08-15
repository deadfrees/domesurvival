package com.wasted.domesurvival.forge.transport.energy;

public enum EnergyPipeTier {
    BASIC(256),
    REINFORCED(1_024),
    HIGH_VOLTAGE(4_096);

    private final int transferPerTick;

    EnergyPipeTier(int transferPerTick) {
        this.transferPerTick = transferPerTick;
    }

    public int transferPerTick() {
        return transferPerTick;
    }
}
