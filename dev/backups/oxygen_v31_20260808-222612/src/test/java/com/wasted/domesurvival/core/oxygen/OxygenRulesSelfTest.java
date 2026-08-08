package com.wasted.domesurvival.core.oxygen;

public final class OxygenRulesSelfTest {
    private OxygenRulesSelfTest() {
    }

    public static void main(String[] args) {
        int max = OxygenRules.BASE_MAX_OXYGEN;
        int oxygen = max;
        int empty = 0;

        // 20 one-second updates outside should exhaust the baseline reserve.
        for (int i = 0; i < max; i++) {
            OxygenRules.StepResult step = OxygenRules.step(oxygen, max, empty, false);
            oxygen = step.oxygen();
            empty = step.emptyUpdates();
            check(!step.shouldDamage(), "damage before reserve was exhausted");
        }
        check(oxygen == 0, "oxygen should reach zero after baseline reserve duration");

        OxygenRules.StepResult firstEmpty = OxygenRules.step(oxygen, max, empty, false);
        check(!firstEmpty.shouldDamage(), "first empty update is grace period");

        OxygenRules.StepResult secondEmpty = OxygenRules.step(
                firstEmpty.oxygen(), max, firstEmpty.emptyUpdates(), false);
        check(secondEmpty.shouldDamage(), "second empty update should damage");

        OxygenRules.StepResult recovery = OxygenRules.step(0, max, secondEmpty.emptyUpdates(), true);
        check(recovery.oxygen() == OxygenRules.REFILL_PER_UPDATE, "breathable recovery rate");
        check(recovery.emptyUpdates() == 0, "breathable air must clear suffocation timer");
        check(!recovery.shouldDamage(), "breathable air must not damage");

        System.out.println("OxygenRulesSelfTest: OK");
        System.out.println("baseMax=" + OxygenRules.BASE_MAX_OXYGEN);
        System.out.println("drainPerSecond=" + OxygenRules.DRAIN_PER_UPDATE);
        System.out.println("refillPerSecond=" + OxygenRules.REFILL_PER_UPDATE);
        System.out.println("damageEveryEmptySeconds=" + OxygenRules.DAMAGE_EVERY_EMPTY_UPDATES);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
