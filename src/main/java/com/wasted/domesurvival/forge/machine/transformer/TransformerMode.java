package com.wasted.domesurvival.forge.machine.transformer;

import com.wasted.domesurvival.forge.transport.energy.EnergyPipeTier;

public enum TransformerMode {
    LV_TO_MV(EnergyPipeTier.BASIC, EnergyPipeTier.REINFORCED),
    MV_TO_LV(EnergyPipeTier.REINFORCED, EnergyPipeTier.BASIC),
    MV_TO_HV(EnergyPipeTier.REINFORCED, EnergyPipeTier.HIGH_VOLTAGE),
    HV_TO_MV(EnergyPipeTier.HIGH_VOLTAGE, EnergyPipeTier.REINFORCED);

    private final EnergyPipeTier inputTier;
    private final EnergyPipeTier outputTier;

    TransformerMode(EnergyPipeTier inputTier, EnergyPipeTier outputTier) {
        this.inputTier = inputTier;
        this.outputTier = outputTier;
    }

    public EnergyPipeTier inputTier() {
        return inputTier;
    }

    public EnergyPipeTier outputTier() {
        return outputTier;
    }

    public int inputRate() {
        return inputTier.transferPerTick();
    }

    public int outputRate() {
        return outputTier.transferPerTick();
    }

    public TransformerMode next() {
        TransformerMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static TransformerMode fromOrdinal(int ordinal) {
        TransformerMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : LV_TO_MV;
    }
}
