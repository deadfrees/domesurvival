package com.wasted.domesurvival.forge.machine.oxygen.complex;

public enum OxygenComplexStatus {
    INCOMPLETE,
    IDLE,
    NO_ATMOSPHERE,
    LOW_ENERGY,
    INTAKING,
    FILTERING,
    COMPRESSING,
    PRODUCING,
    OXYGEN_FULL,
    OPERATIONAL;

    public static OxygenComplexStatus byOrdinal(int ordinal) {
        OxygenComplexStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INCOMPLETE;
    }
}
