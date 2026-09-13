package com.wasted.domesurvival.forge.machine.crusher;

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

public final class IndustrialCrusherRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> INDUSTRIAL_CRUSHER = BLOCKS.register(
            "industrial_crusher",
            () -> new IndustrialCrusherBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.5F, 9.0F))
    );
    public static final RegistryObject<Item> INDUSTRIAL_CRUSHER_ITEM = ITEMS.register(
            "industrial_crusher",
            () -> new BlockItem(INDUSTRIAL_CRUSHER.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockEntityType<IndustrialCrusherBlockEntity>> INDUSTRIAL_CRUSHER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("industrial_crusher",
                    () -> BlockEntityType.Builder.of(IndustrialCrusherBlockEntity::new, INDUSTRIAL_CRUSHER.get()).build(null));
    public static final RegistryObject<MenuType<IndustrialCrusherMenu>> INDUSTRIAL_CRUSHER_MENU =
            MENUS.register("industrial_crusher", () -> IForgeMenuType.create(IndustrialCrusherMenu::new));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }

    private IndustrialCrusherRegistry() {
    }
}
