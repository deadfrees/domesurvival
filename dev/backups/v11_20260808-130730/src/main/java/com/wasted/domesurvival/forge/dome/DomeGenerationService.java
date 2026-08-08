package com.wasted.domesurvival.forge.dome;

import com.wasted.domesurvival.core.dome.BlockPoint;
import com.wasted.domesurvival.core.dome.DomeShellPlanner;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/** Builds the dome in batches so a command does not freeze one server tick. */
public final class DomeGenerationService {
    private static final int BLOCKS_PER_TICK = 750;
    private static final Deque<BlockPoint> QUEUE = new ArrayDeque<>();
    private static boolean running;
    private static int total;
    private static int placed;

    private DomeGenerationService() {
    }

    public static synchronized StartResult start(ServerLevel level) {
        if (running) return StartResult.ALREADY_RUNNING;
        if (DomeSavedData.get(level).isGenerated()) return StartResult.ALREADY_GENERATED;

        Set<BlockPoint> shell = DomeShellPlanner.planOneBlockShell(DomeSpec.wastedV1());
        QUEUE.clear();
        QUEUE.addAll(shell);
        total = QUEUE.size();
        placed = 0;
        running = true;
        return StartResult.STARTED;
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

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !running) return;

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        int budget = BLOCKS_PER_TICK;

        while (budget-- > 0 && !QUEUE.isEmpty()) {
            BlockPoint point = QUEUE.removeFirst();
            BlockPos pos = new BlockPos(point.x(), point.y(), point.z());
            level.setBlock(pos, ModBlocks.REINFORCED_GLASS.get().defaultBlockState(), 3);
            placed++;
        }

        if (QUEUE.isEmpty()) {
            DomeSpec spec = DomeSpec.wastedV1();
            level.setDefaultSpawnPos(new BlockPos(spec.centerX(), spec.baseY(), spec.centerZ()), 0.0F);
            DomeSavedData.get(level).markGenerated();
            running = false;
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("[DomeSurvival] Купол построен. Блоков оболочки: " + placed), false);
        }
    }

    public enum StartResult {
        STARTED,
        ALREADY_RUNNING,
        ALREADY_GENERATED
    }
}
