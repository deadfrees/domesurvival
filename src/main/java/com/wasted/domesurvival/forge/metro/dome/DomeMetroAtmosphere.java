package com.wasted.domesurvival.forge.metro.dome;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Stable Stage-4 API for the permanent breathable central Dome-metro zone.
 * External/generated metro stations deliberately return false here.
 */
public final class DomeMetroAtmosphere {
    private DomeMetroAtmosphere() {
    }

    public static boolean isForcedBreathable(ServerLevel level, BlockPos pos) {
        return DomeMetroSavedData.get(level.getServer()).isInsideBreathableZone(
                level.dimension().location().toString(),
                pos
        );
    }
}
