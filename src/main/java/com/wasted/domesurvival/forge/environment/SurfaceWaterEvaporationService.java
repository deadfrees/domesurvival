package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.config.SurfaceHazardConfig;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.weather.SurfaceWeatherService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Quickly dries directly sun-exposed vanilla water outside the starter dome.
 *
 * Compatibility:
 * this class intentionally checks Blocks.WATER only. Ad Astra oil and every
 * other modded fluid are therefore completely untouched.
 *
 * Performance:
 * no chunk/world scan is performed. Each pass checks a deterministic slice of
 * surface columns around active overworld players. Unlike the old random
 * sampler, every nearby column is guaranteed to be reached after a short cycle.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurfaceWaterEvaporationService {
    private static final int MAX_EFFECTIVE_SAMPLES = 2048;
    private static final int MAX_EFFECTIVE_BUDGET = 256;

    /**
     * Keep the Nether-style hiss audible without playing dozens of identical
     * sounds every second while a large lake is drying.
     */
    private static final int EVAPORATION_SOUND_INTERVAL_TICKS = 10;

    private SurfaceWaterEvaporationService() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !SurfaceHazardConfig.SOLAR_ENABLED.get()
                || !SurfaceHazardConfig.SOLAR_EVAPORATES_WATER.get()) {
            return;
        }

        int speed = SurfaceHazardConfig.SOLAR_WATER_EVAPORATION_SPEED_MULTIPLIER.get();
        int configuredInterval = SurfaceHazardConfig.SOLAR_WATER_EVAPORATION_INTERVAL_TICKS.get();
        int intervalTicks = Math.max(2, configuredInterval / Math.max(1, speed));

        if (Math.floorMod(event.getServer().getTickCount(), intervalTicks) != 0) {
            return;
        }

        ServerLevel level = event.getServer().overworld();
        if (!Level.OVERWORLD.equals(level.dimension())
                || !DomeSavedData.get(level).isGenerated()
                || !level.isDay()
                || SurfaceWeatherService.currentWeather(level) != SurfaceWeatherType.CLEAR) {
            return;
        }

        long passIndex = event.getServer().getTickCount() / intervalTicks;

        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                evaporateAround(level, player, passIndex, speed);
            }
        }
    }

    /**
     * Prevent vanilla infinite-source regeneration while the exact water position
     * is under lethal direct sunlight.
     */
    @SubscribeEvent
    public static void onCreateFluidSource(BlockEvent.CreateFluidSourceEvent event) {
        if (!SurfaceHazardConfig.SOLAR_ENABLED.get()
                || !SurfaceHazardConfig.SOLAR_EVAPORATES_WATER.get()
                || !(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(Blocks.WATER)) {
            return;
        }

        if (SurfaceHazardEnvironment.directlyExposedToSolar(level, event.getPos())) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static void evaporateAround(
            ServerLevel level,
            ServerPlayer player,
            long passIndex,
            int speed
    ) {
        int radius = SurfaceHazardConfig.SOLAR_WATER_EVAPORATION_RADIUS.get();
        int baseSamples = SurfaceHazardConfig.SOLAR_WATER_EVAPORATION_SAMPLES.get();
        int baseBudget = SurfaceHazardConfig.SOLAR_WATER_EVAPORATION_MAX_BLOCKS.get();

        int samples = Math.min(MAX_EFFECTIVE_SAMPLES, Math.max(1, baseSamples * speed));
        int remainingBudget = Math.min(MAX_EFFECTIVE_BUDGET, Math.max(1, baseBudget * speed));

        int diameter = radius * 2 + 1;
        int totalColumns = diameter * diameter;

        long playerOffset = Integer.toUnsignedLong(player.getUUID().hashCode());
        int startIndex = (int) Math.floorMod(passIndex * samples + playerOffset, totalColumns);

        BlockPos center = player.blockPosition();
        BlockPos soundPos = null;

        for (int i = 0; i < samples && remainingBudget > 0; i++) {
            int index = startIndex + i;
            if (index >= totalColumns) {
                index %= totalColumns;
            }

            int localX = index % diameter;
            int localZ = index / diameter;

            int x = center.getX() + localX - radius;
            int z = center.getZ() + localZ - radius;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;

            if (y < level.getMinBuildHeight()) {
                continue;
            }

            BlockPos pos = new BlockPos(x, y, z);

            // Exact vanilla water only; no generic fluid or broad water-tag test.
            if (!level.getBlockState(pos).is(Blocks.WATER)
                    || !SurfaceHazardEnvironment.directlyExposedToSolar(level, pos)) {
                continue;
            }

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            if (soundPos == null) {
                soundPos = pos.immutable();
            }

            remainingBudget--;
        }

        if (soundPos != null
                && Math.floorMod(level.getGameTime(), EVAPORATION_SOUND_INTERVAL_TICKS) == 0L) {
            playNetherEvaporationSound(level, soundPos);
        }
    }

    /**
     * Same sound and pitch profile vanilla uses when water vaporizes in an
     * ultra-warm dimension such as the Nether.
     */
    private static void playNetherEvaporationSound(ServerLevel level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
        );
    }
}
