package com.wasted.domesurvival.forge.machine.solar;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SolarPanelRegistry {
    public static final ResourceLocation SOLAR_PANEL_MK1_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "solar_panel_mk1");
    public static final ResourceLocation SOLAR_PANEL_MK2_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "solar_panel_mk2");
    public static final ResourceLocation SOLAR_PANEL_MK3_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "solar_panel_mk3");
    public static final ResourceLocation SOLAR_PANEL_BLOCK_ENTITY_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "solar_panel");
    public static final ResourceLocation SOLAR_PANEL_MENU_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "solar_panel");

    public static final RegistryObject<Block> SOLAR_PANEL_MK1 =
            RegistryObject.create(SOLAR_PANEL_MK1_ID, ForgeRegistries.BLOCKS);
    public static final RegistryObject<Block> SOLAR_PANEL_MK2 =
            RegistryObject.create(SOLAR_PANEL_MK2_ID, ForgeRegistries.BLOCKS);
    public static final RegistryObject<Block> SOLAR_PANEL_MK3 =
            RegistryObject.create(SOLAR_PANEL_MK3_ID, ForgeRegistries.BLOCKS);

    public static final RegistryObject<Item> SOLAR_PANEL_MK1_ITEM =
            RegistryObject.create(SOLAR_PANEL_MK1_ID, ForgeRegistries.ITEMS);
    public static final RegistryObject<Item> SOLAR_PANEL_MK2_ITEM =
            RegistryObject.create(SOLAR_PANEL_MK2_ID, ForgeRegistries.ITEMS);
    public static final RegistryObject<Item> SOLAR_PANEL_MK3_ITEM =
            RegistryObject.create(SOLAR_PANEL_MK3_ID, ForgeRegistries.ITEMS);

    private static final RegistryObject<BlockEntityType<?>> SOLAR_PANEL_BLOCK_ENTITY =
            RegistryObject.create(SOLAR_PANEL_BLOCK_ENTITY_ID, ForgeRegistries.BLOCK_ENTITY_TYPES);
    private static final RegistryObject<MenuType<?>> SOLAR_PANEL_MENU =
            RegistryObject.create(SOLAR_PANEL_MENU_ID, ForgeRegistries.MENU_TYPES);

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> {
            helper.register(
                    SOLAR_PANEL_MK1_ID,
                    new SolarPanelBlock(panelProperties(), SolarPanelTier.MK1)
            );
            helper.register(
                    SOLAR_PANEL_MK2_ID,
                    new SolarPanelBlock(panelProperties(), SolarPanelTier.MK2)
            );
            helper.register(
                    SOLAR_PANEL_MK3_ID,
                    new SolarPanelBlock(panelProperties(), SolarPanelTier.MK3)
            );
        });

        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            helper.register(SOLAR_PANEL_MK1_ID, new BlockItem(SOLAR_PANEL_MK1.get(), new Item.Properties()));
            helper.register(SOLAR_PANEL_MK2_ID, new BlockItem(SOLAR_PANEL_MK2.get(), new Item.Properties()));
            helper.register(SOLAR_PANEL_MK3_ID, new BlockItem(SOLAR_PANEL_MK3.get(), new Item.Properties()));
        });

        event.register(
                ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,
                SOLAR_PANEL_BLOCK_ENTITY_ID,
                () -> BlockEntityType.Builder.of(
                        SolarPanelBlockEntity::new,
                        SOLAR_PANEL_MK1.get(),
                        SOLAR_PANEL_MK2.get(),
                        SOLAR_PANEL_MK3.get()
                ).build(null)
        );

        event.register(
                ForgeRegistries.Keys.MENU_TYPES,
                SOLAR_PANEL_MENU_ID,
                () -> IForgeMenuType.create(SolarPanelMenu::new)
        );
    }

    private static BlockBehaviour.Properties panelProperties() {
        return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .strength(3.5F, 8.0F)
                .noOcclusion();
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<SolarPanelBlockEntity> blockEntityType() {
        return (BlockEntityType<SolarPanelBlockEntity>) SOLAR_PANEL_BLOCK_ENTITY.get();
    }

    @SuppressWarnings("unchecked")
    public static MenuType<SolarPanelMenu> menuType() {
        return (MenuType<SolarPanelMenu>) SOLAR_PANEL_MENU.get();
    }

    private SolarPanelRegistry() {
    }
}
