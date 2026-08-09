package com.wasted.domesurvival.core.oxygen;

/**
 * Pure-Java decision layer for ambient air, portable tanks and the player's
 * emergency respiratory reserve.
 *
 * Forge/Minecraft item lookups are intentionally outside this class.
 */
public final class OxygenSupplyRules {
    private OxygenSupplyRules() {
    }

    public static SupplyStep step(
            int reserveOxygen,
            int reserveMax,
            int emptyUpdates,
            boolean environmentBreathable,
            boolean tankEquipmentReady,
            int tankOxygen
    ) {
        int tank = Math.max(0, tankOxygen);

        if (environmentBreathable) {
            OxygenRules.StepResult reserve = OxygenRules.step(
                    reserveOxygen, reserveMax, emptyUpdates, true
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

        if (tankEquipmentReady && tank > 0) {
            // While the player is breathing from a tank, lungs recover just as
            // they would in breathable atmosphere. The tank itself is the resource.
            OxygenRules.StepResult reserve = OxygenRules.step(
                    reserveOxygen, reserveMax, emptyUpdates, true
            );

            int remainingTank = tank - 1;

            // Switch the HUD to the emergency reserve immediately after the last
            // tank unit is consumed; this avoids a one-second "0 tank -> full reserve" flash.
            OxygenSource nextSource = remainingTank > 0
                    ? OxygenSource.TANK
                    : OxygenSource.RESERVE;

            return new SupplyStep(
                    reserve.oxygen(),
                    reserve.emptyUpdates(),
                    remainingTank,
                    nextSource,
                    true,
                    false
            );
        }

        OxygenRules.StepResult reserve = OxygenRules.step(
                reserveOxygen, reserveMax, emptyUpdates, false
        );

        return new SupplyStep(
                reserve.oxygen(),
                reserve.emptyUpdates(),
                tank,
                OxygenSource.RESERVE,
                false,
                reserve.shouldDamage()
        );
    }

    public record SupplyStep(
            int reserveOxygen,
            int emptyUpdates,
            int tankOxygen,
            OxygenSource source,
            boolean tankConsumed,
            boolean shouldDamage
    ) {
    }
}
