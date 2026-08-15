package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateMotion;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * V58 adapter that installs the already-stable V57 multiblock gates into the
 * fixed starter-dome airlock.
 *
 * This class contains only deterministic, coordinate-based starter-structure
 * migration. Normal player-built gates remain fully generic and are not
 * special-cased anywhere in the gate controller.
 */
public final class StarterDomeAirlockV58 {
    private static final Direction GATE_FACING = Direction.SOUTH;
    private static final Direction DEFAULT_PANEL_FACING = Direction.WEST;

    private StarterDomeAirlockV58() {
    }

    public static BlockPos innerGateMasterPos() {
        DomeSpec spec = DomeSpec.wastedV1();
        return new BlockPos(
                spec.airlockCenterX(),
                spec.airlockDoorMinY() + 2,
                spec.innerDoorZ()
        );
    }

    public static BlockPos outerGateMasterPos() {
        DomeSpec spec = DomeSpec.wastedV1();
        return new BlockPos(
                spec.airlockCenterX(),
                spec.airlockDoorMinY() + 2,
                spec.outerDoorZ()
        );
    }

    public static BlockPos gateMasterPos(com.wasted.domesurvival.core.airlock.AirlockDoor side) {
        return side == com.wasted.domesurvival.core.airlock.AirlockDoor.INNER
                ? innerGateMasterPos()
                : outerGateMasterPos();
    }

    public static boolean isInstalled(ServerLevel level) {
        AirlockGateBlock gate = AirlockGateRegistry.AIRLOCK_GATE.get();
        return gate.isValidMaster(level, innerGateMasterPos())
                && gate.isValidMaster(level, outerGateMasterPos());
    }

    public static boolean isGateClosed(ServerLevel level,
                                       com.wasted.domesurvival.core.airlock.AirlockDoor side) {
        BlockPos masterPos = gateMasterPos(side);
        BlockState state = level.getBlockState(masterPos);
        return state.getBlock() instanceof AirlockGateBlock gate
                && gate.isValidMaster(level, masterPos)
                && state.getValue(AirlockGateBlock.MOTION) == AirlockGateMotion.CLOSED;
    }

