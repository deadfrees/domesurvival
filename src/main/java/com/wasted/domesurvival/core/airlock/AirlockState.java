package com.wasted.domesurvival.core.airlock;

public record AirlockState(
        boolean innerOpen,
        boolean outerOpen,
        AirlockPressure pressure
) {
    public AirlockState {
        if (innerOpen && outerOpen) {
            throw new IllegalArgumentException("Both airlock doors cannot be open at the same time");
        }
        if (pressure == null) {
            throw new IllegalArgumentException("pressure");
        }
    }

    public static AirlockState initial() {
        return new AirlockState(false, false, AirlockPressure.PRESSURIZED);
    }

    public boolean breathable() {
        return pressure == AirlockPressure.PRESSURIZED && !outerOpen;
    }
}
