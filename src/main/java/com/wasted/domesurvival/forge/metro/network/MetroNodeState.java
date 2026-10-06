package com.wasted.domesurvival.forge.metro.network;

public enum MetroNodeState {
    ACTIVE,
    DISABLED;

    public static MetroNodeState byName(String name) {
        if (name == null || name.isBlank()) return ACTIVE;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ACTIVE;
        }
    }
}
