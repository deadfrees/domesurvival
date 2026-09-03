package com.wasted.domesurvival.forge.bio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Datapack registry for biological-module species and map-independent chest profiles.
 *
 * <p>Files live under {@code data/<namespace>/bio_module_loot/*.json}. A future
 * map or addon can replace the built-in file or add another file without Java changes.</p>
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BioLootData extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile State state = new State(Map.of(), List.of());

    public BioLootData() {
        super(GSON, "bio_module_loot");
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new BioLootData());
    }

    public static boolean isAllowed(ResourceLocation entityId) {
        return state.species().containsKey(entityId);
    }

    @Nullable
    public static Species species(ResourceLocation entityId) {
        return state.species().get(entityId);
    }

    public static List<Species> allSpecies() {
        return List.copyOf(state.species().values());
    }

    @Nullable
    public static SpawnChoice roll(ResourceLocation lootTable, RandomSource random) {
        return roll(lootTable, random, null, null);
    }

    @Nullable
    public static SpawnChoice roll(ResourceLocation lootTable, RandomSource random,
                                   @Nullable ServerLevel level, @Nullable BlockPos chestPos) {
        Profile profile = state.profiles().stream()
                .filter(candidate -> candidate.matches(lootTable))
                .findFirst()
                .orElse(null);

        if (profile == null || random.nextFloat() >= profile.chance()) {
            return null;
        }

        List<Species> candidates = state.species().values().stream()
                .filter(candidate -> profile.groups().contains(candidate.lootGroup()))
                .toList();
        if (candidates.isEmpty()) return null;

        if (level != null && chestPos != null) {
            Species selected = BioModuleDistributionSavedData.get(level).select(
                    candidates,
                    List.copyOf(state.species().values()),
                    chestPos,
                    distributionLocationKey(level, chestPos),
                    random
            );
            return selected == null
                    ? null
                    : new SpawnChoice(selected.entityId(), random.nextFloat() < profile.damagedChance());
        }

        int totalWeight = candidates.stream().mapToInt(Species::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        int selected = random.nextInt(totalWeight);
        for (Species candidate : candidates) {
            selected -= candidate.weight();
            if (selected < 0) {
                return new SpawnChoice(candidate.entityId(), random.nextFloat() < profile.damagedChance());
            }
        }
        return null;
    }

    /**
     * A successful roll occupies one generated building, not merely one chest.
     * Lost Cities buildings are not vanilla StructureStarts, so their stable
     * fallback is the city chunk (one building cell per chunk).
     */
    /**
     * Stable key used to limit generated loot to one biological module per building.
     * The archive cache service also uses this key so its guaranteed samples and
     * ordinary structure loot participate in the same distribution ledger.
     */
    public static String distributionLocationKey(ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> structures =
                level.structureManager().getAllStructuresAt(pos);

        String selected = null;
        for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry : structures.entrySet()) {
            ResourceLocation id = level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .getKey(entry.getKey());
            if (id == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (id.getPath().contains("village")) {
                return "chunk:" + dimension + ":" + Math.floorDiv(pos.getX(), 16)
                        + "," + Math.floorDiv(pos.getZ(), 16);
            }

            long start = entry.getValue().iterator().nextLong();
            String candidate = "structure:" + dimension + ":" + id + "@"
                    + ChunkPos.getX(start) + "," + ChunkPos.getZ(start);
            if (selected == null || candidate.compareTo(selected) < 0) {
                selected = candidate;
            }
        }
        if (selected != null) {
            return selected;
        }
        return "chunk:" + dimension + ":" + Math.floorDiv(pos.getX(), 16)
                + "," + Math.floorDiv(pos.getZ(), 16);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        LinkedHashMap<ResourceLocation, Species> species = new LinkedHashMap<>();
        ArrayList<Profile> profiles = new ArrayList<>();

        objects.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> parseFile(entry.getKey(), entry.getValue(), species, profiles));

        profiles.sort(Comparator.comparingInt(Profile::priority).reversed());
        state = new State(Map.copyOf(species), List.copyOf(profiles));
        LOGGER.info("Loaded {} biological-module species and {} loot profiles",
                species.size(), profiles.size());
    }

    private static void parseFile(ResourceLocation source, JsonElement element,
                                  Map<ResourceLocation, Species> species,
                                  List<Profile> profiles) {
        if (!element.isJsonObject()) {
            LOGGER.warn("Ignoring non-object biological loot file {}", source);
            return;
        }

        JsonObject root = element.getAsJsonObject();
        if (root.has("species") && root.get("species").isJsonArray()) {
            for (JsonElement value : root.getAsJsonArray("species")) {
                parseSpecies(source, value, species);
            }
        }
        if (root.has("profiles") && root.get("profiles").isJsonArray()) {
            for (JsonElement value : root.getAsJsonArray("profiles")) {
                parseProfile(source, value, profiles);
            }
        }
    }

    private static void parseSpecies(ResourceLocation source, JsonElement element,
                                     Map<ResourceLocation, Species> target) {
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        if (object.has("enabled") && !object.get("enabled").getAsBoolean()) return;

        ResourceLocation entityId = ResourceLocation.tryParse(string(object, "entity", ""));
        String rarity = normalized(string(object, "rarity", "common"));
        String group = normalized(string(object, "loot_group", "farm"));
        int weight = boundedInt(object, "weight", 10, 1, 10_000);
        ResourceLocation feedItem = ResourceLocation.tryParse(string(object, "feed", "minecraft:wheat"));
        int feedCount = boundedInt(object, "feed_count", 12, 1, 64);
        int waterMb = boundedInt(object, "water_mb", 1_200, 1, 100_000);
        int energyPerTick = boundedInt(object, "energy_per_tick", 50, 1, 100_000);
        int processTicks = boundedInt(object, "process_ticks", 1_400, 20, 1_000_000);

        if (entityId == null || feedItem == null || group.isBlank()) {
            LOGGER.warn("Ignoring invalid species entry in {}", source);
            return;
        }
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            LOGGER.warn("Ignoring unavailable biological species {} from {}", entityId, source);
            return;
        }
        if (!ForgeRegistries.ITEMS.containsKey(feedItem)) {
            LOGGER.warn("Ignoring biological species {} with unavailable feed {} from {}",
                    entityId, feedItem, source);
            return;
        }
        target.put(entityId, new Species(entityId, rarity, group, weight,
                feedItem, feedCount, waterMb, energyPerTick, processTicks));
    }

    private static void parseProfile(ResourceLocation source, JsonElement element,
                                     List<Profile> target) {
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        String pathPrefix = string(object, "path_prefix", "chests/");
        String namespace = string(object, "namespace", "");
        List<String> contains = strings(object.getAsJsonArray("path_contains"));
        Set<String> groups = new LinkedHashSet<>(strings(object.getAsJsonArray("groups")));
        float chance = boundedFloat(object, "chance", 0.0F, 0.0F, 1.0F);
        float damagedChance = boundedFloat(object, "damaged_chance", 0.5F, 0.0F, 1.0F);
        int priority = boundedInt(object, "priority", 0, -10_000, 10_000);

        if (groups.isEmpty() || chance <= 0.0F) {
            LOGGER.warn("Ignoring empty biological loot profile in {}", source);
            return;
        }
        target.add(new Profile(namespace, pathPrefix, contains, Set.copyOf(groups),
                chance, damagedChance, priority));
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static List<String> strings(@Nullable JsonArray array) {
        if (array == null) return List.of();
        ArrayList<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            try {
                String value = normalized(element.getAsString());
                if (!value.isBlank()) values.add(value);
            } catch (RuntimeException ignored) {
                // A malformed optional entry does not disable the remaining datapack.
            }
        }
        return List.copyOf(values);
    }

    private static int boundedInt(JsonObject object, String key, int fallback, int min, int max) {
        try {
            return object.has(key) ? Math.max(min, Math.min(max, object.get(key).getAsInt())) : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float boundedFloat(JsonObject object, String key, float fallback, float min, float max) {
        try {
            return object.has(key) ? Math.max(min, Math.min(max, object.get(key).getAsFloat())) : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Species(ResourceLocation entityId, String rarity, String lootGroup, int weight,
                          ResourceLocation feedItem, int feedCount, int waterMb,
                          int energyPerTick, int processTicks) { }

    public record SpawnChoice(ResourceLocation entityId, boolean damaged) { }

    private record State(Map<ResourceLocation, Species> species, List<Profile> profiles) { }

    private record Profile(String namespace, String pathPrefix, List<String> pathContains,
                           Set<String> groups, float chance, float damagedChance, int priority) {
        boolean matches(ResourceLocation table) {
            if (!namespace.isBlank() && !namespace.equals(table.getNamespace())) return false;
            String path = table.getPath().toLowerCase(Locale.ROOT);
            if (!path.startsWith(pathPrefix.toLowerCase(Locale.ROOT))) return false;
            return pathContains.isEmpty() || pathContains.stream().anyMatch(path::contains);
        }
    }
}
