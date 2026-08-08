package com.wasted.domesurvival.forge.block;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.airlock.AirlockPanelBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> REINFORCED_GLASS = BLOCKS.register("reinforced_glass",
            () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .strength(50.0F, 1200.0F)
                    .noOcclusion()));

    public static final RegistryObject<Block> DOME_FRAME = BLOCKS.register("dome_frame",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(20.0F, 1200.0F)));

    public static final RegistryObject<Block> DOME_FOUNDATION = BLOCKS.register("dome_foundation",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SMOOTH_STONE)
                    .strength(30.0F, 1200.0F)));


    public static final RegistryObject<Block> AIRLOCK_DOOR = BLOCKS.register("airlock_door",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(35.0F, 1200.0F)));

    public static final RegistryObject<Block> AIRLOCK_PANEL = BLOCKS.register("airlock_panel",
            () -> new AirlockPanelBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(15.0F, 1200.0F)));

    static {
        ITEMS.register("reinforced_glass", () -> new BlockItem(REINFORCED_GLASS.get(), new Item.Properties()));
        ITEMS.register("dome_frame", () -> new BlockItem(DOME_FRAME.get(), new Item.Properties()));
        ITEMS.register("dome_foundation", () -> new BlockItem(DOME_FOUNDATION.get(), new Item.Properties()));
        ITEMS.register("airlock_door", () -> new BlockItem(AIRLOCK_DOOR.get(), new Item.Properties()));
        ITEMS.register("airlock_panel", () -> new BlockItem(AIRLOCK_PANEL.get(), new Item.Properties()));
    }

    private ModBlocks() {
    }
}
