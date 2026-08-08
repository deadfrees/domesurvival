package com.wasted.domesurvival.core.dome;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Produces integer shell positions without touching a Minecraft world.
 * Forge integration will map these positions to reinforced_glass / dome_frame.
 */
public final class DomeShellPlanner {
    private DomeShellPlanner() {
    }

    public static Set<BlockPoint> planOneBlockShell(DomeSpec spec) {
        Set<BlockPoint> result = new LinkedHashSet<>();
        int r = spec.surfaceRadius();
        double inner = r - 1.15;
        double outerSq = (r + 0.35) * (r + 0.35);
        double innerSq = inner * inner;

        // Vertical skirt around the terrain line.
        for (int y = spec.baseY(); y < spec.hemisphereCenterY(); y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double h2 = dx * dx + dz * dz;
                    if (h2 >= innerSq && h2 <= outerSq) {
                        result.add(new BlockPoint(spec.centerX() + dx, y, spec.centerZ() + dz));
                    }
                }
            }
        }

        // Upper hemisphere.
        int cy = spec.hemisphereCenterY();
        for (int dy = 0; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 >= innerSq && d2 <= outerSq) {
                        result.add(new BlockPoint(spec.centerX() + dx, cy + dy, spec.centerZ() + dz));
                    }
                }
            }
        }
        return result;
    }
}
