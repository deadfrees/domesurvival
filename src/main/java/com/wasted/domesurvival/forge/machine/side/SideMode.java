package com.wasted.domesurvival.forge.machine.side;

import java.util.Locale;
import java.util.Set;

/**
 * Input/output permission assigned to one physical side for one resource channel.
 */
public enum SideMode {
    DISABLED(false, false),
    INPUT(true, false),
    OUTPUT(false, true),
    BOTH(true, true);

    private final boolean allowsInput;
    private final boolean allowsOutput;

    SideMode(boolean allowsInput, boolean allowsOutput) {
        this.allowsInput = allowsInput;
        this.allowsOutput = allowsOutput;
    }

    public boolean allowsInput() {
        return allowsInput;
    }

    public boolean allowsOutput() {
        return allowsOutput;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public SideMode next() {
        SideMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Cycles to the next mode accepted by a particular machine/channel rule.
     * This lets a generator expose only DISABLED/OUTPUT while an energy cell can
     * expose all four modes without putting machine-specific rules in this enum.
     */
    public SideMode nextAllowed(Set<SideMode> allowedModes) {
        if (allowedModes == null || allowedModes.isEmpty()) {
            return this;
        }

        SideMode candidate = this;
        for (int i = 0; i < values().length; i++) {
            candidate = candidate.next();
            if (allowedModes.contains(candidate)) {
                return candidate;
            }
        }

        return this;
    }

    public static SideMode fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return DISABLED;
        }

        for (SideMode mode : values()) {
            if (mode.getSerializedName().equals(value)) {
                return mode;
            }
        }

        return DISABLED;
    }
}
