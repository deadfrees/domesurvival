package com.wasted.domesurvival.forge.machine.oxygen;

/** Operating mode of the oxygen filler. VENTILATION is the room-output path used by V60+. */
public enum OxygenFillerMode {
    TANK_FILLING,
    VENTILATION;

    public OxygenFillerMode next() {
        return this == TANK_FILLING ? VENTILATION : TANK_FILLING;
    }

    public static OxygenFillerMode byOrdinal(int ordinal) {
        OxygenFillerMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : TANK_FILLING;
    }
}
