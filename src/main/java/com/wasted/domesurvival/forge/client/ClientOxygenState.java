package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.core.oxygen.OxygenSource;

/**
 * Client-side read-only mirror. The server remains authoritative.
 */
public final class ClientOxygenState {
    private static volatile int oxygen;
    private static volatile int maxOxygen = 1;
    private static volatile boolean breathable = true;
    private static volatile OxygenSource source = OxygenSource.ENVIRONMENT;
    private static volatile boolean initialized;

    private ClientOxygenState() {
    }

    public static void update(
            int newOxygen,
            int newMaxOxygen,
            boolean newBreathable,
            OxygenSource newSource
    ) {
        maxOxygen = Math.max(1, newMaxOxygen);
        oxygen = Math.max(0, Math.min(maxOxygen, newOxygen));
        breathable = newBreathable;
        source = newSource == null ? OxygenSource.RESERVE : newSource;
        initialized = true;
    }

    public static int oxygen() {
        return oxygen;
    }

    public static int maxOxygen() {
        return maxOxygen;
    }

    public static boolean breathable() {
        return breathable;
    }

    public static OxygenSource source() {
        return source;
    }

    public static boolean initialized() {
        return initialized;
    }
}
