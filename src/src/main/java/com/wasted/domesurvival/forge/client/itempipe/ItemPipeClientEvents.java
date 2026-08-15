package com.wasted.domesurvival.forge.client.itempipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ItemPipeClientEvents {
    private ItemPipeClientEvents() { }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ItemPipeRegistry.CONNECTOR_MENU.get(), ItemConnectorScreen::new);
            MenuScreens.register(ItemPipeRegistry.FILTER_MENU.get(), FilteringItemPipeScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ItemPipeRegistry.PACKET_PARTICLE.get(), ItemPipePacketParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ItemPipeRegistry.BLOCK_ENTITY.get(), ItemPipeBlockEntityRenderer::new);
    }
}
