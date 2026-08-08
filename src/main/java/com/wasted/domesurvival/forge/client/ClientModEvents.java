package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.particle.AcidRainParticle;
import com.wasted.domesurvival.forge.client.particle.SandstormParticle;
import com.wasted.domesurvival.forge.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.AIR_LEVEL.id(),
                "oxygen",
                OxygenHudOverlay.HUD
        );
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ACID_RAIN_STREAK.get(), AcidRainParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SANDSTORM_MOTE.get(), SandstormParticle.Provider::new);
    }
}
