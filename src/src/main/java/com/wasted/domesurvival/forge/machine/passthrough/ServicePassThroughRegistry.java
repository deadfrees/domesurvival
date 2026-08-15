package com.wasted.domesurvival.forge.machine.passthrough;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ServicePassThroughRegistry {
    public static final ResourceLocation ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "service_pass_through");

    public static final RegistryObject<ServicePassThroughBlock> BLOCK =
            RegistryObject.create(ID, ForgeRegistries.BLOCKS);

    public static final RegistryObject<Item> ITEM =
            RegistryObject.create(ID, ForgeRegistries.ITEMS);

    public static final RegistryObject<BlockEntityType<ServicePassThroughBlockEntity>> BLOCK_ENTITY =
            RegistryObject.create(ID, ForgeRegistries.BLOCK_ENTITY_TYPES);

    private ServicePassThroughRegistry() {
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ITEM.get());
        }
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> helper.register(
                ID,
                new ServicePassThroughBlock(
                        BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                                .strength(4.0F, 8.0F)
                                .sound(SoundType.METAL)
                                .noOcclusion()
                )
        ));

        event.register(ForgeRegistries.Keys.ITEMS, helper -> helper.register(
                ID,
                new BlockItem(BLOCK.get(), new Item.Properties())
        ));

        event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> helper.register(
                ID,
                BlockEntityType.Builder.of(
                        ServicePassThroughBlockEntity::new,
                        BLOCK.get()
                ).build(null)
        ));
    }
}
