package com.wasted.domesurvival.forge.transport.fluid;

import java.util.Locale;

public enum FluidPipeSideMode {
    AUTO,
    INPUT,
    OUTPUT,
    DISABLED;

    private static final FluidPipeSideMode[] VALUES = values();

    public FluidPipeSideMode cycle(boolean reverse) {
        int delta = reverse ? -1 : 1;
        return VALUES[Math.floorMod(ordinal() + delta, VALUES.length)];
    }

    public boolean allowsDrainFromMachine() {
        return this == AUTO || this == INPUT;
    }

    public boolean allowsFillIntoMachine() {
        return this == AUTO || this == OUTPUT;
    }

    public boolean isConnectionEnabled() {
        return this != DISABLED;
    }

    public String translationKey() {
        return "message.domesurvival.fluid_pipe.mode." + name().toLowerCase(Locale.ROOT);
    }
}
