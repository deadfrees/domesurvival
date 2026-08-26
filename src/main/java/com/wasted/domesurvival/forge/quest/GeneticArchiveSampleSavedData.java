package com.wasted.domesurvival.forge.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists one-time placement of the archive sample cache.
 *
 * The cache is not recreated after players remove or destroy it.
 */
public final class GeneticArchiveSampleSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_genetic_archive_samples_v1";

    private boolean cachePlaced;
    private long cachePos;

    public static GeneticArchiveSampleSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                GeneticArchiveSampleSavedData::load,
                GeneticArchiveSampleSavedData::new,
                DATA_NAME
        );
    }

    public static GeneticArchiveSampleSavedData load(CompoundTag tag) {
        GeneticArchiveSampleSavedData data = new GeneticArchiveSampleSavedData();
        data.cachePlaced = tag.getBoolean("CachePlaced");
        data.cachePos = tag.getLong("CachePos");
        return data;
    }

    public boolean cachePlaced() {
        return cachePlaced;
    }

    public BlockPos cachePos() {
        return BlockPos.of(cachePos);
    }

    public void markCachePlaced(BlockPos pos) {
        cachePlaced = true;
        cachePos = pos.asLong();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("CachePlaced", cachePlaced);
        tag.putLong("CachePos", cachePos);
        return tag;
    }
}
