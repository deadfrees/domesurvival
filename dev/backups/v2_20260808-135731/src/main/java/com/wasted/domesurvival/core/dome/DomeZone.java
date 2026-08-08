package com.wasted.domesurvival.core.dome;

/** Logical environment zone used by later oxygen/damage systems. */
public enum DomeZone {
    AIRLOCK(true),
    UNDERGROUND_SAFE(true),
    SURFACE_SKIRT(true),
    SURFACE_DOME(true),
    OUTSIDE(false);

    private final boolean safe;

    DomeZone(boolean safe) {
        this.safe = safe;
    }

    public boolean isSafe() {
        return safe;
    }
}
