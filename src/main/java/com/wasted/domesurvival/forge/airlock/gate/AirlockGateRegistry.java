package com.wasted.domesurvival.forge.airlock.gate;

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
public final class AirlockGateRegistry {
    private static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<AirlockGateBlock> AIRLOCK_GATE = BLOCKS.register(
            "airlock_gate",
            () -> new AirlockGateBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(6.0F, 18.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> AIRLOCK_GATE_ITEM = ITEMS.register(
            "airlock_gate",
            () -> new BlockItem(AIRLOCK_GATE.get(), new Item.Properties())
    );


    public static final RegistryObject<BlockEntityType<AirlockGateBlockEntity>>
            AIRLOCK_GATE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "airlock_gate",
                    () -> BlockEntityType.Builder.of(
                            AirlockGateBlockEntity::new,
                            AIRLOCK_GATE.get()
                    ).build(null)
            );

    private AirlockGateRegistry() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(AIRLOCK_GATE_ITEM.get());
        }
    }
}
