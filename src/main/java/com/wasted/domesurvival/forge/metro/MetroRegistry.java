package com.wasted.domesurvival.forge.metro;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.metro.worldgen.AbandonedMetroStructure;
import com.wasted.domesurvival.forge.metro.worldgen.AbandonedMetroStructurePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MetroRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DomeSurvival.MOD_ID);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, DomeSurvival.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> METRO_RESTORATION_CONSOLE = BLOCKS.register(
            "metro_restoration_console",
            () -> new MetroRestorationConsoleBlock(
                    BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_TILES)
                            .strength(5.0F, 10.0F)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Block> METRO_TRAIN_CONSOLE = BLOCKS.register(
            "metro_train_console",
            () -> new MetroTrainConsoleBlock(
                    BlockBehaviour.Properties.copy(Blocks.POLISHED_ANDESITE)
                            .strength(5.0F, 10.0F)
            )
    );

    public static final RegistryObject<BlockEntityType<MetroRestorationConsoleBlockEntity>> METRO_RESTORATION_CONSOLE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "metro_restoration_console",
                    () -> BlockEntityType.Builder.of(
                            MetroRestorationConsoleBlockEntity::new,
                            METRO_RESTORATION_CONSOLE.get()
                    ).build(null)
            );

    public static final RegistryObject<MenuType<MetroRestorationConsoleMenu>> METRO_RESTORATION_CONSOLE_MENU =
            MENUS.register(
                    "metro_restoration_console",
                    () -> IForgeMenuType.create(MetroRestorationConsoleMenu::new)
            );

    public static final RegistryObject<StructureType<AbandonedMetroStructure>> ABANDONED_METRO_STRUCTURE =
            STRUCTURE_TYPES.register(
                    "abandoned_metro",
                    () -> () -> AbandonedMetroStructure.CODEC
            );

    public static final RegistryObject<StructurePieceType> ABANDONED_METRO_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "abandoned_metro",
                    () -> AbandonedMetroStructurePiece::new
            );

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECE_TYPES.register(modBus);
    }

    private MetroRegistry() {
    }
}
