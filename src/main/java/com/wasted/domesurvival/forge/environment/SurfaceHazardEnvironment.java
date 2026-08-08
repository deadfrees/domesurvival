package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.weather.SurfaceWeatherService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** O(1) physical exposure classification shared by weather damage and weather visuals. */
public final class SurfaceHazardEnvironment {
    private static final DomeBounds START_DOME = new DomeBounds(DomeSpec.wastedV1());

    private SurfaceHazardEnvironment() {
    }

    public static SurfaceExposure exposure(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return SurfaceExposure.NONE;
        }

        // Keep development/test worlds without the generated dome playable.
        if (!DomeSavedData.get(level).isGenerated()) {
            return SurfaceExposure.NONE;
        }

        if (!isOutsideDome(player)) {
            return SurfaceExposure.NONE;
        }

        BlockPos exposurePos = exposurePosition(player);
        if (!level.canSeeSky(exposurePos)) {
            return SurfaceExposure.NONE;
        }

        SurfaceWeatherType weather = SurfaceWeatherService.currentWeather(level);
        return switch (weather) {
            case SANDSTORM -> SurfaceExposure.SANDSTORM;
            case ACID_THUNDERSTORM -> SurfaceExposure.ACID_THUNDERSTORM;
            case ACID_RAIN -> SurfaceExposure.ACID_RAIN;
            case CLEAR -> level.isDay() ? SurfaceExposure.SOLAR : SurfaceExposure.NONE;
        };
    }

    /** True when the player's eyes are directly open to surface weather outside the dome. */
    public static boolean directlyExposedToWeather(ServerPlayer player) {
        if (!Level.OVERWORLD.equals(player.level().dimension())) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (!DomeSavedData.get(level).isGenerated() || !isOutsideDome(player)) {
            return false;
        }
        return level.canSeeSky(exposurePosition(player));
    }

    private static boolean isOutsideDome(ServerPlayer player) {
        DomeZone zone = START_DOME.classify(player.getX(), player.getY(), player.getZ());
        return zone == DomeZone.OUTSIDE;
    }

    private static BlockPos exposurePosition(ServerPlayer player) {
        return BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
    }
}
