package com.wasted.domesurvival.forge.itempipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ItemPipeRegistry {
    private static final ResourceLocation COPPER_ID = id("copper_item_pipe");
    private static final ResourceLocation STEEL_ID = id("steel_item_pipe");
    private static final ResourceLocation DESH_ID = id("desh_item_pipe");
    private static final ResourceLocation FILTER_ID = id("filtering_item_pipe");
    private static final ResourceLocation BLOCK_ENTITY_ID = id("item_pipe");
    private static final ResourceLocation CONNECTOR_MENU_ID = id("item_pipe_connector");
    private static final ResourceLocation FILTER_MENU_ID = id("filtering_item_pipe");
    private static final ResourceLocation PACKET_PARTICLE_ID = id("item_pipe_packet");

    public static final RegistryObject<ItemPipeBlock> COPPER_PIPE =
            RegistryObject.create(COPPER_ID, ForgeRegistries.BLOCKS);
    public static final RegistryObject<ItemPipeBlock> STEEL_PIPE =
            RegistryObject.create(STEEL_ID, ForgeRegistries.BLOCKS);
    public static final RegistryObject<ItemPipeBlock> DESH_PIPE =
            RegistryObject.create(DESH_ID, ForgeRegistries.BLOCKS);
    public static final RegistryObject<ItemPipeBlock> FILTERING_PIPE =
            RegistryObject.create(FILTER_ID, ForgeRegistries.BLOCKS);

    public static final RegistryObject<Item> COPPER_PIPE_ITEM =
            RegistryObject.create(COPPER_ID, ForgeRegistries.ITEMS);
    public static final RegistryObject<Item> STEEL_PIPE_ITEM =
            RegistryObject.create(STEEL_ID, ForgeRegistries.ITEMS);
    public static final RegistryObject<Item> DESH_PIPE_ITEM =
            RegistryObject.create(DESH_ID, ForgeRegistries.ITEMS);
    public static final RegistryObject<Item> FILTERING_PIPE_ITEM =
            RegistryObject.create(FILTER_ID, ForgeRegistries.ITEMS);

    public static final RegistryObject<BlockEntityType<ItemPipeBlockEntity>> BLOCK_ENTITY =
            RegistryObject.create(BLOCK_ENTITY_ID, ForgeRegistries.BLOCK_ENTITY_TYPES);

    public static final RegistryObject<MenuType<ItemConnectorMenu>> CONNECTOR_MENU =
            RegistryObject.create(CONNECTOR_MENU_ID, ForgeRegistries.MENU_TYPES);
    public static final RegistryObject<MenuType<FilteringItemPipeMenu>> FILTER_MENU =
            RegistryObject.create(FILTER_MENU_ID, ForgeRegistries.MENU_TYPES);

    public static final RegistryObject<SimpleParticleType> PACKET_PARTICLE =
            RegistryObject.create(PACKET_PARTICLE_ID, ForgeRegistries.PARTICLE_TYPES);

    private ItemPipeRegistry() { }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> {
            helper.register(COPPER_ID, create(ItemPipeTier.COPPER, false, 1.5F, 5.0F));
            helper.register(STEEL_ID, create(ItemPipeTier.STEEL, false, 2.5F, 7.0F));
            helper.register(DESH_ID, create(ItemPipeTier.DESH, false, 3.5F, 9.0F));
            helper.register(FILTER_ID, create(ItemPipeTier.FILTERING, true, 3.0F, 8.0F));
        });

        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            helper.register(COPPER_ID, new ItemPipeBlockItem(COPPER_PIPE.get(), new Item.Properties(), ItemPipeTier.COPPER, false));
            helper.register(STEEL_ID, new ItemPipeBlockItem(STEEL_PIPE.get(), new Item.Properties(), ItemPipeTier.STEEL, false));
            helper.register(DESH_ID, new ItemPipeBlockItem(DESH_PIPE.get(), new Item.Properties(), ItemPipeTier.DESH, false));
            helper.register(FILTER_ID, new ItemPipeBlockItem(FILTERING_PIPE.get(), new Item.Properties(), ItemPipeTier.FILTERING, true));
        });

        event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> helper.register(
                BLOCK_ENTITY_ID,
                BlockEntityType.Builder.of(
                        ItemPipeBlockEntity::new,
                        COPPER_PIPE.get(), STEEL_PIPE.get(), DESH_PIPE.get(), FILTERING_PIPE.get()
                ).build(null)
        ));

        event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> {
            helper.register(CONNECTOR_MENU_ID, IForgeMenuType.create(ItemConnectorMenu::new));
            helper.register(FILTER_MENU_ID, IForgeMenuType.create(FilteringItemPipeMenu::new));
        });

        event.register(ForgeRegistries.Keys.PARTICLE_TYPES, helper ->
                helper.register(PACKET_PARTICLE_ID, new SimpleParticleType(false)));
    }

    private static ItemPipeBlock create(ItemPipeTier tier, boolean filtering, float hardness, float resistance) {
        return new ItemPipeBlock(
                tier,
                filtering,
                BlockBehaviour.Properties.of()
                        .strength(hardness, resistance)
                        .sound(SoundType.METAL)
                        .noOcclusion()
        );
    }

    @SubscribeEvent
    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.REDSTONE_BLOCKS) return;
        event.accept(COPPER_PIPE_ITEM.get());
        event.accept(STEEL_PIPE_ITEM.get());
        event.accept(DESH_PIPE_ITEM.get());
        event.accept(FILTERING_PIPE_ITEM.get());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(DomeSurvival.MOD_ID, path);
    }
}
