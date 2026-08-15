package com.wasted.domesurvival.forge.machine.oxygen.complex;

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

public final class OxygenComplexRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<OxygenComplexBlock> AIR_INTAKE = registerPart(
            "oxygen_complex_air_intake", OxygenComplexRole.AIR_INTAKE);
    public static final RegistryObject<OxygenComplexBlock> FILTRATION = registerPart(
            "oxygen_complex_filtration", OxygenComplexRole.FILTRATION);
    public static final RegistryObject<OxygenComplexBlock> COMPRESSION = registerPart(
            "oxygen_complex_compression", OxygenComplexRole.COMPRESSION);
    public static final RegistryObject<OxygenComplexBlock> OUTPUT = registerPart(
            "oxygen_complex_output", OxygenComplexRole.OUTPUT);

    public static final RegistryObject<BlockEntityType<OxygenComplexBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "oxygen_complex",
                    () -> BlockEntityType.Builder.of(
                            OxygenComplexBlockEntity::new,
                            AIR_INTAKE.get(), FILTRATION.get(), COMPRESSION.get(), OUTPUT.get()
                    ).build(null)
            );

    public static final RegistryObject<MenuType<OxygenComplexMenu>> MENU =
            MENUS.register("oxygen_complex", () -> IForgeMenuType.create(OxygenComplexMenu::new));

    private OxygenComplexRegistry() {
    }

    private static RegistryObject<OxygenComplexBlock> registerPart(String id, OxygenComplexRole role) {
        RegistryObject<OxygenComplexBlock> block = BLOCKS.register(
                id,
                () -> new OxygenComplexBlock(
                        role,
                        BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                                .strength(5.0F, 12.0F)
                                .noOcclusion()
                                .lightLevel(state -> state.getValue(OxygenComplexBlock.ACTIVE) ? 3 : 0)
                )
        );
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
    }
}
