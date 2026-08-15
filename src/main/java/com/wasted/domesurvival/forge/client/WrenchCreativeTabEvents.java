package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Adds the service wrench to vanilla Tools & Utilities for recipe-less testing. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WrenchCreativeTabEvents {
    private static final ResourceLocation WRENCH_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "machine_wrench");

    private WrenchCreativeTabEvents() { }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
        Item wrench = ForgeRegistries.ITEMS.getValue(WRENCH_ID);
        if (wrench != null && wrench != Items.AIR) event.accept(wrench);
    }
}
