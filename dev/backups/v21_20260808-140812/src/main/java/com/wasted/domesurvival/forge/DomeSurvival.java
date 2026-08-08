package com.wasted.domesurvival.forge;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.command.DomeCommands;
import com.wasted.domesurvival.forge.dome.DomeGenerationService;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DomeSurvival.MOD_ID)
public final class DomeSurvival {
    public static final String MOD_ID = "domesurvival";

    public DomeSurvival() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.register(DomeGenerationService.class);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        DomeCommands.register(event.getDispatcher());
    }
}
