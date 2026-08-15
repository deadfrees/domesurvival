package com.wasted.domesurvival.forge.transport.energy;

public enum EnergyPipeSideMode {
    AUTO,
    INPUT,
    OUTPUT,
    DISABLED;

    private static final EnergyPipeSideMode[] VALUES = values();

    public EnergyPipeSideMode cycle(boolean reverse) {
        int delta = reverse ? -1 : 1;
        return VALUES[Math.floorMod(ordinal() + delta, VALUES.length)];
    }

    /**
     * INPUT means energy enters the pipe network from the adjacent machine.
     */
    public boolean allowsExtractionFromMachine() {
        return this == AUTO || this == INPUT;
    }

    /**
     * OUTPUT means energy leaves the pipe network into the adjacent machine.
     */
    public boolean allowsInsertionIntoMachine() {
        return this == AUTO || this == OUTPUT;
    }

    public boolean isConnectionEnabled() {
        return this != DISABLED;
    }

    public String translationKey() {
        return "message.domesurvival.energy_pipe.mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
