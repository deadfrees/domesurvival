package com.wasted.domesurvival.core.dome;

import java.util.List;
import java.util.Set;

public final class DomeGeometrySelfTest {
    public static void main(String[] args) {
        DomeSpec spec = DomeSpec.wastedV1();
        DomeBounds bounds = new DomeBounds(spec);

        check(spec.centerX() == -506, "center X");
        check(spec.baseY() == 62, "base Y");
        check(spec.centerZ() == -641, "center Z");
        check(spec.topY() == 115, "dome top Y");

        check(spec.airlockCenterX() == -515, "airlock must align with dirt trail");
        check(spec.airlockShellZ() == -592, "shifted shell Z");
        check(spec.airlockStartZ() == -596, "airlock start Z");
        check(spec.airlockEndZ() == -582, "airlock end Z");
        check(spec.foundationMinY() == 59, "foundation depth");
        check(spec.foundationTopY() == 63, "visible foundation top");

        check(bounds.classify(-506.0, 62.0, -641.0) == DomeZone.SURFACE_SKIRT, "spawn zone");
        check(bounds.isSafe(-506.0, -64.0, -641.0), "bottom of starter mine must be safe");
        check(!bounds.isSafe(-506.0, -65.0, -641.0), "below world minimum is outside V1 safe volume");
        check(bounds.isSafe(-461.0, 0.0, -641.0), "underground radius edge must be safe");
        check(!bounds.isSafe(-460.0, 0.0, -641.0), "one block outside underground radius must be unsafe");
        check(bounds.isSafe(-456.0, 62.0, -641.0), "surface radius edge at base must be safe");
        check(!bounds.isSafe(-455.0, 62.0, -641.0), "surface outside radius must be unsafe except airlock");
        check(bounds.isSafe(-506.0, 115.0, -641.0), "top shell coordinate must be safe");
        check(!bounds.isSafe(-506.0, 116.0, -641.0), "above dome must be unsafe");

        check(bounds.classify(-515.0, 63.0, -585.0) == DomeZone.AIRLOCK, "new airlock must be classified");
        check(bounds.isSafe(-515.0, 63.0, -585.0), "V1.2 test airlock is temporarily safe");
        check(!bounds.isSafe(-506.0, 63.0, -585.0), "old airlock position must no longer be safe");

        Set<BlockPoint> shell = DomeShellPlanner.planOneBlockShell(spec);
        check(shell.size() > 12_000, "shell unexpectedly small: " + shell.size());
        check(shell.size() < 25_000, "shell unexpectedly large: " + shell.size());
        check(shell.contains(new BlockPoint(-506, 115, -641)), "shell must include top");

        List<PlannedBlock> full = DomeStructurePlanner.planFullV13(spec);
        List<PlannedBlock> fromV1 = DomeStructurePlanner.planV13UpgradeFromV1(spec);
        List<PlannedBlock> fromV11 = DomeStructurePlanner.planV13UpgradeFromV11(spec);
        List<PlannedBlock> fromV12 = DomeStructurePlanner.planV13UpgradeFromV12(spec);
        check(full.size() > shell.size(), "V1.3 full plan should include structure extras");
        check(fromV1.size() > 1_000, "V1 -> V1.3 upgrade unexpectedly small: " + fromV1.size());
        check(fromV11.size() > 1_000, "V1.1 -> V1.3 upgrade unexpectedly small: " + fromV11.size());
        check(has(full, new BlockPoint(-506, spec.topY(), -641), StructureMaterial.FRAME), "top must be frame");
        check(has(full, new BlockPoint(spec.airlockCenterX(), spec.airlockFloorY(), -590), StructureMaterial.COARSE_DIRT),
                "dirt trail must continue through new airlock");
        check(has(full, new BlockPoint(spec.airlockCenterX() - spec.airlockHalfWidth(), spec.foundationMinY(), -590), StructureMaterial.FOUNDATION),
                "airlock must have buried foundation");
        check(has(full, new BlockPoint(spec.centerX() + spec.surfaceRadius(), spec.foundationTopY(), spec.centerZ()), StructureMaterial.FOUNDATION),
                "dome must have a visible foundation plinth");
        check(has(fromV12, new BlockPoint(spec.centerX() + spec.surfaceRadius(), spec.foundationTopY(), spec.centerZ()), StructureMaterial.FOUNDATION),
                "V1.2 -> V1.3 upgrade must add visible foundation");
        check(has(fromV12, new BlockPoint(spec.airlockCenterX() - spec.airlockHalfWidth(), spec.baseY(), -589), StructureMaterial.FOUNDATION),
                "V1.2 -> V1.3 upgrade must add visible airlock foundation");
        check(has(fromV11, new BlockPoint(spec.legacyAirlockCenterX(), spec.baseY() + 1, -585), StructureMaterial.AIR),
                "old V1.1 airlock must be removed");
        check(has(fromV11, new BlockPoint(-505, 64, -575), StructureMaterial.CLEAR_AUTHOR_BUILD),
                "author structure cleanup box missing");

        System.out.println("DomeGeometrySelfTest: OK");
        System.out.println("center=" + spec.centerX() + "," + spec.baseY() + "," + spec.centerZ());
        System.out.println("surfaceRadius=" + spec.surfaceRadius());
        System.out.println("undergroundRadius=" + spec.undergroundRadius());
        System.out.println("topY=" + spec.topY());
        System.out.println("plannedShellBlocks=" + shell.size());
        System.out.println("plannedV13FullOps=" + full.size());
        System.out.println("plannedV1toV13Ops=" + fromV1.size());
        System.out.println("plannedV11toV13Ops=" + fromV11.size());
        System.out.println("plannedV12toV13Ops=" + fromV12.size());
        System.out.println("airlockX=" + spec.airlockCenterX());
        System.out.println("airlockZ=" + spec.airlockStartZ() + ".." + spec.airlockEndZ());
        System.out.println("foundationY=" + spec.foundationMinY() + ".." + spec.foundationTopY());
    }

    private static boolean has(List<PlannedBlock> blocks, BlockPoint point, StructureMaterial material) {
        return blocks.stream().anyMatch(b -> b.point().equals(point) && b.material() == material);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
