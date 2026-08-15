package com.wasted.domesurvival.forge.machine.oxygen.complex;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class OxygenComplexFilters {
    public static final TagKey<Item> AIR_FILTERS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(DomeSurvival.MOD_ID, "air_filters")
    );

    private OxygenComplexFilters() {
    }

    public static boolean isAirFilter(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AIR_FILTERS);
    }
}
