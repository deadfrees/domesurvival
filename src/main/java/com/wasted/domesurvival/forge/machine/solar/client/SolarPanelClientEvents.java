package com.wasted.domesurvival.forge.machine.solar.client;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.solar.SolarPanelRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class SolarPanelClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(SolarPanelRegistry.menuType(), SolarPanelScreen::new);
            ItemBlockRenderTypes.setRenderLayer(SolarPanelRegistry.SOLAR_PANEL_MK1.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SolarPanelRegistry.SOLAR_PANEL_MK2.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SolarPanelRegistry.SOLAR_PANEL_MK3.get(), RenderType.cutout());
        });
    }

    private SolarPanelClientEvents() {
    }
}
