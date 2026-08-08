package com.wasted.domesurvival.forge.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class DomeSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_dome";
    private int structureVersion;

    public static DomeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DomeSavedData::load, DomeSavedData::new, DATA_NAME);
    }

    public static DomeSavedData load(CompoundTag tag) {
        DomeSavedData data = new DomeSavedData();
        if (tag.contains("StructureVersion")) {
            data.structureVersion = tag.getInt("StructureVersion");
        } else if (tag.getBoolean("Generated")) {
            // Migration from the first prototype save format.
            data.structureVersion = 1;
        }
        return data;
    }

    public boolean isGenerated() {
        return structureVersion >= 1;
    }

    public int structureVersion() {
        return structureVersion;
    }

    public void markStructureVersion(int version) {
        if (version > structureVersion) {
            structureVersion = version;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Generated", isGenerated());
        tag.putInt("StructureVersion", structureVersion);
        return tag;
    }
}
