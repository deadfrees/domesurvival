package com.wasted.domesurvival.forge.airlock.gate;

import net.minecraft.util.StringRepresentable;

/**
 * Short-lived gate animation state. No permanent ticker is used: the master
 * block advances states only through scheduled block ticks while moving.
 */
public enum AirlockGateMotion implements StringRepresentable {
    CLOSED("closed", 0, false),
    OPENING_1("opening_1", 1, true),
    OPENING_2("opening_2", 2, true),
    OPENING_3("opening_3", 3, true),
    OPENING_4("opening_4", 4, true),
    OPENING_5("opening_5", 5, true),
    OPEN("open", 6, false),
    CLOSING_5("closing_5", 5, true),
    CLOSING_4("closing_4", 4, true),
    CLOSING_3("closing_3", 3, true),
    CLOSING_2("closing_2", 2, true),
    CLOSING_1("closing_1", 1, true);

    private final String serializedName;
    private final int visualStage;
    private final boolean moving;

    AirlockGateMotion(String serializedName, int visualStage, boolean moving) {
        this.serializedName = serializedName;
        this.visualStage = visualStage;
        this.moving = moving;
    }

    public int visualStage() {
        return visualStage;
    }

    public boolean moving() {
        return moving;
    }

    public boolean opening() {
        return this == OPENING_1 || this == OPENING_2 || this == OPENING_3
                || this == OPENING_4 || this == OPENING_5;
    }

    public boolean closing() {
        return this == CLOSING_1 || this == CLOSING_2 || this == CLOSING_3
                || this == CLOSING_4 || this == CLOSING_5;
    }

    public AirlockGateMotion next() {
        return switch (this) {
            case OPENING_1 -> OPENING_2;
            case OPENING_2 -> OPENING_3;
            case OPENING_3 -> OPENING_4;
            case OPENING_4 -> OPENING_5;
            case OPENING_5 -> OPEN;
            case CLOSING_5 -> CLOSING_4;
            case CLOSING_4 -> CLOSING_3;
            case CLOSING_3 -> CLOSING_2;
            case CLOSING_2 -> CLOSING_1;
            case CLOSING_1 -> CLOSED;
            default -> this;
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
