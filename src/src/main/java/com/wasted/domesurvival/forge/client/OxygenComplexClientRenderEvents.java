package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.render.OxygenComplexPortRenderer;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class OxygenComplexClientRenderEvents {
    private OxygenComplexClientRenderEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                OxygenComplexRegistry.BLOCK_ENTITY.get(),
                OxygenComplexPortRenderer::new
        );
    }
}
