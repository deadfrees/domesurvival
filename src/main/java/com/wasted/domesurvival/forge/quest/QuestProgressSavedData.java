package com.wasted.domesurvival.forge.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Global server-authoritative story flag storage.
 *
 * Stored in the Overworld so the state is shared across dimensions, matching
 * the existing Joseph/Workshop progression model.
 */
public final class QuestProgressSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_quest_progress_v1";
    private static final int SCHEMA_VERSION = 1;

    private final Set<String> flags = new LinkedHashSet<>();

    public static QuestProgressSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                QuestProgressSavedData::load,
                QuestProgressSavedData::new,
                DATA_NAME
        );
    }

    public static QuestProgressSavedData load(CompoundTag tag) {
        QuestProgressSavedData data = new QuestProgressSavedData();
        ListTag stored = tag.getList("Flags", Tag.TAG_STRING);

        for (int i = 0; i < stored.size(); i++) {
            String flag = QuestProgressFlags.normalize(stored.getString(i));
            // Preserve syntactically safe future flags even if an older build
            // temporarily opens the world. Command mutation remains registry-gated.
            if (isSafeFlagId(flag)) {
                data.flags.add(flag);
            }
        }

        return data;
    }

    public boolean hasFlag(String rawFlag) {
        return flags.contains(QuestProgressFlags.normalize(rawFlag));
    }

    public boolean setFlag(String rawFlag) {
        String flag = QuestProgressFlags.normalize(rawFlag);
        if (!QuestProgressFlags.isKnown(flag)) {
            return false;
        }

        boolean changed = flags.add(flag);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean clearFlag(String rawFlag) {
        String flag = QuestProgressFlags.normalize(rawFlag);
        boolean changed = flags.remove(flag);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public List<String> sortedFlags() {
        List<String> result = new ArrayList<>(flags);
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public int flagCount() {
        return flags.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("SchemaVersion", SCHEMA_VERSION);

        ListTag stored = new ListTag();
        for (String flag : sortedFlags()) {
            stored.add(StringTag.valueOf(flag));
        }
        tag.put("Flags", stored);

        return tag;
    }

    private static boolean isSafeFlagId(String value) {
        if (value.isEmpty() || value.length() > 96) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_';
            if (!ok) {
                return false;
            }
        }

        return true;
    }
}
