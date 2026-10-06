package com.wasted.domesurvival.forge.machine.organic;

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

public final class OrganicProcessorRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> ORGANIC_PROCESSOR = BLOCKS.register(
            "organic_processor",
            () -> new OrganicProcessorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0F, 8.0F))
    );

    public static final RegistryObject<Item> ORGANIC_PROCESSOR_ITEM = ITEMS.register(
            "organic_processor",
            () -> new BlockItem(ORGANIC_PROCESSOR.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<OrganicProcessorBlockEntity>> ORGANIC_PROCESSOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "organic_processor",
                    () -> BlockEntityType.Builder.of(OrganicProcessorBlockEntity::new, ORGANIC_PROCESSOR.get()).build(null)
            );

    public static final RegistryObject<MenuType<OrganicProcessorMenu>> ORGANIC_PROCESSOR_MENU =
            MENUS.register("organic_processor", () -> IForgeMenuType.create(OrganicProcessorMenu::new));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }

    private OrganicProcessorRegistry() {
    }
}
