package com.wasted.domesurvival.forge.storage.tank;

/** Mutually exclusive storage channel used by a universal tank structure. */
public enum UniversalTankContentKind {
    EMPTY,
    FLUID,
    OXYGEN;

    public static UniversalTankContentKind byOrdinal(int ordinal) {
        UniversalTankContentKind[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EMPTY;
    }
}
