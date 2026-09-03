package com.wasted.domesurvival.forge.dome;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.environment.LastWorldWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Read-only scouting phase used before a LastWorld player selects the dome site. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LastWorldSetupProtection {
    public static final int CURRENT_STARTER_TERRAIN_VERSION = 5;
    private static final String DOME_ENTRY_KEY = "domesurvival_lastworld_dome_entry";
    private static final Component HINT = Component.literal(
            "Режим разведки: выберите место для купола и введите /start (без точки)");
    private static final Map<UUID, Long> LAST_HINT_TICK = new HashMap<>();

    private LastWorldSetupProtection() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isLocked(player)) {
            player.sendSystemMessage(Component.literal(
                    "[LastWorld] Сначала исследуйте окрестности. До команды /start нельзя ломать, ставить и открывать блоки."));
            player.sendSystemMessage(Component.literal(
                    "[LastWorld] На выбранном месте введите /start — рельеф выровняется автоматически."));
            return;
        }

        ServerLevel overworld = player.getServer().overworld();
        DomeSavedData saved = DomeSavedData.get(overworld);
        if (LastWorldWorldState.isLastWorld(overworld) && saved.isGenerated()) {
            upgradeStarterTerrainIfNeeded(overworld, saved);
        }
        if (LastWorldWorldState.isLastWorld(overworld)
                && saved.isGenerated()
                && !hasEnteredDome(player)) {
            DomeSpec spec = saved.domeSpec();
            markDomeEntered(player);
            player.teleportTo(
                    overworld,
                    spec.centerX() + 0.5D,
                    spec.baseY(),
                    spec.centerZ() + 0.5D,
                    player.getYRot(),
                    player.getXRot()
            );
            player.sendSystemMessage(Component.literal(
                    "[LastWorld] Вы присоединились к кампании в центре стартового купола."));
        }
    }

    public static void markDomeEntered(ServerPlayer player) {
        persistentData(player).putBoolean(DOME_ENTRY_KEY, true);
    }

    private static boolean hasEnteredDome(ServerPlayer player) {
        return persistentData(player).getBoolean(DOME_ENTRY_KEY);
    }

    private static CompoundTag persistentData(ServerPlayer player) {
        CompoundTag forgeData = player.getPersistentData();
        if (!forgeData.contains(Player.PERSISTED_NBT_TAG)) {
            forgeData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return forgeData.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void upgradeStarterTerrainIfNeeded(ServerLevel level, DomeSavedData saved) {
        if (saved.starterTerrainVersion() >= CURRENT_STARTER_TERRAIN_VERSION) {
            return;
        }

        DomeSpec spec = saved.domeSpec();
        int radius = spec.surfaceRadius();
        int oldMargin = 4;
        int minX = Math.min(spec.centerX() - radius - oldMargin,
                spec.airlockCenterX() - spec.airlockHalfWidth() - oldMargin);
        int maxX = Math.max(spec.centerX() + radius + oldMargin,
                spec.airlockCenterX() + spec.airlockHalfWidth() + oldMargin);
        int minZ = Math.min(spec.centerZ() - radius - oldMargin,
                spec.airlockStartZ() - oldMargin);
        int maxZ = Math.max(spec.centerZ() + radius + oldMargin,
                spec.airlockEndZ() + oldMargin);
        int updateFlags = Block.UPDATE_CLIENTS
                | Block.UPDATE_KNOWN_SHAPE
                | Block.UPDATE_SUPPRESS_DROPS;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int changed = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = x - spec.centerX();
                int dz = z - spec.centerZ();
                boolean playable = isStarterGroundArea(spec, x, z, dx, dz, radius);
                boolean oldConstructionMargin = isOldConstructionMargin(spec, x, z, dx, dz, radius);
                if (!playable && !oldConstructionMargin) {
                    continue;
                }
                int maxY = spec.topY() + 12;
                for (int y = spec.foundationMinY(); y <= maxY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!playable) {
                        // V4 prepared and copied a four-block environmental
                        // margin. Its terrain cap can stay desert-looking, but
                        // every above-ground block in that band was either
                        // cleared by preparation or imported from the old map.
                        if (y >= spec.baseY()
                                && !state.isAir()
                                && !StarterDomeProtection.isProtectedStructureBlock(level, cursor, state)) {
                            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), updateFlags);
                            changed++;
                        }
                        continue;
                    }
                    if (!isOldStarterTerrain(state)) {
                        continue;
                    }
                    BlockPos above = cursor.above();
                    boolean exposed = level.getBlockState(above).getCollisionShape(level, above).isEmpty();
                    level.setBlock(
                            cursor,
                            exposed ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(),
                            updateFlags
                    );
                    changed++;
                }
            }
        }

        saved.markStarterTerrainVersion(CURRENT_STARTER_TERRAIN_VERSION);
        if (changed > 0) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                    "[LastWorld] Грунт купола обновлён: поверхность покрыта травой, основание заполнено землёй ("
                            + changed + " блоков)."), false);
        }
    }

    private static boolean isOldStarterTerrain(BlockState state) {
        return state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SANDSTONE)
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

    private static boolean isStarterGroundArea(
            DomeSpec spec,
            int x,
            int z,
            int dx,
            int dz,
            int radius
    ) {
        if ((long) dx * dx + (long) dz * dz <= (long) radius * radius) {
            return true;
        }

        // The airlock is a rectangular continuation of the playable dome.
        // Earlier migrations only processed the circular room, leaving its
        // original sand and sandstone floor untouched.
        return x >= spec.airlockCenterX() - spec.airlockHalfWidth()
                && x <= spec.airlockCenterX() + spec.airlockHalfWidth()
                && z >= spec.airlockStartZ()
                && z <= spec.airlockEndZ();
    }

    private static boolean isOldConstructionMargin(
            DomeSpec spec,
            int x,
            int z,
            int dx,
            int dz,
            int radius
    ) {
        int oldDomeRadius = radius + 4;
        if ((long) dx * dx + (long) dz * dz <= (long) oldDomeRadius * oldDomeRadius) {
            return true;
        }
        int oldAirlockMargin = 3;
        return x >= spec.airlockCenterX() - spec.airlockHalfWidth() - oldAirlockMargin
                && x <= spec.airlockCenterX() + spec.airlockHalfWidth() + oldAirlockMargin
                && z >= spec.airlockStartZ() - oldAirlockMargin
                && z <= spec.airlockEndZ() + oldAirlockMargin;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HINT_TICK.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isLocked(player)) {
            event.setCanceled(true);
            hint(player);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isLocked(player)) {
            event.setCanceled(true);
            hint(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isLocked(player)) {
            event.setCanceled(true);
            hint(player);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isLocked(player)) {
            event.setCanceled(true);
            hint(player);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && isLocked(player)) {
            event.setCanceled(true);
            hint(player);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && isLocked(player)) {
            event.setCanceled(true);
            hint(player);
        }
    }

    public static boolean isLocked(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return LastWorldWorldState.isLastWorld(level)
                && !DomeSavedData.get(level).isGenerated();
    }

    private static void hint(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        long previous = LAST_HINT_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous >= 40L) {
            LAST_HINT_TICK.put(player.getUUID(), now);
            player.displayClientMessage(HINT, true);
        }
    }
}
