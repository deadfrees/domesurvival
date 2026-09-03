package com.wasted.domesurvival.forge.data;

import com.wasted.domesurvival.core.airlock.AirlockPressure;
import com.wasted.domesurvival.core.airlock.AirlockState;
import com.wasted.domesurvival.core.dome.DomeSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class DomeSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_dome";

    private int structureVersion;
    private int starterTerrainVersion;
    private boolean hasDomeLocation;
    private int domeCenterX;
    private int domeBaseY;
    private int domeCenterZ;
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
        data.starterTerrainVersion = tag.getInt("StarterTerrainVersion");

        if (tag.contains("DomeCenterX") && tag.contains("DomeBaseY") && tag.contains("DomeCenterZ")) {
            data.hasDomeLocation = true;
            data.domeCenterX = tag.getInt("DomeCenterX");
            data.domeBaseY = tag.getInt("DomeBaseY");
            data.domeCenterZ = tag.getInt("DomeCenterZ");
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

        boolean normalized = false;

        // Self-heal impossible legacy/corrupt state.
        if (data.airlockInnerOpen && data.airlockOuterOpen) {
            data.airlockInnerOpen = false;
            data.airlockOuterOpen = false;
            data.airlockPressure = AirlockPressure.PRESSURIZED;
            normalized = true;
        }

        // V2.2 uses instant re-pressurization after the outer shutter closes.
        // Normalize legacy V2 saves that may contain both shutters closed while
        // the chamber is still marked DEPRESSURIZED.
        if (!data.airlockOuterOpen && data.airlockPressure == AirlockPressure.DEPRESSURIZED) {
            data.airlockPressure = AirlockPressure.PRESSURIZED;
            normalized = true;
        }

        if (normalized) {
            data.setDirty();
        }
        return data;
    }

    public boolean isGenerated() {
        return structureVersion >= 1;
    }

    public int structureVersion() {
        return structureVersion;
    }

    public int starterTerrainVersion() {
        return starterTerrainVersion;
    }

    public void markStarterTerrainVersion(int version) {
        if (version > starterTerrainVersion) {
            starterTerrainVersion = version;
            setDirty();
        }
    }

    public boolean hasDomeLocation() {
        return hasDomeLocation;
    }

    /** Legacy saves keep their original fixed anchor; new LastWorld saves persist the chosen site. */
    public DomeSpec domeSpec() {
        DomeSpec legacy = DomeSpec.wastedV1();
        return hasDomeLocation
                ? legacy.at(domeCenterX, domeBaseY, domeCenterZ)
                : legacy;
    }

    public void setDomeLocation(BlockPos center) {
        hasDomeLocation = true;
        domeCenterX = center.getX();
        domeBaseY = center.getY();
        domeCenterZ = center.getZ();
        setDirty();
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
        tag.putInt("StarterTerrainVersion", starterTerrainVersion);
        if (hasDomeLocation) {
            tag.putInt("DomeCenterX", domeCenterX);
            tag.putInt("DomeBaseY", domeBaseY);
            tag.putInt("DomeCenterZ", domeCenterZ);
        }
        tag.putBoolean("AirlockInnerOpen", airlockInnerOpen);
        tag.putBoolean("AirlockOuterOpen", airlockOuterOpen);
        tag.putString("AirlockPressure", airlockPressure.name());
        return tag;
    }
}
