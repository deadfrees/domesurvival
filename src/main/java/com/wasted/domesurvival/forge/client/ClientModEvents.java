package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.client.screen.CreativeEnergyBufferScreen;
import com.wasted.domesurvival.forge.client.screen.AdamantiumEnergyBufferScreen;
import com.wasted.domesurvival.forge.client.screen.TitanEnergyBufferScreen;
import com.wasted.domesurvival.forge.client.screen.EnergyBufferScreen;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.model.OxygenMaskModel;
import com.wasted.domesurvival.forge.client.model.OxygenTankModel;
import com.wasted.domesurvival.forge.client.particle.AcidRainParticle;
import com.wasted.domesurvival.forge.client.particle.SandstormParticle;
import com.wasted.domesurvival.forge.client.particle.VentilationBubbleParticle;
import com.wasted.domesurvival.forge.client.render.OxygenMaskCurioRenderer;
import com.wasted.domesurvival.forge.client.render.OxygenTankCurioRenderer;
import com.wasted.domesurvival.forge.client.screen.CoalGeneratorScreen;
import com.wasted.domesurvival.forge.client.screen.ShaftFurnaceScreen;
import com.wasted.domesurvival.forge.client.screen.CokeOvenScreen;
import com.wasted.domesurvival.forge.client.screen.WaterPurifierScreen;
import com.wasted.domesurvival.forge.client.screen.OxygenElectrolyzerScreen;
import com.wasted.domesurvival.forge.client.screen.OxygenFillerScreen;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.particle.ModParticles;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.ENERGY_BUFFER_CREATIVE.get(), CreativeEnergyBufferScreen::new);
            MenuScreens.register(ModMenuTypes.ENERGY_BUFFER_ADAMANTIUM.get(), AdamantiumEnergyBufferScreen::new);
            MenuScreens.register(ModMenuTypes.ENERGY_BUFFER_TITAN.get(), TitanEnergyBufferScreen::new);
            MenuScreens.register(ModMenuTypes.ENERGY_BUFFER.get(), EnergyBufferScreen::new);
            MenuScreens.register(ModMenuTypes.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.SHAFT_FURNACE.get(), ShaftFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.COKE_OVEN.get(), CokeOvenScreen::new);
            MenuScreens.register(ModMenuTypes.WATER_PURIFIER.get(), WaterPurifierScreen::new);
            MenuScreens.register(ModMenuTypes.OXYGEN_ELECTROLYZER.get(), OxygenElectrolyzerScreen::new);
            MenuScreens.register(ModMenuTypes.OXYGEN_FILLER.get(), OxygenFillerScreen::new);
            CuriosRendererRegistry.register(
                    ModItems.OXYGEN_MASK.get(),
                    OxygenMaskCurioRenderer::new
            );
            CuriosRendererRegistry.register(
                    ModItems.SMALL_OXYGEN_TANK.get(),
                    OxygenTankCurioRenderer::new
            );
            CuriosRendererRegistry.register(
                    ModItems.MEDIUM_OXYGEN_TANK.get(),
                    OxygenTankCurioRenderer::new
            );
            CuriosRendererRegistry.register(
                    ModItems.LARGE_OXYGEN_TANK.get(),
                    OxygenTankCurioRenderer::new
            );
        });
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.AIR_LEVEL.id(),
                "oxygen",
                OxygenHudOverlay.HUD
        );
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
        event.registerLayerDefinition(
                OxygenMaskModel.LAYER_LOCATION,
                OxygenMaskModel::createBodyLayer
        );

        event.registerLayerDefinition(
                OxygenTankModel.SMALL_LAYER_LOCATION,
                OxygenTankModel::createSmallBodyLayer
        );
        event.registerLayerDefinition(
                OxygenTankModel.MEDIUM_LAYER_LOCATION,
                OxygenTankModel::createMediumBodyLayer
        );
        event.registerLayerDefinition(
                OxygenTankModel.LARGE_LAYER_LOCATION,
                OxygenTankModel::createLargeBodyLayer
        );
    }


    @SubscribeEvent
    public static void registerParticles(
            RegisterParticleProvidersEvent event
    ) {
        event.registerSpriteSet(
                ModParticles.ACID_RAIN_STREAK.get(),
                AcidRainParticle.Provider::new
        );

        event.registerSpriteSet(
                ModParticles.SANDSTORM_MOTE.get(),
                SandstormParticle.Provider::new
        );

        event.registerSpriteSet(
                ModParticles.VENTILATION_BUBBLE.get(),
                VentilationBubbleParticle.Provider::new
        );
    }
}
