package com.wasted.domesurvival.forge.client;

/**
 * Client-side read-only mirror of server oxygen state.
 * Contains no Minecraft client classes so the packet handler remains dedicated-server safe.
 */
public final class ClientOxygenState {
    private static volatile int oxygen;
    private static volatile int maxOxygen = 1;
    private static volatile boolean breathable = true;
    private static volatile boolean initialized;

    private ClientOxygenState() {
    }

    public static void update(int newOxygen, int newMaxOxygen, boolean newBreathable) {
        maxOxygen = Math.max(1, newMaxOxygen);
        oxygen = Math.max(0, Math.min(maxOxygen, newOxygen));
        breathable = newBreathable;
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

    public static boolean initialized() {
        return initialized;
    }
}
