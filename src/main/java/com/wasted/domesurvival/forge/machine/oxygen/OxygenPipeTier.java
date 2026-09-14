package com.wasted.domesurvival.forge.machine.oxygen;

/**
 * Throughput and physical width for the three DomeSurvival oxygen pipe tiers.
 */
public enum OxygenPipeTier {
    BASIC(30, 6.75D, 9.25D),
    REINFORCED(60, 6.75D, 9.25D),
    HIGH_FLOW(120, 6.75D, 9.25D);

    private final int transferRate;
    private final double min;
    private final double max;

    OxygenPipeTier(int transferRate, double min, double max) {
        this.transferRate = transferRate;
        this.min = min;
        this.max = max;
    }

    public int transferRate() {
        return transferRate;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }
}
