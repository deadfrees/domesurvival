package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.metro.dome.DomeMetroAtmosphere;
import com.wasted.domesurvival.forge.metro.RestoredMetroAtmosphere;
import com.wasted.domesurvival.forge.weather.SurfaceWeatherService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** O(1) physical exposure classification shared by weather damage and weather visuals. */
public final class SurfaceHazardEnvironment {
    private SurfaceHazardEnvironment() {
    }

    public static SurfaceExposure exposure(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || !Level.OVERWORLD.equals(level.dimension())) {
            return SurfaceExposure.NONE;
        }

        if (!DomeSavedData.get(level).isGenerated()) {
            return SurfaceExposure.NONE;
        }

        // The central Dome metro is an authored, sealed extension of the Dome.
        // Glass passes skylight, so canSeeSky() alone must not classify this zone
        // as exposed to sandstorms/acid rain/solar damage.
        if (DomeMetroAtmosphere.isForcedBreathable(level, exposurePosition(entity))
                || RestoredMetroAtmosphere.isForcedBreathable(level, exposurePosition(entity))) {
            return SurfaceExposure.NONE;
        }

        if (!isOutsideDome(level, entity.getX(), entity.getY(), entity.getZ())) {
            return SurfaceExposure.NONE;
        }

        BlockPos exposurePos = exposurePosition(entity);
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

    public static boolean directlyExposedToWeather(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || !Level.OVERWORLD.equals(level.dimension())) {
            return false;
        }

        if (!DomeSavedData.get(level).isGenerated()) {
            return false;
        }

        BlockPos exposurePos = exposurePosition(entity);
        if (DomeMetroAtmosphere.isForcedBreathable(level, exposurePos)
                || RestoredMetroAtmosphere.isForcedBreathable(level, exposurePos)) {
            return false;
        }

        if (!isOutsideDome(level, entity.getX(), entity.getY(), entity.getZ())) {
            return false;
        }

        return level.canSeeSky(exposurePos);
    }

    public static boolean directlyExposedToSolar(ServerLevel level, BlockPos pos) {
        if (!Level.OVERWORLD.equals(level.dimension())
                || !DomeSavedData.get(level).isGenerated()
                || !level.isDay()
                || SurfaceWeatherService.currentWeather(level) != SurfaceWeatherType.CLEAR) {
            return false;
        }

        if (DomeMetroAtmosphere.isForcedBreathable(level, pos.above())
                || RestoredMetroAtmosphere.isForcedBreathable(level, pos.above())) {
            return false;
        }

        if (!isOutsideDome(level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D)) {
            return false;
        }

        return level.canSeeSky(pos.above());
    }

    private static boolean isOutsideDome(ServerLevel level, double x, double y, double z) {
        DomeZone zone = new DomeBounds(DomeSavedData.get(level).domeSpec()).classify(x, y, z);
        return zone == DomeZone.OUTSIDE;
    }

    private static BlockPos exposurePosition(LivingEntity entity) {
        return BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
    }
}
