package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.forge.airlock.AirlockService;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.metro.dome.DomeMetroAtmosphere;
import com.wasted.domesurvival.forge.metro.RestoredMetroAtmosphere;
import com.wasted.domesurvival.forge.oxygen.room.SealedRoomManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** O(1) atmosphere lookup for DomeSurvival. */
public final class OxygenEnvironment {
    private OxygenEnvironment() {
    }

    public static boolean isBreathable(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos breathingPos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());

        if (Level.NETHER.equals(level.dimension()) || Level.END.equals(level.dimension())) {
            return SealedRoomManager.isBreathableAt(level, breathingPos);
        }
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return true;
        }
        if (!DomeSavedData.get(level).isGenerated()) {
            return true;
        }

        // Only the special central Dome metro is a permanent breathable extension.
        if (DomeMetroAtmosphere.isForcedBreathable(level, breathingPos)
                || RestoredMetroAtmosphere.isForcedBreathable(level, breathingPos)) {
            return true;
        }

        DomeZone zone = new DomeBounds(DomeSavedData.get(level).domeSpec())
                .classify(player.getX(), player.getY(), player.getZ());
        if (zone == DomeZone.AIRLOCK) {
            return AirlockService.isBreathable(level);
        }
        if (zone.isSafe()) {
            return true;
        }
        return SealedRoomManager.isBreathableAt(level, breathingPos);
    }
}
