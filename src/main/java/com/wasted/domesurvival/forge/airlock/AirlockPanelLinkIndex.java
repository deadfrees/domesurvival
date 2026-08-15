package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.forge.airlock.gate.AirlockGateMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Runtime-only reverse index for loaded V56 panels.
 *
 * Persistent ownership remains panel -> gate. This cache only lets a gate push
 * its visual state to every currently loaded panel without scanning chunks or
 * ticking panel block entities.
 */
public final class AirlockPanelLinkIndex {
    private static final Map<ServerLevel, Map<BlockPos, Set<BlockPos>>> INDEX = new WeakHashMap<>();

    private AirlockPanelLinkIndex() {
    }

    public static void register(ServerLevel level, BlockPos gateMasterPos, BlockPos panelPos) {
        INDEX.computeIfAbsent(level, ignored -> new HashMap<>())
                .computeIfAbsent(gateMasterPos.immutable(), ignored -> new HashSet<>())
                .add(panelPos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos gateMasterPos, BlockPos panelPos) {
        Map<BlockPos, Set<BlockPos>> byGate = INDEX.get(level);
        if (byGate == null) {
            return;
        }

        Set<BlockPos> panels = byGate.get(gateMasterPos);
        if (panels == null) {
            return;
        }

        panels.remove(panelPos);
        if (panels.isEmpty()) {
            byGate.remove(gateMasterPos);
        }
        if (byGate.isEmpty()) {
            INDEX.remove(level);
        }
    }

    public static void syncGate(ServerLevel level, BlockPos gateMasterPos, AirlockGateMotion motion) {
        Map<BlockPos, Set<BlockPos>> byGate = INDEX.get(level);
        if (byGate == null) {
            return;
        }

        Set<BlockPos> panels = byGate.get(gateMasterPos);
        if (panels == null || panels.isEmpty()) {
            return;
        }

        boolean active = motion != AirlockGateMotion.CLOSED;
        for (BlockPos panelPos : new ArrayList<>(panels)) {
            BlockEntity blockEntity = level.getBlockEntity(panelPos);
            if (!(blockEntity instanceof AirlockControlPanelBlockEntity panel)
                    || !panel.isLinkedTo(level.dimension(), gateMasterPos)) {
                panels.remove(panelPos);
                continue;
            }
            setPanelActive(level, panelPos, active);
        }

        if (panels.isEmpty()) {
            byGate.remove(gateMasterPos);
        }
        if (byGate.isEmpty()) {
            INDEX.remove(level);
        }
    }

    public static void setPanelActive(ServerLevel level, BlockPos panelPos, boolean active) {
        BlockState panelState = level.getBlockState(panelPos);
        if (!panelState.is(AirlockPanelRegistry.block())
                || !panelState.hasProperty(AirlockControlPanelBlock.ACTIVE)
                || panelState.getValue(AirlockControlPanelBlock.ACTIVE) == active) {
            return;
        }

        level.setBlock(
                panelPos,
                panelState.setValue(AirlockControlPanelBlock.ACTIVE, active),
                Block.UPDATE_CLIENTS
        );
    }
}
