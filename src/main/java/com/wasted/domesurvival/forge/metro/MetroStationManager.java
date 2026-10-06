package com.wasted.domesurvival.forge.metro;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

/** Thin server API around MetroStationSavedData. No scans and no chunk forcing. */
public final class MetroStationManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MetroStationManager() {
    }

    public static UUID ensureRegistered(ServerLevel level, @Nullable UUID requestedId, BlockPos anchor) {
        MetroStationSavedData data = MetroStationSavedData.get(level.getServer());
        MetroStationRecord before = data.findByAnchor(level.dimension().location().toString(), anchor);
        UUID id = data.claim(requestedId, level.dimension().location().toString(), anchor);
        if (before == null) {
            MetroStationRecord record = data.get(id);
            if (record != null) {
                LOGGER.info("Metro station registered: {} {} @ {} {}",
                        record.stationName(), record.stationId(), record.dimension(), record.anchor().toShortString());
            }
        }
        return id;
    }

    public static void discover(ServerLevel level, UUID stationId) {
        MetroStationSavedData data = MetroStationSavedData.get(level.getServer());
        if (data.markDiscovered(stationId, level.getGameTime())) {
            MetroStationRecord record = data.get(stationId);
            if (record != null) LOGGER.info("Metro station discovered: {} {}", record.stationName(), stationId);
        }
    }

    /**
     * SavedData lookup only. Used by the static train console so it can report the
     * nearest station state without scanning blocks or forcing any chunks.
     */
    @Nullable
    public static MetroStationRecord findNearest(ServerLevel level, BlockPos pos, double maxDistance) {
        String dimension = level.dimension().location().toString();
        double maxDistanceSqr = maxDistance * maxDistance;
        MetroStationRecord nearest = null;
        double nearestSqr = maxDistanceSqr;

        for (MetroStationRecord record : MetroStationSavedData.get(level.getServer()).all()) {
            if (!record.dimension().equals(dimension)) continue;
            double distanceSqr = record.anchor().distSqr(pos);
            if (distanceSqr <= nearestSqr) {
                nearestSqr = distanceSqr;
                nearest = record;
            }
        }
        return nearest;
    }
}
