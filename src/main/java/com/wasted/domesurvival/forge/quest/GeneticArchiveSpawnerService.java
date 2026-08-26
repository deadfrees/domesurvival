package com.wasted.domesurvival.forge.quest;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Places two vanilla hostile-mob spawners inside the selected genetic archive building.
 *
 * The building itself already exists on the authored map. This service never scans the world
 * and never forces chunks: it only searches a tiny local volume after the archive chunk is loaded.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneticArchiveSpawnerService {
    private static final String SIGNAL_FLAG = "GENETIC_ARCHIVE_SIGNAL_FOUND";

    // Selected building / archive point from V1.3.
    private static final BlockPos ARCHIVE_CENTER = new BlockPos(-1088, 92, -676);

    // Search two opposite interior zones around the arrival point.
    private static final List<BlockPos> ZOMBIE_ANCHORS = List.of(
            ARCHIVE_CENTER.offset(7, 0, 0),
            ARCHIVE_CENTER.offset(6, 0, 4),
            ARCHIVE_CENTER.offset(4, 0, 7)
    );

    private static final List<BlockPos> SKELETON_ANCHORS = List.of(
            ARCHIVE_CENTER.offset(-7, 0, 0),
            ARCHIVE_CENTER.offset(-6, 0, -4),
            ARCHIVE_CENTER.offset(-4, 0, -7)
    );

    private static final int LOCAL_SEARCH_RADIUS = 4;
    private static final int VERTICAL_SEARCH_RADIUS = 3;
    private static final double MIN_SEPARATION_SQR = 8.0D * 8.0D;

    private GeneticArchiveSpawnerService() {
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

        GeneticArchiveSpawnerSavedData data = GeneticArchiveSpawnerSavedData.get(level);
        if (data.complete()) {
            return;
        }

        // Never load/generate the archive chunk just to place spawners.
        if (!level.hasChunkAt(ARCHIVE_CENTER)) {
            return;
        }

        if (!data.zombiePlaced()) {
            BlockPos zombie = findInteriorSpot(level, ZOMBIE_ANCHORS, null);
            if (zombie != null && placeSpawner(level, zombie, EntityType.ZOMBIE)) {
                data.markZombiePlaced(zombie);
            }
        }

        if (!data.skeletonPlaced()) {
            BlockPos avoid = data.zombiePlaced() ? data.zombiePos() : null;
            BlockPos skeleton = findInteriorSpot(level, SKELETON_ANCHORS, avoid);
            if (skeleton != null && placeSpawner(level, skeleton, EntityType.SKELETON)) {
                data.markSkeletonPlaced(skeleton);
            }
        }
    }

    private static BlockPos findInteriorSpot(
            ServerLevel level,
            List<BlockPos> anchors,
            BlockPos avoid
    ) {
        for (BlockPos anchor : anchors) {
            for (int radius = 0; radius <= LOCAL_SEARCH_RADIUS; radius++) {
                for (int dy = -VERTICAL_SEARCH_RADIUS; dy <= VERTICAL_SEARCH_RADIUS; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                                continue;
                            }

                            BlockPos pos = anchor.offset(dx, dy, dz);

                            if (!level.hasChunkAt(pos)) {
                                continue;
                            }

                            if (avoid != null && pos.distSqr(avoid) < MIN_SEPARATION_SQR) {
                                continue;
                            }

                            // Do not put a cage directly on the arrival point.
                            if (pos.distSqr(ARCHIVE_CENTER) < 16.0D) {
                                continue;
                            }

                            if (!isSafeInteriorPlacement(level, pos)) {
                                continue;
                            }

                            return pos.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSafeInteriorPlacement(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Only replace actual air: never destroy authored map blocks, containers or machines.
        if (!state.isAir()) {
            return false;
        }

        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }

        // Prefer sheltered positions so the hostile mobs do not immediately burn in sunlight.
        if (level.canSeeSky(pos.above())) {
            return false;
        }

        return true;
    }

    private static boolean placeSpawner(
            ServerLevel level,
            BlockPos pos,
            EntityType<?> type
    ) {
        if (!level.setBlockAndUpdate(pos, Blocks.SPAWNER.defaultBlockState())) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawnerBlockEntity)) {
            // Extremely defensive: if another mod replaced the BE unexpectedly, restore air.
            level.removeBlock(pos, false);
            return false;
        }

        // Forge/Mojmap 1.20.1 BaseSpawner API.
        spawnerBlockEntity.getSpawner().setEntityId(type, level, level.random, pos);
        spawnerBlockEntity.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);

        return true;
    }
}
