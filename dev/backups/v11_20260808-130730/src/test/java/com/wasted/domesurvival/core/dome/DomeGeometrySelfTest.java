package com.wasted.domesurvival.core.dome;

import java.util.Set;

public final class DomeGeometrySelfTest {
    public static void main(String[] args) {
        DomeSpec spec = DomeSpec.wastedV1();
        DomeBounds bounds = new DomeBounds(spec);

        check(spec.centerX() == -506, "center X");
        check(spec.baseY() == 62, "base Y");
        check(spec.centerZ() == -641, "center Z");
        check(spec.topY() == 115, "dome top Y");

        check(bounds.isSafe(-506.0, 62.0, -641.0), "spawn must be safe");
        check(bounds.isSafe(-506.0, -64.0, -641.0), "bottom of starter mine must be safe");
        check(!bounds.isSafe(-506.0, -65.0, -641.0), "below world minimum is outside V1 safe volume");
        check(bounds.isSafe(-461.0, 0.0, -641.0), "underground radius edge must be safe");
        check(!bounds.isSafe(-460.0, 0.0, -641.0), "one block outside underground radius must be unsafe");
        check(bounds.isSafe(-456.0, 62.0, -641.0), "surface radius edge at base must be safe");
        check(!bounds.isSafe(-455.0, 62.0, -641.0), "surface outside radius must be unsafe");
        check(bounds.isSafe(-506.0, 115.0, -641.0), "top shell coordinate must be safe");
        check(!bounds.isSafe(-506.0, 116.0, -641.0), "above dome must be unsafe");

        Set<BlockPoint> shell = DomeShellPlanner.planOneBlockShell(spec);
        check(shell.size() > 12_000, "shell unexpectedly small: " + shell.size());
        check(shell.size() < 25_000, "shell unexpectedly large: " + shell.size());
        check(shell.contains(new BlockPoint(-506, 115, -641)), "shell must include top");

        System.out.println("DomeGeometrySelfTest: OK");
        System.out.println("center=" + spec.centerX() + "," + spec.baseY() + "," + spec.centerZ());
        System.out.println("surfaceRadius=" + spec.surfaceRadius());
        System.out.println("undergroundRadius=" + spec.undergroundRadius());
        System.out.println("topY=" + spec.topY());
        System.out.println("plannedShellBlocks=" + shell.size());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
