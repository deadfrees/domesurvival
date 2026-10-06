package com.wasted.domesurvival.forge.metro.dome;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Protects the central Dome metro infrastructure from ordinary survival breaking
 * and explosion damage. Creative-mode editing remains possible for development.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DomeMetroProtectionEvents {
    private DomeMetroProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null || event.getPlayer().isCreative()) return;
        if (!(event.getPlayer().level() instanceof ServerLevel level)) return;
        if (DomeMetroAtmosphere.isForcedBreathable(level, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        event.getAffectedBlocks().removeIf(pos -> DomeMetroAtmosphere.isForcedBreathable(level, pos));
    }
}
