package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import com.wasted.domesurvival.forge.client.render.AirlockGateBlockEntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only V55D renderer registration. */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class AirlockGateClientEvents {
    private AirlockGateClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                AirlockGateRegistry.AIRLOCK_GATE_BLOCK_ENTITY.get(),
                AirlockGateBlockEntityRenderer::new
        );
    }
}
