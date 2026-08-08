package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.forge.airlock.AirlockService;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * O(1) atmosphere lookup for the current starter-dome phase.
 *
 * No flood-fill, block scanning, chunk loading or neighbor traversal happens here.
 * Player-built airtight rooms will be introduced later as cached atmosphere volumes.
 */
public final class OxygenEnvironment {
    private static final DomeBounds START_DOME = new DomeBounds(DomeSpec.wastedV1());

    private OxygenEnvironment() {
    }

    public static boolean isBreathable(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        // V3 initially governs the WASTED overworld only.
        // This avoids unexpectedly breaking Nether/End gameplay before those rules are designed.
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return true;
        }

        // Development/sandbox worlds without generated DomeSurvival structure remain playable.
        if (!DomeSavedData.get(level).isGenerated()) {
            return true;
        }

        DomeZone zone = START_DOME.classify(player.getX(), player.getY(), player.getZ());
        if (zone == DomeZone.AIRLOCK) {
            return AirlockService.isBreathable(level);
        }
        return zone.isSafe();
    }
}
