package com.wasted.domesurvival.forge.progression;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Workshop construction is finalized by Joseph's Stage 02 script only after
 * the complete resource list is satisfied.
 *
 * This login hook intentionally does NOT auto-build from the legacy
 * iron/copper/redstone Java completion flag, because that would bypass the
 * stone bricks, iron bars, glass panes and pistons required by Stage 02.
 */
@Mod.EventBusSubscriber(modid = "domesurvival")
public final class WorkshopProgressionEvents {
    private WorkshopProgressionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Intentionally empty. See class documentation above.
    }
}
