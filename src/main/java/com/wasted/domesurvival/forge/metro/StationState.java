package com.wasted.domesurvival.forge.metro;

public enum StationState {
    ABANDONED,
    RESTORING,
    RESTORED;

    public static StationState byOrdinal(int ordinal) {
        StationState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ABANDONED;
    }

    public static StationState byName(String name) {
        if (name == null || name.isBlank()) return ABANDONED;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ABANDONED;
        }
    }
}