    public static InstallResult install(ServerLevel level) {
        DomeSpec spec = DomeSpec.wastedV1();

        PanelTarget innerChamberPanel = resolvePanelTarget(
                level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerPanelZ())
        );
        PanelTarget innerDomePanel = resolvePanelTarget(
                level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.innerDomePanelZ())
        );
        PanelTarget outerChamberPanel = resolvePanelTarget(
                level,
                new BlockPos(spec.airlockPanelX(), spec.airlockPanelY(), spec.outerPanelZ())
        );

        if (innerChamberPanel == null || innerDomePanel == null || outerChamberPanel == null) {
            return InstallResult.PANEL_SPACE_BLOCKED;
        }

        AirlockGateBlock gate = AirlockGateRegistry.AIRLOCK_GATE.get();

        BlockPos innerMaster = installGate(level, gate, spec.innerDoorZ());
        if (innerMaster == null) {
            return InstallResult.INNER_GATE_FAILED;
        }

        BlockPos outerMaster = installGate(level, gate, spec.outerDoorZ());
        if (outerMaster == null) {
            return InstallResult.OUTER_GATE_FAILED;
        }

        if (!installAndBindPanel(level, innerChamberPanel, innerMaster)
                || !installAndBindPanel(level, innerDomePanel, innerMaster)
                || !installAndBindPanel(level, outerChamberPanel, outerMaster)) {
            return InstallResult.PANEL_BIND_FAILED;
        }

        AirlockGateBlock.InterlockPairResult pairResult =
                gate.pairInterlock(level, innerMaster, outerMaster);
        if (pairResult != AirlockGateBlock.InterlockPairResult.PAIRED) {
            return InstallResult.INTERLOCK_FAILED;
        }

        // The V58 physical gates are authoritative from now on. Keep the old
        // SavedData mirror normalized only for backwards-compatible commands.
        DomeSavedData.get(level).resetAirlock();
        return InstallResult.SUCCESS;
    }

    @Nullable
    private static BlockPos installGate(ServerLevel level, AirlockGateBlock gate, int z) {
        DomeSpec spec = DomeSpec.wastedV1();
        BlockPos expectedMaster = new BlockPos(
                spec.airlockCenterX(),
                spec.airlockDoorMinY() + 2,
                z
        );

        if (gate.isValidMaster(level, expectedMaster)) {
            BlockState state = level.getBlockState(expectedMaster);
            if (state.getValue(AirlockGateBlock.MOTION) != AirlockGateMotion.CLOSED) {
                return null;
            }
            return expectedMaster;
        }

        BlockState unformed = gate.defaultBlockState()
                .setValue(AirlockGateBlock.FACING, GATE_FACING)
                .setValue(AirlockGateBlock.FORMED, false)
                .setValue(AirlockGateBlock.MASTER, false)
                .setValue(AirlockGateBlock.MOTION, AirlockGateMotion.CLOSED);

        int minX = spec.airlockCenterX() - spec.airlockDoorHalfWidth();
        int minY = spec.airlockDoorMinY();

        for (int x = minX; x <= minX + 4; x++) {
            for (int y = minY; y <= minY + 4; y++) {
                level.setBlock(
                        new BlockPos(x, y, z),
                        unformed,
                        Block.UPDATE_CLIENTS
                );
            }
        }

        BlockPos minCorner = new BlockPos(minX, minY, z);
        return gate.formGenerated5x5(level, minCorner, GATE_FACING)
                ? expectedMaster
                : null;
    }

    @Nullable
    private static PanelTarget resolvePanelTarget(ServerLevel level, BlockPos supportPos) {
        // Preserve an already-mounted V53F/V56 panel if the player migrated it
        // manually before V58. Its exact side is kept and it is simply rebound.
        for (Direction outward : Direction.Plane.HORIZONTAL) {
            BlockPos candidatePos = supportPos.relative(outward);
            BlockState candidate = level.getBlockState(candidatePos);
            if (candidate.is(AirlockPanelRegistry.block())
                    && candidate.hasProperty(AirlockControlPanelBlock.FACING)
                    && candidate.getValue(AirlockControlPanelBlock.FACING) == outward) {
                return new PanelTarget(supportPos, candidatePos, outward);
            }
        }

        BlockPos defaultPanelPos = supportPos.relative(DEFAULT_PANEL_FACING);
        BlockState targetState = level.getBlockState(defaultPanelPos);
        if (!targetState.canBeReplaced() && !targetState.is(AirlockPanelRegistry.block())) {
            return null;
        }

        return new PanelTarget(supportPos, defaultPanelPos, DEFAULT_PANEL_FACING);
    }

    private static boolean installAndBindPanel(ServerLevel level,
                                               PanelTarget target,
                                               BlockPos gateMasterPos) {
        // At panel height (Y=baseY+1) the airlock side wall is foundation in
        // the authoritative DomeStructurePlanner. This also replaces the old
        // full-block AIRLOCK_PANEL placeholder with the normal wall material.
        level.setBlock(
                target.supportPos(),
                ModBlocks.DOME_FOUNDATION.get().defaultBlockState(),
                Block.UPDATE_ALL
        );

        BlockState panelState = AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get()
                .defaultBlockState()
                .setValue(AirlockControlPanelBlock.FACING, target.facing())
                .setValue(AirlockControlPanelBlock.ACTIVE, false);

        level.setBlock(target.panelPos(), panelState, Block.UPDATE_ALL);

        BlockEntity raw = level.getBlockEntity(target.panelPos());
        AirlockControlPanelBlockEntity panel;
        if (raw instanceof AirlockControlPanelBlockEntity existing) {
            panel = existing;
        } else {
            panel = ((AirlockControlPanelBlock) AirlockPanelRegistry.block())
                    .ensureBlockEntity(level, target.panelPos(), panelState);
        }

        panel.bind(level, gateMasterPos);
        return panel.isLinkedTo(level.dimension(), gateMasterPos);
    }

    private record PanelTarget(BlockPos supportPos, BlockPos panelPos, Direction facing) {
    }

    public enum InstallResult {
        SUCCESS,
        PANEL_SPACE_BLOCKED,
        INNER_GATE_FAILED,
        OUTER_GATE_FAILED,
        PANEL_BIND_FAILED,
        INTERLOCK_FAILED
    }
}
