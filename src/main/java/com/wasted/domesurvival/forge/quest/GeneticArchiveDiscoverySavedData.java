package com.wasted.domesurvival.forge.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent state for the delayed, map-independent genetic-archive discovery. */
public final class GeneticArchiveDiscoverySavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_genetic_archive_v1";
    private static final int TARGET_VERSION = 2;

    private long triggerAtGameTime = -1L;
    private long nextLocateAttemptGameTime = -1L;
    private boolean hasTarget;
    private int targetX;
    private int targetY;
    private int targetZ;
    private String targetStructure = "";

    public static GeneticArchiveDiscoverySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                GeneticArchiveDiscoverySavedData::load,
                GeneticArchiveDiscoverySavedData::new,
                DATA_NAME
        );
    }

    public static GeneticArchiveDiscoverySavedData load(CompoundTag tag) {
        GeneticArchiveDiscoverySavedData data = new GeneticArchiveDiscoverySavedData();
        data.triggerAtGameTime = tag.contains("TriggerAtGameTime")
                ? tag.getLong("TriggerAtGameTime") : -1L;
        data.nextLocateAttemptGameTime = tag.contains("NextLocateAttemptGameTime")
                ? tag.getLong("NextLocateAttemptGameTime") : -1L;

        // V1 stored one hard-coded point from the authored WASTED_TEST map.
        // It is deliberately not imported: every V2 world selects a generated structure.
        if (tag.getInt("TargetVersion") >= TARGET_VERSION) {
            data.hasTarget = tag.getBoolean("HasTarget");
            data.targetX = tag.getInt("TargetX");
            data.targetY = tag.getInt("TargetY");
            data.targetZ = tag.getInt("TargetZ");
            data.targetStructure = tag.getString("TargetStructure");
        }
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

    public boolean hasTarget() {
        return hasTarget;
    }

    public BlockPos target() {
        return hasTarget ? new BlockPos(targetX, targetY, targetZ) : null;
    }

    public String targetStructure() {
        return targetStructure;
    }

    public boolean canAttemptLocate(long gameTime) {
        return !hasTarget && gameTime >= nextLocateAttemptGameTime;
    }

    public void retryLocateAt(long gameTime) {
        if (!hasTarget && nextLocateAttemptGameTime != gameTime) {
            nextLocateAttemptGameTime = gameTime;
            setDirty();
        }
    }

    public void selectTarget(BlockPos pos, String structureId) {
        targetX = pos.getX();
        targetY = pos.getY();
        targetZ = pos.getZ();
        targetStructure = structureId == null ? "" : structureId;
        hasTarget = true;
        nextLocateAttemptGameTime = -1L;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("TargetVersion", TARGET_VERSION);
        tag.putLong("TriggerAtGameTime", triggerAtGameTime);
        tag.putLong("NextLocateAttemptGameTime", nextLocateAttemptGameTime);
        tag.putBoolean("HasTarget", hasTarget);
        tag.putInt("TargetX", targetX);
        tag.putInt("TargetY", targetY);
        tag.putInt("TargetZ", targetZ);
        tag.putString("TargetStructure", targetStructure);
        return tag;
    }
}
