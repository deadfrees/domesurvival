package com.wasted.domesurvival.forge.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * One-shot authored interior decoration marker for the genetic archive.
 *
 * Once the interior is decorated, player changes are respected and decorations
 * are never regenerated after being broken or moved.
 */
public final class GeneticArchiveInteriorSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_genetic_archive_interior_v1";
    private boolean decorated;

    public static GeneticArchiveInteriorSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                GeneticArchiveInteriorSavedData::load,
                GeneticArchiveInteriorSavedData::new,
                DATA_NAME
        );
    }

    public static GeneticArchiveInteriorSavedData load(CompoundTag tag) {
        GeneticArchiveInteriorSavedData data = new GeneticArchiveInteriorSavedData();
        data.decorated = tag.getBoolean("Decorated");
        return data;
    }

    public boolean decorated() {
        return decorated;
    }

    public void markDecorated() {
        if (!decorated) {
            decorated = true;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Decorated", decorated);
        return tag;
    }
}
