package com.wasted.domesurvival.forge.bio;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Map-independent distribution state for biological modules.
 *
 * <p>Every available species is assigned once before second copies are used.
 * The second copy is only assigned at least 500 horizontal blocks from the
 * first, preventing one structure cluster from completing a breeding pair.</p>
 */
public final class BioModuleDistributionSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_bio_module_distribution_v1";
    private static final long MIN_PAIR_DISTANCE_SQUARED = 500L * 500L;
    private static final Set<ResourceLocation> GUARANTEED_ARCHIVE_SPECIES = Set.of(
            new ResourceLocation("minecraft", "chicken"),
            new ResourceLocation("minecraft", "sheep"),
            new ResourceLocation("minecraft", "cow"),
            new ResourceLocation("minecraft", "pig")
    );

    private final Map<ResourceLocation, Assignment> assignments = new LinkedHashMap<>();
    private final Set<String> occupiedLocations = new HashSet<>();

    public static BioModuleDistributionSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                BioModuleDistributionSavedData::load,
                BioModuleDistributionSavedData::new,
                DATA_NAME
        );
    }

    public static BioModuleDistributionSavedData load(CompoundTag tag) {
        BioModuleDistributionSavedData data = new BioModuleDistributionSavedData();
        ListTag stored = tag.getList("Assignments", Tag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            ResourceLocation entityId = ResourceLocation.tryParse(entry.getString("Entity"));
            if (entityId == null || !entry.contains("FirstPos", Tag.TAG_LONG)) continue;
            int count = Math.max(1, Math.min(2, entry.getInt("Count")));
            data.assignments.put(entityId, new Assignment(entry.getLong("FirstPos"), count));
        }
        ListTag occupied = tag.getList("OccupiedLocations", Tag.TAG_STRING);
        for (int i = 0; i < occupied.size(); i++) {
            String key = occupied.getString(i);
            if (!key.isBlank()) data.occupiedLocations.add(key);
        }
        return data;
    }

    @Nullable
    public synchronized BioLootData.Species select(List<BioLootData.Species> candidates,
                                                   List<BioLootData.Species> allSpecies,
                                                   BlockPos chestPos,
                                                   String locationKey,
                                                   RandomSource random) {
        if (occupiedLocations.contains(locationKey)) {
            return null;
        }

        List<BioLootData.Species> firstCopies = candidates.stream()
                .filter(species -> !assignments.containsKey(species.entityId()))
                // These four first copies are supplied by the guaranteed archive
                // cache. They must not appear in a random chest before the player
                // reaches the archive and accidentally create an easy nearby pair.
                .filter(species -> !GUARANTEED_ARCHIVE_SPECIES.contains(species.entityId()))
                .toList();
        if (!firstCopies.isEmpty()) {
            BioLootData.Species selected = weighted(firstCopies, random);
            assignments.put(selected.entityId(), new Assignment(chestPos.asLong(), 1));
            occupiedLocations.add(locationKey);
            setDirty();
            return selected;
        }

        // A profile whose own species were already discovered must not start
        // producing pairs while first copies from other structure categories
        // are still missing. This is the central exploration rule: complete
        // the species catalogue first, then search far away for breeding pairs.
        boolean allFirstCopiesAssigned = allSpecies.stream()
                .allMatch(species -> assignments.containsKey(species.entityId()));
        if (!allFirstCopiesAssigned) {
            return null;
        }

        List<BioLootData.Species> distantSecondCopies = candidates.stream()
                .filter(species -> {
                    Assignment assignment = assignments.get(species.entityId());
                    return assignment != null && assignment.count() == 1
                            && horizontalDistanceSquared(BlockPos.of(assignment.firstPos()), chestPos)
                            >= MIN_PAIR_DISTANCE_SQUARED;
                })
                .toList();
        if (!distantSecondCopies.isEmpty()) {
            BioLootData.Species selected = weighted(distantSecondCopies, random);
            Assignment previous = assignments.get(selected.entityId());
            assignments.put(selected.entityId(), new Assignment(previous.firstPos(), 2));
            occupiedLocations.add(locationKey);
            setDirty();
            return selected;
        }

        boolean allPaired = allSpecies.stream().allMatch(species -> {
            Assignment assignment = assignments.get(species.entityId());
            return assignment != null && assignment.count() >= 2;
        });
        if (!allPaired) {
            return null;
        }

        List<BioLootData.Species> distantExtras = candidates.stream()
                .filter(species -> horizontalDistanceSquared(
                        BlockPos.of(assignments.get(species.entityId()).firstPos()), chestPos)
                        >= MIN_PAIR_DISTANCE_SQUARED)
                .toList();
        if (distantExtras.isEmpty()) {
            return null;
        }
        BioLootData.Species selected = weighted(distantExtras, random);
        occupiedLocations.add(locationKey);
        setDirty();
        return selected;
    }

    /**
     * Registers the four story samples placed in the guaranteed archive cache.
     * Idempotent so old worlds can migrate the first time their archive chunk is
     * loaded. If a pre-fix world already rolled one of these species elsewhere,
     * the guaranteed cache is recorded as its second copy.
     */
    public synchronized void recordGuaranteedArchiveSamples(BlockPos cachePos, String locationKey) {
        boolean changed = occupiedLocations.add(locationKey);

        for (ResourceLocation entityId : GUARANTEED_ARCHIVE_SPECIES) {
            Assignment existing = assignments.get(entityId);
            if (existing == null) {
                assignments.put(entityId, new Assignment(cachePos.asLong(), 1));
                changed = true;
            } else if (existing.count() == 1 && existing.firstPos() != cachePos.asLong()) {
                // A legacy world may already contain a random first copy. Count
                // the archive specimen as the second and use the archive as the
                // distance anchor for any much later extras.
                assignments.put(entityId, new Assignment(cachePos.asLong(), 2));
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    private static BioLootData.Species weighted(List<BioLootData.Species> candidates, RandomSource random) {
        int totalWeight = candidates.stream().mapToInt(BioLootData.Species::weight).sum();
        int selectedWeight = random.nextInt(Math.max(1, totalWeight));
        for (BioLootData.Species candidate : candidates) {
            selectedWeight -= candidate.weight();
            if (selectedWeight < 0) return candidate;
        }
        return candidates.get(candidates.size() - 1);
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    public synchronized int assignedSpeciesCount() {
        return assignments.size();
    }

    public synchronized int pairedSpeciesCount() {
        return (int) assignments.values().stream().filter(value -> value.count() >= 2).count();
    }

    public synchronized int occupiedLocationCount() {
        return occupiedLocations.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag stored = new ListTag();
        assignments.forEach((entityId, assignment) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Entity", entityId.toString());
            entry.putLong("FirstPos", assignment.firstPos());
            entry.putInt("Count", assignment.count());
            stored.add(entry);
        });
        tag.put("Assignments", stored);
        ListTag occupied = new ListTag();
        occupiedLocations.stream().sorted()
                .map(net.minecraft.nbt.StringTag::valueOf)
                .forEach(occupied::add);
        tag.put("OccupiedLocations", occupied);
        return tag;
    }

    private record Assignment(long firstPos, int count) { }
}
