package com.wasted.domesurvival.forge.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class DomeSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_dome";
    private boolean generated;

    public static DomeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DomeSavedData::load, DomeSavedData::new, DATA_NAME);
    }

    public static DomeSavedData load(CompoundTag tag) {
        DomeSavedData data = new DomeSavedData();
        data.generated = tag.getBoolean("Generated");
        return data;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void markGenerated() {
        generated = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Generated", generated);
        return tag;
    }
}
