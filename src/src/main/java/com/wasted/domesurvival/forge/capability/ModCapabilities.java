package com.wasted.domesurvival.forge.capability;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Custom machine capabilities owned by DomeSurvival. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {
    public static final Capability<IOxygenStorage> OXYGEN =
            CapabilityManager.get(new CapabilityToken<>() {});

    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IOxygenStorage.class);
    }
}
