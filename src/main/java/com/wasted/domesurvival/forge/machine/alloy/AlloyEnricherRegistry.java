package com.wasted.domesurvival.forge.machine.alloy;

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

public final class AlloyEnricherRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> ALLOY_ENRICHER = BLOCKS.register(
            "alloy_enricher",
            () -> new AlloyEnricherBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.5F, 9.0F))
    );

    public static final RegistryObject<Item> ALLOY_ENRICHER_ITEM = ITEMS.register(
            "alloy_enricher",
            () -> new BlockItem(ALLOY_ENRICHER.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<AlloyEnricherBlockEntity>> ALLOY_ENRICHER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "alloy_enricher",
                    () -> BlockEntityType.Builder.of(AlloyEnricherBlockEntity::new, ALLOY_ENRICHER.get()).build(null)
            );

    public static final RegistryObject<MenuType<AlloyEnricherMenu>> ALLOY_ENRICHER_MENU =
            MENUS.register("alloy_enricher", () -> IForgeMenuType.create(AlloyEnricherMenu::new));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }

    private AlloyEnricherRegistry() {
    }
}
