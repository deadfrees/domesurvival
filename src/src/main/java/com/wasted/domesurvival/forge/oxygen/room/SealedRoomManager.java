package com.wasted.domesurvival.forge.oxygen.room;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateMotion;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-authoritative, on-demand airtight-room discovery and cache.
 *
 * V61 keeps room geometry transient and adds only a compact persistent atmosphere record:
 * - a flood-fill is performed only when a ventilation outlet has no valid cache;
 * - block/gate events invalidate only rooms whose interior/boundary touched the changed position;
 * - normal machine ticks are O(1) cache lookups after discovery;
 * - unloaded chunks are never force-loaded by the room scanner;
 * - persisted atmosphere uses room id + geometry signature + oxygen amount, not saved cell lists.
 */
public final class SealedRoomManager {
    /** Hard stop against accidental scans into the entire overworld/cave network. */
    public static final int MAX_INTERIOR_BLOCKS = 65_536;
    public static final int MAX_AXIS_DISTANCE = 64;

    /** Optional datapack compatibility hooks for modded blocks. */
    public static final TagKey<Block> AIRTIGHT_BLOCKS = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(DomeSurvival.MOD_ID, "airtight_blocks")
    );
    public static final TagKey<Block> NON_AIRTIGHT_BLOCKS = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(DomeSurvival.MOD_ID, "non_airtight_blocks")
    );

    private static final Map<ServerLevel, LevelCache> LEVEL_CACHES = new WeakHashMap<>();

    private SealedRoomManager() {
    }

    public enum RoomState {
        UNKNOWN,
        SEALED,
        OPEN,
        TOO_LARGE,
        UNLOADED,
        LEAKING,
        DEPRESSURIZED;

        public boolean sealed() {
            return this == SEALED;
        }

        public static RoomState byOrdinal(int ordinal) {
            RoomState[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : UNKNOWN;
        }
    }

    /** Immutable data safe to expose to machines/GUI without exposing cache internals. */
    public record RoomSnapshot(
            RoomState state,
            int volume,
            long roomId,
            long geometrySignature,
            BlockPos outletPos
    ) {
        public boolean sealed() {
            return state.sealed();
        }
    }

    /**
     * Returns a cached room for this outlet, or performs one bounded discovery if invalidated/missing.
     * No chunks are loaded by this call.
     */
    public static RoomSnapshot getOrDiscover(ServerLevel level, BlockPos outletPos) {
        LevelCache cache = cache(level);
        long outletKey = outletPos.asLong();
        CachedRoom existing = cache.roomsByOutlet.get(outletKey);
        if (existing != null) {
            return existing.snapshot;
        }

        CachedRoom pending = cache.pendingRevalidation.get(outletKey);
        if (pending != null) {
            RoomSnapshot revalidated = revalidatePending(level, cache, outletKey, pending);
            if (revalidated != null) return revalidated;
        }

        CachedRoom discovered = discover(level, outletPos.immutable());
        register(cache, outletKey, discovered);
        if (discovered.snapshot.sealed()) {
            RoomAtmosphereSavedData.get(level).reconcileSealed(
                    discovered.snapshot, outletKey, level.getGameTime()
            );
            stopLeakingSessionIfResealed(cache, discovered.snapshot);
        }
        return discovered.snapshot;
    }

    /** Future V61/V62 lookup: find an already-discovered sealed atmosphere containing this cell. */
    @Nullable
    public static RoomSnapshot findCachedSealedRoomContaining(ServerLevel level, BlockPos pos) {
        LevelCache cache = LEVEL_CACHES.get(level);
        if (cache == null) return null;

        LongOpenHashSet candidates = cache.roomsByChunk.get(chunkKey(pos));
        if (candidates == null || candidates.isEmpty()) return null;

        long packed = pos.asLong();
        LongIterator iterator = candidates.iterator();
        while (iterator.hasNext()) {
            CachedRoom room = cache.roomsByOutlet.get(iterator.nextLong());
            if (room != null && room.snapshot.sealed() && room.interior.contains(packed)) {
                return room.snapshot;
            }
        }
        return null;
    }

    /**
     * O(1)-ish cached atmosphere lookup used by OxygenEnvironment once per player oxygen update.
     * This method never starts a flood-fill and never loads chunks.
     */
    public static boolean isBreathableAt(ServerLevel level, BlockPos pos) {
        RoomSnapshot room = findCachedSealedRoomContaining(level, pos);
        if (room != null && RoomAtmosphereSavedData.get(level).isBreathable(room)) {
            return true;
        }

        LevelCache cache = LEVEL_CACHES.get(level);
        if (cache == null) return false;
        LongOpenHashSet candidates = cache.leakingRoomsByChunk.get(chunkKey(pos));
        if (candidates == null || candidates.isEmpty()) return false;

        long packed = pos.asLong();
        long gameTime = level.getGameTime();
        LongIterator iterator = candidates.iterator();
        while (iterator.hasNext()) {
            LeakingRoom leaking = cache.leakingRoomsById.get(iterator.nextLong());
            if (leaking != null
                    && leaking.interior.contains(packed)
                    && RoomAtmosphereSavedData.get(level).isLeakingBreathable(
                            leaking.roomId, leaking.geometrySignature, gameTime
                    )) {
                return true;
            }
        }
        return false;
    }

    /** O(number of affected cached rooms), normally zero or one. */
    public static void invalidateAt(ServerLevel level, BlockPos pos) {
        LevelCache cache = LEVEL_CACHES.get(level);
        if (cache == null || cache.roomsByOutlet.isEmpty()) return;

        LongOpenHashSet candidates = cache.roomsByChunk.get(chunkKey(pos));
        if (candidates == null || candidates.isEmpty()) return;

        long packed = pos.asLong();
        long[] candidateKeys = candidates.toLongArray();
        for (long outletKey : candidateKeys) {
            CachedRoom room = cache.roomsByOutlet.get(outletKey);
            if (room != null && room.dependencies.contains(packed)) {
                if (room.snapshot.sealed()) {
                    cache.pendingRevalidation.putIfAbsent(outletKey, room);
                }
                removeCached(cache, outletKey);
            }
        }
    }

    /** Invalidates a changed position and all six immediate neighbors. */
    public static void invalidateAround(ServerLevel level, BlockPos pos) {
        invalidateAt(level, pos);
        for (Direction direction : Direction.values()) {
            invalidateAt(level, pos.relative(direction));
        }
    }

    /**
     * Explicit V57+ gate hook. V62 no longer deletes oxygen here. The old sealed
     * geometry is queued and revalidated; only a real SEALED -> OPEN/TOO_LARGE
     * transition starts gradual depressurization.
     */
    public static void invalidateGatePart(ServerLevel level, BlockPos gatePartPos) {
        invalidateAround(level, gatePartPos);
    }

    /** Invalidate every cached room that touched a loading/unloading chunk. */
    public static void invalidateChunk(ServerLevel level, ChunkPos chunkPos) {
        LevelCache cache = LEVEL_CACHES.get(level);
        if (cache == null || cache.roomsByOutlet.isEmpty()) return;

        LongOpenHashSet candidates = cache.roomsByChunk.get(chunkPos.toLong());
        if (candidates == null || candidates.isEmpty()) return;

        long[] affected = candidates.toLongArray();
        for (long outletKey : affected) {
            removeCached(cache, outletKey);
        }
    }

    /** Remove one ventilation outlet and all reverse-index references when its machine disappears. */
    public static void forgetOutlet(ServerLevel level, BlockPos outletPos) {
        long outletKey = outletPos.asLong();
        LevelCache cache = LEVEL_CACHES.get(level);
        if (cache != null) {
            removeCached(cache, outletKey);
            cache.pendingRevalidation.remove(outletKey);
            for (LeakingRoom leaking : cache.leakingRoomsById.values()) {
                leaking.outletKeys.remove(outletKey);
            }
        }
        RoomAtmosphereSavedData.get(level).forgetOutlet(outletKey);
    }

    /**
     * V62 level tick: advances persisted pressure loss and processes a bounded number
     * of event-invalidated rooms. No chunks are force-loaded.
     */
    public static void tickLevel(ServerLevel level) {
        RoomAtmosphereSavedData atmosphere = RoomAtmosphereSavedData.get(level);
        long gameTime = level.getGameTime();
        if (Math.floorMod(gameTime, 5L) == 0L) {
            atmosphere.tickLeaks(gameTime);
        }

        LevelCache cache = LEVEL_CACHES.get(level);
        if (cache == null) return;

        long[] pendingKeys = cache.pendingRevalidation.keySet().toLongArray();
        int budget = Math.min(8, pendingKeys.length);
        for (int i = 0; i < budget; i++) {
            long outletKey = pendingKeys[i];
            CachedRoom old = cache.pendingRevalidation.get(outletKey);
            if (old != null) {
                revalidatePending(level, cache, outletKey, old);
            }
        }

        long[] leakingIds = cache.leakingRoomsById.keySet().toLongArray();
        for (long roomId : leakingIds) {
            LeakingRoom leaking = cache.leakingRoomsById.get(roomId);
            if (leaking == null) continue;
            RoomAtmosphereSavedData.LeakAtmosphereSnapshot pressure =
                    atmosphere.getLeak(roomId, leaking.geometrySignature, gameTime);
            if (pressure == null || pressure.depressurized()) {
                removeLeakingSession(cache, roomId);
            }
        }
    }

    /** Called on ServerLevel unload to avoid retaining transient geometry caches. */
    public static void clearLevel(ServerLevel level) {
        LEVEL_CACHES.remove(level);
    }

    @Nullable
    private static RoomSnapshot revalidatePending(ServerLevel level,
                                                  LevelCache cache,
                                                  long outletKey,
                                                  CachedRoom old) {
        BlockPos outletPos = BlockPos.of(outletKey);
        if (!level.hasChunkAt(outletPos)) return null;

        CachedRoom discovered = discover(level, outletPos);
        if (discovered.snapshot.state() == RoomState.UNLOADED) {
            return null;
        }

        cache.pendingRevalidation.remove(outletKey);
        register(cache, outletKey, discovered);

        RoomAtmosphereSavedData atmosphere = RoomAtmosphereSavedData.get(level);
        if (discovered.snapshot.sealed()) {
            atmosphere.reconcileSealed(discovered.snapshot, outletKey, level.getGameTime());
            stopLeakingSessionIfResealed(cache, discovered.snapshot);
        } else if (old.snapshot.sealed()
                && (discovered.snapshot.state() == RoomState.OPEN
                || discovered.snapshot.state() == RoomState.TOO_LARGE)) {
            atmosphere.startLeak(old.snapshot, outletKey, level.getGameTime());
            beginLeakingSession(cache, old, outletKey);
        }
        return discovered.snapshot;
    }

    private static void beginLeakingSession(LevelCache cache, CachedRoom old, long outletKey) {
        long roomId = old.snapshot.roomId();
        LeakingRoom current = cache.leakingRoomsById.get(roomId);
        if (current != null && current.geometrySignature == old.snapshot.geometrySignature()) {
            current.outletKeys.add(outletKey);
            return;
        }
        if (current != null) {
            removeLeakingSession(cache, roomId);
        }

        LongOpenHashSet interior = new LongOpenHashSet(old.interior);
        LongOpenHashSet chunks = new LongOpenHashSet();
        LongIterator iterator = interior.iterator();
        while (iterator.hasNext()) {
            chunks.add(chunkKey(BlockPos.of(iterator.nextLong())));
        }

        LongOpenHashSet outlets = new LongOpenHashSet();
        outlets.add(outletKey);
        LeakingRoom leaking = new LeakingRoom(
                roomId, old.snapshot.geometrySignature(), interior, chunks, outlets
        );
        cache.leakingRoomsById.put(roomId, leaking);

        LongIterator chunkIterator = chunks.iterator();
        while (chunkIterator.hasNext()) {
            long key = chunkIterator.nextLong();
            cache.leakingRoomsByChunk
                    .computeIfAbsent(key, ignored -> new LongOpenHashSet())
                    .add(roomId);
        }
    }

    private static void stopLeakingSessionIfResealed(LevelCache cache, RoomSnapshot sealed) {
        LeakingRoom leaking = cache.leakingRoomsById.get(sealed.roomId());
        if (leaking != null && leaking.geometrySignature == sealed.geometrySignature()) {
            removeLeakingSession(cache, sealed.roomId());
        }
    }

    private static void removeLeakingSession(LevelCache cache, long roomId) {
        LeakingRoom leaking = cache.leakingRoomsById.remove(roomId);
        if (leaking == null) return;

        LongIterator iterator = leaking.chunkKeys.iterator();
        while (iterator.hasNext()) {
            long chunk = iterator.nextLong();
            LongOpenHashSet ids = cache.leakingRoomsByChunk.get(chunk);
            if (ids == null) continue;
            ids.remove(roomId);
            if (ids.isEmpty()) cache.leakingRoomsByChunk.remove(chunk);
        }
    }

    public static boolean isAirtightBoundary(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(NON_AIRTIGHT_BLOCKS)) {
            return false;
        }
        if (state.is(AIRTIGHT_BLOCKS)) {
            return true;
        }

        if (state.getBlock() instanceof AirlockGateBlock && state.hasProperty(AirlockGateBlock.MOTION)) {
            return state.hasProperty(AirlockGateBlock.FORMED)
                    && state.getValue(AirlockGateBlock.FORMED)
                    && state.getValue(AirlockGateBlock.MOTION) == AirlockGateMotion.CLOSED;
        }

        // A fluid cell is not an oxygen-air volume and acts as a boundary for V60.
        if (!state.getFluidState().isEmpty()) {
            return true;
        }

        // Full collision blocks are airtight by default. Slabs/stairs/fences/doors/etc.
        // remain traversable leaks unless a datapack explicitly opts them into AIRTIGHT_BLOCKS.
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private static CachedRoom discover(ServerLevel level, BlockPos outletPos) {
        LongOpenHashSet interior = new LongOpenHashSet();
        LongOpenHashSet boundary = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        long outletKey = outletPos.asLong();
        if (!level.hasChunkAt(outletPos)) {
            interior.add(outletKey);
            return result(RoomState.UNLOADED, interior, boundary, outletPos);
        }

        BlockState outletState = level.getBlockState(outletPos);
        if (isAirtightBoundary(level, outletPos, outletState)) {
            boundary.add(outletKey);
            return result(RoomState.OPEN, interior, boundary, outletPos);
        }

        interior.add(outletKey);
        queue.enqueue(outletKey);
        long canonicalCell = outletKey;
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty()) {
            long packed = queue.dequeueLong();
            BlockPos current = BlockPos.of(packed);

            if (packed < canonicalCell) {
                canonicalCell = packed;
            }

            if (outsideSearchBounds(outletPos, current)) {
                return result(RoomState.TOO_LARGE, interior, boundary, outletPos, canonicalCell);
            }

            if (!level.hasChunkAt(current)) {
                return result(RoomState.UNLOADED, interior, boundary, outletPos, canonicalCell);
            }

            // Reaching direct sky means the ventilation volume has escaped to the outside atmosphere.
            if (level.canSeeSky(current)) {
                return result(RoomState.OPEN, interior, boundary, outletPos, canonicalCell);
            }

            if (interior.size() > MAX_INTERIOR_BLOCKS) {
                return result(RoomState.TOO_LARGE, interior, boundary, outletPos, canonicalCell);
            }

            for (Direction direction : Direction.values()) {
                int nx = current.getX() + direction.getStepX();
                int ny = current.getY() + direction.getStepY();
                int nz = current.getZ() + direction.getStepZ();

                if (ny < level.getMinBuildHeight() || ny >= level.getMaxBuildHeight()) {
                    return result(RoomState.OPEN, interior, boundary, outletPos, canonicalCell);
                }

                neighborPos.set(nx, ny, nz);
                long neighborKey = neighborPos.asLong();

                if (interior.contains(neighborKey) || boundary.contains(neighborKey)) {
                    continue;
                }

                if (outsideSearchBounds(outletPos, neighborPos)) {
                    interior.add(neighborKey);
                    return result(RoomState.TOO_LARGE, interior, boundary, outletPos, canonicalCell);
                }

                if (!level.hasChunkAt(neighborPos)) {
                    interior.add(neighborKey);
                    return result(RoomState.UNLOADED, interior, boundary, outletPos, canonicalCell);
                }

                BlockState neighborState = level.getBlockState(neighborPos);
                if (isAirtightBoundary(level, neighborPos, neighborState)) {
                    boundary.add(neighborKey);
                    continue;
                }

                if (interior.add(neighborKey)) {
                    if (interior.size() > MAX_INTERIOR_BLOCKS) {
                        return result(RoomState.TOO_LARGE, interior, boundary, outletPos, canonicalCell);
                    }
                    queue.enqueue(neighborKey);
                }
            }
        }

        return result(RoomState.SEALED, interior, boundary, outletPos, canonicalCell);
    }

    private static boolean outsideSearchBounds(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) > MAX_AXIS_DISTANCE
                || Math.abs(pos.getY() - origin.getY()) > MAX_AXIS_DISTANCE
                || Math.abs(pos.getZ() - origin.getZ()) > MAX_AXIS_DISTANCE;
    }

    private static CachedRoom result(RoomState state,
                                     LongOpenHashSet interior,
                                     LongOpenHashSet boundary,
                                     BlockPos outletPos) {
        long roomId = outletPos.asLong();
        if (!interior.isEmpty()) {
            LongIterator iterator = interior.iterator();
            roomId = iterator.nextLong();
            while (iterator.hasNext()) {
                roomId = Math.min(roomId, iterator.nextLong());
            }
        }
        return result(state, interior, boundary, outletPos, roomId);
    }

    private static CachedRoom result(RoomState state,
                                     LongOpenHashSet interior,
                                     LongOpenHashSet boundary,
                                     BlockPos outletPos,
                                     long canonicalCell) {
        LongOpenHashSet dependencies = new LongOpenHashSet(interior.size() + boundary.size());
        dependencies.addAll(interior);
        dependencies.addAll(boundary);

        LongOpenHashSet dependencyChunks = new LongOpenHashSet();
        LongIterator dependencyIterator = dependencies.iterator();
        while (dependencyIterator.hasNext()) {
            long packed = dependencyIterator.nextLong();
            BlockPos pos = BlockPos.of(packed);
            dependencyChunks.add(chunkKey(pos));
        }

        RoomSnapshot snapshot = new RoomSnapshot(
                state,
                interior.size(),
                canonicalCell,
                geometrySignature(interior),
                outletPos.immutable()
        );
        return new CachedRoom(snapshot, interior, dependencies, dependencyChunks);
    }

    private static LevelCache cache(ServerLevel level) {
        return LEVEL_CACHES.computeIfAbsent(level, ignored -> new LevelCache());
    }

    private static void register(LevelCache cache, long outletKey, CachedRoom room) {
        removeCached(cache, outletKey);
        cache.roomsByOutlet.put(outletKey, room);

        LongIterator iterator = room.dependencyChunks.iterator();
        while (iterator.hasNext()) {
            long chunkKey = iterator.nextLong();
            cache.roomsByChunk
                    .computeIfAbsent(chunkKey, ignored -> new LongOpenHashSet())
                    .add(outletKey);
        }
    }

    private static void removeCached(LevelCache cache, long outletKey) {
        CachedRoom old = cache.roomsByOutlet.remove(outletKey);
        if (old == null) return;

        LongIterator iterator = old.dependencyChunks.iterator();
        while (iterator.hasNext()) {
            long chunkKey = iterator.nextLong();
            LongOpenHashSet outlets = cache.roomsByChunk.get(chunkKey);
            if (outlets == null) continue;

            outlets.remove(outletKey);
            if (outlets.isEmpty()) {
                cache.roomsByChunk.remove(chunkKey);
            }
        }
    }

    /**
     * Order-independent fingerprint of the interior cells. It is persisted with room oxygen
     * instead of persisting the full cell set. Geometry changes therefore cannot silently reuse
     * atmosphere from an unrelated room that happens to have the same canonical room id.
     */
    private static long geometrySignature(LongOpenHashSet interior) {
        long sum = 0x9E3779B97F4A7C15L ^ interior.size();
        long xor = 0xC2B2AE3D27D4EB4FL;
        LongIterator iterator = interior.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            long mixed = mix64(packed);
            sum += mixed;
            xor ^= Long.rotateLeft(mixed, (int) (packed & 63L));
        }
        return mix64(sum ^ xor ^ ((long) interior.size() * 0x165667B19E3779F9L));
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static final class LevelCache {
        private final Long2ObjectOpenHashMap<CachedRoom> roomsByOutlet = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<LongOpenHashSet> roomsByChunk = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<CachedRoom> pendingRevalidation = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<LeakingRoom> leakingRoomsById = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<LongOpenHashSet> leakingRoomsByChunk = new Long2ObjectOpenHashMap<>();
    }

    private record CachedRoom(
            RoomSnapshot snapshot,
            LongOpenHashSet interior,
            LongOpenHashSet dependencies,
            LongOpenHashSet dependencyChunks
    ) {
    }

    private static final class LeakingRoom {
        private final long roomId;
        private final long geometrySignature;
        private final LongOpenHashSet interior;
        private final LongOpenHashSet chunkKeys;
        private final LongOpenHashSet outletKeys;

        private LeakingRoom(long roomId,
                            long geometrySignature,
                            LongOpenHashSet interior,
                            LongOpenHashSet chunkKeys,
                            LongOpenHashSet outletKeys) {
            this.roomId = roomId;
            this.geometrySignature = geometrySignature;
            this.interior = interior;
            this.chunkKeys = chunkKeys;
            this.outletKeys = outletKeys;
        }
    }
}
