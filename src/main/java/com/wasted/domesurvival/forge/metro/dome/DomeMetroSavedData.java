package com.wasted.domesurvival.forge.metro.dome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Server-side source of truth for the special metro station below the Dome.
 * It is intentionally separate from ordinary generated MetroStationRecord entries.
 *
 * V1.8 additionally stores the entrance/facing so the central Dome station has a
 * precise, persistent breathable-zone definition. Ordinary/worldgen metro stations
 * never use this override.
 */
public final class DomeMetroSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_dome_metro";
    private static final int DATA_VERSION = 2;
    public static final int CURRENT_DOME_METRO_VERSION = 10;

    private boolean unlocked;
    private int domeMetroVersion;
    private String dimension = Level.OVERWORLD.location().toString();
    @Nullable private BlockPos domeAnchor;
    @Nullable private BlockPos entrance;
    @Nullable private BlockPos stationAnchor;
    @Nullable private BlockPos trainConsolePos;
    private Direction forward = Direction.WEST;
    @Nullable private UUID unlockedBy;
    private long unlockedAt;
    private long builtAt;

    public static DomeMetroSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DomeMetroSavedData::load,
                DomeMetroSavedData::new,
                DATA_NAME
        );
    }

    public static DomeMetroSavedData load(CompoundTag tag) {
        DomeMetroSavedData data = new DomeMetroSavedData();
        data.unlocked = tag.getBoolean("DOME_METRO_UNLOCKED");
        data.domeMetroVersion = Math.max(0, tag.getInt("DomeMetroVersion"));
        if (tag.contains("Dimension", Tag.TAG_STRING) && !tag.getString("Dimension").isBlank()) {
            data.dimension = tag.getString("Dimension");
        }
        if (tag.contains("DomeAnchor", Tag.TAG_LONG)) data.domeAnchor = BlockPos.of(tag.getLong("DomeAnchor"));
        if (tag.contains("Entrance", Tag.TAG_LONG)) data.entrance = BlockPos.of(tag.getLong("Entrance"));
        if (tag.contains("StationAnchor", Tag.TAG_LONG)) data.stationAnchor = BlockPos.of(tag.getLong("StationAnchor"));
        if (tag.contains("TrainConsolePos", Tag.TAG_LONG)) data.trainConsolePos = BlockPos.of(tag.getLong("TrainConsolePos"));
        if (tag.contains("Facing", Tag.TAG_STRING)) {
            try {
                Direction parsed = Direction.byName(tag.getString("Facing").toLowerCase(Locale.ROOT));
                if (parsed != null && parsed.getAxis().isHorizontal()) data.forward = parsed;
            }
            catch (RuntimeException ignored) {
                data.forward = Direction.WEST;
            }
        }
        if (tag.hasUUID("UnlockedBy")) data.unlockedBy = tag.getUUID("UnlockedBy");
        data.unlockedAt = Math.max(0L, tag.getLong("UnlockedAt"));
        data.builtAt = Math.max(0L, tag.getLong("BuiltAt"));

        // Migration safety: a built station always implies the persistent unlock flag.
        if (data.domeMetroVersion > 0 && !data.unlocked) {
            data.unlocked = true;
            data.setDirty();
        }
        return data;
    }

    public boolean unlock(String dimension, BlockPos domeAnchor, @Nullable UUID actor, long gameTime) {
        boolean changed = false;
        if (!unlocked) {
            unlocked = true;
            unlockedBy = actor;
            unlockedAt = Math.max(0L, gameTime);
            changed = true;
        }
        if (this.domeAnchor == null) {
            this.dimension = dimension;
            this.domeAnchor = domeAnchor.immutable();
            changed = true;
        }
        if (changed) setDirty();
        return changed;
    }

    public void markBuilt(int version,
                          BlockPos entrance,
                          BlockPos stationAnchor,
                          BlockPos trainConsolePos,
                          Direction forward,
                          long gameTime) {
        domeMetroVersion = Math.max(domeMetroVersion, version);
        this.entrance = entrance.immutable();
        this.stationAnchor = stationAnchor.immutable();
        this.trainConsolePos = trainConsolePos.immutable();
        if (forward.getAxis().isHorizontal()) this.forward = forward;
        builtAt = Math.max(0L, gameTime);
        setDirty();
    }

    public boolean unlocked() { return unlocked; }
    public int domeMetroVersion() { return domeMetroVersion; }
    public String dimension() { return dimension; }
    @Nullable public BlockPos domeAnchor() { return domeAnchor; }
    @Nullable public BlockPos entrance() { return entrance; }
    @Nullable public BlockPos stationAnchor() { return stationAnchor; }
    @Nullable public BlockPos trainConsolePos() { return trainConsolePos; }
    public Direction forward() { return forward; }
    @Nullable public UUID unlockedBy() { return unlockedBy; }
    public long unlockedAt() { return unlockedAt; }
    public long builtAt() { return builtAt; }

    public boolean needsBuild() {
        return unlocked && domeMetroVersion < CURRENT_DOME_METRO_VERSION;
    }

    public boolean isBuilt() {
        return domeMetroVersion >= CURRENT_DOME_METRO_VERSION && stationAnchor != null && trainConsolePos != null;
    }

    public boolean isNearCentralStation(String dimension, BlockPos pos, double radius) {
        if (!isBuilt() || !this.dimension.equals(dimension)) return false;
        double radiusSqr = radius * radius;
        return stationAnchor.distSqr(pos) <= radiusSqr || trainConsolePos.distSqr(pos) <= radiusSqr;
    }

    /**
     * V1.8 forced-breathable zone for the STARTING/CENTRAL Dome metro only.
     *
     * This is deliberately geometry-bounded rather than a broad radius, so players
     * standing outside the sealed metro walls do not receive free oxygen.
     */
    public boolean isInsideBreathableZone(String dimension, BlockPos pos) {
        if (!isBuilt() || !this.dimension.equals(dimension) || entrance == null || stationAnchor == null) return false;

        Direction fwd = forward.getAxis().isHorizontal() ? forward : Direction.WEST;
        Direction right = fwd.getClockWise();

        // Entire Dome metro access route: interior approach, airlock portal,
        // transfer gallery, stair tube and lower connector.
        int corridorR = local(pos, entrance, right);
        int corridorF = local(pos, entrance, fwd);
        boolean inCorridor = Math.abs(corridorR) <= 5
                && corridorF >= -24 && corridorF <= 42
                && pos.getY() >= stationAnchor.getY() - 4
                && pos.getY() <= entrance.getY() + 10;
        if (inCorridor) return true;

        // The framed 5x5 metro gate sits inside the Dome-side tunnel and must
        // always stay breathable even while the player stands close to the frame.
        boolean inAirlockPortal = Math.abs(corridorR) <= 4
                && corridorF >= -7 && corridorF <= -3
                && pos.getY() >= entrance.getY() - 1
                && pos.getY() <= entrance.getY() + 6;
        if (inAirlockPortal) return true;

        // Central hall + train + capped rail stubs. These dimensions match the
        // constructed Stage-4 central station, not ordinary generated stations.
        int stationR = local(pos, stationAnchor, right);
        int stationF = local(pos, stationAnchor, fwd);
        return Math.abs(stationR) <= 36
                && stationF >= -13 && stationF <= 12
                && pos.getY() >= stationAnchor.getY() - 2
                && pos.getY() <= stationAnchor.getY() + 12;
    }

    private static int local(BlockPos pos, BlockPos origin, Direction axis) {
        return (pos.getX() - origin.getX()) * axis.getStepX()
                + (pos.getZ() - origin.getZ()) * axis.getStepZ();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("DataVersion", DATA_VERSION);
        tag.putBoolean("DOME_METRO_UNLOCKED", unlocked);
        tag.putInt("DomeMetroVersion", domeMetroVersion);
        tag.putString("Dimension", dimension);
        if (domeAnchor != null) tag.putLong("DomeAnchor", domeAnchor.asLong());
        if (entrance != null) tag.putLong("Entrance", entrance.asLong());
        if (stationAnchor != null) tag.putLong("StationAnchor", stationAnchor.asLong());
        if (trainConsolePos != null) tag.putLong("TrainConsolePos", trainConsolePos.asLong());
        tag.putString("Facing", forward.getName());
        if (unlockedBy != null) tag.putUUID("UnlockedBy", unlockedBy);
        tag.putLong("UnlockedAt", unlockedAt);
        tag.putLong("BuiltAt", builtAt);
        return tag;
    }
}
