package com.wasted.domesurvival.forge.machine.gasturbine;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GasTurbineGeneratorRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> GAS_TURBINE_GENERATOR = BLOCKS.register(
            "gas_turbine_generator",
            () -> new GasTurbineGeneratorBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.5F, 9.0F))
    );

    public static final RegistryObject<Item> GAS_TURBINE_GENERATOR_ITEM = ITEMS.register(
            "gas_turbine_generator",
            () -> new BlockItem(GAS_TURBINE_GENERATOR.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<GasTurbineGeneratorBlockEntity>> GAS_TURBINE_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "gas_turbine_generator",
                    () -> BlockEntityType.Builder.of(
                            GasTurbineGeneratorBlockEntity::new,
                            GAS_TURBINE_GENERATOR.get()
                    ).build(null)
            );

    public static final RegistryObject<MenuType<GasTurbineGeneratorMenu>> GAS_TURBINE_GENERATOR_MENU =
            MENUS.register(
                    "gas_turbine_generator",
                    () -> IForgeMenuType.create(GasTurbineGeneratorMenu::new)
            );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }

    private GasTurbineGeneratorRegistry() {
    }
}
