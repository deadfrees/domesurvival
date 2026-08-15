package com.wasted.domesurvival.core.airlock;

/**
 * Pure Java interlock logic.
 *
 * V2.2 still uses instant pressure equalisation rather than a timed cycle:
 * - opening the outer shutter vents the chamber;
 * - closing the outer shutter immediately restores pressure;
 * - the two shutters can never be open at the same time.
 */
public final class AirlockStateMachine {
    private AirlockStateMachine() {
    }

    public static AirlockTransition toggle(AirlockState current, AirlockDoor door) {
        if (door == AirlockDoor.INNER) {
            if (current.innerOpen()) {
                return ok(new AirlockState(false, false, AirlockPressure.PRESSURIZED),
                        "Внутренняя дверь закрыта.");
            }
            if (current.outerOpen()) {
                return denied(current, "Внешняя дверь открыта — сначала закройте её.");
            }
            return ok(new AirlockState(true, false, AirlockPressure.PRESSURIZED),
                    "Внутренняя дверь открыта.");
        }

        if (current.outerOpen()) {
            return ok(new AirlockState(false, false, AirlockPressure.PRESSURIZED),
                    "Внешняя дверь закрыта, камера снова под давлением.");
        }
        if (current.innerOpen()) {
            return denied(current, "Внутренняя дверь открыта — сначала закройте её.");
        }
        return ok(new AirlockState(false, true, AirlockPressure.DEPRESSURIZED),
                "Внешняя дверь открыта, камера разгерметизирована.");
    }

    public static AirlockState reset() {
        return AirlockState.initial();
    }

    private static AirlockTransition ok(AirlockState state, String message) {
        return new AirlockTransition(true, state, message);
    }

    private static AirlockTransition denied(AirlockState state, String message) {
        return new AirlockTransition(false, state, message);
    }
}
