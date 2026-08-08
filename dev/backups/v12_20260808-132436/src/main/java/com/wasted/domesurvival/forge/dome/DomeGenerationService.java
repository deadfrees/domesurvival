package com.wasted.domesurvival.forge.dome;

import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeStructurePlanner;
import com.wasted.domesurvival.core.dome.PlannedBlock;
import com.wasted.domesurvival.core.dome.StructureMaterial;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Builds/updates the dome in batches so one command cannot freeze the server tick. */
public final class DomeGenerationService {
    public static final int CURRENT_STRUCTURE_VERSION = 2; // V1.1 visual structure
    private static final int BLOCKS_PER_TICK = 750;
    private static final Deque<PlannedBlock> QUEUE = new ArrayDeque<>();
    private static boolean running;
    private static int total;
    private static int placed;
    private static int targetVersion;
    private static Operation operation = Operation.NONE;

    private DomeGenerationService() {
    }

    public static synchronized StartResult startGenerate(ServerLevel level) {
        if (running) return StartResult.ALREADY_RUNNING;
        if (DomeSavedData.get(level).isGenerated()) return StartResult.ALREADY_GENERATED;

        List<PlannedBlock> plan = DomeStructurePlanner.planFullV11(DomeSpec.wastedV1());
        begin(plan, CURRENT_STRUCTURE_VERSION, Operation.GENERATE);
        return StartResult.STARTED;
    }

    public static synchronized StartResult startUpgrade(ServerLevel level) {
        if (running) return StartResult.ALREADY_RUNNING;
        int version = DomeSavedData.get(level).structureVersion();
        if (version < 1) return StartResult.NOT_GENERATED;
        if (version >= CURRENT_STRUCTURE_VERSION) return StartResult.UP_TO_DATE;

        List<PlannedBlock> plan = DomeStructurePlanner.planV11Upgrade(DomeSpec.wastedV1());
        begin(plan, CURRENT_STRUCTURE_VERSION, Operation.UPGRADE);
        return StartResult.STARTED;
    }

    private static void begin(List<PlannedBlock> plan, int version, Operation op) {
        QUEUE.clear();
        QUEUE.addAll(plan);
        total = QUEUE.size();
        placed = 0;
        targetVersion = version;
        operation = op;
        running = true;
    }

    public static synchronized boolean isRunning() {
        return running;
    }

    public static synchronized int total() {
        return total;
    }

    public static synchronized int placed() {
        return placed;
    }

    public static synchronized String operationName() {
        return operation.name().toLowerCase();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !running) return;

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        int budget = BLOCKS_PER_TICK;

        while (budget-- > 0 && !QUEUE.isEmpty()) {
            PlannedBlock planned = QUEUE.removeFirst();
            BlockPos pos = new BlockPos(planned.point().x(), planned.point().y(), planned.point().z());
            if (planned.material() == StructureMaterial.GLASS) {
                level.setBlock(pos, ModBlocks.REINFORCED_GLASS.get().defaultBlockState(), 3);
            } else if (planned.material() == StructureMaterial.FRAME) {
                level.setBlock(pos, ModBlocks.DOME_FRAME.get().defaultBlockState(), 3);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            placed++;
        }

        if (QUEUE.isEmpty()) {
            DomeSpec spec = DomeSpec.wastedV1();
            level.setDefaultSpawnPos(new BlockPos(spec.centerX(), spec.baseY(), spec.centerZ()), 0.0F);
            DomeSavedData.get(level).markStructureVersion(targetVersion);
            Operation finished = operation;
            operation = Operation.NONE;
            running = false;
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("[DomeSurvival] " + finished.name().toLowerCase()
                            + " завершён. Изменено блоков: " + placed
                            + ", structureVersion=" + targetVersion), false);
        }
    }

    public enum StartResult {
        STARTED,
        ALREADY_RUNNING,
        ALREADY_GENERATED,
        NOT_GENERATED,
        UP_TO_DATE
    }

    private enum Operation {
        NONE,
        GENERATE,
        UPGRADE
    }
}
