package com.wasted.domesurvival.forge.metro.network;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** One startup synchronization; no metro graph work is performed every tick. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MetroNetworkEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MetroNetworkEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MetroNetworkService.SyncResult result = MetroNetworkService.synchronize(event.getServer());
        LOGGER.info("Metro network synchronized: nodes={} routes={} nodeDelta={} routeDelta={}",
                result.nodeCount(), result.routeCount(), result.nodeDelta(), result.routeDelta());
    }
}
