package com.wasted.domesurvival.forge.client.metro;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Stage-7 client-only overlay registration.
 *
 * Kept in its own MOD-bus subscriber so passenger travel does not depend on
 * the exact formatting/content of ClientModEvents.java.
 */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class MetroTravelClientEvents {
    private MetroTravelClientEvents() {
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.AIR_LEVEL.id(),
                "metro_travel",
                MetroTravelOverlay.HUD
        );
    }
}
