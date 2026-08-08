package com.wasted.domesurvival.core.dome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Plans current visual structure without touching Minecraft classes. */
public final class DomeStructurePlanner {
    private DomeStructurePlanner() {
    }

    public static List<PlannedBlock> planFullV14(DomeSpec spec) {
        LinkedHashMap<BlockPoint, StructureMaterial> plan = new LinkedHashMap<>();
        for (BlockPoint p : DomeShellPlanner.planOneBlockShell(spec)) {
            put(plan, p, StructureMaterial.GLASS);
        }
        applyCurrentExtras(plan, spec);
        return toList(plan);
    }

    /** Upgrade the original glass-only V1 directly to current V1.2. */
    public static List<PlannedBlock> planV14UpgradeFromV1(DomeSpec spec) {
        LinkedHashMap<BlockPoint, StructureMaterial> plan = new LinkedHashMap<>();
        applyCurrentExtras(plan, spec);
        return toList(plan);
    }

    /**
     * Migrate an already-built V1.1 dome:
     * - remove the old off-path airlock,
     * - close its old shell opening,
     * - remove the small WASTED author structure immediately beyond it,
     * - deepen the perimeter foundation,
     * - build the new path-aligned airlock with its own foundation.
     */
    public static List<PlannedBlock> planV14UpgradeFromV11(DomeSpec spec) {
        LinkedHashMap<BlockPoint, StructureMaterial> plan = new LinkedHashMap<>();
        removeLegacyAirlock(plan, spec);
        restoreLegacyOpening(plan, spec);
        clearAuthorStructure(plan);
        addFoundation(plan, spec);
        addPathAlignedAirlock(plan, spec);
        carveCurrentAirlockOpening(plan, spec);
        return toList(plan);
    }

    /** Upgrade a V1.2 dome directly to the current inward-only V1.4 foundation. */
    public static List<PlannedBlock> planV14UpgradeFromV12(DomeSpec spec) {
        LinkedHashMap<BlockPoint, StructureMaterial> plan = new LinkedHashMap<>();
        addFoundation(plan, spec);
        addPathAlignedAirlockFoundation(plan, spec);
        carveCurrentAirlockOpening(plan, spec);
        return toList(plan);
    }

    /**
     * V1.3 -> V1.4 migration:
     * remove the part of the old plinth that projected outside R=50,
     * then add/retain the correct inward technical ring and airlock footing.
     */
    public static List<PlannedBlock> planV14UpgradeFromV13(DomeSpec spec) {
        LinkedHashMap<BlockPoint, StructureMaterial> plan = new LinkedHashMap<>();
        clearV13ExternalFoundation(plan, spec);
        addFoundation(plan, spec);
        addPathAlignedAirlockFoundation(plan, spec);
        carveCurrentAirlockOpening(plan, spec);
        return toList(plan);
    }

    private static void applyCurrentExtras(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        addFoundation(plan, spec);
        addRibs(plan, spec);
        addMidRing(plan, spec);
        clearAuthorStructure(plan);
        addPathAlignedAirlock(plan, spec);
        carveCurrentAirlockOpening(plan, spec);
    }

