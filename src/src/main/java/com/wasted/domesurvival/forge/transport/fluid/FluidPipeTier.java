package com.wasted.domesurvival.forge.transport.fluid;

public enum FluidPipeTier {
    BASIC(128),
    REINFORCED(512),
    HIGH_PRESSURE(2_048);

    private final int transferPerTick;

    FluidPipeTier(int transferPerTick) {
        this.transferPerTick = transferPerTick;
    }

    public int transferPerTick() {
        return transferPerTick;
    }
}
