package com.wasted.domesurvival.forge.transport.fluid;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidPipeClientEvents {
    private FluidPipeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(FluidPipeRegistry.BASIC_FLUID_PIPE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(FluidPipeRegistry.REINFORCED_FLUID_PIPE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(FluidPipeRegistry.HIGH_PRESSURE_FLUID_PIPE.get(), RenderType.cutout());
        });
    }
}
