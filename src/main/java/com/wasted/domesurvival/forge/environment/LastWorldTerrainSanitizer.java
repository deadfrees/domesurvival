package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enforces LastWorld's dry desert surface after all structure features have
 * finished placing their blocks.
 *
 * <p>The base noise settings already disable aquifers and use air as the
 * default fluid. Imported Lost Cities buildings and third-party structures can
 * still carry small patches of grass, dirt, or decorative vanilla water in
 * their templates, however. A newly completed chunk is therefore sanitized
 * once, while it is still being loaded for the first time.</p>
 *
 * <p>Only the custom {@code domesurvival:lastworld} noise settings are
 * recognized. Normal overworlds, the old WASTED_TEST save, modded fluids, deep
 * underground dirt, farmland, crops, and every non-overworld dimension remain
 * untouched.</p>
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LastWorldTerrainSanitizer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean STARTUP_DIAGNOSTIC_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean NEW_CHUNK_DIAGNOSTIC_LOGGED = new AtomicBoolean();
    private static final Map<ServerLevel, ArrayDeque<ScheduledChunk>> PENDING = new WeakHashMap<>();
    private static final Map<ServerLevel, Set<Long>> SCHEDULED = new WeakHashMap<>();
    private static final int SURFACE_LAYER_MIN_Y = 40;
    private static final int DELAYED_PASS_TICKS = 20;
    private static final int MAX_DELAYED_CHUNKS_PER_TICK = 2;

    private LastWorldTerrainSanitizer() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        boolean lastWorld = LastWorldWorldState.isLastWorld(level);
        if (STARTUP_DIAGNOSTIC_LOGGED.compareAndSet(false, true)) {
            LOGGER.info(
                    "LastWorld terrain sanitizer probe: active={}, newChunk={}, status={}",
                    lastWorld,
                    event.isNewChunk(),
                    event.getChunk().getStatus()
            );
        }

        if (!lastWorld) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        int immediateChanges = event.isNewChunk() ? sanitize(level, chunk, false) : 0;
        if (!event.isNewChunk()) {
            return;
        }
        long chunkKey = ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z);
        synchronized (PENDING) {
            Set<Long> scheduled = SCHEDULED.computeIfAbsent(level, ignored -> new HashSet<>());
            if (scheduled.add(chunkKey)) {
                PENDING.computeIfAbsent(level, ignored -> new ArrayDeque<>()).addLast(
                        new ScheduledChunk(
                                chunk.getPos().x,
                                chunk.getPos().z,
                                level.getGameTime() + DELAYED_PASS_TICKS
                        )
                );
            }
        }

        if (NEW_CHUNK_DIAGNOSTIC_LOGGED.compareAndSet(false, true)) {
            LOGGER.info(
                    "LastWorld chunk sanitizer scheduled: status={}, immediateChanges={}",
                    chunk.getStatus(),
                    immediateChanges
            );
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ServerLevel level = event.getServer().overworld();
        ArrayDeque<ScheduledChunk> queue;
        synchronized (PENDING) {
            queue = PENDING.get(level);
        }
        if (queue == null || queue.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        int processed = 0;
        while (processed < MAX_DELAYED_CHUNKS_PER_TICK) {
            ScheduledChunk scheduled;
            synchronized (PENDING) {
                scheduled = queue.peekFirst();
                if (scheduled == null || scheduled.dueGameTime() > now) {
                    break;
                }
                queue.removeFirst();
            }

            long chunkKey = ChunkPos.asLong(scheduled.chunkX(), scheduled.chunkZ());
            try {
                var chunk = level.getChunkSource().getChunkNow(scheduled.chunkX(), scheduled.chunkZ());
                if (chunk != null) {
                    sanitize(level, chunk, true);
                }
            } finally {
                synchronized (PENDING) {
                    Set<Long> scheduledKeys = SCHEDULED.get(level);
                    if (scheduledKeys != null) {
                        scheduledKeys.remove(chunkKey);
                    }
                }
            }
            processed++;
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // Never retain chunk references between integrated-server sessions.
        // Delayed passes only touch chunks that are still loaded; reloading or
        // mutating chunks during unload can keep the world-save loop alive.
        synchronized (PENDING) {
            PENDING.clear();
            SCHEDULED.clear();
        }
    }

    private static int sanitize(ServerLevel level, ChunkAccess chunk, boolean notifyClients) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int changed = 0;

        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
                for (int y = SURFACE_LAYER_MIN_Y; y < topY; y++) {
                    cursor.set(startX + localX, y, startZ + localZ);
                    if (isTransferredDomeBlock(level, cursor)) {
                        continue;
                    }
                    BlockState state = chunk.getBlockState(cursor);
                    BlockState replacement = replacementFor(state);
                    if (replacement != null) {
                        chunk.setBlockState(cursor, replacement, false);
                        if (notifyClients) {
                            level.getChunkSource().blockChanged(cursor);
                        }
                        changed++;
                    }
                }
            }
        }

        if (changed > 0) {
            chunk.setUnsaved(true);
        }
        return changed;
    }

    private static BlockState replacementFor(BlockState state) {
        if (state.is(Blocks.WATER)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            return Blocks.SAND.defaultBlockState();
        }
        if (state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MUD)) {
            return Blocks.SANDSTONE.defaultBlockState();
        }
        return null;
    }

    private static boolean isTransferredDomeBlock(ServerLevel level, BlockPos pos) {
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        int minX = spec.centerX() - spec.surfaceRadius() - 4;
        int maxX = spec.centerX() + spec.surfaceRadius() + 4;
        int minZ = spec.centerZ() - spec.surfaceRadius() - 4;
        int maxZ = Math.max(spec.centerZ() + spec.surfaceRadius() + 4, spec.airlockEndZ() + 4);
        return pos.getY() >= spec.foundationMinY()
                && pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }

    private record ScheduledChunk(int chunkX, int chunkZ, long dueGameTime) {
    }
}
