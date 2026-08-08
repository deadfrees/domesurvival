package com.wasted.domesurvival.core.dome;

/** Authoritative geometry for breathable/safe dome volume. */
public final class DomeBounds {
    private final DomeSpec spec;

    public DomeBounds(DomeSpec spec) {
        this.spec = spec;
    }

    public DomeSpec spec() {
        return spec;
    }

    public DomeZone classify(double x, double y, double z) {
        if (isAirlockSafe(x, y, z)) return DomeZone.AIRLOCK;
        if (isUndergroundSafe(x, y, z)) return DomeZone.UNDERGROUND_SAFE;
        if (isSkirtSafe(x, y, z)) return DomeZone.SURFACE_SKIRT;
        if (isHemisphereSafe(x, y, z)) return DomeZone.SURFACE_DOME;
        return DomeZone.OUTSIDE;
    }

    public boolean isSafe(double x, double y, double z) {
        return classify(x, y, z).isSafe();
    }

    public boolean isAirlockSafe(double x, double y, double z) {
        return x >= spec.centerX() - 2.5
                && x <= spec.centerX() + 2.5
                && z >= spec.airlockStartZ()
                && z <= spec.airlockEndZ()
                && y >= spec.baseY()
                && y < spec.airlockCeilingY();
    }

    public boolean isUndergroundSafe(double x, double y, double z) {
        if (y < spec.undergroundMinY() || y >= spec.baseY()) return false;
        return horizontalDistanceSquared(x, z) <= square(spec.undergroundRadius());
    }

    public boolean isSkirtSafe(double x, double y, double z) {
        return y >= spec.baseY()
                && y < spec.hemisphereCenterY()
                && horizontalDistanceSquared(x, z) <= square(spec.surfaceRadius());
    }

    public boolean isHemisphereSafe(double x, double y, double z) {
        if (y < spec.hemisphereCenterY()) return false;
        double dx = x - spec.centerX();
        double dy = y - spec.hemisphereCenterY();
        double dz = z - spec.centerZ();
        return dx * dx + dy * dy + dz * dz <= square(spec.surfaceRadius());
    }

    private double horizontalDistanceSquared(double x, double z) {
        double dx = x - spec.centerX();
        double dz = z - spec.centerZ();
        return dx * dx + dz * dz;
    }

    private static double square(double value) {
        return value * value;
    }
}
