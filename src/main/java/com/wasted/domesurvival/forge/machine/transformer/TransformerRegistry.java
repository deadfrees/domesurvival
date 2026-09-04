package com.wasted.domesurvival.forge.machine.transformer;

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

public final class TransformerRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> TRANSFORMER = BLOCKS.register(
            "transformer",
            () -> new TransformerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.5F, 9.0F))
    );

    public static final RegistryObject<Item> TRANSFORMER_ITEM = ITEMS.register(
            "transformer",
            () -> new BlockItem(TRANSFORMER.get(), new Item.Properties()) {
                @Override
                public Component getName(ItemStack stack) {
                    return Component.literal("Силовой трансформатор");
                }
            }
    );

    public static final RegistryObject<BlockEntityType<TransformerBlockEntity>> TRANSFORMER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "transformer",
                    () -> BlockEntityType.Builder.of(TransformerBlockEntity::new, TRANSFORMER.get()).build(null)
            );

    public static final RegistryObject<MenuType<TransformerMenu>> TRANSFORMER_MENU =
            MENUS.register("transformer", () -> IForgeMenuType.create(TransformerMenu::new));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }

    private TransformerRegistry() {
    }
}