    /**
     * V1.4 perimeter foundation.
     *
     * The plinth now sits UNDER the shell and grows inward only.  Its outer edge
     * never extends beyond the dome radius, so the exterior remains WASTED terrain.
     * The visible inner technical ring is four blocks thick (R=46..50).
     */
    private static void addFoundation(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int r = spec.surfaceRadius();
        double innerSq = (r - 4.0) * (r - 4.0);
        double outerSq = (double) r * r;

        for (int y = spec.foundationMinY(); y <= spec.foundationTopY(); y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double h2 = dx * dx + dz * dz;
                    if (h2 >= innerSq && h2 <= outerSq) {
                        put(plan, new BlockPoint(spec.centerX() + dx, y, spec.centerZ() + dz), StructureMaterial.FOUNDATION);
                    }
                }
            }
        }
    }

    /**
     * Removes only V1.3's mistakenly external dome foundation (R>50).
     * Forge applies this conditionally only to domesurvival:dome_foundation,
     * then restores a simple WASTED-like sand/sandstone surface.
     */
    private static void clearV13ExternalFoundation(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int r = spec.surfaceRadius();
        double keepSq = (double) r * r;
        double legacyOuterSq = (r + 2.5) * (r + 2.5);

        for (int y = spec.foundationMinY(); y <= spec.foundationTopY(); y++) {
            for (int dx = -r - 3; dx <= r + 3; dx++) {
                for (int dz = -r - 3; dz <= r + 3; dz++) {
                    double h2 = dx * dx + dz * dz;
                    if (h2 > keepSq && h2 <= legacyOuterSq) {
                        put(plan, new BlockPoint(spec.centerX() + dx, y, spec.centerZ() + dz),
                                StructureMaterial.CLEAR_LEGACY_FOUNDATION);
                    }
                }
            }
        }
    }

    private static void addRibs(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        for (BlockPoint p : DomeShellPlanner.planOneBlockShell(spec)) {
            if (p.y() < spec.hemisphereCenterY()) continue;
            int dx = p.x() - spec.centerX();
            int dz = p.z() - spec.centerZ();
            if (dx == 0 || dz == 0) {
                put(plan, p, StructureMaterial.FRAME);
            }
        }
        put(plan, new BlockPoint(spec.centerX(), spec.topY(), spec.centerZ()), StructureMaterial.FRAME);
    }

    private static void addMidRing(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int ringY = spec.hemisphereCenterY() + spec.surfaceRadius() / 2;
        for (BlockPoint p : DomeShellPlanner.planOneBlockShell(spec)) {
            if (p.y() == ringY) {
                put(plan, p, StructureMaterial.FRAME);
            }
        }
    }

    /** New V1.2 airlock centered on the coarse-dirt trail (X=-515). */
    private static void addPathAlignedAirlock(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int cx = spec.airlockCenterX();
        int half = spec.airlockHalfWidth();
        int floor = spec.airlockFloorY();
        int ceiling = spec.airlockCeilingY();

        for (int z = spec.airlockStartZ(); z <= spec.airlockEndZ(); z++) {
            // Two buried foundation courses under the whole airlock.
            for (int y = spec.foundationMinY(); y < floor; y++) {
                for (int x = cx - half; x <= cx + half; x++) {
                    put(plan, new BlockPoint(x, y, z), StructureMaterial.FOUNDATION);
                }
            }

            // Keep the visible dirt trail continuous through the airlock.
            for (int x = cx - half; x <= cx + half; x++) {
                StructureMaterial floorMaterial = Math.abs(x - cx) <= 2
                        ? StructureMaterial.COARSE_DIRT
                        : StructureMaterial.FOUNDATION;
                put(plan, new BlockPoint(x, floor, z), floorMaterial);
                put(plan, new BlockPoint(x, ceiling, z), StructureMaterial.FRAME);
            }

            for (int y = spec.baseY(); y < ceiling; y++) {
                StructureMaterial wallMaterial = y <= spec.foundationTopY()
                        ? StructureMaterial.FOUNDATION
                        : StructureMaterial.GLASS;
                put(plan, new BlockPoint(cx - half, y, z), wallMaterial);
                put(plan, new BlockPoint(cx + half, y, z), wallMaterial);
            }
        }

        addOpenArch(plan, spec, spec.airlockStartZ());
        addOpenArch(plan, spec, spec.airlockEndZ());
    }

    /** Only the foundation-related pieces needed when upgrading an existing V1.2 airlock. */
    private static void addPathAlignedAirlockFoundation(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int cx = spec.airlockCenterX();
        int half = spec.airlockHalfWidth();
        int floor = spec.airlockFloorY();

        for (int z = spec.airlockStartZ(); z <= spec.airlockEndZ(); z++) {
            for (int y = spec.foundationMinY(); y < floor; y++) {
                for (int x = cx - half; x <= cx + half; x++) {
                    put(plan, new BlockPoint(x, y, z), StructureMaterial.FOUNDATION);
                }
            }
            put(plan, new BlockPoint(cx - half, floor, z), StructureMaterial.FOUNDATION);
            put(plan, new BlockPoint(cx + half, floor, z), StructureMaterial.FOUNDATION);
            for (int y = spec.baseY(); y <= spec.foundationTopY(); y++) {
                put(plan, new BlockPoint(cx - half, y, z), StructureMaterial.FOUNDATION);
                put(plan, new BlockPoint(cx + half, y, z), StructureMaterial.FOUNDATION);
            }
        }
    }

    private static void addOpenArch(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec, int z) {
        int cx = spec.airlockCenterX();
        int half = spec.airlockHalfWidth();
        int floor = spec.airlockFloorY();
        int ceiling = spec.airlockCeilingY();
        for (int y = floor; y <= ceiling; y++) {
            put(plan, new BlockPoint(cx - half, y, z), StructureMaterial.FRAME);
            put(plan, new BlockPoint(cx + half, y, z), StructureMaterial.FRAME);
        }
        for (int x = cx - half; x <= cx + half; x++) {
            put(plan, new BlockPoint(x, ceiling, z), StructureMaterial.FRAME);
        }
    }

    private static void carveCurrentAirlockOpening(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int shellZ = spec.airlockShellZ();
        int cx = spec.airlockCenterX();
        for (int z = shellZ - 3; z <= shellZ + 2; z++) {
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int y = spec.baseY(); y < spec.airlockCeilingY(); y++) {
                    put(plan, new BlockPoint(x, y, z), StructureMaterial.AIR);
                }
            }
        }
    }

    private static void removeLegacyAirlock(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int cx = spec.legacyAirlockCenterX();
        int half = spec.airlockHalfWidth();
        int floor = spec.airlockFloorY();
        int ceiling = spec.airlockCeilingY();

        for (int z = spec.legacyAirlockStartZ(); z <= spec.legacyAirlockEndZ(); z++) {
            // Remove old walls/roof/arches.
            for (int x = cx - half; x <= cx + half; x++) {
                for (int y = spec.baseY(); y <= ceiling; y++) {
                    put(plan, new BlockPoint(x, y, z), StructureMaterial.AIR);
                }
                // Restore a simple WASTED sand surface where the old frame floor was.
                put(plan, new BlockPoint(x, floor, z), StructureMaterial.SAND);
            }
        }
    }

    /** Re-seal the hole cut in the dome by V1.1's old centered airlock. */
    private static void restoreLegacyOpening(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int shellZ = spec.centerZ() + spec.surfaceRadius();
        Set<BlockPoint> shell = DomeShellPlanner.planOneBlockShell(spec);
        int midRingY = spec.hemisphereCenterY() + spec.surfaceRadius() / 2;

        for (BlockPoint p : shell) {
            if (p.x() < spec.centerX() - 3 || p.x() > spec.centerX() + 3) continue;
            if (p.z() < shellZ - 3 || p.z() > shellZ + 3) continue;
            if (p.y() < spec.baseY() || p.y() > spec.airlockCeilingY()) continue;

            StructureMaterial material = (p.x() == spec.centerX() || p.z() == spec.centerZ() || p.y() == midRingY)
                    ? StructureMaterial.FRAME
                    : StructureMaterial.GLASS;
            put(plan, p, material);
        }
    }

    /**
     * Exact small author-built object seen immediately beyond the old airlock.
     * Cleanup is conditional in Forge code, so natural sand/stone is left untouched.
     */
    private static void clearAuthorStructure(Map<BlockPoint, StructureMaterial> plan) {
        for (int x = -507; x <= -503; x++) {
            for (int z = -582; z <= -566; z++) {
                for (int y = 62; y <= 66; y++) {
                    put(plan, new BlockPoint(x, y, z), StructureMaterial.CLEAR_AUTHOR_BUILD);
                }
            }
        }
    }

    private static void put(Map<BlockPoint, StructureMaterial> plan, BlockPoint point, StructureMaterial material) {
        plan.put(point, material);
    }

    private static List<PlannedBlock> toList(Map<BlockPoint, StructureMaterial> plan) {
        List<PlannedBlock> out = new ArrayList<>(plan.size());
        for (Map.Entry<BlockPoint, StructureMaterial> entry : plan.entrySet()) {
            out.add(new PlannedBlock(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(out);
    }
}
