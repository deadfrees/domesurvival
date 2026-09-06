package com.wasted.domesurvival.forge.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * World-global progression state.
 *
 * <p>FTB Quests remains responsible for quest/chapter presentation. This SavedData
 * is the neutral gameplay authority that machines, recipes, NPCs and other systems
 * can query without taking a hard compile-time dependency on FTB Quests.</p>
 */
public final class ProgressionSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_progression";
    private static final String NBT_UNLOCKED = "Unlocked";

    private final Set<String> unlocked = new TreeSet<>();

    public ProgressionSavedData() {
        unlocked.add(ProgressionKeys.SURVIVAL);
    }

    public static ProgressionSavedData load(CompoundTag tag) {
        ProgressionSavedData data = new ProgressionSavedData();
        data.unlocked.clear();

        ListTag list = tag.getList(NBT_UNLOCKED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String normalized = ProgressionKeys.normalize(list.getString(i));
            if (normalized != null) {
                data.unlocked.add(normalized);
            }
        }

        // SURVIVAL is the base state and must never disappear from an old/new world.
        data.unlocked.add(ProgressionKeys.SURVIVAL);
        return data;
    }

    public static ProgressionSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        ProgressionSavedData::load,
                        ProgressionSavedData::new,
                        DATA_NAME
                );
    }

    public boolean isUnlocked(String branch) {
        return unlocked.contains(branch);
    }

    public Set<String> unlockedView() {
        return Collections.unmodifiableSet(unlocked);
    }

    public boolean unlock(String branch) {
        boolean changed = unlocked.add(branch);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean lock(String branch) {
        if (ProgressionKeys.SURVIVAL.equals(branch)) {
            return false;
        }

        boolean changed = unlocked.remove(branch);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean reset() {
        boolean alreadyBaseOnly =
                unlocked.size() == 1
                        && unlocked.contains(ProgressionKeys.SURVIVAL);

        unlocked.clear();
        unlocked.add(ProgressionKeys.SURVIVAL);

        if (!alreadyBaseOnly) {
            setDirty();
            return true;
        }

        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String branch : unlocked) {
            list.add(StringTag.valueOf(branch));
        }
        tag.put(NBT_UNLOCKED, list);
        return tag;
    }
}
