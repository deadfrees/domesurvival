package com.wasted.domesurvival.forge.loot;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.compat.lostcities.LostCitiesBuildingCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/**
 * Guarantees discoverable storage in selected generated buildings that ship
 * without a chest. Work is performed only when a player enters a fully loaded
 * structure piece, so no chunks are generated or force-loaded by this service.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneratedStructureStorageService {
    private static final TagKey<Structure> STORAGE_TARGETS = TagKey.create(
            Registries.STRUCTURE,
            new ResourceLocation(DomeSurvival.MOD_ID, "loot_storage_guaranteed")
    );
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final int PLACEMENT_RADIUS = 8;
    private static final int VERTICAL_RADIUS = 4;

    private GeneratedStructureStorageService() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !Level.OVERWORLD.equals(player.level().dimension())
                || player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(player.blockPosition(), STORAGE_TARGETS);
        if (start == null || !start.isValid()) {
            resolveLostCitiesBuilding(level, player);
            return;
        }

        ResourceLocation structureId = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getKey(start.getStructure());
        if (structureId == null) return;

        String key = structureId + "@" + start.getChunkPos().x + "," + start.getChunkPos().z;
        GeneratedStructureStorageSavedData data = GeneratedStructureStorageSavedData.get(level);
        if (data.isHandled(key)) return;

        BoundingBox bounds = start.getBoundingBox();
        ResourceLocation lootTable = lootTableFor(structureId);
        if (resolveLoadedContainer(level, bounds, lootTable)) {
            data.markHandled(key);
            return;
        }

        BlockPos pos = findPlacement(level, bounds, player.blockPosition(), true);
        if (pos == null) pos = findPlacement(level, bounds, player.blockPosition(), false);
        if (pos != null && placeStorage(level, pos, lootTable)) {
            data.markHandled(key);
        }
    }

    private static void resolveLostCitiesBuilding(ServerLevel level, ServerPlayer player) {
        int chunkX = player.chunkPosition().x;
        int chunkZ = player.chunkPosition().z;
        LostCitiesBuildingCompat.BuildingCell building =
                LostCitiesBuildingCompat.buildingAt(level, chunkX, chunkZ);
        if (building == null) return;

        GeneratedStructureStorageSavedData data = GeneratedStructureStorageSavedData.get(level);
        if (data.isHandled(building.key())) return;

        int minX = building.originChunkX() << 4;
        int minZ = building.originChunkZ() << 4;
        int maxX = ((building.originChunkX() + building.width()) << 4) - 1;
        int maxZ = ((building.originChunkZ() + building.depth()) << 4) - 1;
        BoundingBox bounds = new BoundingBox(
                minX, level.getMinBuildHeight(), minZ,
                maxX, level.getMaxBuildHeight() - 1, maxZ
        );
        ResourceLocation lootTable = GeneratedLootCategory.tableFor(building.buildingId());
        if (resolveLoadedContainer(level, bounds, lootTable)) {
            data.markHandled(building.key());
            return;
        }

        BlockPos pos = findPlacement(level, bounds, player.blockPosition(), true);
        if (pos == null) pos = findPlacement(level, bounds, player.blockPosition(), false);
        if (pos != null && placeStorage(level, pos, lootTable)) {
            data.markHandled(building.key());
        }
    }

    private static boolean resolveLoadedContainer(
            ServerLevel level,
            BoundingBox bounds,
            ResourceLocation lootTable
    ) {
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX << 4, bounds.minY(), chunkZ << 4);
                if (!level.hasChunkAt(probe)) continue;
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof Container && bounds.isInside(blockEntity.getBlockPos())) {
                        if (GeneratedContainerLootService.assignLootIfEmpty(level, blockEntity, lootTable)) {
                            // This path runs from a normal player tick after the
                            // chunk is fully loaded, so the standard dirty mark is safe.
                            blockEntity.setChanged();
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static BlockPos findPlacement(
            ServerLevel level,
            BoundingBox bounds,
            BlockPos anchor,
            boolean sheltered
    ) {
        for (int radius = 1; radius <= PLACEMENT_RADIUS; radius++) {
            for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = anchor.offset(dx, dy, dz);
                        if (!bounds.isInside(pos) || !level.hasChunkAt(pos)) continue;
                        if (!level.getBlockState(pos).isAir()) continue;
                        BlockPos below = pos.below();
                        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                        if (sheltered && level.canSeeSky(pos.above())) continue;
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean placeStorage(ServerLevel level, BlockPos pos, ResourceLocation lootTable) {
        var state = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        if (!level.setBlockAndUpdate(pos, state)) return false;
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            level.removeBlock(pos, false);
            return false;
        }
        barrel.setLootTable(lootTable, level.random.nextLong());
        barrel.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return true;
    }

    private static ResourceLocation lootTableFor(ResourceLocation structureId) {
        return GeneratedLootCategory.tableFor(structureId);
    }
}
