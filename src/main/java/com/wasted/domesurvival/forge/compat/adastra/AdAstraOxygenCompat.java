package com.wasted.domesurvival.forge.compat.adastra;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.oxygen.OxygenEquipment;
import com.wasted.domesurvival.forge.oxygen.PlayerOxygenData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Optional one-way oxygen bridge for Ad Astra 1.20.1.
 *
 * DomeSurvival remains authoritative for oxygen storage, HUD bubbles and consumption.
 * Ad Astra must not start its no-oxygen state while DomeSurvival still has breathable
 * reserve bubbles, even after a portable tank has just run empty.
 *
 * No hard dependency on Ad Astra is introduced.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AdAstraOxygenCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String AD_ASTRA_MOD_ID = "ad_astra";
    private static final String ENTITY_OXYGEN_EVENT =
            "earth.terrarium.adastra.api.events.AdAstraEvents$EntityOxygenEvent";

    private static boolean registered;

    private AdAstraOxygenCompat() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(AD_ASTRA_MOD_ID)) {
            return;
        }
        event.enqueueWork(AdAstraOxygenCompat::registerBridge);
    }

    private static synchronized void registerBridge() {
        if (registered) {
            return;
        }

        try {
            Class<?> listenerType = Class.forName(ENTITY_OXYGEN_EVENT);
            Object listener = Proxy.newProxyInstance(
                    listenerType.getClassLoader(),
                    new Class<?>[]{listenerType},
                    (proxy, method, args) -> handleInvocation(proxy, method, args)
            );

            Method register = listenerType.getMethod("register", listenerType);
            register.invoke(null, listener);
            registered = true;

            String version = ModList.get().getModContainerById(AD_ASTRA_MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("unknown");
            LOGGER.info("DomeSurvival Ad Astra oxygen bridge V26 enabled for Ad Astra {}.", version);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn(
                    "Ad Astra is installed, but DomeSurvival could not register the optional oxygen bridge. " +
                            "DomeSurvival will continue without Ad Astra oxygen compatibility.",
                    exception
            );
        }
    }

    private static Object handleInvocation(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "DomeSurvivalAdAstraOxygenListenerV26";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> args != null && args.length == 1 && proxy == args[0];
                default -> null;
            };
        }

        if (!"hasOxygen".equals(method.getName()) || args == null || args.length != 2) {
            return booleanArgument(args, 1, false);
        }

        boolean adAstraAlreadyHasOxygen = booleanArgument(args, 1, false);
        if (adAstraAlreadyHasOxygen) {
            return true;
        }

        if (!(args[0] instanceof Entity entity) || !(entity instanceof ServerPlayer player)) {
            return false;
        }

        return domeSurvivalHasBreathableOxygen(player);
    }

    /**
     * Important order:
     * 1. The player's DomeSurvival reserve is the same reserve represented by the HUD bubbles.
     *    While at least one bubble/unit remains, Ad Astra must not consider the player oxygenless.
     * 2. A working mask+tank also counts as oxygen while the tank contains O2.
     * 3. Only when both are empty/unavailable does Ad Astra receive false.
     *
     * This keeps the original DomeSurvival rule: oxygen damage can only begin after the
     * DomeSurvival oxygen reserve reaches zero.
     */
    private static boolean domeSurvivalHasBreathableOxygen(ServerPlayer player) {
        if (PlayerOxygenData.oxygen(player) > 0) {
            return true;
        }

        OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
        return tank != null
                && tank.oxygen() > 0
                && OxygenEquipment.tankEquipmentReady(player, tank);
    }

    private static boolean booleanArgument(Object[] args, int index, boolean fallback) {
        if (args == null || index < 0 || index >= args.length || !(args[index] instanceof Boolean value)) {
            return fallback;
        }
        return value;
    }
}
