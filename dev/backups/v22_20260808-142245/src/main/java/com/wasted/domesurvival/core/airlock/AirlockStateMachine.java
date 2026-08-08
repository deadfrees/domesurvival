package com.wasted.domesurvival.core.airlock;

/**
 * Pure Java interlock logic. V2 uses instant equalisation/venting when a door
 * is requested. Timed decompression can be added later without changing the
 * door interlock contract.
 */
public final class AirlockStateMachine {
    private AirlockStateMachine() {
    }

    public static AirlockTransition toggle(AirlockState current, AirlockDoor door) {
        if (door == AirlockDoor.INNER) {
            if (current.innerOpen()) {
                return ok(new AirlockState(false, false, AirlockPressure.PRESSURIZED),
                        "Inner shutter closed; chamber remains pressurized.");
            }
            if (current.outerOpen()) {
                return denied(current, "Outer shutter is open. Close it first.");
            }
            return ok(new AirlockState(true, false, AirlockPressure.PRESSURIZED),
                    "Chamber pressurized; inner shutter opened.");
        }

        if (current.outerOpen()) {
            return ok(new AirlockState(false, false, AirlockPressure.DEPRESSURIZED),
                    "Outer shutter closed; chamber remains depressurized.");
        }
        if (current.innerOpen()) {
            return denied(current, "Inner shutter is open. Close it first.");
        }
        return ok(new AirlockState(false, true, AirlockPressure.DEPRESSURIZED),
                "Chamber depressurized; outer shutter opened.");
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
