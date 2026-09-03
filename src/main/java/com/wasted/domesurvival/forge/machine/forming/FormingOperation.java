package com.wasted.domesurvival.forge.machine.forming;

import java.util.Locale;

/**
 * Mechanical operation selected on the universal forming machine.
 *
 * <p>The serialized names are part of the data-pack contract. Keep them stable so
 * recipes and existing worlds remain compatible across updates.</p>
 */
public enum FormingOperation {
    PRESS,
    GEAR,
    ROD,
    WIRE,
    TUBE;

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public FormingOperation next() {
        FormingOperation[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static FormingOperation fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return PRESS;
        }

        for (FormingOperation operation : values()) {
            if (operation.getSerializedName().equalsIgnoreCase(value)) {
                return operation;
            }
        }

        return PRESS;
    }

    public static FormingOperation fromOrdinal(int ordinal) {
        FormingOperation[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PRESS;
    }
}
