package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.forge.airlock.AirlockService;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.oxygen.room.SealedRoomManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * O(1) atmosphere lookup for the current starter-dome phase.
 *
 * No flood-fill, block scanning, chunk loading or neighbor traversal happens here.
 * V61 player-built rooms are queried only through the already-discovered SealedRoomManager cache.
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
        if (zone.isSafe()) {
            return true;
        }

        // Player-built rooms are breathable only after V61 atmosphere filling completed.
        // This is a cache lookup only; it never starts room discovery from the player tick.
        BlockPos breathingPos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return SealedRoomManager.isBreathableAt(level, breathingPos);
    }
}
