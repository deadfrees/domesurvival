package com.wasted.domesurvival.forge.oxygen.room;

/**
 * Central room-atmosphere balance constants.
 *
 * V61: one sealed interior block requires 10 mB of oxygen and ventilation fills
 * at up to 10 mB/t for 5 FE per oxygen mB.
 *
 * V62.4: room capacity and both pressure processes scale with interior volume.
 * Ventilation fills at an exact average of 10/3 mB/t while an open room loses
 * an exact average of 11/2 mB/t. This makes refill exactly 3x slower and
 * depressurization exactly 2x slower than V62.3 without floating-point drift.
 * Ventilation never compensates an active leak; the room must be resealed first.
 */
public final class RoomAtmosphereRules {
    public static final int OXYGEN_MB_PER_BLOCK = 10;

    /** Exact average refill rate: 10 / 3 mB per tick. */
    public static final int VENTILATION_FILL_MB_NUMERATOR = 10;
    public static final int VENTILATION_FILL_MB_DENOMINATOR = 3;

    public static final int ENERGY_FE_PER_OXYGEN_MB = 5;

    /**
     * Exact average oxygen-loss rate for an actively leaking room: 11 / 2 mB/t.
     *
     * Since room capacity is volume * OXYGEN_MB_PER_BLOCK, a larger room contains
     * proportionally more oxygen and therefore takes proportionally longer to
     * depressurize.
     */
    public static final int DEPRESSURIZATION_MB_NUMERATOR = 11;
    public static final int DEPRESSURIZATION_MB_DENOMINATOR = 2;

    /**
     * Emergency grace threshold while a room is actively leaking.
     * Below 50% of its original pressure the leaking room is no longer breathable.
     * Sealed-room V61 behavior remains unchanged: a sealed room is breathable only at 100%.
     */
    public static final int LEAK_BREATHABLE_PRESSURE_PERMILLE = 500;

    private RoomAtmosphereRules() {
    }

    public static int requiredOxygen(int volume) {
        if (volume <= 0) return 0;
        long required = (long) volume * OXYGEN_MB_PER_BLOCK;
        return required > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) required;
    }

    /** Full refill duration for a completely empty sealed room. */
    public static long fullRefillTicks(int volume) {
        return ticksForRationalRate(
                requiredOxygen(volume),
                VENTILATION_FILL_MB_NUMERATOR,
                VENTILATION_FILL_MB_DENOMINATOR
        );
    }

    /** Full pressure-loss duration for a fully pressurized room. */
    public static long fullDepressurizationTicks(int volume) {
        return ticksForRationalRate(
                requiredOxygen(volume),
                DEPRESSURIZATION_MB_NUMERATOR,
                DEPRESSURIZATION_MB_DENOMINATOR
        );
    }

    /** Duration for the current amount of oxygen to escape through an active leak. */
    public static long depressurizationTicksForOxygen(int oxygen) {
        return ticksForRationalRate(
                oxygen,
                DEPRESSURIZATION_MB_NUMERATOR,
                DEPRESSURIZATION_MB_DENOMINATOR
        );
    }

    /**
     * Per-tick room refill budget producing an exact 10/3 mB/t average:
     * 3, 3, 4, 3, 3, 4, ...
     */
    public static int ventilationFillBudget(long gameTime) {
        long phase = Math.floorMod(gameTime, VENTILATION_FILL_MB_DENOMINATOR);
        long previousTotal = (phase * VENTILATION_FILL_MB_NUMERATOR)
                / VENTILATION_FILL_MB_DENOMINATOR;
        long currentTotal = ((phase + 1L) * VENTILATION_FILL_MB_NUMERATOR)
                / VENTILATION_FILL_MB_DENOMINATOR;
        return (int) Math.max(0L, currentTotal - previousTotal);
    }

    /**
     * Oxygen escaped after elapsed ticks at the exact 11/2 mB/t average.
     */
    public static long escapedOxygen(long elapsedTicks) {
        if (elapsedTicks <= 0L) return 0L;
        return (elapsedTicks * DEPRESSURIZATION_MB_NUMERATOR)
                / DEPRESSURIZATION_MB_DENOMINATOR;
    }

    private static long ticksForRationalRate(int amount, int numerator, int denominator) {
        if (amount <= 0 || numerator <= 0 || denominator <= 0) return 0L;
        long scaledAmount = (long) amount * denominator;
        return (scaledAmount + numerator - 1L) / numerator;
    }

    public static int pressurePermille(int oxygen, int required) {
        if (required <= 0 || oxygen <= 0) return 0;
        if (oxygen >= required) return 1000;
        return (int) Math.min(1000L, ((long) oxygen * 1000L) / required);
    }

    public static int volumeFromRequiredOxygen(int required) {
        if (required <= 0) return 0;
        return Math.max(1, required / OXYGEN_MB_PER_BLOCK);
    }
}
