package com.wasted.domesurvival.core.dome;

import com.wasted.domesurvival.core.airlock.AirlockDoor;
import com.wasted.domesurvival.core.airlock.AirlockPressure;
import com.wasted.domesurvival.core.airlock.AirlockState;
import com.wasted.domesurvival.core.airlock.AirlockStateMachine;
import com.wasted.domesurvival.core.airlock.AirlockTransition;

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
        check(spec.innerDoorZ() == -595, "inner shutter Z");
        check(spec.outerDoorZ() == -583, "outer shutter Z");
        check(spec.innerPanelZ() == -594, "inner chamber-side panel Z");
        check(spec.innerDomePanelZ() == -596, "inner dome-side panel Z");
        check(spec.outerPanelZ() == -584, "outer panel Z");
        check(spec.foundationMinY() == 59, "foundation depth");
        check(spec.foundationTopY() == 63, "visible foundation top");

        DomeSpec moved = spec.at(128, 74, -320);
        DomeBounds movedBounds = new DomeBounds(moved);
        check(moved.centerX() == 128 && moved.baseY() == 74 && moved.centerZ() == -320,
                "dynamic LastWorld anchor");
        check(moved.surfaceRadius() == spec.surfaceRadius()
                        && moved.undergroundRadius() == spec.undergroundRadius(),
                "dynamic anchor must preserve dome geometry");
        check(moved.airlockCenterX() - moved.centerX()
                        == spec.airlockCenterX() - spec.centerX(),
                "dynamic airlock X offset");
        check(moved.airlockShellZ() - moved.centerZ()
                        == spec.airlockShellZ() - spec.centerZ(),
                "dynamic airlock Z offset");
        check(movedBounds.classify(128.0, 74.0, -320.0) == DomeZone.SURFACE_SKIRT,
                "dynamic spawn zone");

        check(bounds.classify(-506.0, 62.0, -641.0) == DomeZone.SURFACE_SKIRT, "spawn zone");
        check(bounds.isSafe(-506.0, -64.0, -641.0), "bottom of starter mine must be safe");
        check(!bounds.isSafe(-506.0, -65.0, -641.0), "below world minimum is outside safe volume");
        check(bounds.isSafe(-461.0, 0.0, -641.0), "underground radius edge must be safe");
        check(!bounds.isSafe(-460.0, 0.0, -641.0), "one block outside underground radius must be unsafe");
        check(bounds.classify(-515.0, 63.0, -585.0) == DomeZone.AIRLOCK, "airlock classification");
        check(!DomeZone.AIRLOCK.isSafe(), "airlock must use runtime pressure, not static safety");

        Set<BlockPoint> shell = DomeShellPlanner.planOneBlockShell(spec);
        List<PlannedBlock> full = DomeStructurePlanner.planFullV23(spec);
        List<PlannedBlock> v2Upgrade = DomeStructurePlanner.planV2UpgradeFromV14(spec);
        List<PlannedBlock> v23Upgrade = DomeStructurePlanner.planV23UpgradeFromV2(spec);

        check(shell.size() > 12_000, "shell unexpectedly small: " + shell.size());
        boolean hasVoxelizedOuterEdge = false;
        long playableRadiusSqr = (long) spec.surfaceRadius() * spec.surfaceRadius();
        long outerDiameter = 2L * spec.surfaceRadius() + 1L;
        for (BlockPoint point : shell) {
            long dx = (long) point.x() - spec.centerX();
            long dz = (long) point.z() - spec.centerZ();
            long horizontalSqr = dx * dx + dz * dz;
            if (horizontalSqr > playableRadiusSqr) {
                hasVoxelizedOuterEdge = true;
            }
            check(4L * horizontalSqr <= outerDiameter * outerDiameter,
                    "shell exceeds the permitted half-block placement band: " + point);
        }
        check(hasVoxelizedOuterEdge,
                "geometry regression: test no longer exercises the voxelized outer shell edge");
        check(has(full, new BlockPoint(spec.airlockCenterX(), spec.airlockDoorMinY(), spec.innerDoorZ()),
                StructureMaterial.AIRLOCK_DOOR), "inner shutter missing");
        check(has(full, new BlockPoint(spec.airlockCenterX(), spec.airlockDoorMaxY(), spec.outerDoorZ()),
                StructureMaterial.AIRLOCK_DOOR), "outer shutter missing");
        check(has(full, new BlockPoint(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerPanelZ()),
                StructureMaterial.AIRLOCK_PANEL), "inner panel missing");
        check(has(full, new BlockPoint(spec.airlockPanelX(), spec.airlockPanelY(), spec.outerPanelZ()),
                StructureMaterial.AIRLOCK_PANEL), "outer panel missing");
        check(has(full, new BlockPoint(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerDomePanelZ()),
                StructureMaterial.AIRLOCK_PANEL), "dome-side inner panel missing");
        check(v2Upgrade.size() == 52, "V1.4 -> V2 should add 50 shutter + 2 panel blocks: " + v2Upgrade.size());
        check(v23Upgrade.size() == 1, "V2 -> V2.3 should add exactly one dome-side panel: " + v23Upgrade.size());

        AirlockState state = AirlockState.initial();
        check(state.breathable(), "initial chamber must be breathable");

        AirlockTransition outerOpen = AirlockStateMachine.toggle(state, AirlockDoor.OUTER);
        check(outerOpen.allowed(), "outer should open from sealed state");
        check(outerOpen.state().outerOpen(), "outer must be open");
        check(outerOpen.state().pressure() == AirlockPressure.DEPRESSURIZED, "outer opening must vent chamber");
        check(!outerOpen.state().breathable(), "vented chamber must not be breathable");

        AirlockTransition deniedInner = AirlockStateMachine.toggle(outerOpen.state(), AirlockDoor.INNER);
        check(!deniedInner.allowed(), "inner must be interlocked while outer is open");

        AirlockTransition outerClose = AirlockStateMachine.toggle(outerOpen.state(), AirlockDoor.OUTER);
        check(outerClose.allowed() && !outerClose.state().outerOpen(), "outer must close");
        check(outerClose.state().pressure() == AirlockPressure.PRESSURIZED,
                "closing outer shutter must restore chamber pressure");
        check(outerClose.state().breathable(), "closed outer shutter must make chamber breathable");

        AirlockTransition innerOpen = AirlockStateMachine.toggle(outerClose.state(), AirlockDoor.INNER);
        check(innerOpen.allowed(), "inner should open after outer is closed");
        check(innerOpen.state().innerOpen(), "inner must be open");
        check(innerOpen.state().pressure() == AirlockPressure.PRESSURIZED, "inner opening must pressurize");
        check(innerOpen.state().breathable(), "pressurized chamber must be breathable");

        AirlockTransition deniedOuter = AirlockStateMachine.toggle(innerOpen.state(), AirlockDoor.OUTER);
        check(!deniedOuter.allowed(), "outer must be interlocked while inner is open");

        System.out.println("DomeGeometrySelfTest: OK");
        System.out.println("plannedShellBlocks=" + shell.size());
        System.out.println("plannedV23FullOps=" + full.size());
        System.out.println("plannedV14toV2Ops=" + v2Upgrade.size());
        System.out.println("plannedV2toV23Ops=" + v23Upgrade.size());
        System.out.println("airlockDoorsZ=" + spec.innerDoorZ() + "," + spec.outerDoorZ());
        System.out.println("airlockPanelsZ=" + spec.innerDomePanelZ() + "," + spec.innerPanelZ() + "," + spec.outerPanelZ());
        System.out.println("AirlockStateMachine: OK");
    }

    private static boolean has(List<PlannedBlock> blocks, BlockPoint point, StructureMaterial material) {
        return blocks.stream().anyMatch(b -> b.point().equals(point) && b.material() == material);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
