package com.wasted.domesurvival.forge.machine.solar;

public enum SolarPanelTier {
    MK1(6, 2_000, 16),
    MK2(18, 8_000, 48),
    MK3(48, 24_000, 128);

    private final int generationPerTick;
    private final int energyCapacity;
    private final int maxOutputPerTick;

    SolarPanelTier(int generationPerTick, int energyCapacity, int maxOutputPerTick) {
        this.generationPerTick = generationPerTick;
        this.energyCapacity = energyCapacity;
        this.maxOutputPerTick = maxOutputPerTick;
    }

    public int generationPerTick() {
        return generationPerTick;
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    public int maxOutputPerTick() {
        return maxOutputPerTick;
    }
}
