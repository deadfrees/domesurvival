package com.wasted.domesurvival.forge.loot;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Gives world-generated empty chests a loot table on the first creation of a
 * chunk. Player-placed containers are not affected because later chunk loads
 * have {@code isNewChunk() == false}.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneratedContainerLootService {
    private static final int MAX_CHUNKS_PER_TICK = 4;
    private static final Map<ServerLevel, ArrayDeque<Long>> PENDING = new WeakHashMap<>();
    private static final Map<ServerLevel, Set<Long>> SCHEDULED = new WeakHashMap<>();

    private GeneratedContainerLootService() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        long chunkKey = chunk.getPos().toLong();
        synchronized (PENDING) {
            Set<Long> scheduled = SCHEDULED.computeIfAbsent(level, ignored -> new HashSet<>());
            if (scheduled.add(chunkKey)) {
                PENDING.computeIfAbsent(level, ignored -> new ArrayDeque<>()).addLast(chunkKey);
            }
        }
    }

    /**
     * ChunkEvent.Load fires while a proto chunk is still being promoted to a
     * full chunk. Calling BlockEntity#setChanged from that callback asks the
     * chunk cache for the same chunk and deadlocks the integrated server. Work
     * is therefore deferred to a later server tick and only touches chunks
     * which are still loaded.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            int processed = 0;
            while (processed < MAX_CHUNKS_PER_TICK) {
                Long chunkKey;
                synchronized (PENDING) {
                    ArrayDeque<Long> queue = PENDING.get(level);
                    chunkKey = queue == null ? null : queue.pollFirst();
                }
                if (chunkKey == null) {
                    break;
                }

                int chunkX = net.minecraft.world.level.ChunkPos.getX(chunkKey);
                int chunkZ = net.minecraft.world.level.ChunkPos.getZ(chunkKey);
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk != null) {
                    fillNewChunk(level, chunk);
                }

                synchronized (PENDING) {
                    Set<Long> scheduled = SCHEDULED.get(level);
                    if (scheduled != null) {
                        scheduled.remove(chunkKey);
                    }
                }
                processed++;
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (PENDING) {
            PENDING.clear();
            SCHEDULED.clear();
        }
    }

    private static void fillNewChunk(ServerLevel level, LevelChunk chunk) {
        boolean changed = false;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof ChestBlockEntity)
                    && !(blockEntity instanceof BarrelBlockEntity)) {
                continue;
            }
            ResourceLocation table = lootTableFor(level, blockEntity.getBlockPos());
            changed |= assignLootIfEmpty(level, blockEntity, table);
        }
        if (changed) {
            // Mark the already-resolved owning chunk directly. Do not call
            // BlockEntity#setChanged here: it performs a reentrant chunk lookup.
            chunk.setUnsaved(true);
        }
    }

    static boolean assignLootIfEmpty(
            ServerLevel level,
            BlockEntity blockEntity,
            ResourceLocation lootTable
    ) {
        if (!(blockEntity instanceof RandomizableContainerBlockEntity randomizable)
                || !(blockEntity instanceof Container container)
                || blockEntity.saveWithoutMetadata().contains("LootTable")) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) return false;
        }
        randomizable.setLootTable(lootTable, level.random.nextLong());
        return true;
    }

    static ResourceLocation lootTableFor(ServerLevel level, BlockPos pos) {
        return GeneratedLootCategory.tableAt(level, pos);
    }
}
