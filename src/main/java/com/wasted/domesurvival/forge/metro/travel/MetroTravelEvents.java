package com.wasted.domesurvival.forge.metro.travel;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MetroTravelEvents {
    private MetroTravelEvents() {
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MetroTravelService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MetroTravelService.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MetroTravelService.cancel(
                    player,
                    "\u041f\u043e\u0435\u0437\u0434\u043a\u0430 \u043e\u0442\u043c\u0435\u043d\u0435\u043d\u0430: \u0438\u0437\u043c\u0435\u0440\u0435\u043d\u0438\u0435 \u0431\u044b\u043b\u043e \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u043e \u0432\u043d\u0435 \u043c\u0435\u0442\u0440\u043e."
            );
        }
    }

    @SubscribeEvent
    public static void playerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MetroTravelService.cancel(
                    player,
                    "\u041f\u043e\u0435\u0437\u0434\u043a\u0430 \u043e\u0442\u043c\u0435\u043d\u0435\u043d\u0430 \u0438\u0437-\u0437\u0430 \u0441\u043c\u0435\u0440\u0442\u0438 \u0438\u0433\u0440\u043e\u043a\u0430."
            );
        }
    }
}
