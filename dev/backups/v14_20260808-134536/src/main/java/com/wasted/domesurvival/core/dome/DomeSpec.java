package com.wasted.domesurvival.core.dome;

/**
 * Geometry agreed for the WASTED starter dome.
 * Pure Java on purpose so geometry can be tested without Minecraft/Forge.
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

    /*
     * V1.2 airlock placement.
     * The WASTED coarse-dirt trail in front of the dome runs roughly through
     * X=-518..-510 at the shell and bends slightly west farther out.
     * X=-515 puts the 7-block-wide airlock directly over that trail.
     */
    public int airlockCenterX() {
        return centerX - 9; // -515
    }

    public int airlockHalfWidth() {
        return 3;
    }

    public int airlockInsideDepth() {
        return 4;
    }

    public int airlockOutsideLength() {
        return 10;
    }

    /** South shell Z at the shifted airlock X. */
    public int airlockShellZ() {
        int dx = airlockCenterX() - centerX;
        double dz = Math.sqrt((double) surfaceRadius * surfaceRadius - (double) dx * dx);
        return centerZ + (int) Math.round(dz);
    }

    public int airlockStartZ() {
        return airlockShellZ() - airlockInsideDepth();
    }

    public int airlockEndZ() {
        return airlockShellZ() + airlockOutsideLength();
    }

    /** Terrain/path surface block is at Y=61; player feet are at Y=62. */
    public int airlockFloorY() {
        return baseY - 1;
    }

    public int airlockCeilingY() {
        return baseY + 5;
    }

    public int foundationMinY() {
        return baseY - 3;
    }

    /**
     * V1.3 makes the foundation a real visible plinth instead of mostly buried footing.
     * Path surface is Y=61, so Y=62..63 is clearly visible above the terrain.
     */
    public int foundationTopY() {
        return baseY + 1;
    }

    // Previous V1.1 airlock coordinates, needed only for a clean V1.1 -> V1.2 migration.
    public int legacyAirlockCenterX() {
        return centerX;
    }

    public int legacyAirlockStartZ() {
        return centerZ + surfaceRadius - 4;
    }

    public int legacyAirlockEndZ() {
        return centerZ + surfaceRadius + 10;
    }
}
