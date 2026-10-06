package com.wasted.domesurvival.forge.metro;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.airlock.AirlockControlPanelBlock;
import com.wasted.domesurvival.forge.airlock.AirlockControlPanelBlockEntity;
import com.wasted.domesurvival.forge.airlock.AirlockPanelRegistry;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Authored infrastructure added to a restored external metro station.
 *
 * Geometry comes from the fixed Stage-2 station layout:
 * restoration console = center(-13,+2,-6)
 * north station doorway = center X[-2..+2], Y[+1..+5], Z[-10]
 *
 * No world scan or chunk forcing is used.
 */
public final class MetroStationInfrastructure {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GATE_SIZE = 5;

    private MetroStationInfrastructure() {
    }

    public static boolean ensureRestoredEntrance(ServerLevel level, BlockPos restorationAnchor) {
        Layout layout = Layout.from(restorationAnchor);

        if (!allLoaded(level, layout)) {
            return false;
        }

        AirlockGateBlock gate = AirlockGateRegistry.AIRLOCK_GATE.get();
        boolean changed = false;

        if (!gate.isValidMaster(level, layout.gateMaster())) {
            // Replace only the authored 5x5 doorway cells. This is the same
            // opening that is AIR in the abandoned Stage-2 structure.
            for (int row = 0; row < GATE_SIZE; row++) {
                for (int col = 0; col < GATE_SIZE; col++) {
                    BlockPos pos = layout.gateMin().offset(col, row, 0);
                    level.setBlock(
                            pos,
                            gate.defaultBlockState()
                                    .setValue(AirlockGateBlock.FACING, Direction.NORTH),
                            Block.UPDATE_CLIENTS
                    );
                }
            }

            if (!gate.formGenerated5x5(level, layout.gateMin(), Direction.NORTH)) {
                LOGGER.warn("Could not form restored metro airlock gate at {}", layout.gateMin().toShortString());
                return false;
            }
            changed = true;
        }

        changed |= ensurePanel(level, layout.outsidePanel(), Direction.NORTH, layout.gateMaster());
        changed |= ensurePanel(level, layout.insidePanel(), Direction.SOUTH, layout.gateMaster());

        if (changed) {
            LOGGER.info(
                    "Restored metro entrance commissioned: gate={} outsidePanel={} insidePanel={}",
                    layout.gateMaster().toShortString(),
                    layout.outsidePanel().toShortString(),
                    layout.insidePanel().toShortString()
            );
        }
        return changed;
    }

    public static void removeRestoredEntrance(ServerLevel level, BlockPos restorationAnchor) {
        Layout layout = Layout.from(restorationAnchor);

        if (level.hasChunkAt(layout.outsidePanel())
                && level.getBlockState(layout.outsidePanel()).is(AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get())) {
            level.setBlockAndUpdate(layout.outsidePanel(), Blocks.AIR.defaultBlockState());
        }
        if (level.hasChunkAt(layout.insidePanel())
                && level.getBlockState(layout.insidePanel()).is(AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get())) {
            level.setBlockAndUpdate(layout.insidePanel(), Blocks.AIR.defaultBlockState());
        }

        // Replacing the first formed segment makes AirlockGateBlock safely
        // disassemble the remaining multiblock; the loop then clears all 25
        // authored doorway cells back to AIR.
        for (int row = 0; row < GATE_SIZE; row++) {
            for (int col = 0; col < GATE_SIZE; col++) {
                BlockPos pos = layout.gateMin().offset(col, row, 0);
                if (level.hasChunkAt(pos)
                        && level.getBlockState(pos).is(AirlockGateRegistry.AIRLOCK_GATE.get())) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static boolean ensurePanel(ServerLevel level,
                                       BlockPos pos,
                                       Direction facing,
                                       BlockPos gateMaster) {
        BlockState wanted = AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get()
                .defaultBlockState()
                .setValue(AirlockControlPanelBlock.FACING, facing)
                .setValue(AirlockControlPanelBlock.ACTIVE, false);

        boolean changed = false;
        BlockState current = level.getBlockState(pos);
        if (!current.is(AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get())
                || !current.hasProperty(AirlockControlPanelBlock.FACING)
                || current.getValue(AirlockControlPanelBlock.FACING) != facing) {
            level.setBlockAndUpdate(pos, wanted);
            changed = true;
        }

        BlockState state = level.getBlockState(pos);
        AirlockControlPanelBlock panelBlock = AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL.get();
        AirlockControlPanelBlockEntity panel = panelBlock.ensureBlockEntity(level, pos, state);
        if (!panel.isLinkedTo(level.dimension(), gateMaster)) {
            panel.bind(level, gateMaster);
            changed = true;
        }
        return changed;
    }

    private static boolean allLoaded(ServerLevel level, Layout layout) {
        if (!level.hasChunkAt(layout.outsidePanel()) || !level.hasChunkAt(layout.insidePanel())) {
            return false;
        }
        for (int row = 0; row < GATE_SIZE; row++) {
            for (int col = 0; col < GATE_SIZE; col++) {
                if (!level.hasChunkAt(layout.gateMin().offset(col, row, 0))) {
                    return false;
                }
            }
        }
        return true;
    }

    private record Layout(
            BlockPos gateMin,
            BlockPos gateMaster,
            BlockPos outsidePanel,
            BlockPos insidePanel
    ) {
        static Layout from(BlockPos anchor) {
            // center = anchor + (13,-2,+6)
            BlockPos center = anchor.offset(13, -2, 6);
            BlockPos gateMin = center.offset(-2, 1, -10);
            BlockPos gateMaster = gateMin.offset(2, 2, 0);

            // Both panels are mounted on the intact wall cell immediately left
            // of the 5-wide doorway: one on the stairs side and one inside.
            BlockPos outsidePanel = center.offset(-3, 2, -11);
            BlockPos insidePanel = center.offset(-3, 2, -9);
            return new Layout(
                    gateMin.immutable(),
                    gateMaster.immutable(),
                    outsidePanel.immutable(),
                    insidePanel.immutable()
            );
        }
    }
}
