package com.wasted.domesurvival.forge.machine.oxygen;

/**
 * Throughput and physical width for the three DomeSurvival oxygen pipe tiers.
 */
public enum OxygenPipeTier {
    BASIC(120, 5.25D, 10.75D),
    REINFORCED(240, 4.50D, 11.50D),
    HIGH_FLOW(480, 3.80D, 12.20D);

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
