package com.wasted.domesurvival.forge.data;

import com.wasted.domesurvival.core.airlock.AirlockPressure;
import com.wasted.domesurvival.core.airlock.AirlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class DomeSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_dome";

    private int structureVersion;
    private boolean airlockInnerOpen;
    private boolean airlockOuterOpen;
    private AirlockPressure airlockPressure = AirlockPressure.PRESSURIZED;

    public static DomeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DomeSavedData::load, DomeSavedData::new, DATA_NAME);
    }

    public static DomeSavedData load(CompoundTag tag) {
        DomeSavedData data = new DomeSavedData();
        if (tag.contains("StructureVersion")) {
            data.structureVersion = tag.getInt("StructureVersion");
        } else if (tag.getBoolean("Generated")) {
            data.structureVersion = 1;
        }

        data.airlockInnerOpen = tag.getBoolean("AirlockInnerOpen");
        data.airlockOuterOpen = tag.getBoolean("AirlockOuterOpen");

        if (tag.contains("AirlockPressure")) {
            try {
                data.airlockPressure = AirlockPressure.valueOf(tag.getString("AirlockPressure"));
            } catch (IllegalArgumentException ignored) {
                data.airlockPressure = AirlockPressure.PRESSURIZED;
            }
        }

        // Self-heal impossible legacy/corrupt state.
        if (data.airlockInnerOpen && data.airlockOuterOpen) {
            data.airlockInnerOpen = false;
            data.airlockOuterOpen = false;
            data.airlockPressure = AirlockPressure.PRESSURIZED;
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

    public AirlockState airlockState() {
        return new AirlockState(airlockInnerOpen, airlockOuterOpen, airlockPressure);
    }

    public void setAirlockState(AirlockState state) {
        airlockInnerOpen = state.innerOpen();
        airlockOuterOpen = state.outerOpen();
        airlockPressure = state.pressure();
        setDirty();
    }

    public void resetAirlock() {
        setAirlockState(AirlockState.initial());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Generated", isGenerated());
        tag.putInt("StructureVersion", structureVersion);
        tag.putBoolean("AirlockInnerOpen", airlockInnerOpen);
        tag.putBoolean("AirlockOuterOpen", airlockOuterOpen);
        tag.putString("AirlockPressure", airlockPressure.name());
        return tag;
    }
}
