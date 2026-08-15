package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.core.oxygen.OxygenRules;
import com.wasted.domesurvival.core.oxygen.OxygenSource;
import com.wasted.domesurvival.core.oxygen.OxygenSupplyRules;
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
 * V3.2 performance characteristics:
 * - simulation still runs once per second, not once per tick;
 * - focused Curios "oxygen_mask" and "back" handler lookups, no vanilla inventory scan;
 * - no world block scan/chunk traversal;
 * - one NBT integer update/sec only while a tank is actively supplying oxygen;
 * - HUD packet is sent only on authoritative state changes.
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
            OxygenEquipment.migrateLegacyHeadMask(player);
            OxygenEquipment.migrateLegacyChestTank(player);
            PlayerOxygenData.oxygen(player);
            PlayerOxygenData.clearLastBreathable(player);
            syncCurrent(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerOxygenData.clearLastBreathable(player);
            syncCurrent(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerOxygenData.clearLastBreathable(player);
            syncCurrent(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (event.isWasDeath()) {
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
        boolean environmentBreathable = bypass || OxygenEnvironment.isBreathable(player);

        int reserveBefore = PlayerOxygenData.oxygen(player);
        int emptyBefore = PlayerOxygenData.emptyUpdates(player);
        int reserveMax = PlayerOxygenData.maxOxygen(player);

        OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
        int tankBefore = tank == null ? 0 : tank.oxygen();
        boolean tankReady = !bypass && OxygenEquipment.tankEquipmentReady(player, tank);

        OxygenSupplyRules.SupplyStep step;

        if (bypass) {
            step = new OxygenSupplyRules.SupplyStep(
                    reserveMax,
                    0,
                    tankBefore,
                    OxygenSource.ENVIRONMENT,
                    false,
                    false
            );
        } else {
            step = OxygenSupplyRules.step(
                    reserveBefore,
                    reserveMax,
                    emptyBefore,
                    environmentBreathable,
                    tankReady,
                    tankBefore
            );
        }

        boolean reserveChanged =
                step.reserveOxygen() != reserveBefore || step.emptyUpdates() != emptyBefore;

        if (reserveChanged) {
            PlayerOxygenData.set(
                    player,
                    step.reserveOxygen(),
                    step.emptyUpdates()
            );
        }

        boolean tankChanged = false;
        if (tank != null && step.tankConsumed() && step.tankOxygen() != tankBefore) {
            tank.setOxygen(step.tankOxygen());
            tankChanged = true;
        }

        boolean environmentChanged =
                PlayerOxygenData.updateLastBreathable(player, environmentBreathable);

        if (step.shouldDamage()) {
            player.hurt(
                    player.damageSources().generic(),
                    OxygenRules.SUFFOCATION_DAMAGE
            );
        }

        if (reserveChanged || tankChanged || environmentChanged) {
            sendSnapshot(player, environmentBreathable, step.source());
        }
    }

    /**
     * Immediate authoritative HUD/state refresh for admin test commands.
     * Normal gameplay still syncs only when state changes.
     */
    public static void forceSync(ServerPlayer player) {
        syncCurrent(player);
    }

    private static void syncCurrent(ServerPlayer player) {
        boolean bypass = player.isCreative() || player.isSpectator();
        boolean environmentBreathable = bypass || OxygenEnvironment.isBreathable(player);

        OxygenSource source = currentSource(player, environmentBreathable, bypass);
        sendSnapshot(player, environmentBreathable, source);
        PlayerOxygenData.updateLastBreathable(player, environmentBreathable);
    }

    private static OxygenSource currentSource(
            ServerPlayer player,
            boolean environmentBreathable,
            boolean bypass
    ) {
        if (bypass || environmentBreathable) {
            return OxygenSource.ENVIRONMENT;
        }

        OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
        if (OxygenEquipment.tankEquipmentReady(player, tank)
                && tank != null
                && tank.oxygen() > 0) {
            return OxygenSource.TANK;
        }

        return OxygenSource.RESERVE;
    }

    private static void sendSnapshot(
            ServerPlayer player,
            boolean environmentBreathable,
            OxygenSource source
    ) {
        int current;
        int max;

        if (source == OxygenSource.TANK) {
            OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
            if (tank != null && tank.oxygen() > 0) {
                current = tank.oxygen();
                max = tank.capacity();
            } else {
                source = OxygenSource.RESERVE;
                current = PlayerOxygenData.oxygen(player);
                max = PlayerOxygenData.maxOxygen(player);
            }
        } else {
            current = PlayerOxygenData.oxygen(player);
            max = PlayerOxygenData.maxOxygen(player);
        }

        ModNetwork.sendTo(
                player,
                new OxygenSyncPacket(
                        current,
                        max,
                        environmentBreathable,
                        source
                )
        );
    }
}
