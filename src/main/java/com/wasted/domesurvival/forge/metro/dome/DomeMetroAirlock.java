package com.wasted.domesurvival.forge.metro.dome;

import com.wasted.domesurvival.forge.airlock.AirlockControlPanelBlock;
import com.wasted.domesurvival.forge.airlock.AirlockControlPanelBlockEntity;
import com.wasted.domesurvival.forge.airlock.AirlockPanelRegistry;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateMotion;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import com.wasted.domesurvival.forge.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exact Stage-4 adapter for the project's real V56/V58 airlock system.
 *
 * V1.10 deliberately does NOT scan the world, clone NBT, use reflection, or guess
 * registry ids. It uses the same registered gate and wall-panel classes as the
 * normal starter-Dome exit and binds two panels directly to one generated gate.
 */
public final class DomeMetroAirlock {
    private static final int GATE_SIZE = 5;

    private DomeMetroAirlock() {
    }

    public static InstallResult install(ServerLevel level,
                                        BlockPos entrance,
                                        Direction forward,
                                        Direction right,
                                        int gateForwardOffset) {
        if (!forward.getAxis().isHorizontal() || !right.getAxis().isHorizontal()) {
            return new InstallResult(false, null, null, null, "invalid_direction");
        }

        AirlockGateBlock gate = AirlockGateRegistry.AIRLOCK_GATE.get();
        BlockPos gateCenter = entrance.relative(forward, gateForwardOffset);
        int minY = entrance.getY();

        BlockPos expectedMaster = new BlockPos(gateCenter.getX(), minY + 2, gateCenter.getZ());
        BlockPos masterPos = expectedMaster;

        if (!gate.isValidMaster(level, expectedMaster)) {
            BlockState unformed = gate.defaultBlockState()
                    .setValue(AirlockGateBlock.FACING, forward)
                    .setValue(AirlockGateBlock.FORMED, false)
                    .setValue(AirlockGateBlock.MASTER, false)
                    .setValue(AirlockGateBlock.MOTION, AirlockGateMotion.CLOSED);

            if (forward.getAxis() == Direction.Axis.X) {
                int minZ = gateCenter.getZ() - 2;
                for (int z = minZ; z < minZ + GATE_SIZE; z++) {
                    for (int y = minY; y < minY + GATE_SIZE; y++) {
                        level.setBlock(new BlockPos(gateCenter.getX(), y, z), unformed, Block.UPDATE_CLIENTS);
                    }
                }
            }
            else {
                int minX = gateCenter.getX() - 2;
                for (int x = minX; x < minX + GATE_SIZE; x++) {
                    for (int y = minY; y < minY + GATE_SIZE; y++) {
                        level.setBlock(new BlockPos(x, y, gateCenter.getZ()), unformed, Block.UPDATE_CLIENTS);
                    }
                }
            }

            AirlockGateBlock.CommissionedGate commissioned =
                    gate.commissionLargestSquare(level, expectedMaster, forward);
            if (commissioned == null || commissioned.size() != GATE_SIZE) {
                return new InstallResult(false, null, null, null, "gate_form_failed");
            }
            masterPos = commissioned.masterPos();
            if (!gate.isValidMaster(level, masterPos)) {
                return new InstallResult(false, null, null, null, "gate_master_invalid");
            }
        }

        // Both controls are mounted on the same inner tunnel wall but on opposite
        // sides of the sliding gate: one reachable from the Dome, one from Metro.
        Direction panelFacing = right.getOpposite();
        BlockPos domePanelPos = local(entrance, right, forward, 3, 1, gateForwardOffset - 2);
        BlockPos metroPanelPos = local(entrance, right, forward, 3, 1, gateForwardOffset + 2);
        BlockPos domeSupport = domePanelPos.relative(panelFacing.getOpposite());
        BlockPos metroSupport = metroPanelPos.relative(panelFacing.getOpposite());

        // Same non-survival Dome material used by the normal structure. This also
        // guarantees canSurvive() for the wall-mounted control panels.
        level.setBlock(domeSupport, ModBlocks.DOME_FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(metroSupport, ModBlocks.DOME_FRAME.get().defaultBlockState(), Block.UPDATE_ALL);

        if (!installAndBindPanel(level, domePanelPos, panelFacing, masterPos)) {
            return new InstallResult(false, masterPos, null, null, "dome_panel_bind_failed");
        }
        if (!installAndBindPanel(level, metroPanelPos, panelFacing, masterPos)) {
            return new InstallResult(false, masterPos, domePanelPos, null, "metro_panel_bind_failed");
        }

        return new InstallResult(true, masterPos, domePanelPos, metroPanelPos, "ok");
    }

    private static boolean installAndBindPanel(ServerLevel level,
                                               BlockPos panelPos,
                                               Direction facing,
                                               BlockPos gateMasterPos) {
        BlockState panelState = AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get()
                .defaultBlockState()
                .setValue(AirlockControlPanelBlock.FACING, facing)
                .setValue(AirlockControlPanelBlock.ACTIVE, false);

        level.setBlock(panelPos, panelState, Block.UPDATE_ALL);
        BlockEntity raw = level.getBlockEntity(panelPos);
        AirlockControlPanelBlockEntity panel;
        if (raw instanceof AirlockControlPanelBlockEntity existing) {
            panel = existing;
        }
        else {
            panel = ((AirlockControlPanelBlock) AirlockPanelRegistry.block())
                    .ensureBlockEntity(level, panelPos, panelState);
        }

        panel.bind(level, gateMasterPos);
        return panel.isLinkedTo(level.dimension(), gateMasterPos);
    }

    private static BlockPos local(BlockPos origin, Direction right, Direction forward,
                                  int r, int u, int f) {
        return origin.relative(right, r).relative(forward, f).above(u);
    }

    public record InstallResult(boolean success,
                                BlockPos masterPos,
                                BlockPos domePanelPos,
                                BlockPos metroPanelPos,
                                String detail) {
    }
}
