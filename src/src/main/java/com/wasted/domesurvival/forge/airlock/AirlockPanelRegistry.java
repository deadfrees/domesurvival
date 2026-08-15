package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AirlockPanelRegistry {
    private static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<AirlockControlPanelBlock> AIRLOCK_CONTROL_PANEL = BLOCKS.register(
            "airlock_control_panel",
            () -> new AirlockControlPanelBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 8.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> AIRLOCK_CONTROL_PANEL_ITEM = ITEMS.register(
            "airlock_control_panel",
            () -> new BlockItem(AIRLOCK_CONTROL_PANEL.get(), new Item.Properties())
    );

    public static final RegistryObject<AirlockBindingKeyItem> AIRLOCK_BINDING_KEY = ITEMS.register(
            "airlock_binding_key",
            () -> new AirlockBindingKeyItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<BlockEntityType<AirlockControlPanelBlockEntity>>
            AIRLOCK_CONTROL_PANEL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "airlock_control_panel",
                    () -> BlockEntityType.Builder.of(
                            AirlockControlPanelBlockEntity::new,
                            AIRLOCK_CONTROL_PANEL.get()
                    ).build(null)
            );

    private AirlockPanelRegistry() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(AIRLOCK_CONTROL_PANEL_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(AIRLOCK_BINDING_KEY.get());
        }
    }

    public static net.minecraft.world.level.block.Block block() {
        return AIRLOCK_CONTROL_PANEL.get();
    }
}
