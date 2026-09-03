package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Identifies worlds created with the distributable LastWorld noise preset. */
public final class LastWorldWorldState {
    private static final ResourceKey<NoiseGeneratorSettings> LASTWORLD_SETTINGS = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, "lastworld")
    );

    private LastWorldWorldState() {
    }

    public static boolean isLastWorld(ServerLevel level) {
        if (!Level.OVERWORLD.equals(level.dimension())
                || !(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator)) {
            return false;
        }
        return generator.generatorSettings().unwrapKey()
                .map(LASTWORLD_SETTINGS::equals)
                .orElse(false);
    }
}
