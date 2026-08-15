package com.wasted.domesurvival.core.oxygen;

/**
 * Pure oxygen source selection rules.
 *
 * V27: a portable tank is the active breathing reserve while it is supplying O2.
 * The hidden base reserve must not refill behind the tank, otherwise the HUD can
 * start a second full bubble cycle when the tank reaches zero.
 */
public final class OxygenSupplyRules {
    private OxygenSupplyRules() {
    }

    public static SupplyStep step(
            int reserveOxygen,
            int reserveMax,
            int emptyUpdates,
            boolean environmentBreathable,
            boolean tankReady,
            int tankOxygen
    ) {
        int tank = Math.max(0, tankOxygen);

        // Safe environment: restore the normal short breathing reserve and do not use the tank.
        if (environmentBreathable) {
            OxygenRules.StepResult reserve = OxygenRules.step(
                    reserveOxygen,
                    reserveMax,
                    emptyUpdates,
                    true
            );
            return new SupplyStep(
                    reserve.oxygen(),
                    reserve.emptyUpdates(),
                    tank,
                    OxygenSource.ENVIRONMENT,
                    false,
                    false
            );
        }

        // Tank is the active reserve outside breathable areas.
        // IMPORTANT: keep the hidden base reserve at zero while the tank is supplying.
        // This prevents the HUD from restarting with a second bubble cycle at tank depletion.
        if (tankReady && tank > 0) {
            int remainingTank = Math.max(0, tank - 1);
            OxygenSource nextSource = remainingTank > 0
                    ? OxygenSource.TANK
                    : OxygenSource.RESERVE;

            return new SupplyStep(
                    0,
                    0,
                    remainingTank,
                    nextSource,
                    false,
                    true
            );
        }

        // No usable tank: consume the normal player reserve and damage only according
        // to the existing OxygenRules once that reserve has reached zero.
        OxygenRules.StepResult reserve = OxygenRules.step(
                reserveOxygen,
                reserveMax,
                emptyUpdates,
                false
        );
        return new SupplyStep(
                reserve.oxygen(),
                reserve.emptyUpdates(),
                tank,
                OxygenSource.RESERVE,
                reserve.shouldDamage(),
                false
        );
    }

    public record SupplyStep(
            int reserveOxygen,
            int emptyUpdates,
            int tankOxygen,
            OxygenSource source,
            boolean shouldDamage,
            boolean tankConsumed
    ) {
    }
}
