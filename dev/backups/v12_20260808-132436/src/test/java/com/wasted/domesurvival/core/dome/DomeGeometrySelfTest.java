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

        check(bounds.classify(-506.0, 62.0, -641.0) == DomeZone.SURFACE_SKIRT, "spawn zone");
        check(bounds.isSafe(-506.0, -64.0, -641.0), "bottom of starter mine must be safe");
        check(!bounds.isSafe(-506.0, -65.0, -641.0), "below world minimum is outside V1 safe volume");
        check(bounds.isSafe(-461.0, 0.0, -641.0), "underground radius edge must be safe");
        check(!bounds.isSafe(-460.0, 0.0, -641.0), "one block outside underground radius must be unsafe");
        check(bounds.isSafe(-456.0, 62.0, -641.0), "surface radius edge at base must be safe");
        check(!bounds.isSafe(-455.0, 62.0, -641.0), "surface outside radius must be unsafe except airlock");
        check(bounds.isSafe(-506.0, 115.0, -641.0), "top shell coordinate must be safe");
        check(!bounds.isSafe(-506.0, 116.0, -641.0), "above dome must be unsafe");

        check(bounds.classify(-506.0, 63.0, -585.0) == DomeZone.AIRLOCK, "airlock outside dome must be classified");
        check(bounds.isSafe(-506.0, 63.0, -585.0), "V1.1 test airlock is temporarily safe");
        check(!bounds.isSafe(-500.0, 63.0, -585.0), "beside airlock must be outside");

        Set<BlockPoint> shell = DomeShellPlanner.planOneBlockShell(spec);
        check(shell.size() > 12_000, "shell unexpectedly small: " + shell.size());
        check(shell.size() < 25_000, "shell unexpectedly large: " + shell.size());
        check(shell.contains(new BlockPoint(-506, 115, -641)), "shell must include top");

        List<PlannedBlock> full = DomeStructurePlanner.planFullV11(spec);
        List<PlannedBlock> upgrade = DomeStructurePlanner.planV11Upgrade(spec);
        check(full.size() > shell.size(), "V1.1 full plan should include structure extras");
        check(upgrade.size() > 1_000, "V1.1 upgrade unexpectedly small: " + upgrade.size());
        check(has(full, new BlockPoint(-506, spec.topY(), -641), StructureMaterial.FRAME), "top must be frame");
        check(has(full, new BlockPoint(-506, spec.baseY(), spec.centerZ() + spec.surfaceRadius()), StructureMaterial.AIR),
                "south shell must be carved for airlock");
        check(has(full, new BlockPoint(spec.centerX() - spec.airlockHalfWidth(), spec.baseY(), spec.airlockEndZ()), StructureMaterial.FRAME),
                "outer airlock arch must be framed");

        System.out.println("DomeGeometrySelfTest: OK");
        System.out.println("center=" + spec.centerX() + "," + spec.baseY() + "," + spec.centerZ());
        System.out.println("surfaceRadius=" + spec.surfaceRadius());
        System.out.println("undergroundRadius=" + spec.undergroundRadius());
        System.out.println("topY=" + spec.topY());
        System.out.println("plannedShellBlocks=" + shell.size());
        System.out.println("plannedV11FullOps=" + full.size());
        System.out.println("plannedV11UpgradeOps=" + upgrade.size());
        System.out.println("airlockZ=" + spec.airlockStartZ() + ".." + spec.airlockEndZ());
    }

    private static boolean has(List<PlannedBlock> blocks, BlockPoint point, StructureMaterial material) {
        return blocks.stream().anyMatch(b -> b.point().equals(point) && b.material() == material);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
