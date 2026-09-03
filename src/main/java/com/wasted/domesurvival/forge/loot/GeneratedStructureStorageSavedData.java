package com.wasted.domesurvival.forge.loot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Records generated structures whose storage requirement was already resolved. */
public final class GeneratedStructureStorageSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_generated_structure_storage_v1";
    private final Set<String> handled = new HashSet<>();

    public static GeneratedStructureStorageSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                GeneratedStructureStorageSavedData::load,
                GeneratedStructureStorageSavedData::new,
                DATA_NAME
        );
    }

    private static GeneratedStructureStorageSavedData load(CompoundTag tag) {
        GeneratedStructureStorageSavedData data = new GeneratedStructureStorageSavedData();
        ListTag values = tag.getList("Handled", Tag.TAG_STRING);
        for (int i = 0; i < values.size(); i++) data.handled.add(values.getString(i));
        return data;
    }

    public boolean isHandled(String key) {
        return handled.contains(key);
    }

    public void markHandled(String key) {
        if (handled.add(key)) setDirty();
    }

    public int handledCount() {
        return handled.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag values = new ListTag();
        handled.stream().sorted().map(StringTag::valueOf).forEach(values::add);
        tag.put("Handled", values);
        return tag;
    }
}
