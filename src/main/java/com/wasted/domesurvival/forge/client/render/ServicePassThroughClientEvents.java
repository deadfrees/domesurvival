package com.wasted.domesurvival.forge.client.render;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ServicePassThroughClientEvents {
    private ServicePassThroughClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ServicePassThroughRegistry.BLOCK_ENTITY.get(),
                ServicePassThroughRenderer::new
        );
    }
}
