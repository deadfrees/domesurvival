package com.wasted.domesurvival.forge.loot;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.compat.lostcities.LostCitiesBuildingCompat;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.lanos.LanosTrunkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.Heightmap;
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
    private static final int LANOS_CHANCE_PERCENT = 18;
    private static final int LANOS_SEARCH_RADIUS = 14;
    private static final ResourceLocation LANOS_TRUNK_LOOT =
            new ResourceLocation(DomeSurvival.MOD_ID, "chests/lanos_trunk");

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
        tryPlaceLanos(level, start.getBoundingBox(), player.blockPosition(), key, data);
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

        int minX = building.originChunkX() << 4;
        int minZ = building.originChunkZ() << 4;
        int maxX = ((building.originChunkX() + building.width()) << 4) - 1;
        int maxZ = ((building.originChunkZ() + building.depth()) << 4) - 1;
        BoundingBox bounds = new BoundingBox(
                minX, level.getMinBuildHeight(), minZ,
                maxX, level.getMaxBuildHeight() - 1, maxZ
        );
        tryPlaceLanos(level, bounds, player.blockPosition(), building.key(), data);
        if (data.isHandled(building.key())) return;
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

    private static void tryPlaceLanos(
            ServerLevel level,
            BoundingBox bounds,
            BlockPos playerPos,
            String structureKey,
            GeneratedStructureStorageSavedData data
    ) {
        String decorationKey = "lanos:" + structureKey;
        if (data.isHandled(decorationKey)) return;

        RandomSource random = RandomSource.create(level.getSeed() ^ (long) structureKey.hashCode() * 0x9E3779B97F4A7C15L);
        if (random.nextInt(100) >= LANOS_CHANCE_PERCENT) {
            data.markHandled(decorationKey);
            return;
        }

        LanosPlacement placement = findLanosPlacement(level, bounds, playerPos, random);
        if (placement == null) return;

        BlockState state = (random.nextInt(100) < 85
                ? ModBlocks.LANOS_ABANDONED.get()
                : ModBlocks.LANOS_DECORATIVE.get()).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, placement.facing());
        if (level.setBlockAndUpdate(placement.pos(), state)) {
            if (level.getBlockEntity(placement.pos()) instanceof LanosTrunkBlockEntity trunk) {
                trunk.setLootTable(LANOS_TRUNK_LOOT, random.nextLong());
                trunk.setChanged();
            }
            data.markHandled(decorationKey);
        }
    }

    private static LanosPlacement findLanosPlacement(
            ServerLevel level,
            BoundingBox bounds,
            BlockPos playerPos,
            RandomSource random
    ) {
        for (int attempt = 0; attempt < 36; attempt++) {
            int gap = 2 + random.nextInt(5);
            int side = random.nextInt(4);
            int x;
            int z;
            if (side < 2) {
                x = bounds.minX() + random.nextInt(Math.max(1, bounds.maxX() - bounds.minX() + 1));
                z = side == 0 ? bounds.minZ() - gap : bounds.maxZ() + gap;
            } else {
                x = side == 2 ? bounds.minX() - gap : bounds.maxX() + gap;
                z = bounds.minZ() + random.nextInt(Math.max(1, bounds.maxZ() - bounds.minZ() + 1));
            }

            // Large underground structures can have distant bounding-box edges.
            // Wait until the player naturally loads that edge; never force chunks.
            if (Math.abs(x - playerPos.getX()) > LANOS_SEARCH_RADIUS * 2
                    || Math.abs(z - playerPos.getZ()) > LANOS_SEARCH_RADIUS * 2) continue;

            BlockPos column = new BlockPos(x, playerPos.getY(), z);
            if (!level.hasChunkAt(column)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (insideGeneratedDome(level, pos)) continue;

            Direction facing = Direction.from2DDataValue(random.nextInt(4));
            if (canFitLanos(level, pos, facing)) return new LanosPlacement(pos, facing);
        }
        return null;
    }

    private static boolean canFitLanos(ServerLevel level, BlockPos anchor, Direction facing) {
        boolean alongX = facing.getAxis() == Direction.Axis.X;
        int minX = alongX ? -3 : -1;
        int maxX = alongX ? 3 : 1;
        int minZ = alongX ? -1 : -3;
        int maxZ = alongX ? 1 : 3;

        for (int dx = minX; dx <= maxX; dx++) {
            for (int dz = minZ; dz <= maxZ; dz++) {
                BlockPos floor = anchor.offset(dx, -1, dz);
                if (!level.hasChunkAt(floor)
                        || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
                for (int dy = 0; dy <= 2; dy++) {
                    if (!level.getBlockState(anchor.offset(dx, dy, dz)).isAir()) return false;
                }
            }
        }
        return true;
    }

    private static boolean insideGeneratedDome(ServerLevel level, BlockPos pos) {
        DomeSavedData dome = DomeSavedData.get(level);
        return dome.isGenerated() && new DomeBounds(dome.domeSpec()).isSafe(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }

    private record LanosPlacement(BlockPos pos, Direction facing) {
    }

    private static ResourceLocation lootTableFor(ResourceLocation structureId) {
        return GeneratedLootCategory.tableFor(structureId);
    }
}
