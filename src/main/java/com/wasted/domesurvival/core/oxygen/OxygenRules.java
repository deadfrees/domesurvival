package com.wasted.domesurvival.core.oxygen;

/**
 * Pure-Java oxygen rules. No Minecraft/Forge dependencies by design.
 *
 * One oxygen unit currently represents roughly one second of unassisted breathing
 * outside a breathable environment. Equipment will extend/supply this later.
 */
public final class OxygenRules {
    private OxygenRules() {
    }

    /** Server simulation cadence. Never run the environment simulation every game tick. */
    public static final int UPDATE_INTERVAL_TICKS = 20;

    /** Baseline unassisted reserve: ~20 seconds. */
    public static final int BASE_MAX_OXYGEN = 20;

    /** One second of reserve consumed per server update while atmosphere is not breathable. */
    public static final int DRAIN_PER_UPDATE = 1;

    /** Fast lung recovery after returning to breathable air. */
    public static final int REFILL_PER_UPDATE = 5;

    /**
     * Once the reserve is empty, damage is applied every oxygen simulation update.
     * With UPDATE_INTERVAL_TICKS=20 this means once per second.
     */
    public static final int DAMAGE_EVERY_EMPTY_UPDATES = 1;

    /**
     * 4 Minecraft damage points = 2 hearts.
     * This is intentionally stronger than natural regeneration at full hunger.
     */
    public static final float SUFFOCATION_DAMAGE = 4.0F;

    /** HUD is normalized to ten vanilla-style bubbles regardless of future tank capacity. */
    public static final int HUD_BUBBLES = 10;

    public static StepResult step(int oxygen, int maxOxygen, int emptyUpdates, boolean breathable) {
        int max = Math.max(1, maxOxygen);
        int current = clamp(oxygen, 0, max);
        int empty = Math.max(0, emptyUpdates);

        if (breathable) {
            int next = Math.min(max, current + REFILL_PER_UPDATE);
            return new StepResult(next, 0, false);
        }

        if (current > 0) {
            int next = Math.max(0, current - DRAIN_PER_UPDATE);
            // Reaching zero starts a one-update grace period; damage begins next update.
            return new StepResult(next, 0, false);
        }

        int nextEmpty = empty + 1;
        boolean damage = nextEmpty % DAMAGE_EVERY_EMPTY_UPDATES == 0;
        return new StepResult(0, nextEmpty, damage);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record StepResult(int oxygen, int emptyUpdates, boolean shouldDamage) {
    }
}
