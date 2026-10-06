package com.wasted.domesurvival.forge.metro;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RestoredMetroAtmosphereEvents {
    private RestoredMetroAtmosphereEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RestoredMetroAtmosphere.rebuild(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RestoredMetroAtmosphere.clear(event.getServer());
    }
}
