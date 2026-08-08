package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.config.SurfaceHazardConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative sun, acid-rain and sandstorm damage. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurfaceHazardService {
    private SurfaceHazardService() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        int intervalTicks = SurfaceHazardConfig.DAMAGE_INTERVAL_TICKS.get();
        // 10 ticks is the default because vanilla LivingEntity hurt cooldown makes
        // substantially faster repeated hurt() calls unreliable. The +5 phase offset keeps
        // surface hits between the oxygen system's normal whole-second boundaries.
        if (Math.floorMod(event.getServer().getTickCount() - 5, intervalTicks) != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }

        switch (SurfaceHazardEnvironment.exposure(player)) {
            case SOLAR -> damage(player, SurfaceHazardConfig.SOLAR_ENABLED.get(),
                    SurfaceHazardConfig.SOLAR_DAMAGE.get().floatValue(), true);
            case ACID_RAIN -> damage(player, SurfaceHazardConfig.ACID_RAIN_ENABLED.get(),
                    SurfaceHazardConfig.ACID_RAIN_DAMAGE.get().floatValue(), false);
            case ACID_THUNDERSTORM -> damage(player, SurfaceHazardConfig.ACID_RAIN_ENABLED.get(),
                    SurfaceHazardConfig.ACID_THUNDER_DAMAGE.get().floatValue(), false);
            case SANDSTORM -> damage(player, SurfaceHazardConfig.SANDSTORM_ENABLED.get(),
                    SurfaceHazardConfig.SANDSTORM_DAMAGE.get().floatValue(), false);
            case NONE -> {
            }
        }
    }

    private static void damage(ServerPlayer player, boolean enabled, float amount, boolean fireType) {
        if (!enabled || amount <= 0.0F) {
            return;
        }
        player.hurt(fireType ? player.damageSources().onFire() : player.damageSources().generic(), amount);
    }
}
