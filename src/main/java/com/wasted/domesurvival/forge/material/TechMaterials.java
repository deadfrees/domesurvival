package com.wasted.domesurvival.forge.material;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * DomeSurvival's native technology-material branch, migrated from the former GOTEICRAFT project.
 * Common metals deliberately keep conventional registry IDs so Forge material tags can bridge
 * them with Thermal/Mekanism/other compatible mods without hard dependencies.
 */
public final class TechMaterials {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DomeSurvival.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);

    public static final RegistryObject<Block> TIN_ORE = ore("tin_ore");
    public static final RegistryObject<Block> DEEPSLATE_TIN_ORE = deepslateOre("deepslate_tin_ore");
    public static final RegistryObject<Block> TIN_BLOCK = storageBlock("tin_block");
    public static final RegistryObject<Item> RAW_TIN = item("raw_tin");
    public static final RegistryObject<Item> TIN_INGOT = item("tin_ingot");
    public static final RegistryObject<Item> TIN_NUGGET = item("tin_nugget");

    public static final RegistryObject<Block> LEAD_ORE = ore("lead_ore");
    public static final RegistryObject<Block> DEEPSLATE_LEAD_ORE = deepslateOre("deepslate_lead_ore");
    public static final RegistryObject<Block> LEAD_BLOCK = storageBlock("lead_block");
    public static final RegistryObject<Item> RAW_LEAD = item("raw_lead");
    public static final RegistryObject<Item> LEAD_INGOT = item("lead_ingot");
    public static final RegistryObject<Item> LEAD_NUGGET = item("lead_nugget");

    public static final RegistryObject<Block> SILVER_ORE = ore("silver_ore");
    public static final RegistryObject<Block> DEEPSLATE_SILVER_ORE = deepslateOre("deepslate_silver_ore");
    public static final RegistryObject<Block> SILVER_BLOCK = storageBlock("silver_block");
    public static final RegistryObject<Item> RAW_SILVER = item("raw_silver");
    public static final RegistryObject<Item> SILVER_INGOT = item("silver_ingot");
    public static final RegistryObject<Item> SILVER_NUGGET = item("silver_nugget");

    public static final RegistryObject<Block> NICKEL_ORE = ore("nickel_ore");
    public static final RegistryObject<Block> DEEPSLATE_NICKEL_ORE = deepslateOre("deepslate_nickel_ore");
    public static final RegistryObject<Block> NICKEL_BLOCK = storageBlock("nickel_block");
    public static final RegistryObject<Item> RAW_NICKEL = item("raw_nickel");
    public static final RegistryObject<Item> NICKEL_INGOT = item("nickel_ingot");
    public static final RegistryObject<Item> NICKEL_NUGGET = item("nickel_nugget");

    public static final RegistryObject<Block> GOTEIUM_ORE = ore("goteium_ore");
    public static final RegistryObject<Block> DEEPSLATE_GOTEIUM_ORE = deepslateOre("deepslate_goteium_ore");
    public static final RegistryObject<Block> GOTEIUM_BLOCK = storageBlock("goteium_block");
    public static final RegistryObject<Item> RAW_GOTEIUM = item("raw_goteium");
    public static final RegistryObject<Item> GOTEIUM_INGOT = item("goteium_ingot");
    public static final RegistryObject<Item> GOTEIUM_NUGGET = item("goteium_nugget");

    public static final RegistryObject<Block> VOLTARIUM_ORE = ore("voltarium_ore");
    public static final RegistryObject<Block> DEEPSLATE_VOLTARIUM_ORE = deepslateOre("deepslate_voltarium_ore");
    public static final RegistryObject<Block> VOLTARIUM_BLOCK = storageBlock("voltarium_block");
    public static final RegistryObject<Item> RAW_VOLTARIUM = item("raw_voltarium");
    public static final RegistryObject<Item> VOLTARIUM_INGOT = item("voltarium_ingot");
    public static final RegistryObject<Item> VOLTARIUM_NUGGET = item("voltarium_nugget");

    public static final RegistryObject<Block> SOLARITE_ORE = ore("solarite_ore");
    public static final RegistryObject<Block> DEEPSLATE_SOLARITE_ORE = deepslateOre("deepslate_solarite_ore");
    public static final RegistryObject<Block> SOLARITE_BLOCK = storageBlock("solarite_block");
    public static final RegistryObject<Item> SOLARITE_SHARD = item("solarite_shard");
    public static final RegistryObject<Item> SOLARITE_CRYSTAL = item("solarite_crystal");

    /** First Forming Press products migrated from GOTEICRAFT. */
    public static final RegistryObject<Item> COPPER_PLATE = item("copper_plate");
    public static final RegistryObject<Item> TIN_PLATE = item("tin_plate");

    static {
        blockItem("tin_ore", TIN_ORE);
        blockItem("deepslate_tin_ore", DEEPSLATE_TIN_ORE);
        blockItem("tin_block", TIN_BLOCK);
        blockItem("lead_ore", LEAD_ORE);
        blockItem("deepslate_lead_ore", DEEPSLATE_LEAD_ORE);
        blockItem("lead_block", LEAD_BLOCK);
        blockItem("silver_ore", SILVER_ORE);
        blockItem("deepslate_silver_ore", DEEPSLATE_SILVER_ORE);
        blockItem("silver_block", SILVER_BLOCK);
        blockItem("nickel_ore", NICKEL_ORE);
        blockItem("deepslate_nickel_ore", DEEPSLATE_NICKEL_ORE);
        blockItem("nickel_block", NICKEL_BLOCK);
        blockItem("goteium_ore", GOTEIUM_ORE);
        blockItem("deepslate_goteium_ore", DEEPSLATE_GOTEIUM_ORE);
        blockItem("goteium_block", GOTEIUM_BLOCK);
        blockItem("voltarium_ore", VOLTARIUM_ORE);
        blockItem("deepslate_voltarium_ore", DEEPSLATE_VOLTARIUM_ORE);
        blockItem("voltarium_block", VOLTARIUM_BLOCK);
        blockItem("solarite_ore", SOLARITE_ORE);
        blockItem("deepslate_solarite_ore", DEEPSLATE_SOLARITE_ORE);
        blockItem("solarite_block", SOLARITE_BLOCK);
    }

    private static RegistryObject<Block> ore(String id) {
        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)));
    }

    private static RegistryObject<Block> deepslateOre(String id) {
        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE)));
    }

    private static RegistryObject<Block> storageBlock(String id) {
        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    }

    private static RegistryObject<Item> item(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(64)));
    }

    private static void blockItem(String id, RegistryObject<? extends Block> block) {
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private TechMaterials() {
    }
}
