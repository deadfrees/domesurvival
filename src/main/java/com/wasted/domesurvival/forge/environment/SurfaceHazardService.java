package com.wasted.domesurvival.forge.environment;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.config.SurfaceHazardConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative sun, acid-rain and sandstorm damage. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurfaceHazardService {
    private static final int MOB_SOLAR_FIRE_SECONDS = 3;

    private SurfaceHazardService() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        int intervalTicks = SurfaceHazardConfig.DAMAGE_INTERVAL_TICKS.get();
        if (Math.floorMod(event.getServer().getTickCount() - 5, intervalTicks) != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    /**
     * Passive, neutral, hostile and modded mobs use the same solar exposure
     * classification as players. They visibly burn with vanilla fire rendering,
     * while the actual solar damage remains generic so fire immunity does not
     * protect a mob from the lethal sun.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !mob.isAlive()
                || mob.level().isClientSide()) {
            return;
        }

        int intervalTicks = SurfaceHazardConfig.DAMAGE_INTERVAL_TICKS.get();
        if (Math.floorMod(mob.tickCount - 5, intervalTicks) != 0) {
            return;
        }

        if (!SurfaceHazardConfig.SOLAR_ENABLED.get()) {
            return;
        }

        float amount = SurfaceHazardConfig.SOLAR_DAMAGE.get().floatValue();
        if (amount <= 0.0F || SurfaceHazardEnvironment.exposure(mob) != SurfaceExposure.SOLAR) {
            return;
        }

        mob.setSecondsOnFire(MOB_SOLAR_FIRE_SECONDS);
        mob.hurt(mob.damageSources().generic(), amount);
    }

    private static void tickPlayer(ServerPlayer player) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }

        if (SurfaceSuitEquipment.hasFullSuit(player)) {
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
