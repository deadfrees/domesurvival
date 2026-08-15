package com.wasted.domesurvival.forge.storage.tank;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Isolated V63 registry so the tank can be added without disturbing existing machine registries. */
public final class UniversalTankRegistry {
    private static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<UniversalTankBlock> UNIVERSAL_TANK = BLOCKS.register(
            "universal_tank",
            () -> new UniversalTankBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F, 1200.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> UNIVERSAL_TANK_ITEM = ITEMS.register(
            "universal_tank",
            () -> new BlockItem(UNIVERSAL_TANK.get(), new Item.Properties())
    );

    public static final RegistryObject<BlockEntityType<UniversalTankBlockEntity>> UNIVERSAL_TANK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "universal_tank",
                    () -> BlockEntityType.Builder.of(
                            UniversalTankBlockEntity::new,
                            UNIVERSAL_TANK.get()
                    ).build(null)
            );

    public static final RegistryObject<MenuType<UniversalTankMenu>> UNIVERSAL_TANK_MENU =
            MENU_TYPES.register(
                    "universal_tank",
                    () -> IForgeMenuType.create(UniversalTankMenu::new)
            );

    private UniversalTankRegistry() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
    }

}
