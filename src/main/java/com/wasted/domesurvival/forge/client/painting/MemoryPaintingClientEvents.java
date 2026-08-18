package com.wasted.domesurvival.forge.client.painting;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.registry.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class MemoryPaintingClientEvents {
    private MemoryPaintingClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntityTypes.MEMORY_PAINTING.get(),
                MemoryPaintingRenderer::new
        );
    }
}
