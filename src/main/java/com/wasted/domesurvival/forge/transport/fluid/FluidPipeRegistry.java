package com.wasted.domesurvival.forge.transport.fluid;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FluidPipeRegistry {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<FluidPipeBlock> BASIC_FLUID_PIPE = BLOCKS.register(
            "basic_fluid_pipe",
            () -> new FluidPipeBlock(
                    FluidPipeTier.BASIC,
                    BlockBehaviour.Properties.of()
                            .strength(1.5F, 5.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<FluidPipeBlock> REINFORCED_FLUID_PIPE = BLOCKS.register(
            "reinforced_fluid_pipe",
            () -> new FluidPipeBlock(
                    FluidPipeTier.REINFORCED,
                    BlockBehaviour.Properties.of()
                            .strength(2.5F, 7.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<FluidPipeBlock> HIGH_PRESSURE_FLUID_PIPE = BLOCKS.register(
            "high_pressure_fluid_pipe",
            () -> new FluidPipeBlock(
                    FluidPipeTier.HIGH_PRESSURE,
                    BlockBehaviour.Properties.of()
                            .strength(3.5F, 10.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> BASIC_FLUID_PIPE_ITEM = ITEMS.register(
            "basic_fluid_pipe",
            () -> new FluidPipeBlockItem(
                    BASIC_FLUID_PIPE.get(),
                    new Item.Properties(),
                    FluidPipeTier.BASIC.transferPerTick()
            )
    );

    public static final RegistryObject<Item> REINFORCED_FLUID_PIPE_ITEM = ITEMS.register(
            "reinforced_fluid_pipe",
            () -> new FluidPipeBlockItem(
                    REINFORCED_FLUID_PIPE.get(),
                    new Item.Properties(),
                    FluidPipeTier.REINFORCED.transferPerTick()
            )
    );

    public static final RegistryObject<Item> HIGH_PRESSURE_FLUID_PIPE_ITEM = ITEMS.register(
            "high_pressure_fluid_pipe",
            () -> new FluidPipeBlockItem(
                    HIGH_PRESSURE_FLUID_PIPE.get(),
                    new Item.Properties(),
                    FluidPipeTier.HIGH_PRESSURE.transferPerTick()
            )
    );

    public static final RegistryObject<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                            FluidPipeBlockEntity::new,
                            BASIC_FLUID_PIPE.get(),
                            REINFORCED_FLUID_PIPE.get(),
                            HIGH_PRESSURE_FLUID_PIPE.get()
                    ).build(null)
            );

    private FluidPipeRegistry() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.REDSTONE_BLOCKS) return;

        event.accept(BASIC_FLUID_PIPE_ITEM.get());
        event.accept(REINFORCED_FLUID_PIPE_ITEM.get());
        event.accept(HIGH_PRESSURE_FLUID_PIPE_ITEM.get());
    }
}
