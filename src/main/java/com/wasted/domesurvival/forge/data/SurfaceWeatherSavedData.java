package com.wasted.domesurvival.forge.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent scheduler state for custom sandstorms. Stored separately from dome/oxygen state. */
public final class SurfaceWeatherSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_surface_weather";

    private int sandstormSecondsRemaining;
    private int sandstormCooldownSeconds;

    public static SurfaceWeatherSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SurfaceWeatherSavedData::load,
                SurfaceWeatherSavedData::new,
                DATA_NAME
        );
    }

    public static SurfaceWeatherSavedData load(CompoundTag tag) {
        SurfaceWeatherSavedData data = new SurfaceWeatherSavedData();
        data.sandstormSecondsRemaining = Math.max(0, tag.getInt("SandstormSecondsRemaining"));
        data.sandstormCooldownSeconds = Math.max(0, tag.getInt("SandstormCooldownSeconds"));
        return data;
    }

    public boolean sandstormActive() {
        return sandstormSecondsRemaining > 0;
    }

    public int sandstormSecondsRemaining() {
        return sandstormSecondsRemaining;
    }

    public int sandstormCooldownSeconds() {
        return sandstormCooldownSeconds;
    }

    public void startSandstorm(int durationSeconds) {
        sandstormSecondsRemaining = Math.max(1, durationSeconds);
        sandstormCooldownSeconds = 0;
        setDirty();
    }

    public void stopSandstorm() {
        if (sandstormSecondsRemaining != 0) {
            sandstormSecondsRemaining = 0;
            setDirty();
        }
    }

    public void setCooldownSeconds(int seconds) {
        sandstormCooldownSeconds = Math.max(0, seconds);
        setDirty();
    }

    public boolean tickSandstormSecond() {
        if (sandstormSecondsRemaining <= 0) {
            return false;
        }
        sandstormSecondsRemaining--;
        setDirty();
        return sandstormSecondsRemaining == 0;
    }

    public boolean tickCooldownSecond() {
        if (sandstormCooldownSeconds <= 0) {
            return true;
        }
        sandstormCooldownSeconds--;
        setDirty();
        return sandstormCooldownSeconds == 0;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("SandstormSecondsRemaining", sandstormSecondsRemaining);
        tag.putInt("SandstormCooldownSeconds", sandstormCooldownSeconds);
        return tag;
    }
}
