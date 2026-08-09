package com.wasted.domesurvival.core.oxygen;

public final class OxygenSupplyRulesSelfTest {
    private OxygenSupplyRulesSelfTest() {
    }

    public static void main(String[] args) {
        int max = OxygenRules.BASE_MAX_OXYGEN;

        // 1. Ambient atmosphere: reserve recovers, tank untouched.
        OxygenSupplyRules.SupplyStep ambient = OxygenSupplyRules.step(
                5, max, 3, true, true, 100
        );
        check(ambient.source() == OxygenSource.ENVIRONMENT, "ambient source");
        check(ambient.reserveOxygen() == 10, "ambient reserve refill");
        check(ambient.tankOxygen() == 100, "ambient must not refill or consume tank");
        check(!ambient.tankConsumed(), "ambient must not consume tank");

        // 2. Proper mask+tank equipment: tank is consumed, reserve recovers.
        OxygenSupplyRules.SupplyStep tank = OxygenSupplyRules.step(
                5, max, 0, false, true, 120
        );
        check(tank.source() == OxygenSource.TANK, "tank source");
        check(tank.tankOxygen() == 119, "tank drain");
        check(tank.reserveOxygen() == 10, "reserve recovers while tank supplies air");
        check(tank.tankConsumed(), "tank consumption flag");
        check(!tank.shouldDamage(), "tank must prevent suffocation");

        // 3. Tank without complete equipment is ignored.
        OxygenSupplyRules.SupplyStep noMask = OxygenSupplyRules.step(
                20, max, 0, false, false, 120
        );
        check(noMask.source() == OxygenSource.RESERVE, "incomplete equipment uses reserve");
        check(noMask.tankOxygen() == 120, "incomplete equipment must not consume tank");
        check(noMask.reserveOxygen() == 19, "reserve drains without usable tank");

        // 4. Last tank unit switches immediately to reserve display.
        OxygenSupplyRules.SupplyStep lastTank = OxygenSupplyRules.step(
                20, max, 0, false, true, 1
        );
        check(lastTank.tankOxygen() == 0, "last tank unit consumed");
        check(lastTank.source() == OxygenSource.RESERVE, "switch to reserve after last tank unit");
        check(lastTank.reserveOxygen() == 20, "reserve remains full after tank breathing");

        // 5. Empty tank falls back to normal V3.1 reserve/suffocation.
        int reserve = 1;
        int empty = 0;
        OxygenSupplyRules.SupplyStep reserveStep = OxygenSupplyRules.step(
                reserve, max, empty, false, true, 0
        );
        check(reserveStep.reserveOxygen() == 0, "reserve reaches zero");
        check(!reserveStep.shouldDamage(), "no damage on transition to zero");

        OxygenSupplyRules.SupplyStep suffocate = OxygenSupplyRules.step(
                reserveStep.reserveOxygen(),
                max,
                reserveStep.emptyUpdates(),
                false,
                true,
                0
        );
        check(suffocate.shouldDamage(), "V3.1 suffocation retained");

        System.out.println("OxygenSupplyRulesSelfTest V3.2: OK");
        System.out.println("tankCapacityTest=120");
        System.out.println("tankDrainPerSecond=1");
        System.out.println("reserveFallbackSeconds=" + OxygenRules.BASE_MAX_OXYGEN);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
