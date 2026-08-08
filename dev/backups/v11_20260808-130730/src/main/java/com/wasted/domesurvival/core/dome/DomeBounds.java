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

    public boolean isSafe(double x, double y, double z) {
        return isUndergroundSafe(x, y, z)
                || isSkirtSafe(x, y, z)
                || isHemisphereSafe(x, y, z);
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
