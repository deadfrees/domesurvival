package com.wasted.domesurvival.core.dome;

/**
 * Logical environment zone. AIRLOCK is dynamic: its breathable state depends
 * on the runtime pressure stored by the server, so it is not "always safe".
 */
public enum DomeZone {
    AIRLOCK(false),
    UNDERGROUND_SAFE(true),
    SURFACE_SKIRT(true),
    SURFACE_DOME(true),
    OUTSIDE(false);

    private final boolean alwaysSafe;

    DomeZone(boolean alwaysSafe) {
        this.alwaysSafe = alwaysSafe;
    }

    /** Static safety only. AIRLOCK must be resolved by AirlockService. */
    public boolean isSafe() {
        return alwaysSafe;
    }
}
