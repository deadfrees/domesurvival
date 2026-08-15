package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.render.UniversalTankBlockEntityRenderer;
import com.wasted.domesurvival.forge.client.screen.UniversalTankScreen;
import com.wasted.domesurvival.forge.storage.tank.UniversalTankRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client registration for V63 reservoir model and GUI. */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class UniversalTankClientEvents {
    private UniversalTankClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(
                    UniversalTankRegistry.UNIVERSAL_TANK_MENU.get(),
                    UniversalTankScreen::new
            );
            ItemBlockRenderTypes.setRenderLayer(
                    UniversalTankRegistry.UNIVERSAL_TANK.get(),
                    RenderType.cutout()
            );
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                UniversalTankRegistry.UNIVERSAL_TANK_BLOCK_ENTITY.get(),
                UniversalTankBlockEntityRenderer::new
        );
    }
}
