package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.hopper.HopperRegistryEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class HopperClientEvents {
    private HopperClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(
                        HopperRegistryEvents.TIERED_HOPPER_MENU.get(),
                        TieredHopperScreen::new
                )
        );
    }
}
