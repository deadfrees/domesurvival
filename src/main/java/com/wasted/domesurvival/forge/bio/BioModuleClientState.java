package com.wasted.domesurvival.forge.bio;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Client-side mirror of the server datapack registry, used only for display and slot hints. */
public final class BioModuleClientState {
    private static volatile Map<ResourceLocation, BioLootData.Species> species = Map.of();

    private BioModuleClientState() {
    }

    public static void replace(Collection<BioLootData.Species> values) {
        species = values.stream().collect(Collectors.toUnmodifiableMap(
                BioLootData.Species::entityId,
                Function.identity(),
                (first, ignored) -> first
        ));
    }

    @Nullable
    public static BioLootData.Species species(ResourceLocation entityId) {
        return species.get(entityId);
    }

    public static boolean isAllowed(ResourceLocation entityId) {
        return species.containsKey(entityId);
    }

    public static List<BioLootData.Species> allSpecies() {
        return List.copyOf(species.values());
    }
}
