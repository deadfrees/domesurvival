package com.wasted.domesurvival.forge.quest;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Authored one-time interior dressing for the existing genetic-archive building.
 *
 * The coordinates and zones were derived from the actual Anvil region file of
 * the map, not guessed from a radius. The layout intentionally keeps the center
 * clear for the quest arrival/sample cache and leaves both hostile spawner
 * positions usable as failed containment bays.
 *
 * No world scan and no forced chunk loading.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneticArchiveInteriorService {
    private static final String SIGNAL_FLAG = "GENETIC_ARCHIVE_SIGNAL_FOUND";

    private static final BlockPos ARCHIVE_CENTER = new BlockPos(-1088, 92, -676);

    // The authored decor spans four normally-adjacent chunks.
    private static final List<BlockPos> REQUIRED_LOADED_POINTS = List.of(
            new BlockPos(-1097, 92, -680),
            new BlockPos(-1079, 92, -680),
            new BlockPos(-1097, 92, -668),
            new BlockPos(-1079, 92, -668)
    );

    private GeneticArchiveInteriorService() {
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

        GeneticArchiveInteriorSavedData data = GeneticArchiveInteriorSavedData.get(level);
        if (data.decorated()) {
            return;
        }

        // This decorator belongs only to the authored legacy archive. In a
        // normal generated LastWorld save the selected archive is dynamic, so
        // never touch the old map coordinates unless they are also the target
        // chosen for this world.
        BlockPos selectedTarget = GeneticArchiveDiscoverySavedData.get(level).target();
        if (selectedTarget == null
                || horizontalDistanceSqr(selectedTarget, ARCHIVE_CENTER) > 64L * 64L) {
            return;
        }

        for (BlockPos pos : REQUIRED_LOADED_POINTS) {
            if (!level.hasChunkAt(pos)) {
                return;
            }
        }

        if (!matchesAuthoredBuilding(level)) {
            return;
        }

        decorate(level);
        data.markDecorated();
    }

    /**
     * A small signature protects other worlds from receiving this authored
     * layout if somebody reuses the same coordinates with a different map.
     */
    private static boolean matchesAuthoredBuilding(ServerLevel level) {
        return level.getBlockState(new BlockPos(-1101, 93, -687)).is(Blocks.END_STONE_BRICK_STAIRS)
                && level.getBlockState(new BlockPos(-1098, 95, -684)).is(Blocks.COBBLESTONE)
                && level.getBlockState(new BlockPos(-1099, 99, -685)).is(Blocks.IRON_BARS)
                && level.getBlockState(new BlockPos(-1096, 102, -682)).is(Blocks.WHITE_TERRACOTTA)
                && level.getBlockState(new BlockPos(-1088, 103, -676)).is(Blocks.END_STONE_BRICKS);
    }

    private static void decorate(ServerLevel level) {
        // -------------------------------------------------------------
        // NORTH WALL — four tall preservation columns.
        // Three are stable; the fourth is visibly damaged.
        // -------------------------------------------------------------
        placeCryoColumn(level, -1093, -680, false);
        placeCryoColumn(level, -1089, -680, false);
        placeCryoColumn(level, -1085, -680, false);
        placeCryoColumn(level, -1081, -680, true);

        // -------------------------------------------------------------
        // WEST / EAST — failed quarantine bays around the two spawner
        // zones introduced by the previous difficulty phase.
        // South faces stay open so spawned mobs can pressure the player.
        // -------------------------------------------------------------
        addWestContainment(level);
        addEastContainment(level);

        // -------------------------------------------------------------
        // SOUTH — two damaged laboratory benches.
        // -------------------------------------------------------------
        addLabBench(level, -1094, -668, true);
        addLabBench(level, -1085, -668, false);

        placeIfAir(level, new BlockPos(-1096, 92, -670), Blocks.CAULDRON.defaultBlockState());
        placeIfAir(level, new BlockPos(-1080, 92, -670), Blocks.CAULDRON.defaultBlockState());

        // Side storage, deliberately away from the central genetic-sample cache.
        placeArchiveBarrel(level, new BlockPos(-1097, 92, -668), true);
        placeArchiveBarrel(level, new BlockPos(-1079, 92, -668), false);

        // -------------------------------------------------------------
        // Ceiling lighting beneath the existing solid Y=103 roof.
        // -------------------------------------------------------------
        placeIfAir(level, new BlockPos(-1093, 102, -674), Blocks.SEA_LANTERN.defaultBlockState());
        placeIfAir(level, new BlockPos(-1083, 102, -674), Blocks.SEA_LANTERN.defaultBlockState());
        placeIfAir(level, new BlockPos(-1093, 102, -679), Blocks.SEA_LANTERN.defaultBlockState());
        placeIfAir(level, new BlockPos(-1083, 102, -679), Blocks.SEA_LANTERN.defaultBlockState());
    }

    private static void placeCryoColumn(ServerLevel level, int x, int z, boolean damaged) {
        placeIfAir(level, new BlockPos(x, 92, z), Blocks.IRON_BLOCK.defaultBlockState());

        if (damaged) {
            // Broken containment: gaps + red glass + exposed bars.
            placeIfAir(level, new BlockPos(x, 93, z), Blocks.RED_STAINED_GLASS.defaultBlockState());
            placeIfAir(level, new BlockPos(x, 94, z), Blocks.IRON_BARS.defaultBlockState());
            placeIfAir(level, new BlockPos(x, 95, z), Blocks.RED_STAINED_GLASS.defaultBlockState());
        } else {
            placeIfAir(level, new BlockPos(x, 93, z), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            placeIfAir(level, new BlockPos(x, 94, z), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            placeIfAir(level, new BlockPos(x, 95, z), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        }

        placeIfAir(level, new BlockPos(x, 96, z), Blocks.SEA_LANTERN.defaultBlockState());
    }

    private static void addWestContainment(ServerLevel level) {
        for (int y = 92; y <= 93; y++) {
            for (int z = -677; z <= -675; z++) {
                placeIfAir(level, new BlockPos(-1097, y, z), Blocks.IRON_BARS.defaultBlockState());

                // One deliberate break in the inner wall.
                if (!(y == 93 && z == -675)) {
                    placeIfAir(level, new BlockPos(-1093, y, z), Blocks.IRON_BARS.defaultBlockState());
                }
            }

            for (int x = -1096; x <= -1094; x++) {
                placeIfAir(level, new BlockPos(x, y, -677), Blocks.IRON_BARS.defaultBlockState());
            }
        }
    }

    private static void addEastContainment(ServerLevel level) {
        for (int y = 92; y <= 93; y++) {
            for (int z = -677; z <= -675; z++) {
                // One deliberate break toward the central aisle.
                if (!(y == 92 && z == -675)) {
                    placeIfAir(level, new BlockPos(-1083, y, z), Blocks.IRON_BARS.defaultBlockState());
                }
                placeIfAir(level, new BlockPos(-1080, y, z), Blocks.IRON_BARS.defaultBlockState());
            }

            for (int x = -1082; x <= -1081; x++) {
                placeIfAir(level, new BlockPos(x, y, -677), Blocks.IRON_BARS.defaultBlockState());
            }
        }
    }

    private static void addLabBench(ServerLevel level, int startX, int z, boolean leftBench) {
        for (int dx = 0; dx < 4; dx++) {
            placeIfAir(
                    level,
                    new BlockPos(startX + dx, 92, z),
                    Blocks.SMOOTH_QUARTZ.defaultBlockState()
            );
        }

        placeIfAir(level, new BlockPos(startX, 93, z), Blocks.BREWING_STAND.defaultBlockState());
        placeIfAir(level, new BlockPos(startX + 1, 93, z), Blocks.DAYLIGHT_DETECTOR.defaultBlockState());
        placeIfAir(level, new BlockPos(startX + 2, 93, z), Blocks.FLOWER_POT.defaultBlockState());

        // A dead comparator reads visually as an abandoned lab control panel.
        placeIfAir(level, new BlockPos(startX + 3, 93, z), Blocks.COMPARATOR.defaultBlockState());

        if (leftBench) {
            placeIfAir(level, new BlockPos(startX + 1, 94, z), Blocks.END_ROD.defaultBlockState());
        }
    }

    private static void placeArchiveBarrel(ServerLevel level, BlockPos pos, boolean withLog) {
        if (!canPlace(level, pos)) {
            return;
        }

        BlockState state = Blocks.BARREL.defaultBlockState()
                .setValue(BarrelBlock.FACING, Direction.UP);

        if (!level.setBlockAndUpdate(pos, state)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BarrelBlockEntity barrel)) {
            level.removeBlock(pos, false);
            return;
        }

        if (withLog) {
            barrel.setItem(1, archiveLog());
            barrel.setItem(5, new ItemStack(Items.PAPER, 8));
            barrel.setItem(9, new ItemStack(Items.GLASS_BOTTLE, 6));
            barrel.setItem(13, new ItemStack(Items.REDSTONE, 4));
            barrel.setItem(17, new ItemStack(Items.IRON_NUGGET, 12));
        } else {
            barrel.setItem(2, new ItemStack(Items.GLASS_BOTTLE, 5));
            barrel.setItem(6, new ItemStack(Items.REDSTONE, 3));
            barrel.setItem(10, new ItemStack(Items.IRON_INGOT, 2));
            barrel.setItem(14, new ItemStack(Items.WHEAT_SEEDS, 6));
            barrel.setItem(18, new ItemStack(Items.SUGAR, 4));
        }

        barrel.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private static ItemStack archiveLog() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();

        tag.putString("title", "Протокол 7-Б");
        tag.putString("author", "Генетический отдел");
        tag.putBoolean("resolved", true);

        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
                "Объект 7-Б. Автоматическая консервация завершена. "
                        + "Стабильные линии: птица, овца, крупный рогатый скот."
        ))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
                "Линия свиньи повреждена при аварийном отключении. "
                        + "Инкубационный комплекс остановлен. Образец не уничтожать."
        ))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
                "Если энергоснабжение когда-нибудь восстановят, "
                        + "потребуется чистая вода и новая камера инкубации."
        ))));

        tag.put("pages", pages);
        return book;
    }

    private static boolean canPlace(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).isAir();
    }

    private static void placeIfAir(ServerLevel level, BlockPos pos, BlockState state) {
        if (canPlace(level, pos)) {
            level.setBlockAndUpdate(pos, state);
        }
    }

    private static long horizontalDistanceSqr(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }
}
