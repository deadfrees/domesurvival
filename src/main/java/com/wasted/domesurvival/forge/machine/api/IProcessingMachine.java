package com.wasted.domesurvival.forge.machine.api;

/**
 * Small read-only contract shared by processing machines.
 *
 * <p>Critical gameplay decisions remain in each server-side BlockEntity. This API
 * only exposes normalized state for menus, diagnostics and future outpost
 * monitoring.</p>
 */
public interface IProcessingMachine {
    MachineOperatingState operatingState();

    int progressTicks();

    int requiredTicks();

    default boolean isWorking() {
        return operatingState() == MachineOperatingState.WORKING;
    }

    default float progressFraction() {
        int required = requiredTicks();
        if (required <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, progressTicks() / (float) required));
    }
}
