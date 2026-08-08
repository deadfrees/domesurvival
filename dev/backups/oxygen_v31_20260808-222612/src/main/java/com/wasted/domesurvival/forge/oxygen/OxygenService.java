package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.core.oxygen.OxygenRules;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.network.ModNetwork;
import com.wasted.domesurvival.forge.network.OxygenSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-authoritative oxygen simulation.
 *
 * Performance rules:
 * - one global server callback per tick;
 * - actual player simulation only every 20 ticks;
 * - O(1) arithmetic environment classification;
 * - no block/chunk scans;
 * - packet only when oxygen or breathable-state changed.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OxygenService {
    private static int serverTickAccumulator;

    private OxygenService() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        serverTickAccumulator++;
        if (serverTickAccumulator < OxygenRules.UPDATE_INTERVAL_TICKS) {
            return;
        }
        serverTickAccumulator = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Force data initialization and first HUD sync.
            PlayerOxygenData.oxygen(player);
            PlayerOxygenData.clearLastBreathable(player);
            sync(player, OxygenEnvironment.isBreathable(player));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerOxygenData.clearLastBreathable(player);
            sync(player, OxygenEnvironment.isBreathable(player));
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerOxygenData.clearLastBreathable(player);
            sync(player, OxygenEnvironment.isBreathable(player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (event.isWasDeath()) {
            // Respawn starts with a clean respiratory reserve.
            PlayerOxygenData.reset(newPlayer);
        } else {
            PlayerOxygenData.copy(event.getOriginal(), newPlayer);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }

        boolean bypass = player.isCreative() || player.isSpectator();
        boolean breathable = bypass || OxygenEnvironment.isBreathable(player);

        int beforeOxygen = PlayerOxygenData.oxygen(player);
        int beforeEmpty = PlayerOxygenData.emptyUpdates(player);
        int max = PlayerOxygenData.maxOxygen(player);

        OxygenRules.StepResult step;
        if (bypass) {
            step = new OxygenRules.StepResult(max, 0, false);
        } else {
            step = OxygenRules.step(beforeOxygen, max, beforeEmpty, breathable);
        }

        boolean oxygenChanged = step.oxygen() != beforeOxygen || step.emptyUpdates() != beforeEmpty;
        if (oxygenChanged) {
            PlayerOxygenData.set(player, step.oxygen(), step.emptyUpdates());
        }

        boolean environmentChanged = PlayerOxygenData.updateLastBreathable(player, breathable);

        if (step.shouldDamage()) {
            player.hurt(player.damageSources().drown(), OxygenRules.SUFFOCATION_DAMAGE);
        }

        if (oxygenChanged || environmentChanged) {
            sync(player, breathable);
        }
    }

    public static void sync(ServerPlayer player, boolean breathable) {
        ModNetwork.sendTo(
                player,
                new OxygenSyncPacket(
                        PlayerOxygenData.oxygen(player),
                        PlayerOxygenData.maxOxygen(player),
                        breathable
                )
        );
        PlayerOxygenData.updateLastBreathable(player, breathable);
    }
}
