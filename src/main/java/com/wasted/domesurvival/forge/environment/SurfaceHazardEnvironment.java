package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.weather.SurfaceWeatherService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** O(1) physical exposure classification shared by weather damage and weather visuals. */
public final class SurfaceHazardEnvironment {
    private SurfaceHazardEnvironment() {
    }

    /**
     * Generic living-entity exposure classification.
     *
     * ServerPlayer callers continue to work unchanged, while mobs can now reuse
     * the exact same dome / sky / weather rules instead of duplicating geometry.
     */
    public static SurfaceExposure exposure(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || !Level.OVERWORLD.equals(level.dimension())) {
            return SurfaceExposure.NONE;
        }

        // Keep development/test worlds without the generated dome playable.
        if (!DomeSavedData.get(level).isGenerated()) {
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

    /** True when the entity's eyes are directly open to surface weather outside the dome. */
    public static boolean directlyExposedToWeather(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || !Level.OVERWORLD.equals(level.dimension())) {
            return false;
        }

        if (!DomeSavedData.get(level).isGenerated()
                || !isOutsideDome(level, entity.getX(), entity.getY(), entity.getZ())) {
            return false;
        }

        return level.canSeeSky(exposurePosition(entity));
    }

    /**
     * Exact direct-sun test for a world block position.
     *
     * Used by water evaporation. The block must be outside the authored dome,
     * directly open to the sky, in the overworld, during clear daytime weather.
     */
    public static boolean directlyExposedToSolar(ServerLevel level, BlockPos pos) {
        if (!Level.OVERWORLD.equals(level.dimension())
                || !DomeSavedData.get(level).isGenerated()
                || !level.isDay()
                || SurfaceWeatherService.currentWeather(level) != SurfaceWeatherType.CLEAR) {
            return false;
        }

        if (!isOutsideDome(level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        )) {
            return false;
        }

        // Test the air immediately above the fluid surface. This avoids treating
        // water under a roof, glass or another cover as sun-exposed.
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
