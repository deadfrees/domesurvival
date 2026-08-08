package com.wasted.domesurvival.core.dome;

/**
 * Version-1 geometry agreed for WASTED.
 * No Minecraft/Forge types here on purpose: this class is unit-testable with plain Java.
 */
public record DomeSpec(
        int centerX,
        int baseY,
        int centerZ,
        int surfaceRadius,
        int skirtHeight,
        int undergroundRadius,
        int undergroundMinY
) {
    public DomeSpec {
        if (surfaceRadius < 4) throw new IllegalArgumentException("surfaceRadius must be >= 4");
        if (skirtHeight < 0) throw new IllegalArgumentException("skirtHeight must be >= 0");
        if (undergroundRadius < 1 || undergroundRadius > surfaceRadius) {
            throw new IllegalArgumentException("undergroundRadius must be in 1..surfaceRadius");
        }
        if (undergroundMinY >= baseY) throw new IllegalArgumentException("undergroundMinY must be below baseY");
    }

    public static DomeSpec wastedV1() {
        return new DomeSpec(-506, 62, -641, 50, 3, 45, -64);
    }

    public int hemisphereCenterY() {
        return baseY + skirtHeight;
    }

    public int topY() {
        return hemisphereCenterY() + surfaceRadius;
    }
}
