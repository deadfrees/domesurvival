package com.wasted.domesurvival.forge.quest;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.bio.BioLootData;
import com.wasted.domesurvival.forge.bio.BioModuleDistributionSavedData;
import com.wasted.domesurvival.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Places the physical genetic samples at the generated structure selected for this world.
 *
 * The service performs only a tiny local search after the archive chunk is already loaded.
 * It never scans the world and never forces/generates chunks.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneticArchiveSampleCacheService {
    private static final String SIGNAL_FLAG = "GENETIC_ARCHIVE_SIGNAL_FOUND";
    private static final int CONTAINER_CHUNK_RADIUS = 4;
    private static final int PLACEMENT_RADIUS = 8;
    private static final int VERTICAL_RADIUS = 6;

    private GeneticArchiveSampleCacheService() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        ServerLevel level = event.getServer().overworld();

        if (!QuestProgressService.has(level, SIGNAL_FLAG)) {
            return;
        }

        GeneticArchiveSampleSavedData data = GeneticArchiveSampleSavedData.get(level);
        if (data.cachePlaced()) {
            registerGuaranteedDistribution(level, data);
            return;
        }

        BlockPos target = GeneticArchiveDiscoverySavedData.get(level).target();
        if (target == null || !level.hasChunkAt(target)) {
            return;
        }

        BlockPos container = findNearestLoadedContainer(level, target);
        BlockPos pos = null;
        if (container != null) {
            pos = findCacheSpot(level, container, true);
            if (pos == null) {
                pos = findCacheSpot(level, container, false);
            }
        }
        if (pos == null) {
            // Structure locators are allowed to return a technical Y (commonly
            // zero). If the building has no loaded storage of its own, anchor
            // the guaranteed archive barrel to the actual surface instead of
            // searching an arbitrary underground layer forever.
            int surfaceY = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    target.getX(),
                    target.getZ()
            );
            BlockPos surfaceAnchor = new BlockPos(target.getX(), surfaceY, target.getZ());
            pos = findCacheSpot(level, surfaceAnchor, false);
            if (pos == null && isSafePlacement(level, surfaceAnchor, false)) {
                pos = surfaceAnchor;
            }
        }
        if (pos != null && placeCache(level, pos)) {
            data.markCachePlaced(pos);
            registerGuaranteedDistribution(level, data);
        }
    }

    private static void registerGuaranteedDistribution(
            ServerLevel level,
            GeneticArchiveSampleSavedData data
    ) {
        if (data.distributionRegistered()) {
            return;
        }

        BlockPos cachePos = data.cachePos();
        if (!level.hasChunkAt(cachePos)) {
            return;
        }

        BioModuleDistributionSavedData.get(level).recordGuaranteedArchiveSamples(
                cachePos,
                BioLootData.distributionLocationKey(level, cachePos)
        );
        data.markDistributionRegistered();
    }

    private static BlockPos findNearestLoadedContainer(ServerLevel level, BlockPos target) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int targetChunkX = target.getX() >> 4;
        int targetChunkZ = target.getZ() >> 4;

        for (int chunkX = targetChunkX - CONTAINER_CHUNK_RADIUS;
             chunkX <= targetChunkX + CONTAINER_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = targetChunkZ - CONTAINER_CHUNK_RADIUS;
                 chunkZ <= targetChunkZ + CONTAINER_CHUNK_RADIUS; chunkZ++) {
                BlockPos chunkProbe = new BlockPos(chunkX << 4, target.getY(), chunkZ << 4);
                if (!level.hasChunkAt(chunkProbe)) {
                    continue;
                }

                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof Container)) {
                        continue;
                    }

                    BlockPos pos = blockEntity.getBlockPos();
                    double distance = horizontalDistanceSqr(pos, target);
                    if (distance < bestDistance) {
                        best = pos.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    private static BlockPos findCacheSpot(ServerLevel level, BlockPos anchor, boolean sheltered) {
        for (int radius = 1; radius <= PLACEMENT_RADIUS; radius++) {
            for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos pos = anchor.offset(dx, dy, dz);
                        if (isSafePlacement(level, pos, sheltered)) {
                            return pos.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafePlacement(ServerLevel level, BlockPos pos, boolean sheltered) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }

        if (sheltered && level.canSeeSky(pos.above())) {
            return false;
        }

        return true;
    }

    private static boolean placeCache(ServerLevel level, BlockPos pos) {
        BlockState barrelState = Blocks.BARREL.defaultBlockState()
                .setValue(BarrelBlock.FACING, Direction.UP);

        if (!level.setBlockAndUpdate(pos, barrelState)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BarrelBlockEntity barrel)) {
            level.removeBlock(pos, false);
            return false;
        }

        // Each archive contributes only one specimen of a species. Additional
        // modules must be recovered from the global structure-loot system.
        barrel.setItem(2, new ItemStack(ModItems.CHICKEN_CRYOCAPSULE.get(), 1));
        barrel.setItem(6, new ItemStack(ModItems.SHEEP_CRYOCAPSULE.get(), 1));
        barrel.setItem(10, new ItemStack(ModItems.COW_CRYOCAPSULE.get(), 1));
        barrel.setItem(14, new ItemStack(ModItems.DAMAGED_PIG_CRYOCAPSULE.get(), 1));
        barrel.setChanged();

        level.sendBlockUpdated(pos, barrelState, barrelState, 3);
        return true;
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }
}
