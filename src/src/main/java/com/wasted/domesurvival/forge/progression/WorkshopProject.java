package com.wasted.domesurvival.forge.progression;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * First shared Joseph Cooper project.
 *
 * Stage 3 remains vanilla-resource-only. Tech-mod materials are introduced later.
 */
public final class WorkshopProject {
    public static final String ID = "workshop";

    public static final int IRON_REQUIRED = 64;
    public static final int COPPER_REQUIRED = 32;
    public static final int REDSTONE_REQUIRED = 24;

    private static final TagKey<Item> IRON_INGOTS =
            TagKey.create(Registries.ITEM, new ResourceLocation("forge", "ingots/iron"));

    private static final TagKey<Item> COPPER_INGOTS =
            TagKey.create(Registries.ITEM, new ResourceLocation("forge", "ingots/copper"));

    private WorkshopProject() {
    }

    public static boolean isIron(ItemStack stack) {
        return stack.is(IRON_INGOTS);
    }

    public static boolean isCopper(ItemStack stack) {
        return stack.is(COPPER_INGOTS);
    }

    public static boolean isRedstone(ItemStack stack) {
        return stack.is(Items.REDSTONE);
    }
}
