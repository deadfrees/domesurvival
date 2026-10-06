package com.wasted.domesurvival.forge.metro.dome;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Runtime protection for the isolated CENTRAL metro station under the Dome.
 *
 * The scope is intentionally narrow: only the persistent Stage-4 central station
 * and its sealed access corridor are forced breathable. Worldgen/restored metro
 * stations are not affected.
 *
 * This keeps vanilla air full and suppresses only oxygen/vacuum/asphyxia-specific
 * effects/damage while the player is inside the central station zone. The public
 * DomeMetroAtmosphere.isForcedBreathable(...) method is also the stable hook for
 * DomeSurvival's atmosphere evaluator.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DomeMetroAtmosphereEvents {
    private DomeMetroAtmosphereEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!DomeMetroAtmosphere.isForcedBreathable(level, player.blockPosition())) return;

        // Vanilla air must never run down in the central Dome metro.
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            player.setAirSupply(player.getMaxAirSupply());
        }

        // Avoid doing registry/effect work every tick. This is event-driven per
        // player and bounded; no room scan or chunk forcing is involved.
        if ((player.tickCount % 10) != 0) return;
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            MobEffect type = effect.getEffect();
            var key = BuiltInRegistries.MOB_EFFECT.getKey(type);
            if (key != null && looksLikeOxygenHazard(key.getPath())) {
                player.removeEffect(type);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!DomeMetroAtmosphere.isForcedBreathable(level, player.blockPosition())) return;
        if (looksLikeOxygenHazard(event.getSource())) event.setCanceled(true);
    }

    private static boolean looksLikeOxygenHazard(DamageSource source) {
        return looksLikeOxygenHazard(source.getMsgId());
    }

    private static boolean looksLikeOxygenHazard(String id) {
        String value = id.toLowerCase(Locale.ROOT);
        return value.contains("oxygen")
                || value.contains("o2")
                || value.contains("asphyx")
                || value.contains("hypoxia")
                || value.contains("vacuum")
                || value.contains("airless")
                || value.contains("no_air")
                || value.contains("noair");
    }
}
