package com.wasted.domesurvival.forge.machine.forming;

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

public final class FormingPressRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> FORMING_PRESS = BLOCKS.register(
            "forming_press",
            () -> new FormingPressBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0F, 8.0F))
    );

    public static final RegistryObject<Item> FORMING_PRESS_ITEM = ITEMS.register(
            "forming_press",
            () -> new BlockItem(FORMING_PRESS.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<FormingPressBlockEntity>> FORMING_PRESS_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "forming_press",
                    () -> BlockEntityType.Builder.of(FormingPressBlockEntity::new, FORMING_PRESS.get()).build(null)
            );

    public static final RegistryObject<MenuType<FormingPressMenu>> FORMING_PRESS_MENU =
            MENUS.register("forming_press", () -> IForgeMenuType.create(FormingPressMenu::new));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }

    private FormingPressRegistry() {
    }
}
