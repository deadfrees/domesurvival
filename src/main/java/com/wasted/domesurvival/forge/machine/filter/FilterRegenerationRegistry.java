package com.wasted.domesurvival.forge.machine.filter;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FilterRegenerationRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> FILTER_REGENERATION_STATION = BLOCKS.register(
            "filter_regeneration_station",
            () -> new FilterRegenerationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0F, 8.0F))
    );

    public static final RegistryObject<Item> FILTER_REGENERATION_STATION_ITEM = ITEMS.register(
            "filter_regeneration_station",
            () -> new BlockItem(FILTER_REGENERATION_STATION.get(), new Item.Properties()) {
                @Override
                public Component getName(ItemStack stack) {
                    return Component.literal("Станция регенерации фильтров");
                }
            }
    );

    public static final RegistryObject<BlockEntityType<FilterRegenerationBlockEntity>> FILTER_REGENERATION_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "filter_regeneration_station",
                    () -> BlockEntityType.Builder.of(
                            FilterRegenerationBlockEntity::new,
                            FILTER_REGENERATION_STATION.get()
                    ).build(null)
            );

    public static final RegistryObject<MenuType<FilterRegenerationMenu>> FILTER_REGENERATION_MENU =
            MENUS.register("filter_regeneration_station", () -> IForgeMenuType.create(FilterRegenerationMenu::new));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }

    private FilterRegenerationRegistry() {
    }
}
