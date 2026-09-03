package com.wasted.domesurvival.forge;

import com.wasted.domesurvival.forge.airlock.gate.AirlockGateRegistry;
import com.wasted.domesurvival.forge.airlock.AirlockPanelRegistry;
import com.wasted.domesurvival.forge.transport.fluid.FluidPipeRegistry;
import com.wasted.domesurvival.forge.hopper.HopperRegistryEvents;
import com.wasted.domesurvival.forge.airlock.AirlockService;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.command.DomeCommands;
import com.wasted.domesurvival.forge.compat.lostcities.LostCitiesBuildingCompat;
import com.wasted.domesurvival.forge.config.SurfaceHazardConfig;
import com.wasted.domesurvival.forge.dome.DomeGenerationService;
import com.wasted.domesurvival.forge.enchantment.ModEnchantments;
import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.loot.ModLootModifiers;
import com.wasted.domesurvival.forge.material.TechMaterials;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexRegistry;
import com.wasted.domesurvival.forge.network.ModNetwork;
import com.wasted.domesurvival.forge.particle.ModParticles;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import com.wasted.domesurvival.forge.registry.ModEntityTypes;
import com.wasted.domesurvival.forge.registry.ModPaintingVariants;
import com.wasted.domesurvival.forge.sound.ModSounds;
import com.wasted.domesurvival.forge.storage.tank.UniversalTankRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DomeSurvival.MOD_ID)
public final class DomeSurvival {
    public static final String MOD_ID = "domesurvival";

    public DomeSurvival() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        AirlockPanelRegistry.register(modBus);
        AirlockGateRegistry.register(modBus);
        FluidPipeRegistry.register(modBus);
        HopperRegistryEvents.register(modBus);
        UniversalTankRegistry.register(modBus);
        OxygenComplexRegistry.register(modBus);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModItems.ITEMS.register(modBus);
        TechMaterials.BLOCKS.register(modBus);
        TechMaterials.ITEMS.register(modBus);
        ModEnchantments.ENCHANTMENTS.register(modBus);
        ModLootModifiers.SERIALIZERS.register(modBus);
        ModEntityTypes.ENTITY_TYPES.register(modBus);
        ModPaintingVariants.PAINTING_VARIANTS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModParticles.PARTICLE_TYPES.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModMenuTypes.MENU_TYPES.register(modBus);
        modBus.addListener(LostCitiesBuildingCompat::enqueueImc);

        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SurfaceHazardConfig.SPEC);

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.register(DomeGenerationService.class);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        DomeCommands.register(event.getDispatcher());
    }

    private void onServerStarted(ServerStartedEvent event) {
        AirlockService.syncVisuals(event.getServer().overworld());
    }
}
