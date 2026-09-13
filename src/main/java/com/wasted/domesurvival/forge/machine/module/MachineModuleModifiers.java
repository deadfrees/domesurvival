package com.wasted.domesurvival.forge.machine.module;

/**
 * Immutable module effect bundle.
 *
 * <p>Percent values are fixed-point integers instead of floating point so machine
 * calculations remain deterministic on dedicated servers. 100 means unchanged.</p>
 */
public record MachineModuleModifiers(
        int speedPercent,
        int energyPercent,
        int bufferPercent,
        boolean automation,
        boolean preserveProgress,
        boolean communication
) {
    public static final MachineModuleModifiers IDENTITY = new MachineModuleModifiers(
            100,
            100,
            100,
            false,
            false,
            false
    );

    public MachineModuleModifiers {
        if (speedPercent <= 0 || energyPercent <= 0 || bufferPercent <= 0) {
            throw new IllegalArgumentException("Machine module percentages must be positive");
        }
    }

    public MachineModuleModifiers combine(MachineModuleModifiers other) {
        if (other == null) {
            return this;
        }
        return new MachineModuleModifiers(
                combinePercent(speedPercent, other.speedPercent),
                combinePercent(energyPercent, other.energyPercent),
                combinePercent(bufferPercent, other.bufferPercent),
                automation || other.automation,
                preserveProgress || other.preserveProgress,
                communication || other.communication
        );
    }

    public int applyEnergyCost(int baseValue) {
        return scaleIntCeil(baseValue, energyPercent);
    }

    public int applyBufferCapacity(int baseValue) {
        return scaleIntCeil(baseValue, bufferPercent);
    }

    public long applyBufferCapacity(long baseValue) {
        return scaleLongCeil(baseValue, bufferPercent);
    }

    /**
     * Converts a speed modifier to processing ticks. Higher speed means fewer ticks.
     */
    public int applyProcessingTicks(int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        long scaled = ((long) baseTicks * 100L + speedPercent - 1L) / speedPercent;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, scaled));
    }

    private static int combinePercent(int first, int second) {
        long value = ((long) first * second + 50L) / 100L;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, value));
    }

    private static int scaleIntCeil(int baseValue, int percent) {
        if (baseValue <= 0) {
            return 0;
        }
        long value = ((long) baseValue * percent + 99L) / 100L;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, value));
    }

    private static long scaleLongCeil(long baseValue, int percent) {
        if (baseValue <= 0L) {
            return 0L;
        }
        if (baseValue > Long.MAX_VALUE / percent) {
            return Long.MAX_VALUE;
        }
        long product = baseValue * percent;
        if (product > Long.MAX_VALUE - 99L) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (product + 99L) / 100L);
    }
}
