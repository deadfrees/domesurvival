package com.wasted.domesurvival.forge.hopper;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HopperRegistryEvents {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<TieredHopperBlock> COPPER_HOPPER =
            BLOCKS.register(
                    "copper_hopper",
                    () -> new TieredHopperBlock(
                            HopperTier.COPPER,
                            BlockBehaviour.Properties.of()
                                    .strength(3.0F, 4.8F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final RegistryObject<TieredHopperBlock> STEEL_HOPPER =
            BLOCKS.register(
                    "steel_hopper",
                    () -> new TieredHopperBlock(
                            HopperTier.STEEL,
                            BlockBehaviour.Properties.of()
                                    .strength(4.0F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final RegistryObject<TieredHopperBlock> DESH_HOPPER =
            BLOCKS.register(
                    "desh_hopper",
                    () -> new TieredHopperBlock(
                            HopperTier.DESH,
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 8.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()
                    )
            );

    public static final RegistryObject<Item> COPPER_HOPPER_ITEM =
            ITEMS.register(
                    "copper_hopper",
                    () -> new TieredHopperBlockItem(
                            COPPER_HOPPER.get(),
                            new Item.Properties(),
                            HopperTier.COPPER
                    )
            );

    public static final RegistryObject<Item> STEEL_HOPPER_ITEM =
            ITEMS.register(
                    "steel_hopper",
                    () -> new TieredHopperBlockItem(
                            STEEL_HOPPER.get(),
                            new Item.Properties(),
                            HopperTier.STEEL
                    )
            );

    public static final RegistryObject<Item> DESH_HOPPER_ITEM =
            ITEMS.register(
                    "desh_hopper",
                    () -> new TieredHopperBlockItem(
                            DESH_HOPPER.get(),
                            new Item.Properties(),
                            HopperTier.DESH
                    )
            );

    public static final RegistryObject<Item> VANILLA_TO_COPPER_UPGRADE =
            ITEMS.register(
                    "hopper_upgrade_vanilla_to_copper",
                    () -> new HopperUpgradeItem(
                            HopperUpgradeItem.Path.VANILLA_TO_COPPER,
                            new Item.Properties().stacksTo(16)
                    )
            );

    public static final RegistryObject<Item> COPPER_TO_STEEL_UPGRADE =
            ITEMS.register(
                    "hopper_upgrade_copper_to_steel",
                    () -> new HopperUpgradeItem(
                            HopperUpgradeItem.Path.COPPER_TO_STEEL,
                            new Item.Properties().stacksTo(16)
                    )
            );

    public static final RegistryObject<Item> STEEL_TO_DESH_UPGRADE =
            ITEMS.register(
                    "hopper_upgrade_steel_to_desh",
                    () -> new HopperUpgradeItem(
                            HopperUpgradeItem.Path.STEEL_TO_DESH,
                            new Item.Properties().stacksTo(16)
                    )
            );

    public static final RegistryObject<BlockEntityType<TieredHopperBlockEntity>> HOPPER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "tiered_hopper",
                    () -> BlockEntityType.Builder.of(
                            TieredHopperBlockEntity::new,
                            COPPER_HOPPER.get(),
                            STEEL_HOPPER.get(),
                            DESH_HOPPER.get()
                    ).build(null)
            );

    public static final RegistryObject<MenuType<TieredHopperMenu>> TIERED_HOPPER_MENU =
            MENU_TYPES.register(
                    "tiered_hopper",
                    () -> IForgeMenuType.create(TieredHopperMenu::new)
            );

    private HopperRegistryEvents() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.REDSTONE_BLOCKS) {
            return;
        }

        event.accept(COPPER_HOPPER_ITEM.get());
        event.accept(STEEL_HOPPER_ITEM.get());
        event.accept(DESH_HOPPER_ITEM.get());

        event.accept(VANILLA_TO_COPPER_UPGRADE.get());
        event.accept(COPPER_TO_STEEL_UPGRADE.get());
        event.accept(STEEL_TO_DESH_UPGRADE.get());
    }

    public static int inventorySize(Block block) {
        if (block == COPPER_HOPPER.get()) return HopperTier.COPPER.slots();
        if (block == STEEL_HOPPER.get()) return HopperTier.STEEL.slots();
        if (block == DESH_HOPPER.get()) return HopperTier.DESH.slots();

        return HopperTier.COPPER.slots();
    }
}
