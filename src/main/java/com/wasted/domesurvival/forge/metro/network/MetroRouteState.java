package com.wasted.domesurvival.forge.metro.network;

public enum MetroRouteState {
    ACTIVE,
    DISABLED,
    DAMAGED;

    public static MetroRouteState byName(String name) {
        if (name == null || name.isBlank()) return ACTIVE;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ACTIVE;
        }
    }
}
