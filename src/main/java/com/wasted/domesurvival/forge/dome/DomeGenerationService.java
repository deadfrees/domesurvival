package com.wasted.domesurvival.forge.dome;

import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeStructurePlanner;
import com.wasted.domesurvival.core.dome.PlannedBlock;
import com.wasted.domesurvival.core.dome.StructureMaterial;
import com.wasted.domesurvival.forge.airlock.StarterDomeAirlockV58;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateMotion;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/** Builds/updates the dome in batches so one command cannot freeze the server tick. */
public final class DomeGenerationService {
    public static final int CURRENT_STRUCTURE_VERSION = 8; // V58: starter dome uses the universal V57 gate system
    private static final int BLOCKS_PER_TICK = 750;
    private static final Deque<PlannedBlock> QUEUE = new ArrayDeque<>();

    private static final Set<Block> AUTHOR_BUILD_BLOCKS = Set.of(
            Blocks.WHITE_WOOL,
            Blocks.BLACK_WOOL,
            Blocks.BROWN_TERRACOTTA,
            Blocks.BIRCH_PLANKS,
            Blocks.CHEST
    );

    private static boolean running;
    private static int total;
    private static int placed;
    private static int targetVersion;
    private static Operation operation = Operation.NONE;
    private static boolean resetAirlockOnFinish;

    private DomeGenerationService() {
    }

    public static synchronized StartResult startGenerate(ServerLevel level) {
        if (running) return StartResult.ALREADY_RUNNING;
        if (DomeSavedData.get(level).isGenerated()) return StartResult.ALREADY_GENERATED;

        List<PlannedBlock> plan = DomeStructurePlanner.planFullV23(DomeSavedData.get(level).domeSpec());
        resetAirlockOnFinish = true;
        begin(plan, CURRENT_STRUCTURE_VERSION, Operation.GENERATE);
        return StartResult.STARTED;
    }

    public static synchronized StartResult startUpgrade(ServerLevel level) {
        if (running) return StartResult.ALREADY_RUNNING;
        int version = DomeSavedData.get(level).structureVersion();
        if (version < 1) return StartResult.NOT_GENERATED;
        if (version >= CURRENT_STRUCTURE_VERSION) return StartResult.UP_TO_DATE;

        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        List<PlannedBlock> v2 = DomeStructurePlanner.planV2UpgradeFromV14(spec);
        List<PlannedBlock> v23 = DomeStructurePlanner.planV23UpgradeFromV2(spec);
        List<PlannedBlock> plan = switch (version) {
            case 1 -> concat(concat(DomeStructurePlanner.planV14UpgradeFromV1(spec), v2), v23);
            case 2 -> concat(concat(DomeStructurePlanner.planV14UpgradeFromV11(spec), v2), v23);
            case 3 -> concat(concat(DomeStructurePlanner.planV14UpgradeFromV12(spec), v2), v23);
            case 4 -> concat(concat(DomeStructurePlanner.planV14UpgradeFromV13(spec), v2), v23);
            case 5 -> concat(v2, v23);
            case 6 -> v23;
            case 7 -> List.of(); // V58 is finalized by the post-build airlock migration below.
            default -> List.of();
        };
        resetAirlockOnFinish = version < 6;
        begin(plan, CURRENT_STRUCTURE_VERSION, Operation.UPGRADE);
        return StartResult.STARTED;
    }

    private static List<PlannedBlock> concat(List<PlannedBlock> first, List<PlannedBlock> second) {
        ArrayList<PlannedBlock> out = new ArrayList<>(first.size() + second.size());
        out.addAll(first);
        out.addAll(second);
        return List.copyOf(out);
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
            apply(level, pos, planned.material());
            placed++;
        }

        if (QUEUE.isEmpty()) {
            DomeSpec spec = DomeSavedData.get(level).domeSpec();

            StarterDomeAirlockV58.InstallResult airlockResult =
                    StarterDomeAirlockV58.install(level);
            if (airlockResult != StarterDomeAirlockV58.InstallResult.SUCCESS) {
                Operation failedOperation = operation;
                operation = Operation.NONE;
                running = false;
                resetAirlockOnFinish = false;
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("[DomeSurvival] "
                                + failedOperation.name().toLowerCase()
                                + " остановлен: V58 airlock migration="
                                + airlockResult.name()
                                + ". StructureVersion не повышен."), false);
                return;
            }

            level.setDefaultSpawnPos(
                    new BlockPos(spec.centerX(), spec.baseY(), spec.centerZ()),
                    0.0F
            );

            DomeSavedData saved = DomeSavedData.get(level);
            saved.markStructureVersion(targetVersion);
            // V58 gates are installed CLOSED. Normalize the legacy mirror too.
            saved.resetAirlock();

            resetAirlockOnFinish = false;
            Operation finished = operation;
            operation = Operation.NONE;
            running = false;
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("[DomeSurvival] " + finished.name().toLowerCase()
                            + " завершён. Операций: " + placed
                            + ", V58 шлюз=OK"
                            + ", structureVersion=" + targetVersion), false);
        }
    }

    private static void apply(ServerLevel level, BlockPos pos, StructureMaterial material) {
        switch (material) {
            case GLASS -> level.setBlock(pos, ModBlocks.REINFORCED_GLASS.get().defaultBlockState(), 3);
            case FRAME -> level.setBlock(pos, ModBlocks.DOME_FRAME.get().defaultBlockState(), 3);
            case FOUNDATION -> level.setBlock(pos, ModBlocks.DOME_FOUNDATION.get().defaultBlockState(), 3);
            case AIR -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            case SAND -> level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
            case COARSE_DIRT -> level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
            // V58: planner keeps these legacy material names as abstract hardware
            // slots, but current generation places the universal gate/panel system.
            case AIRLOCK_DOOR -> level.setBlock(
                    pos,
                    AirlockGateRegistry.AIRLOCK_GATE.get().defaultBlockState()
                            .setValue(AirlockGateBlock.FACING, net.minecraft.core.Direction.SOUTH)
                            .setValue(AirlockGateBlock.FORMED, false)
                            .setValue(AirlockGateBlock.MASTER, false)
                            .setValue(AirlockGateBlock.MOTION, AirlockGateMotion.CLOSED),
                    3
            );
            case AIRLOCK_PANEL -> level.setBlock(
                    pos,
                    ModBlocks.DOME_FOUNDATION.get().defaultBlockState(),
                    3
            );
            case CLEAR_AUTHOR_BUILD -> {
                BlockState state = level.getBlockState(pos);
                if (AUTHOR_BUILD_BLOCKS.contains(state.getBlock())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            case CLEAR_LEGACY_FOUNDATION -> {
                if (level.getBlockState(pos).is(ModBlocks.DOME_FOUNDATION.get())) {
                    DomeSpec spec = DomeSavedData.get(level).domeSpec();
                    if (pos.getY() >= spec.baseY()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    } else if (pos.getY() == spec.airlockFloorY()) {
                        level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.SANDSTONE.defaultBlockState(), 3);
                    }
                }
            }
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
