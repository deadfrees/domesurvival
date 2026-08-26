package com.wasted.domesurvival.forge.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent state for the delayed genetic-archive discovery.
 *
 * The timer is measured in world game ticks, so ten minutes means ten minutes
 * of actual server gameplay. Offline server downtime does not consume it.
 */
public final class GeneticArchiveDiscoverySavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_genetic_archive_v1";
    // Existing map building selected in-game.
    // Screenshot position: -1087.324 / 92.000 / -675.597.
    // Negative decimal coordinates are floored to the containing block.
    private static final int FIXED_TARGET_X = -1088;
    private static final int FIXED_TARGET_Y = 92;
    private static final int FIXED_TARGET_Z = -676;
    private long triggerAtGameTime = -1L;
    private boolean hasTarget;
    private int targetX;
    private int targetZ;

    public static GeneticArchiveDiscoverySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                GeneticArchiveDiscoverySavedData::load,
                GeneticArchiveDiscoverySavedData::new,
                DATA_NAME
        );
    }

    public static GeneticArchiveDiscoverySavedData load(CompoundTag tag) {
        GeneticArchiveDiscoverySavedData data = new GeneticArchiveDiscoverySavedData();
        data.triggerAtGameTime = tag.getLong("TriggerAtGameTime");
        if (!tag.contains("TriggerAtGameTime")) {
            data.triggerAtGameTime = -1L;
        }

        data.hasTarget = tag.getBoolean("HasTarget");
        data.targetX = tag.getInt("TargetX");
        data.targetZ = tag.getInt("TargetZ");
        return data;
    }

    public boolean isScheduled() {
        return triggerAtGameTime >= 0L;
    }

    public long triggerAtGameTime() {
        return triggerAtGameTime;
    }

    public void scheduleAt(long gameTime) {
        if (triggerAtGameTime != gameTime) {
            triggerAtGameTime = gameTime;
            setDirty();
        }
    }

    public void clearSchedule() {
        if (triggerAtGameTime >= 0L) {
            triggerAtGameTime = -1L;
            setDirty();
        }
    }

    public void ensureTarget(ServerLevel level) {
        // Migrate any V1 procedural target already stored in an existing world.
        if (hasTarget && targetX == FIXED_TARGET_X && targetZ == FIXED_TARGET_Z) {
            return;
        }

        targetX = FIXED_TARGET_X;
        targetZ = FIXED_TARGET_Z;
        hasTarget = true;
        setDirty();
    }

    public BlockPos target(ServerLevel level) {
        ensureTarget(level);
        return new BlockPos(targetX, FIXED_TARGET_Y, targetZ);
    }
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("TriggerAtGameTime", triggerAtGameTime);
        tag.putBoolean("HasTarget", hasTarget);
        tag.putInt("TargetX", targetX);
        tag.putInt("TargetZ", targetZ);
        return tag;
    }
}
