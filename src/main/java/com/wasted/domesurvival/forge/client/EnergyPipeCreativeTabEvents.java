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

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EnergyPipeCreativeTabEvents {
    private static final ResourceLocation[] PIPE_IDS = {
            new ResourceLocation(DomeSurvival.MOD_ID, "basic_energy_pipe"),
            new ResourceLocation(DomeSurvival.MOD_ID, "reinforced_energy_pipe"),
            new ResourceLocation(DomeSurvival.MOD_ID, "high_voltage_energy_pipe")
    };

    private EnergyPipeCreativeTabEvents() { }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.REDSTONE_BLOCKS) return;

        for (ResourceLocation id : PIPE_IDS) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null && item != Items.AIR) event.accept(item);
        }
    }
}
