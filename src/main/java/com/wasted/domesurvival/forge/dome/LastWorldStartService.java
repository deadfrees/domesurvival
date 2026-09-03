package com.wasted.domesurvival.forge.dome;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.airlock.StarterDomeAirlockV58;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.environment.LastWorldWorldState;
import com.wasted.domesurvival.forge.progression.JosephNpcCommands;
import com.wasted.domesurvival.forge.progression.WorkshopUpgradeApplier;
import com.wasted.domesurvival.forge.quest.QuestActionEvents;
import com.wasted.domesurvival.forge.weather.SurfaceWeatherService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import org.slf4j.Logger;

/** Builds the packaged authored dome at the site selected with /domestart. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LastWorldStartService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int OPENING_SANDSTORM_SECONDS = 180;
    private static final int TERRAFORM_BLOCKS_PER_TICK = 6000;
    private static final int TERRAFORM_TOP_CLEARANCE = 16;
    private static final int CLEANUP_BLOCKS_PER_TICK = 6000;
    private static final int TERRAFORM_UPDATE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Deque<LastWorldDomeTransfer.Tile> QUEUE = new ArrayDeque<>();

    private static DomeSpec targetSpec;
    private static boolean running;
    private static int placed;
    private static int total;
    private static int terrainX;
    private static int terrainY;
    private static int terrainZ;
    private static int terrainColumnEndY;
    private static int terrainChangedBlocks;
    private static boolean terrainColumnActive;
    private static int cleanupX;
    private static int cleanupY;
    private static int cleanupZ;
    private static int removedProgressBlocks;
    private static int removedProgressAnimals;
    private static int preparedSoilBlocks;
    private static int starterLights;
    private static Phase phase = Phase.IDLE;

    private LastWorldStartService() {
    }

    public static synchronized StartResult begin(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LOGGER.info("LastWorld start requested by {} at {}", player.getGameProfile().getName(), player.blockPosition());
        if (!Level.OVERWORLD.equals(level.dimension()) || !LastWorldWorldState.isLastWorld(level)) {
            return new StartResult(Status.WRONG_WORLD,
                    "Команда работает только в новом мире, созданном с пресетом LastWorld.");
        }

        DomeSavedData saved = DomeSavedData.get(level);
        if (saved.isGenerated()) {
            return new StartResult(Status.ALREADY_STARTED, "Игра уже запущена, купол существует.");
        }
        if (running) {
            return new StartResult(Status.ALREADY_RUNNING,
                    "Купол уже строится: " + placed + "/" + total + " секций.");
        }

        SiteResult site = inspectSite(level, player.blockPosition());
        if (!site.valid()) {
            return new StartResult(Status.BAD_SITE, site.message());
        }

        DomeSpec selected = DomeSpec.wastedV1().at(site.center().getX(), site.center().getY(), site.center().getZ());
        saved.setDomeLocation(site.center());
        targetSpec = selected;
        QUEUE.clear();
        QUEUE.addAll(LastWorldDomeTransfer.tiles());
        total = QUEUE.size();
        placed = 0;
        beginTerrainPreparation();
        running = true;
        phase = Phase.PREPARING;

        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "[LastWorld] Место утверждено. Рельеф будет автоматически срезан или достроен под основание купола."), false);
        LOGGER.info("LastWorld dome construction accepted: center={}, tiles={}", site.center(), total);
        return new StartResult(Status.STARTED,
                "Подготовка площадки и строительство начались. Оставайтесь рядом.");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !running) {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (phase == Phase.PREPARING) {
            if (!tickTerrainPreparation(level)) {
                return;
            }
            phase = Phase.PLACING;
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "[LastWorld] Основание готово (изменено блоков: " + terrainChangedBlocks
                            + "). Начинается установка секций купола."), false);
            LOGGER.info("LastWorld terrain preparation completed: changedBlocks={}", terrainChangedBlocks);
            return;
        }

        if (phase == Phase.PLACING) {
            LastWorldDomeTransfer.Tile tile = QUEUE.pollFirst();
            if (tile != null && !placeTile(level, tile)) {
                fail(server, "Не удалось разместить секцию " + tile.id() + ". Можно повторить /domestart.");
                return;
            }
            if (tile != null) {
                placed++;
            }
            if (!QUEUE.isEmpty()) {
                return;
            }
            WorkshopUpgradeApplier.RemovalResult workshop =
                    WorkshopUpgradeApplier.prepareInitialState(level);
            if (workshop != WorkshopUpgradeApplier.RemovalResult.REMOVED) {
                fail(server, "Не удалось убрать мастерскую из стартового состояния: "
                        + workshop.name() + ". Можно повторить /domestart.");
                return;
            }
            beginCleanup();
            phase = Phase.CLEANING;
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "[LastWorld] Каркас готов. Подготавливается стартовое состояние без построек из будущих заданий."), false);
            return;
        }

        if (phase == Phase.CLEANING && !tickCleanup(level)) {
            return;
        }

        StarterDomeAirlockV58.InstallResult airlock = StarterDomeAirlockV58.install(level);
        if (airlock != StarterDomeAirlockV58.InstallResult.SUCCESS) {
            fail(server, "Не удалось активировать шлюз: " + airlock.name() + ". Можно повторить /domestart.");
            return;
        }

        DomeSavedData saved = DomeSavedData.get(level);
        saved.markStructureVersion(DomeGenerationService.CURRENT_STRUCTURE_VERSION);
        saved.markStarterTerrainVersion(LastWorldSetupProtection.CURRENT_STARTER_TERRAIN_VERSION);
        saved.resetAirlock();
        starterLights = installStarterLighting(level);
        JosephNpcCommands.StarterNpcRestore npcs = JosephNpcCommands.ensureStarterNpcs(level);
        LOGGER.info("LastWorld NPC restore: {}", npcs);
        if (!npcs.complete()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "[LastWorld] Купол готов, но не все NPC восстановлены (" + npcs
                            + "). Проверь загрузку CustomNPCs и внешних сценариев."), false);
        }
        AABB domeInterior = new AABB(
                targetSpec.centerX() - targetSpec.surfaceRadius(),
                targetSpec.baseY() - 2,
                targetSpec.centerZ() - targetSpec.surfaceRadius(),
                targetSpec.centerX() + targetSpec.surfaceRadius() + 1,
                targetSpec.topY() + 1,
                targetSpec.centerZ() + targetSpec.surfaceRadius() + 1
        );
        var animals = level.getEntitiesOfClass(Animal.class, domeInterior);
        removedProgressAnimals = animals.size();
        animals.forEach(Animal::discard);
        level.getEntitiesOfClass(ItemEntity.class, domeInterior).forEach(ItemEntity::discard);
        BlockPos spawn = new BlockPos(targetSpec.centerX(), targetSpec.baseY(), targetSpec.centerZ());
        level.setDefaultSpawnPos(spawn, 0.0F);
        level.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (Level.OVERWORLD.equals(player.level().dimension())) {
                LastWorldSetupProtection.markDomeEntered(player);
                player.teleportTo(level,
                        targetSpec.centerX() + 0.5D,
                        targetSpec.baseY(),
                        targetSpec.centerZ() + 0.5D,
                        player.getYRot(),
                        player.getXRot());
                QuestActionEvents.onDomeStarted(player);
            }
        }

        level.setWeatherParameters(6000, 0, false, false);
        SurfaceWeatherService.startSandstorm(level, OPENING_SANDSTORM_SECONDS);
        running = false;
        phase = Phase.IDLE;
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "[LastWorld] Купол развёрнут в стартовом состоянии (убрано объектов будущей прогрессии: "
                        + removedProgressBlocks + ", животных: " + removedProgressAnimals
                        + ", подготовлено блоков грунта: " + preparedSoilBlocks
                        + ", стартовых фонарей: " + starterLights
                        + "). Игра началась — снаружи начинается смертельная песчаная буря."), false);
    }

    @SubscribeEvent
    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        QUEUE.clear();
        targetSpec = null;
        running = false;
        placed = 0;
        total = 0;
        phase = Phase.IDLE;
    }

    public static synchronized boolean isRunning() {
        return running;
    }

    public static synchronized String progress() {
        return placed + "/" + total;
    }

    private static boolean placeTile(ServerLevel level, LastWorldDomeTransfer.Tile tile) {
        StructureTemplate template = level.getServer().getStructureManager().get(tile.id()).orElse(null);
        if (template == null || targetSpec == null) {
            return false;
        }
        BlockPos origin = LastWorldDomeTransfer.targetOrigin(tile, targetSpec);
        int maxX = origin.getX() + tile.size().getX() - 1;
        int maxZ = origin.getZ() + tile.size().getZ() - 1;
        for (int chunkX = origin.getX() >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = origin.getZ() >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
        StructurePlaceSettings settings = new StructurePlaceSettings()
                // Starter NPCs are recreated deterministically after all tiles
                // are placed. Importing template entities caused duplicate
                // Joseph instances at tile-relative positions.
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .addProcessor(domePlacementBounds())
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        return template.placeInWorld(
                level,
                origin,
                origin,
                settings,
                RandomSource.create(level.getSeed() ^ origin.asLong()),
                Block.UPDATE_CLIENTS
        );
    }

    private static void beginTerrainPreparation() {
        terrainX = cleanupMinX();
        terrainZ = cleanupMinZ();
        terrainY = 0;
        terrainColumnEndY = -1;
        terrainChangedBlocks = 0;
        terrainColumnActive = false;
    }

    /**
     * Creates a level, supported construction pad without requiring naturally
     * flat terrain. Only the playable dome/airlock footprint is changed: the
     * exported four-block capture margin must never terraform a new world.
     */
    private static boolean tickTerrainPreparation(ServerLevel level) {
        int maxX = cleanupMaxX();
        int maxZ = cleanupMaxZ();
        int padY = targetSpec.baseY() - 1;
        int clearTopY = Math.min(
                level.getMaxBuildHeight() - 1,
                targetSpec.topY() + TERRAFORM_TOP_CLEARANCE
        );
        int budget = TERRAFORM_BLOCKS_PER_TICK;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (budget > 0) {
            if (terrainX > maxX) {
                return true;
            }

            if (!isConstructionFootprint(terrainX, terrainZ)) {
                advanceTerrainColumn();
                continue;
            }

            if (!terrainColumnActive) {
                level.getChunk(terrainX >> 4, terrainZ >> 4);
                int surfaceY = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        terrainX,
                        terrainZ
                ) - 1;
                terrainY = Math.max(
                        level.getMinBuildHeight(),
                        Math.min(surfaceY + 1, padY)
                );
                terrainColumnEndY = Math.max(surfaceY, clearTopY);
                terrainColumnActive = true;
            }

            cursor.set(terrainX, terrainY, terrainZ);
            BlockState desired;
            if (terrainY < padY) {
                desired = Blocks.DIRT.defaultBlockState();
            } else if (terrainY == padY) {
                desired = Blocks.GRASS_BLOCK.defaultBlockState();
            } else {
                desired = Blocks.AIR.defaultBlockState();
            }

            if (!level.getBlockState(cursor).equals(desired)) {
                level.setBlock(cursor, desired, TERRAFORM_UPDATE_FLAGS);
                terrainChangedBlocks++;
            }
            budget--;
            terrainY++;

            if (terrainY > terrainColumnEndY) {
                advanceTerrainColumn();
            }
        }
        return false;
    }

    private static boolean isConstructionFootprint(int x, int z) {
        return isPlayableInterior(x, z) || isOuterShellColumn(x, z);
    }

    private static void advanceTerrainColumn() {
        terrainColumnActive = false;
        terrainZ++;
        if (terrainZ > cleanupMaxZ()) {
            terrainZ = cleanupMinZ();
            terrainX++;
        }
    }

    private static void beginCleanup() {
        cleanupX = cleanupMinX();
        cleanupY = targetSpec.foundationMinY();
        cleanupZ = cleanupMinZ();
        removedProgressBlocks = 0;
        removedProgressAnimals = 0;
        preparedSoilBlocks = 0;
        starterLights = 0;
    }

    /** Returns true after the entire playable dome interior has been normalized. */
    private static boolean tickCleanup(ServerLevel level) {
        int maxX = cleanupMaxX();
        int minY = targetSpec.foundationMinY();
        int maxY = targetSpec.topY() + 12;
        int minZ = cleanupMinZ();
        int maxZ = cleanupMaxZ();

        int budget = CLEANUP_BLOCKS_PER_TICK;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (budget-- > 0) {
            if (cleanupX > maxX) {
                return true;
            }

            cursor.set(cleanupX, cleanupY, cleanupZ);
            BlockState state = level.getBlockState(cursor);
            if (StarterDomeInitialState.shouldRemove(state)) {
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                removedProgressBlocks++;
            } else if (isPlayableInterior(cleanupX, cleanupZ) && state.is(Blocks.DIRT_PATH)) {
                // Joseph's first road reward must not be visible before its quest.
                level.setBlock(cursor, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
                removedProgressBlocks++;
            } else if (isPlayableInterior(cleanupX, cleanupZ)
                    && isStarterTerrain(state)) {
                BlockState above = level.getBlockState(cursor.above());
                boolean exposed = above.isAir()
                        || StarterDomeInitialState.shouldRemove(above)
                        || above.getCollisionShape(level, cursor.above()).isEmpty();
                level.setBlock(
                        cursor,
                        exposed ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(),
                        Block.UPDATE_ALL
                );
                preparedSoilBlocks++;
            }

            cleanupY++;
            if (cleanupY > maxY) {
                cleanupY = minY;
                cleanupZ++;
                if (cleanupZ > maxZ) {
                    cleanupZ = minZ;
                    cleanupX++;
                }
            }
        }
        return false;
    }

    private static boolean isStarterTerrain(BlockState state) {
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
            return true;
        }
        return state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.SMOOTH_SANDSTONE)
                || state.is(Blocks.SANDSTONE_STAIRS)
                || state.is(Blocks.SANDSTONE_SLAB)
                || state.is(Blocks.CUT_SANDSTONE_SLAB)
                || state.is(Blocks.SMOOTH_SANDSTONE_STAIRS)
                || state.is(Blocks.SMOOTH_SANDSTONE_SLAB)
                || state.is(Blocks.SANDSTONE_WALL)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.CUT_RED_SANDSTONE)
                || state.is(Blocks.CHISELED_RED_SANDSTONE)
                || state.is(Blocks.SMOOTH_RED_SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE_STAIRS)
                || state.is(Blocks.RED_SANDSTONE_SLAB)
                || state.is(Blocks.CUT_RED_SANDSTONE_SLAB)
                || state.is(Blocks.SMOOTH_RED_SANDSTONE_STAIRS)
                || state.is(Blocks.SMOOTH_RED_SANDSTONE_SLAB)
                || state.is(Blocks.RED_SANDSTONE_WALL);
    }

    /** Only blocks belonging to the dome shell/interior or its airlock leave the template. */
    private static StructureProcessor domePlacementBounds() {
        return new StructureProcessor() {
            @Override
            @Nullable
            public StructureTemplate.StructureBlockInfo processBlock(
                    LevelReader level,
                    BlockPos offset,
                    BlockPos pivot,
                    StructureTemplate.StructureBlockInfo original,
                    StructureTemplate.StructureBlockInfo placed,
                    StructurePlaceSettings settings
            ) {
                BlockPos pos = placed.pos();
                if (!isPlayableInterior(pos.getX(), pos.getZ())
                        && !isExpectedOuterShellBlock(pos, placed.state())) {
                    return null;
                }
                return placed;
            }

            @Override
            protected StructureProcessorType<?> getType() {
                // This processor is runtime-only and is never serialized.
                return StructureProcessorType.BLOCK_IGNORE;
            }
        };
    }

    private static boolean isPlayableInterior(int x, int z) {
        int dx = x - targetSpec.centerX();
        int dz = z - targetSpec.centerZ();
        int radius = targetSpec.surfaceRadius();
        if ((long) dx * dx + (long) dz * dz <= (long) radius * radius) {
            return true;
        }
        return x >= targetSpec.airlockCenterX() - targetSpec.airlockHalfWidth()
                && x <= targetSpec.airlockCenterX() + targetSpec.airlockHalfWidth()
                && z >= targetSpec.airlockStartZ()
                && z <= targetSpec.airlockEndZ();
    }

    /**
     * The voxelized spherical shell can extend less than half a block beyond
     * the mathematical R=50 floor circle. This is dome geometry, not the old
     * four-block template margin.
     */
    private static boolean isOuterShellColumn(int x, int z) {
        int dx = x - targetSpec.centerX();
        int dz = z - targetSpec.centerZ();
        long distanceSqrTimesFour = 4L * ((long) dx * dx + (long) dz * dz);
        long outerDiameter = 2L * targetSpec.surfaceRadius() + 1L;
        return distanceSqrTimesFour <= outerDiameter * outerDiameter;
    }

    private static boolean isExpectedOuterShellBlock(BlockPos pos, BlockState state) {
        return isOuterShellColumn(pos.getX(), pos.getZ())
                && (state.is(ModBlocks.REINFORCED_GLASS.get())
                || state.is(ModBlocks.DOME_FRAME.get()));
    }

    /** Adds unobtrusive lighting only where the authored layout left a clear grass tile. */
    private static int installStarterLighting(ServerLevel level) {
        int[][] offsets = {{10, 0}, {-10, 0}, {0, 10}, {0, -10}};
        int placed = 0;
        for (int[] offset : offsets) {
            BlockPos ground = new BlockPos(
                    targetSpec.centerX() + offset[0],
                    targetSpec.baseY() - 1,
                    targetSpec.centerZ() + offset[1]
            );
            BlockPos post = ground.above();
            BlockPos lamp = post.above();
            if ((level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                    || level.getBlockState(ground).is(Blocks.DIRT))
                    && level.getBlockState(post).isAir()
                    && level.getBlockState(lamp).isAir()) {
                level.setBlock(post, Blocks.STONE_BRICK_WALL.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(lamp, Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
                placed++;
            }
        }
        return placed;
    }

    private static int cleanupMinX() {
        return targetSpec.centerX() - targetSpec.surfaceRadius() - 4;
    }

    private static int cleanupMaxX() {
        return targetSpec.centerX() + targetSpec.surfaceRadius() + 4;
    }

    private static int cleanupMinZ() {
        return targetSpec.centerZ() - targetSpec.surfaceRadius() - 4;
    }

    private static int cleanupMaxZ() {
        return Math.max(
                targetSpec.centerZ() + targetSpec.surfaceRadius() + 4,
                targetSpec.airlockEndZ() + 4
        );
    }

    private static SiteResult inspectSite(ServerLevel level, BlockPos requested) {
        int centerX = requested.getX();
        int centerZ = requested.getZ();
        level.getChunk(centerX >> 4, centerZ >> 4);
        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
        int requiredTop = baseY + DomeSpec.wastedV1().surfaceRadius() + TERRAFORM_TOP_CLEARANCE;
        if (baseY < level.getMinBuildHeight() + 8 || requiredTop >= level.getMaxBuildHeight()) {
            return new SiteResult(false, BlockPos.ZERO,
                    "На этой высоте купол выйдет за границы мира. Выберите место ниже или выше.");
        }
        return new SiteResult(true, new BlockPos(centerX, baseY, centerZ),
                "Площадка будет автоматически выровнена.");
    }

    private static void fail(MinecraftServer server, String reason) {
        QUEUE.clear();
        running = false;
        phase = Phase.IDLE;
        targetSpec = null;
        LOGGER.error("LastWorld dome construction stopped: {}", reason);
        server.getPlayerList().broadcastSystemMessage(Component.literal("[LastWorld] " + reason), false);
    }

    private record SiteResult(boolean valid, BlockPos center, String message) {
    }

    public record StartResult(Status status, String message) {
        public boolean started() {
            return status == Status.STARTED;
        }
    }

    public enum Status {
        STARTED,
        WRONG_WORLD,
        ALREADY_STARTED,
        ALREADY_RUNNING,
        BAD_SITE,
        MISSING_TEMPLATE
    }

    private enum Phase {
        IDLE,
        PREPARING,
        PLACING,
        CLEANING
    }
}
