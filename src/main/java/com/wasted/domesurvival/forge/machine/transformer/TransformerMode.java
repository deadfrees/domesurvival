package com.wasted.domesurvival.forge.machine.transformer;

import com.wasted.domesurvival.forge.transport.energy.EnergyPipeTier;
import org.jetbrains.annotations.Nullable;

/**
 * Automatic transformer pair.
 *
 * <p>Only adjacent voltage classes are convertible. BASIC <-> HIGH_VOLTAGE therefore
 * requires two transformers with a REINFORCED segment between them.</p>
 */
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

    @Nullable
    public static TransformerMode fromTiers(
            @Nullable EnergyPipeTier inputTier,
            @Nullable EnergyPipeTier outputTier
    ) {
        if (inputTier == null || outputTier == null) {
            return null;
        }

        for (TransformerMode mode : values()) {
            if (mode.inputTier == inputTier && mode.outputTier == outputTier) {
                return mode;
            }
        }

        return null;
    }

    @Nullable
    public static TransformerMode fromOrdinalOrNull(int ordinal) {
        TransformerMode[] values = values();
        return ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : null;
    }
}
