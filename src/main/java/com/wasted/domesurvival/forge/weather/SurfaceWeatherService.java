package com.wasted.domesurvival.forge.weather;

import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.config.SurfaceHazardConfig;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.data.SurfaceWeatherSavedData;
import com.wasted.domesurvival.forge.environment.SurfaceExposure;
import com.wasted.domesurvival.forge.environment.SurfaceHazardEnvironment;
import com.wasted.domesurvival.forge.network.ModNetwork;
import com.wasted.domesurvival.forge.network.SurfaceWeatherSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the custom surface-weather scheduler and S2C synchronization.
 *
 * Vanilla rain/thunder are reinterpreted as acid weather. Sandstorms are custom,
 * persisted per overworld and only start while vanilla weather is clear, avoiding
 * rain/sand visual overlap and avoiding any forced biome/chunk work.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurfaceWeatherService {
    private static final Map<UUID, SyncKey> LAST_SYNC = new HashMap<>();

    private SurfaceWeatherService() {
    }

    public static SurfaceWeatherType currentWeather(ServerLevel level) {
        if (!Level.OVERWORLD.equals(level.dimension()) || !DomeSavedData.get(level).isGenerated()) {
            return SurfaceWeatherType.CLEAR;
        }

        if (SurfaceHazardConfig.SANDSTORM_ENABLED.get()
                && SurfaceWeatherSavedData.get(level).sandstormActive()) {
            return SurfaceWeatherType.SANDSTORM;
        }
        if (level.isThundering()) {
            return SurfaceWeatherType.ACID_THUNDERSTORM;
        }
        if (level.isRaining()) {
            return SurfaceWeatherType.ACID_RAIN;
        }
        return SurfaceWeatherType.CLEAR;
    }

    public static int sandstormSecondsRemaining(ServerLevel level) {
        return SurfaceWeatherSavedData.get(level).sandstormSecondsRemaining();
    }

    public static int sandstormCooldownSeconds(ServerLevel level) {
        return SurfaceWeatherSavedData.get(level).sandstormCooldownSeconds();
    }

    public static int startSandstorm(ServerLevel level) {
        int duration = randomBetween(
                level,
                SurfaceHazardConfig.SANDSTORM_MIN_DURATION_SECONDS.get(),
                SurfaceHazardConfig.SANDSTORM_MAX_DURATION_SECONDS.get()
        );
        return startSandstorm(level, duration);
    }

    public static int startSandstorm(ServerLevel level, int durationSeconds) {
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return 0;
        }
        int duration = Math.max(1, durationSeconds);
        SurfaceWeatherSavedData.get(level).startSandstorm(duration);
        invalidatePlayerSync();
        return duration;
    }

    public static void stopSandstorm(ServerLevel level) {
        SurfaceWeatherSavedData data = SurfaceWeatherSavedData.get(level);
        data.stopSandstorm();
        scheduleNextSandstorm(level, data);
        invalidatePlayerSync();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        int serverTick = event.getServer().getTickCount();
        if (serverTick % 20 == 0) {
            tickScheduler(event.getServer().overworld());
        }
        if (serverTick % 10 == 0) {
            syncPlayers(event.getServer().overworld());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SYNC.remove(player.getUUID());
            syncPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SYNC.remove(player.getUUID());
            syncPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SYNC.remove(player.getUUID());
            syncPlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SYNC.remove(event.getEntity().getUUID());
    }

    private static void tickScheduler(ServerLevel level) {
        if (!Level.OVERWORLD.equals(level.dimension()) || !DomeSavedData.get(level).isGenerated()) {
            return;
        }

        SurfaceWeatherSavedData data = SurfaceWeatherSavedData.get(level);
        if (!SurfaceHazardConfig.SANDSTORM_ENABLED.get()) {
            if (data.sandstormActive()) {
                data.stopSandstorm();
                invalidatePlayerSync();
            }
            return;
        }

        if (data.sandstormActive()) {
            if (data.tickSandstormSecond()) {
                scheduleNextSandstorm(level, data);
                invalidatePlayerSync();
            }
            return;
        }

        if (data.sandstormCooldownSeconds() <= 0) {
            scheduleNextSandstorm(level, data);
            return;
        }

        // A sandstorm does not start on top of vanilla rain/thunder. The timer pauses
        // during wet weather so transitions remain visually coherent.
        if (level.isRaining() || level.isThundering()) {
            return;
        }

        if (data.tickCooldownSecond()) {
            data.startSandstorm(randomBetween(
                    level,
                    SurfaceHazardConfig.SANDSTORM_MIN_DURATION_SECONDS.get(),
                    SurfaceHazardConfig.SANDSTORM_MAX_DURATION_SECONDS.get()
            ));
            invalidatePlayerSync();
        }
    }

    private static void syncPlayers(ServerLevel overworld) {
        for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
            syncPlayer(player, false);
        }
    }

    private static void syncPlayer(ServerPlayer player, boolean force) {
        ServerLevel level = player.serverLevel();
        SurfaceWeatherType weather = currentWeather(level);
        SurfaceExposure surfaceExposure = SurfaceHazardEnvironment.exposure(player);
        boolean exposed = weather != SurfaceWeatherType.CLEAR
                && SurfaceHazardEnvironment.directlyExposedToWeather(player);
        boolean solarExposed = surfaceExposure == SurfaceExposure.SOLAR;
        boolean solarActive = Level.OVERWORLD.equals(level.dimension())
                && DomeSavedData.get(level).isGenerated()
                && weather == SurfaceWeatherType.CLEAR
                && level.isDay();
        int secondsRemaining = weather == SurfaceWeatherType.SANDSTORM
                ? sandstormSecondsRemaining(level)
                : 0;
        var domeSpec = DomeSavedData.get(level).domeSpec();

        SyncKey key = new SyncKey(weather, exposed, solarActive, solarExposed);
        SyncKey old = LAST_SYNC.put(player.getUUID(), key);
        if (!force && key.equals(old)) {
            return;
        }

        ModNetwork.sendTo(player, new SurfaceWeatherSyncPacket(
                weather,
                exposed,
                solarActive,
                solarExposed,
                secondsRemaining,
                domeSpec.centerX(),
                domeSpec.baseY(),
                domeSpec.centerZ()
        ));
    }

    private static void scheduleNextSandstorm(ServerLevel level, SurfaceWeatherSavedData data) {
        data.setCooldownSeconds(randomBetween(
                level,
                SurfaceHazardConfig.SANDSTORM_MIN_INTERVAL_SECONDS.get(),
                SurfaceHazardConfig.SANDSTORM_MAX_INTERVAL_SECONDS.get()
        ));
    }

    private static int randomBetween(ServerLevel level, int configuredMin, int configuredMax) {
        int min = Math.min(configuredMin, configuredMax);
        int max = Math.max(configuredMin, configuredMax);
        if (min == max) {
            return min;
        }
        return min + level.getRandom().nextInt(max - min + 1);
    }

    private static void invalidatePlayerSync() {
        LAST_SYNC.clear();
    }

    private record SyncKey(
            SurfaceWeatherType weather,
            boolean exposed,
            boolean solarActive,
            boolean solarExposed
    ) {
    }
}
