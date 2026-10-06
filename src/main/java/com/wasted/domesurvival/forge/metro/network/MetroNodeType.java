package com.wasted.domesurvival.forge.metro.network;

public enum MetroNodeType {
    DOME,
    RESTORED_STATION;

    public static MetroNodeType byName(String name) {
        if (name == null || name.isBlank()) return RESTORED_STATION;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return RESTORED_STATION;
        }
    }
}
