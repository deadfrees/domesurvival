package com.wasted.domesurvival.forge.machine.side;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/** Shared low-profile machine connector visual. */
public enum PortVisual implements StringRepresentable {
    OFF("off"),
    INPUT("input"),
    OUTPUT("output");

    private final String serializedName;

    PortVisual(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }

    public static PortVisual fromMode(SideMode mode) {
        return switch (mode) {
            case INPUT -> INPUT;
            case OUTPUT, BOTH -> OUTPUT;
            case DISABLED -> OFF;
        };
    }
}
