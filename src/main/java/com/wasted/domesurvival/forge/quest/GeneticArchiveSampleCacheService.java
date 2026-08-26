package com.wasted.domesurvival.forge.quest;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Places the physical genetic samples inside the selected archive building.
 *
 * The service performs only a tiny local search after the archive chunk is already loaded.
 * It never scans the world and never forces/generates chunks.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneticArchiveSampleCacheService {
    private static final String SIGNAL_FLAG = "GENETIC_ARCHIVE_SIGNAL_FOUND";
    private static final BlockPos ARCHIVE_CENTER = new BlockPos(-1088, 92, -676);

    private static final List<BlockPos> CACHE_ANCHORS = List.of(
            ARCHIVE_CENTER.offset(2, 0, 0),
            ARCHIVE_CENTER.offset(-2, 0, 0),
            ARCHIVE_CENTER.offset(0, 0, 2),
            ARCHIVE_CENTER.offset(0, 0, -2),
            ARCHIVE_CENTER.offset(3, 0, 1),
            ARCHIVE_CENTER.offset(-3, 0, -1)
    );

    private static final int SEARCH_RADIUS = 2;
    private static final int VERTICAL_RADIUS = 2;

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
            return;
        }

        if (!level.hasChunkAt(ARCHIVE_CENTER)) {
            return;
        }

        BlockPos pos = findCacheSpot(level);
        if (pos != null && placeCache(level, pos)) {
            data.markCachePlaced(pos);
        }
    }

    private static BlockPos findCacheSpot(ServerLevel level) {
        for (BlockPos anchor : CACHE_ANCHORS) {
            for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
                for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                                continue;
                            }

                            BlockPos pos = anchor.offset(dx, dy, dz);

                            if (!level.hasChunkAt(pos)) {
                                continue;
                            }

                            // Keep the crate close to the selected interior arrival point.
                            if (pos.distSqr(ARCHIVE_CENTER) > 16.0D) {
                                continue;
                            }

                            if (isSafePlacement(level, pos)) {
                                return pos.immutable();
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSafePlacement(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }

        // Keep the sample cache inside the building.
        if (level.canSeeSky(pos.above())) {
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

        barrel.setItem(2, new ItemStack(ModItems.CHICKEN_CRYOCAPSULE.get(), 2));
        barrel.setItem(6, new ItemStack(ModItems.SHEEP_CRYOCAPSULE.get(), 2));
        barrel.setItem(10, new ItemStack(ModItems.COW_CRYOCAPSULE.get(), 2));
        barrel.setItem(14, new ItemStack(ModItems.DAMAGED_PIG_CRYOCAPSULE.get(), 1));
        barrel.setChanged();

        level.sendBlockUpdated(pos, barrelState, barrelState, 3);
        return true;
    }
}
