package com.wasted.domesurvival.forge.technology;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Server-authoritative world-global technology state.
 *
 * <p>The payload is compact, versioned and forward-tolerant. Unknown root fields
 * are preserved so an older build does not silently destroy data written by a
 * newer build.</p>
 */
public final class TechnologySavedData extends SavedData {
    public static final int DATA_VERSION = 1;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "domesurvival_technologies";
    private static final String NBT_VERSION = "Version";
    private static final String NBT_UNLOCKED = "Unlocked";

    private int loadedVersion = DATA_VERSION;
    private final Set<String> unlocked = new TreeSet<>();
    private final CompoundTag passthrough = new CompoundTag();

    public static TechnologySavedData load(CompoundTag tag) {
        TechnologySavedData data = new TechnologySavedData();
        data.loadedVersion = tag.contains(NBT_VERSION, Tag.TAG_INT)
                ? Math.max(0, tag.getInt(NBT_VERSION))
                : 0;

        data.passthrough.merge(tag.copy());
        data.passthrough.remove(NBT_VERSION);
        data.passthrough.remove(NBT_UNLOCKED);

        if (tag.contains(NBT_UNLOCKED, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_UNLOCKED, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String normalized = TechnologyKeys.normalize(list.getString(i));
                if (normalized != null) {
                    data.unlocked.add(normalized);
                }
            }
        } else if (tag.contains(NBT_UNLOCKED)) {
            LOGGER.warn("Technology SavedData contains malformed '{}' payload; loading with an empty unlock set", NBT_UNLOCKED);
        }

        if (data.loadedVersion > DATA_VERSION) {
            LOGGER.warn(
                    "Technology SavedData version {} is newer than supported version {}; unknown fields will be preserved",
                    data.loadedVersion,
                    DATA_VERSION
            );
        }

        data.migrateIfNeeded();
        return data;
    }

    public static TechnologySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TechnologySavedData::load,
                TechnologySavedData::new,
                DATA_NAME
        );
    }

    private void migrateIfNeeded() {
        if (loadedVersion < DATA_VERSION) {
            loadedVersion = DATA_VERSION;
            setDirty();
        }
    }

    public int dataVersion() {
        return loadedVersion;
    }

    public boolean isUnlocked(String id) {
        return unlocked.contains(id);
    }

    public Set<String> unlockedView() {
        return Collections.unmodifiableSet(unlocked);
    }

    public boolean unlock(String id) {
        boolean changed = unlocked.add(id);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean lock(String id) {
        boolean changed = unlocked.remove(id);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.merge(passthrough.copy());
        tag.putInt(NBT_VERSION, Math.max(DATA_VERSION, loadedVersion));

        ListTag list = new ListTag();
        for (String id : unlocked) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(NBT_UNLOCKED, list);
        return tag;
    }
}
