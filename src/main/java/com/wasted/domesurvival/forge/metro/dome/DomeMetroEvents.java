package com.wasted.domesurvival.forge.metro.dome;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Event-driven recovery only; no tick polling and no chunk scanning. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DomeMetroEvents {
    private DomeMetroEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        DomeMetroService.ensureBuilt(event.getServer());
    }
}
