package com.wasted.domesurvival.forge.client.music;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.client.weather.ClientSurfaceWeatherState;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexRegistry;
import com.wasted.domesurvival.forge.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * DomeSurvival music controller — vanilla-style scheduler.
 *
 * Core rule:
 * WORLD EVENTS NEVER START OR STOP MUSIC.
 *
 * Weather, combat, night, base, interiors and scripted signals only affect the
 * candidate pool when the random timer decides that it is time to play music.
 * A currently playing track is always allowed to finish naturally.
 *
 * Flow:
 * 1) Enter world -> random silence.
 * 2) Timer expires -> inspect current context once and choose one random track.
 * 3) Track plays once (repeat=false) and finishes naturally.
 * 4) Long random silence.
 * 5) Repeat.
 *
 * This intentionally behaves much closer to vanilla Minecraft's MusicManager
 * concept of a next-song delay than the previous event-driven implementation.
 */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DomeMusicClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int SOUND_START_GRACE_TICKS = 20;

    // No music immediately after world entry.
    // 2-5 minutes, randomized every entry.
    private static final int FIRST_DELAY_MIN_TICKS = 20 * 60 * 2;
    private static final int FIRST_DELAY_MAX_TICKS = 20 * 60 * 5;

    // Vanilla-like quiet time after every completed track.
    // 3-8 minutes, randomized independently every time.
    private static final int BETWEEN_TRACKS_MIN_TICKS = 20 * 60 * 3;
    private static final int BETWEEN_TRACKS_MAX_TICKS = 20 * 60 * 8;

    private static final int RECENT_HISTORY_SIZE = 4;

    private static final int TECH_SCAN_HORIZONTAL_RADIUS = 6;
    private static final int TECH_SCAN_VERTICAL_RADIUS = 4;

    private static final double HOSTILE_SCAN_RADIUS = 32.0D;
    private static final double HOSTILE_CLOSE_RADIUS_SQ = 12.0D * 12.0D;
    private static final double HOSTILE_IMMEDIATE_RADIUS_SQ = 6.0D * 6.0D;

    private static final Path LAST_TRACK_FILE =
            FMLPaths.CONFIGDIR.get().resolve("domesurvival-music-last-track.txt");

    private static final Track MAIN_MENU =
            new Track("main_menu", ModSounds.MUSIC_MAIN_MENU);

    private static final Track T01 = new Track("01", ModSounds.MUSIC_01);
    private static final Track T02 = new Track("02", ModSounds.MUSIC_02);
    private static final Track T03 = new Track("03", ModSounds.MUSIC_03);
    private static final Track T05 = new Track("05", ModSounds.MUSIC_05);
    private static final Track T06 = new Track("06", ModSounds.MUSIC_06);
    private static final Track T07 = new Track("07", ModSounds.MUSIC_07);
    private static final Track T08 = new Track("08", ModSounds.MUSIC_08);
    private static final Track T09 = new Track("09", ModSounds.MUSIC_09);
    private static final Track T10 = new Track("10", ModSounds.MUSIC_10);
    private static final Track T11 = new Track("11", ModSounds.MUSIC_11);
    private static final Track T12 = new Track("12", ModSounds.MUSIC_12);
    private static final Track T13 = new Track("13", ModSounds.MUSIC_13);
    private static final Track T14 = new Track("14", ModSounds.MUSIC_14);
    private static final Track T15 = new Track("15", ModSounds.MUSIC_15);
    private static final Track T16 = new Track("16", ModSounds.MUSIC_16);
    private static final Track T17 = new Track("17", ModSounds.MUSIC_17);
    private static final Track T18 = new Track("18", ModSounds.MUSIC_18);
    private static final Track T19 = new Track("19", ModSounds.MUSIC_19);
    private static final Track T20 = new Track("20", ModSounds.MUSIC_20);
    private static final Track T21 = new Track("21", ModSounds.MUSIC_21);
    private static final Track T22 = new Track("22", ModSounds.MUSIC_22);

    private static final Deque<String> RECENT_TRACKS = new ArrayDeque<>();

    private static Random musicRandom = new Random(System.nanoTime());

    private static SimpleSoundInstance currentMusic;
    private static Track currentTrack;
    private static MusicContext lastChosenContext = MusicContext.NONE;

    private static long clientTicks;
    private static long currentTrackStartedTick = Long.MIN_VALUE / 4;
    private static long nextMusicTick = Long.MAX_VALUE / 4;

    private static boolean wasInWorld;
    private static String persistedLastTrackId;

    private DomeMusicClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        clientTicks++;

        Minecraft minecraft = Minecraft.getInstance();

        // DomeSurvival owns the background MUSIC scheduling.
        // This does not mute ambience, weather, blocks, mobs or machines.
        minecraft.getMusicManager().stopPlaying();

        if (minecraft.level == null || minecraft.player == null) {
            handleMenu(minecraft);
            wasInWorld = false;
            return;
        }

        if (!wasInWorld) {
            enterWorld(minecraft);
            wasInWorld = true;
        }

        handleWorld(minecraft, minecraft.level, minecraft.player);
    }

    private static void enterWorld(Minecraft minecraft) {
        stopCurrent(minecraft, "entered world");

        RECENT_TRACKS.clear();
        lastChosenContext = MusicContext.NONE;

        long seed = System.nanoTime()
                ^ System.currentTimeMillis()
                ^ clientTicks
                ^ (long) minecraft.player.getId() * 0x9E3779B97F4A7C15L;

        musicRandom = new Random(seed);
        persistedLastTrackId = loadPersistedLastTrack();

        int delay = randomBetween(FIRST_DELAY_MIN_TICKS, FIRST_DELAY_MAX_TICKS);
        nextMusicTick = clientTicks + delay;

        LOGGER.info(
                "[DomeMusic] VANILLA scheduler entered world. First music in {} sec. persistedLast={}",
                delay / 20,
                persistedLastTrackId
        );
    }

    private static void handleMenu(Minecraft minecraft) {
        nextMusicTick = Long.MAX_VALUE / 4;
        lastChosenContext = MusicContext.MENU;

        if (currentTrack != null && currentTrack != MAIN_MENU) {
            stopCurrent(minecraft, "returned to menu");
        }

        if (currentTrack == MAIN_MENU && currentMusic != null) {
            if (clientTicks - currentTrackStartedTick <= SOUND_START_GRACE_TICKS) {
                return;
            }

            if (minecraft.getSoundManager().isActive(currentMusic)) {
                return;
            }

            currentMusic = null;
            currentTrack = null;
        }

        if (currentMusic == null) {
            playTrack(minecraft, MAIN_MENU);
        }
    }

    private static void handleWorld(
            Minecraft minecraft,
            ClientLevel level,
            LocalPlayer player
    ) {
        // Current gameplay music is NEVER cut because context changed.
        if (currentMusic != null) {
            if (clientTicks - currentTrackStartedTick <= SOUND_START_GRACE_TICKS) {
                return;
            }

            if (minecraft.getSoundManager().isActive(currentMusic)) {
                return;
            }

            Track finished = currentTrack;
            currentMusic = null;
            currentTrack = null;

            int silence = randomBetween(
                    BETWEEN_TRACKS_MIN_TICKS,
                    BETWEEN_TRACKS_MAX_TICKS
            );
            nextMusicTick = clientTicks + silence;

            LOGGER.info(
                    "[DomeMusic] VANILLA track {} finished. Silence={} sec.",
                    finished == null ? "unknown" : finished.id(),
                    silence / 20
            );

            return;
        }

        if (clientTicks < nextMusicTick) {
            // Signals are intentionally NOT polled here.
            // They remain pending until the random timer reaches zero.
            return;
        }

        MusicSelection selection = selectRandomMusic(level, player);
        if (selection.track() == null) {
            int retry = randomBetween(20 * 30, 20 * 60);
            nextMusicTick = clientTicks + retry;
            return;
        }

        lastChosenContext = selection.context();
        playTrack(minecraft, selection.track());

        LOGGER.info(
                "[DomeMusic] VANILLA RANDOM context={} selected={} recent={} persistedLast={}",
                selection.context(),
                selection.track().id(),
                RECENT_TRACKS,
                persistedLastTrackId
        );
    }

    private static MusicSelection selectRandomMusic(
            ClientLevel level,
            LocalPlayer player
    ) {
        // Scripted requests do not interrupt current music and do not bypass the
        // random timer. They only alter the pool at the next scheduled slot.
        if (DomeMusicSignals.pollStoryOverride()) {
            return new MusicSelection(
                    chooseRandom(List.of(T03, T08, T12, T15)),
                    MusicContext.STORY
            );
        }

        if (DomeMusicSignals.pollDiscovery()) {
            return new MusicSelection(
                    chooseRandom(List.of(T08, T03, T12, T05, T15)),
                    MusicContext.DISCOVERY
            );
        }

        HostileSnapshot hostiles = scanHostiles(level, player);

        if (hostiles.immediate() >= 4 || hostiles.total() >= 10) {
            return new MusicSelection(
                    chooseRandom(List.of(T20, T19, T18, T14)),
                    MusicContext.COMBAT_CRITICAL
            );
        }

        if (hostiles.immediate() >= 2 || hostiles.close() >= 4) {
            return new MusicSelection(
                    chooseRandom(List.of(T19, T18, T20, T14, T07)),
                    MusicContext.COMBAT_HEAVY
            );
        }

        if (hostiles.immediate() >= 1 || hostiles.close() >= 2) {
            return new MusicSelection(
                    chooseRandom(List.of(T18, T07, T13, T14, T21)),
                    MusicContext.DANGER
            );
        }

        if (ClientSurfaceWeatherState.weatherActive()
                && ClientSurfaceWeatherState.exposed()) {
            return new MusicSelection(
                    chooseRandom(List.of(T22, T14, T07, T12, T06)),
                    MusicContext.STORM
            );
        }

        if (ClientSurfaceWeatherState.solarExposed()) {
            return new MusicSelection(
                    chooseRandom(List.of(T13, T14, T07, T12)),
                    MusicContext.ALERT
            );
        }

        boolean inBase = isInsideStarterBase(player.blockPosition());

        if (inBase) {
            if (hasTechnicalMachineNearby(level, player.blockPosition())) {
                return new MusicSelection(
                        chooseRandom(List.of(T11, T01, T17, T12, T16)),
                        MusicContext.TECH_BASE
                );
            }

            return new MusicSelection(
                    chooseRandom(List.of(T01, T17, T12, T16, T21)),
                    MusicContext.BASE
            );
        }

        if (!level.canSeeSky(player.blockPosition().above())) {
            return new MusicSelection(
                    chooseRandom(List.of(T10, T06, T12, T05, T21)),
                    MusicContext.INTERIOR
            );
        }

        long dayTime = Math.floorMod(level.getDayTime(), 24000L);

        if (dayTime >= 13000L && dayTime < 23000L) {
            return new MusicSelection(
                    chooseRandom(List.of(T09, T06, T12, T15, T05, T02)),
                    MusicContext.NIGHT
            );
        }

        double distance = distanceFromStarterBase(player);

        List<Track> exploration = new ArrayList<>(
                List.of(T15, T05, T12, T02, T06, T21)
        );

        if (distance < 100.0D) {
            exploration.add(T17);
        }

        if (distance >= 250.0D) {
            exploration.add(T09);
            exploration.add(T08);
        }

        return new MusicSelection(
                chooseRandom(exploration),
                MusicContext.EXPLORATION
        );
    }

    /**
     * Uniform random selection.
     *
     * No weights, no sequential cursor, no source-list ordering.
     * The last persisted track and recent session history are removed whenever
     * there is at least one alternative.
     */
    private static Track chooseRandom(List<Track> sourcePool) {
        if (sourcePool == null || sourcePool.isEmpty()) {
            return null;
        }

        List<Track> pool = new ArrayList<>(sourcePool);

        if (pool.size() > 1 && persistedLastTrackId != null) {
            List<Track> filtered = new ArrayList<>();

            for (Track track : pool) {
                if (!track.id().equals(persistedLastTrackId)) {
                    filtered.add(track);
                }
            }

            if (!filtered.isEmpty()) {
                pool = filtered;
            }
        }

        if (pool.size() > 1 && !RECENT_TRACKS.isEmpty()) {
            List<Track> filtered = new ArrayList<>();

            for (Track track : pool) {
                if (!RECENT_TRACKS.contains(track.id())) {
                    filtered.add(track);
                }
            }

            if (!filtered.isEmpty()) {
                pool = filtered;
            }
        }

        Collections.shuffle(pool, musicRandom);

        return pool.get(musicRandom.nextInt(pool.size()));
    }

    private static void playTrack(Minecraft minecraft, Track track) {
        currentMusic = new SimpleSoundInstance(
                track.sound().get().getLocation(),
                SoundSource.MUSIC,
                1.0F,
                1.0F,
                RandomSource.create(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true
        );

        currentTrack = track;
        currentTrackStartedTick = clientTicks;

        minecraft.getSoundManager().play(currentMusic);

        if (track != MAIN_MENU) {
            rememberTrack(track.id());
            persistedLastTrackId = track.id();
            savePersistedLastTrack(track.id());
        }
    }

    private static void rememberTrack(String id) {
        RECENT_TRACKS.remove(id);
        RECENT_TRACKS.addFirst(id);

        while (RECENT_TRACKS.size() > RECENT_HISTORY_SIZE) {
            RECENT_TRACKS.removeLast();
        }
    }

    private static String loadPersistedLastTrack() {
        if (!Files.isRegularFile(LAST_TRACK_FILE)) {
            return null;
        }

        try {
            String value = Files.readString(
                    LAST_TRACK_FILE,
                    StandardCharsets.UTF_8
            ).trim();

            return value.isEmpty() ? null : value;
        } catch (IOException exception) {
            LOGGER.warn(
                    "[DomeMusic] Could not read {}.",
                    LAST_TRACK_FILE,
                    exception
            );
            return null;
        }
    }

    private static void savePersistedLastTrack(String id) {
        try {
            Files.createDirectories(LAST_TRACK_FILE.getParent());
            Files.writeString(
                    LAST_TRACK_FILE,
                    id + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            LOGGER.warn(
                    "[DomeMusic] Could not write {}.",
                    LAST_TRACK_FILE,
                    exception
            );
        }
    }

    private static HostileSnapshot scanHostiles(
            ClientLevel level,
            LocalPlayer player
    ) {
        List<Mob> hostiles = level.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(HOSTILE_SCAN_RADIUS),
                mob -> mob.isAlive()
                        && !mob.isRemoved()
                        && (mob instanceof Enemy
                        || mob.getType().getCategory() == MobCategory.MONSTER)
        );

        int close = 0;
        int immediate = 0;

        for (Mob mob : hostiles) {
            double distanceSq = player.distanceToSqr(mob);

            if (distanceSq <= HOSTILE_CLOSE_RADIUS_SQ) {
                close++;
            }

            if (distanceSq <= HOSTILE_IMMEDIATE_RADIUS_SQ) {
                immediate++;
            }
        }

        return new HostileSnapshot(hostiles.size(), close, immediate);
    }

    private static boolean isInsideStarterBase(BlockPos pos) {
        DomeSpec spec = com.wasted.domesurvival.forge.client.weather.ClientSurfaceWeatherState.domeSpec();

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (isInsideAirlock(spec, x, y, z)) {
            return true;
        }

        long dx = (long) x - spec.centerX();
        long dz = (long) z - spec.centerZ();
        long horizontalSq = dx * dx + dz * dz;

        if (y < spec.baseY()) {
            return y >= spec.undergroundMinY()
                    && horizontalSq
                    <= (long) spec.undergroundRadius() * spec.undergroundRadius();
        }

        if (y <= spec.hemisphereCenterY()) {
            return horizontalSq
                    <= (long) spec.surfaceRadius() * spec.surfaceRadius();
        }

        long dy = (long) y - spec.hemisphereCenterY();
        long radiusSq = (long) spec.surfaceRadius() * spec.surfaceRadius();

        return horizontalSq + dy * dy <= radiusSq;
    }

    private static boolean isInsideAirlock(
            DomeSpec spec,
            int x,
            int y,
            int z
    ) {
        int minX = spec.airlockCenterX() - spec.airlockHalfWidth();
        int maxX = spec.airlockCenterX() + spec.airlockHalfWidth();

        return x >= minX
                && x <= maxX
                && z >= spec.airlockStartZ()
                && z <= spec.airlockEndZ()
                && y >= spec.airlockFloorY()
                && y <= spec.airlockCeilingY();
    }

    private static double distanceFromStarterBase(LocalPlayer player) {
        DomeSpec spec = com.wasted.domesurvival.forge.client.weather.ClientSurfaceWeatherState.domeSpec();

        double dx = player.getX() - spec.centerX();
        double dz = player.getZ() - spec.centerZ();

        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * This scan now runs only when the random music timer expires.
     * At 3-8 minute intervals it is effectively negligible.
     */
    private static boolean hasTechnicalMachineNearby(
            ClientLevel level,
            BlockPos center
    ) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int minY = Math.max(
                level.getMinBuildHeight(),
                center.getY() - TECH_SCAN_VERTICAL_RADIUS
        );

        int maxY = Math.min(
                level.getMaxBuildHeight() - 1,
                center.getY() + TECH_SCAN_VERTICAL_RADIUS
        );

        for (int y = minY; y <= maxY; y++) {
            for (int dx = -TECH_SCAN_HORIZONTAL_RADIUS;
                 dx <= TECH_SCAN_HORIZONTAL_RADIUS;
                 dx++) {
                for (int dz = -TECH_SCAN_HORIZONTAL_RADIUS;
                     dz <= TECH_SCAN_HORIZONTAL_RADIUS;
                     dz++) {
                    cursor.set(
                            center.getX() + dx,
                            y,
                            center.getZ() + dz
                    );

                    if (isTechnicalMachine(
                            level.getBlockState(cursor).getBlock()
                    )) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isTechnicalMachine(Block block) {
        return block == ModBlocks.COPPER_FURNACE.get()
                || block == ModBlocks.SHAFT_FURNACE.get()
                || block == ModBlocks.COKE_OVEN.get()
                || block == ModBlocks.COAL_GENERATOR.get()
                || block == ModBlocks.WATER_PURIFIER.get()
                || block == ModBlocks.OXYGEN_ELECTROLYZER.get()
                || block == ModBlocks.OXYGEN_FILLER.get()
                || block == ModBlocks.ENERGY_BUFFER.get()
                || block == ModBlocks.ENERGY_BUFFER_TITAN.get()
                || block == ModBlocks.ENERGY_BUFFER_ADAMANTIUM.get()
                || block == ModBlocks.ENERGY_BUFFER_CREATIVE.get()
                || block == OxygenComplexRegistry.AIR_INTAKE.get()
                || block == OxygenComplexRegistry.FILTRATION.get()
                || block == OxygenComplexRegistry.COMPRESSION.get()
                || block == OxygenComplexRegistry.OUTPUT.get();
    }

    private static void stopCurrent(
            Minecraft minecraft,
            String reason
    ) {
        if (currentMusic != null) {
            minecraft.getSoundManager().stop(currentMusic);
        }

        if (currentTrack != null) {
            LOGGER.info(
                    "[DomeMusic] Stopping {}: {}",
                    currentTrack.id(),
                    reason
            );
        }

        currentMusic = null;
        currentTrack = null;
    }

    private static int randomBetween(
            int minInclusive,
            int maxInclusive
    ) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }

        return minInclusive
                + musicRandom.nextInt(maxInclusive - minInclusive + 1);
    }

    static String debugStatus() {
        long remainingTicks = Math.max(0L, nextMusicTick - clientTicks);

        return "mode=VANILLA_STYLE"
                + ", context=" + lastChosenContext
                + ", track=" + (currentTrack == null
                ? "none"
                : currentTrack.id())
                + ", nextInSec=" + (remainingTicks / 20L)
                + ", recent=" + RECENT_TRACKS
                + ", persistedLast=" + persistedLastTrackId;
    }

    private enum MusicContext {
        NONE,
        MENU,
        STORY,
        DISCOVERY,
        COMBAT_CRITICAL,
        COMBAT_HEAVY,
        DANGER,
        STORM,
        ALERT,
        TECH_BASE,
        BASE,
        INTERIOR,
        NIGHT,
        EXPLORATION
    }

    private record Track(
            String id,
            RegistryObject<SoundEvent> sound
    ) {
    }

    private record MusicSelection(
            Track track,
            MusicContext context
    ) {
    }

    private record HostileSnapshot(
            int total,
            int close,
            int immediate
    ) {
    }
}
