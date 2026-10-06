package com.wasted.domesurvival.forge.metro;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent server-side identity and restoration progress of one generated station. */
public final class MetroStationRecord {
    private final UUID stationId;
    private final String dimension;
    private final BlockPos anchor;
    private String stationName;
    private StationState state;
    private boolean discovered;
    private boolean restored;
    @Nullable private UUID restoredBy;
    private long discoveredAt;
    private long restoredAt;
    private final Map<String, Integer> restorationDeposits = new LinkedHashMap<>();

    MetroStationRecord(UUID stationId,
                       String dimension,
                       BlockPos anchor,
                       String stationName,
                       StationState state,
                       boolean discovered,
                       boolean restored,
                       @Nullable UUID restoredBy,
                       long discoveredAt,
                       long restoredAt,
                       @Nullable Map<String, Integer> restorationDeposits) {
        this.stationId = stationId;
        this.dimension = dimension;
        this.anchor = anchor.immutable();
        this.stationName = stationName;
        this.state = state;
        this.discovered = discovered;
        this.restored = restored;
        this.restoredBy = restoredBy;
        this.discoveredAt = Math.max(0L, discoveredAt);
        this.restoredAt = Math.max(0L, restoredAt);
        if (restorationDeposits != null) {
            restorationDeposits.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && value > 0) {
                    this.restorationDeposits.put(key, value);
                }
            });
        }
    }

    public UUID stationId() { return stationId; }
    public String dimension() { return dimension; }
    public BlockPos anchor() { return anchor; }
    public String stationName() { return stationName; }
    public StationState state() { return state; }
    public boolean discovered() { return discovered; }
    public boolean restored() { return restored; }
    @Nullable public UUID restoredBy() { return restoredBy; }
    public long discoveredAt() { return discoveredAt; }
    public long restoredAt() { return restoredAt; }

    public int restorationDeposit(String requirementId) {
        return Math.max(0, restorationDeposits.getOrDefault(requirementId, 0));
    }

    public Map<String, Integer> restorationDeposits() {
        return Map.copyOf(restorationDeposits);
    }

    void rename(String stationName) {
        if (stationName != null && !stationName.isBlank()) this.stationName = stationName;
    }

    boolean markDiscovered(long gameTime) {
        if (discovered) return false;
        discovered = true;
        discoveredAt = Math.max(0L, gameTime);
        return true;
    }

    void setState(StationState state) {
        this.state = state == null ? StationState.ABANDONED : state;
        this.restored = this.state == StationState.RESTORED;
    }

    void markRestored(@Nullable UUID playerId, long gameTime) {
        state = StationState.RESTORED;
        restored = true;
        restoredBy = playerId;
        restoredAt = Math.max(0L, gameTime);
    }

    int addRestorationDeposit(String requirementId, int amount, int maximum) {
        if (requirementId == null || requirementId.isBlank() || amount <= 0 || maximum <= 0) return 0;
        int oldValue = restorationDeposit(requirementId);
        int newValue = Math.min(maximum, oldValue + amount);
        if (newValue <= oldValue) return 0;
        restorationDeposits.put(requirementId, newValue);
        return newValue - oldValue;
    }

    void replaceRestorationDeposits(Map<String, Integer> deposits) {
        restorationDeposits.clear();
        if (deposits == null) return;
        deposits.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && value > 0) {
                restorationDeposits.put(key, value);
            }
        });
    }

    void clearRestorationDeposits() {
        restorationDeposits.clear();
    }

    void resetRestoration() {
        state = StationState.ABANDONED;
        restored = false;
        restoredBy = null;
        restoredAt = 0L;
        clearRestorationDeposits();
    }
}
