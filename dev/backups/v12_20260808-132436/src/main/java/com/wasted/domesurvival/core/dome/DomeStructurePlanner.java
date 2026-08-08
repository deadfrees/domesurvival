package com.wasted.domesurvival.core.dome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plans V1.1 visual structure without touching Minecraft classes. */
public final class DomeStructurePlanner {
    private DomeStructurePlanner() {
    }

    public static List<PlannedBlock> planFullV11(DomeSpec spec) {
        LinkedHashMap<BlockPoint, StructureMaterial> plan = new LinkedHashMap<>();
        for (BlockPoint p : DomeShellPlanner.planOneBlockShell(spec)) {
            put(plan, p, StructureMaterial.GLASS);
        }
        applyV11Extras(plan, spec);
        return toList(plan);
    }

    /** Upgrade an already-generated V1 glass shell to V1.1. */
    public static List<PlannedBlock> planV11Upgrade(DomeSpec spec) {
        LinkedHashMap<BlockPoint, StructureMaterial> plan = new LinkedHashMap<>();
        applyV11Extras(plan, spec);
        return toList(plan);
    }

    private static void applyV11Extras(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        addFoundation(plan, spec);
        addRibs(plan, spec);
        addMidRing(plan, spec);
        addAirlockPassage(plan, spec);
        carveAirlockOpening(plan, spec);
    }

    private static void addFoundation(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int r = spec.surfaceRadius();
        double innerSq = (r - 1.5) * (r - 1.5);
        double outerSq = (r + 1.5) * (r + 1.5);
        for (int y = spec.baseY() - 1; y <= spec.baseY(); y++) {
            for (int dx = -r - 2; dx <= r + 2; dx++) {
                for (int dz = -r - 2; dz <= r + 2; dz++) {
                    double h2 = dx * dx + dz * dz;
                    if (h2 >= innerSq && h2 <= outerSq) {
                        put(plan, new BlockPoint(spec.centerX() + dx, y, spec.centerZ() + dz), StructureMaterial.FRAME);
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
            // Two perpendicular vertical meridian planes = four visible ribs.
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

    private static void addAirlockPassage(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int cx = spec.centerX();
        int half = spec.airlockHalfWidth();
        int floor = spec.airlockFloorY();
        int ceiling = spec.airlockCeilingY();

        for (int z = spec.airlockStartZ(); z <= spec.airlockEndZ(); z++) {
            for (int x = cx - half; x <= cx + half; x++) {
                put(plan, new BlockPoint(x, floor, z), StructureMaterial.FRAME);
                put(plan, new BlockPoint(x, ceiling, z), StructureMaterial.FRAME);
            }
            for (int y = spec.baseY(); y < ceiling; y++) {
                put(plan, new BlockPoint(cx - half, y, z), StructureMaterial.GLASS);
                put(plan, new BlockPoint(cx + half, y, z), StructureMaterial.GLASS);
            }
        }

        // Frame arches mark the inner and outer future door positions, but remain open in V1.1.
        addOpenArch(plan, spec, spec.airlockStartZ());
        addOpenArch(plan, spec, spec.airlockEndZ());
    }

    private static void addOpenArch(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec, int z) {
        int cx = spec.centerX();
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

    private static void carveAirlockOpening(Map<BlockPoint, StructureMaterial> plan, DomeSpec spec) {
        int shellZ = spec.centerZ() + spec.surfaceRadius();
        for (int z = shellZ - 2; z <= shellZ + 2; z++) {
            for (int x = spec.centerX() - 2; x <= spec.centerX() + 2; x++) {
                for (int y = spec.baseY(); y < spec.airlockCeilingY(); y++) {
                    put(plan, new BlockPoint(x, y, z), StructureMaterial.AIR);
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
