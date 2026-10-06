package com.wasted.domesurvival.forge.metro;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Cached breathable volumes for RESTORED external metro stations.
 *
 * The SavedData registry is scanned only when the cache is rebuilt (server
 * start / restoration / reset), never during player ticks. Runtime lookup is
 * dimension + chunk bucket + a tiny bounded AABB test.
 */
public final class RestoredMetroAtmosphere {
    private static final Map<MinecraftServer, Index> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RestoredMetroAtmosphere() {
    }

    public static void rebuild(MinecraftServer server) {
        Map<String, Map<Long, List<Bounds>>> byDimension = new HashMap<>();

        for (MetroStationRecord station : MetroStationSavedData.get(server).all()) {
            if (station.state() != StationState.RESTORED || !station.restored()) continue;

            Bounds bounds = Bounds.fromAnchor(station.anchor());
            Map<Long, List<Bounds>> byChunk =
                    byDimension.computeIfAbsent(station.dimension(), ignored -> new HashMap<>());

            int minChunkX = bounds.minX() >> 4;
            int maxChunkX = bounds.maxX() >> 4;
            int minChunkZ = bounds.minZ() >> 4;
            int maxChunkZ = bounds.maxZ() >> 4;

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    long key = ChunkPos.asLong(chunkX, chunkZ);
                    byChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(bounds);
                }
            }
        }

        // Freeze lists so atmosphere checks never observe mutation.
        Map<String, Map<Long, List<Bounds>>> frozen = new HashMap<>();
        byDimension.forEach((dimension, chunks) -> {
            Map<Long, List<Bounds>> frozenChunks = new HashMap<>();
            chunks.forEach((key, values) -> frozenChunks.put(key, List.copyOf(values)));
            frozen.put(dimension, Map.copyOf(frozenChunks));
        });

        CACHE.put(server, new Index(Map.copyOf(frozen)));
    }

    public static void clear(MinecraftServer server) {
        CACHE.remove(server);
    }

    public static boolean isForcedBreathable(ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        Index index = CACHE.get(server);
        if (index == null) {
            rebuild(server);
            index = CACHE.get(server);
            if (index == null) return false;
        }

        Map<Long, List<Bounds>> chunks =
                index.byDimension().get(level.dimension().location().toString());
        if (chunks == null) return false;

        List<Bounds> candidates =
                chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (candidates == null) return false;

        for (Bounds bounds : candidates) {
            if (bounds.contains(pos)) return true;
        }
        return false;
    }

    private record Index(Map<String, Map<Long, List<Bounds>>> byDimension) {
    }

    /**
     * Fixed Stage-2 restored volume.
     *
     * Includes the station hall, platforms, static train and short tunnel
     * segments. It begins immediately inside the new north airlock and does not
     * include the outside stair descent.
     */
    private record Bounds(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    ) {
        static Bounds fromAnchor(BlockPos anchor) {
            BlockPos center = anchor.offset(13, -2, 6);
            int baseY = center.getY();

            return new Bounds(
                    center.getX() - 25,
                    baseY + 1,
                    center.getZ() - 9,
                    center.getX() + 25,
                    baseY + 9,
                    center.getZ() + 7
            );
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }
}
