package com.wasted.domesurvival.forge.machine.oxygen.complex;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Small controller-backup store used only when the OUTPUT/controller block is absent.
 * It lets a temporarily incomplete 2x2 complex recover its FE/O2/intermediate buffers.
 */
public final class OxygenComplexSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_oxygen_complex";
    private static final String TAG_RECORDS = "Records";
    private static final String TAG_POS = "ControllerPos";
    private static final String TAG_STATE = "State";

    private final Map<Long, CompoundTag> records = new HashMap<>();

    public static OxygenComplexSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                OxygenComplexSavedData::load,
                OxygenComplexSavedData::new,
                DATA_NAME
        );
    }

    public static OxygenComplexSavedData load(CompoundTag tag) {
        OxygenComplexSavedData data = new OxygenComplexSavedData();
        ListTag list = tag.getList(TAG_RECORDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains(TAG_POS) || !entry.contains(TAG_STATE, Tag.TAG_COMPOUND)) {
                continue;
            }
            data.records.put(entry.getLong(TAG_POS), entry.getCompound(TAG_STATE).copy());
        }
        return data;
    }

    public void store(BlockPos controllerPos, CompoundTag state) {
        records.put(controllerPos.asLong(), state.copy());
        setDirty();
    }

    public CompoundTag getSnapshot(BlockPos controllerPos) {
        CompoundTag state = records.get(controllerPos.asLong());
        return state == null ? null : state.copy();
    }

    public void remove(BlockPos controllerPos) {
        if (records.remove(controllerPos.asLong()) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, CompoundTag> entry : records.entrySet()) {
            CompoundTag record = new CompoundTag();
            record.putLong(TAG_POS, entry.getKey());
            record.put(TAG_STATE, entry.getValue().copy());
            list.add(record);
        }
        tag.put(TAG_RECORDS, list);
        return tag;
    }
}
