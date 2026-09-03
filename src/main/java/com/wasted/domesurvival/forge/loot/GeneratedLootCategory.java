package com.wasted.domesurvival.forge.loot;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Locale;
import java.util.Map;

/** Shared semantic classification for generated-building storage and loot. */
final class GeneratedLootCategory {
    private GeneratedLootCategory() {
    }

    static ResourceLocation tableFor(ResourceLocation structureId) {
        return table(classify(structureId.toString()));
    }

    static ResourceLocation tableAt(ServerLevel level, BlockPos pos) {
        Category selected = Category.CITY;
        Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> structures =
                level.structureManager().getAllStructuresAt(pos);
        for (Structure structure : structures.keySet()) {
            ResourceLocation id = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getKey(structure);
            if (id == null) continue;
            Category candidate = classify(id.toString());
            if (candidate.priority > selected.priority) {
                selected = candidate;
            }
        }
        return table(selected);
    }

    private static ResourceLocation table(Category category) {
        return new ResourceLocation(DomeSurvival.MOD_ID, "chests/lastworld/" + category.table);
    }

    private static Category classify(String rawId) {
        String value = rawId.toLowerCase(Locale.ROOT);
        if (containsAny(value,
                "temple", "stronghold", "catacomb", "archeolog", "laboratory",
                "archive", "obelisk", "monastery", "underground_cabin")) {
            return Category.RESEARCH;
        }
        if (containsAny(value,
                "dungeon", "grave", "crypt", "fort", "mansion", "asylum", "portal")) {
            return Category.DANGER;
        }
        if (containsAny(value,
                "mineshaft", "mining", "foundry", "factory", "mechanical", "scorched")) {
            return Category.MINING;
        }
        if (containsAny(value,
                "village", "house", "camp", "town", "cabin", "ship", "well")) {
            return Category.LOGISTICS;
        }
        return Category.CITY;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private enum Category {
        CITY("city_salvage", 0),
        LOGISTICS("logistics", 1),
        MINING("mining", 2),
        DANGER("rare", 3),
        RESEARCH("medical_research", 4);

        private final String table;
        private final int priority;

        Category(String table, int priority) {
            this.table = table;
            this.priority = priority;
        }
    }
}
