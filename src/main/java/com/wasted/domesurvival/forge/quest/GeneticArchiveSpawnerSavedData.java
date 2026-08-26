package com.wasted.domesurvival.forge.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists one-time placement of the two hostile spawners at the genetic archive.
 *
 * Once a position is recorded, breaking that spawner later does NOT recreate it.
 */
public final class GeneticArchiveSpawnerSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_genetic_archive_spawners_v1";

    private boolean zombiePlaced;
    private boolean skeletonPlaced;
    private long zombiePos;
    private long skeletonPos;

    public static GeneticArchiveSpawnerSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                GeneticArchiveSpawnerSavedData::load,
                GeneticArchiveSpawnerSavedData::new,
                DATA_NAME
        );
    }

    public static GeneticArchiveSpawnerSavedData load(CompoundTag tag) {
        GeneticArchiveSpawnerSavedData data = new GeneticArchiveSpawnerSavedData();
        data.zombiePlaced = tag.getBoolean("ZombiePlaced");
        data.skeletonPlaced = tag.getBoolean("SkeletonPlaced");
        data.zombiePos = tag.getLong("ZombiePos");
        data.skeletonPos = tag.getLong("SkeletonPos");
        return data;
    }

    public boolean zombiePlaced() {
        return zombiePlaced;
    }

    public boolean skeletonPlaced() {
        return skeletonPlaced;
    }

    public boolean complete() {
        return zombiePlaced && skeletonPlaced;
    }

    public BlockPos zombiePos() {
        return BlockPos.of(zombiePos);
    }

    public BlockPos skeletonPos() {
        return BlockPos.of(skeletonPos);
    }

    public void markZombiePlaced(BlockPos pos) {
        zombiePlaced = true;
        zombiePos = pos.asLong();
        setDirty();
    }

    public void markSkeletonPlaced(BlockPos pos) {
        skeletonPlaced = true;
        skeletonPos = pos.asLong();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("ZombiePlaced", zombiePlaced);
        tag.putBoolean("SkeletonPlaced", skeletonPlaced);
        tag.putLong("ZombiePos", zombiePos);
        tag.putLong("SkeletonPos", skeletonPos);
        return tag;
    }
}
