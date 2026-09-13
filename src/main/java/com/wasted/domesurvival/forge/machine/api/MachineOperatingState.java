package com.wasted.domesurvival.forge.machine.api;

/**
 * Shared server-side operating states used by machine GUIs and diagnostics.
 */
public enum MachineOperatingState {
    IDLE,
    NO_ENERGY,
    NO_RECIPE,
    OUTPUT_BLOCKED,
    WORKING
}
