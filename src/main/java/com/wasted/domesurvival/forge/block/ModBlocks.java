package com.wasted.domesurvival.forge.block;

import net.minecraft.world.level.block.SoundType;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeTier;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlockItem;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlock;
import cofh.thermal.core.common.item.WrenchItem;
import com.wasted.domesurvival.forge.machine.energy.CreativeEnergyBufferBlock;
import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlock;
import com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferBlock;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlock;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockItem;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.airlock.AirlockPanelBlock;
import com.wasted.domesurvival.forge.machine.coal.CoalGeneratorBlock;
import com.wasted.domesurvival.forge.machine.water.WaterPurifierBlock;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenElectrolyzerBlock;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerBlock;
import com.wasted.domesurvival.forge.machine.bio.BioincubatorBlock;
import com.wasted.domesurvival.forge.machine.sieve.SandSieveBlock;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeBlock;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeTier;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceBlock;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnacePartBlock;
import com.wasted.domesurvival.forge.machine.shaft.CokeOvenBlock;
import com.wasted.domesurvival.forge.machine.shaft.CokeOvenPartBlock;
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


    /**
     * Universal passive machine chassis used as the structural base for DomeSurvival machines.
     * No BlockEntity or ticking is required: this is intentionally a cheap-to-render crafting component.
     */
    public static final RegistryObject<Block> COPPER_FURNACE = BLOCKS.register("copper_furnace",
            () -> new CopperFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK)
                    .strength(3.0F, 6.0F)
                    .noOcclusion()));

    public static final RegistryObject<Block> SHAFT_FURNACE = BLOCKS.register("shaft_furnace",
            () -> new ShaftFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.BLAST_FURNACE)
                    .strength(4.0F, 8.0F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(ShaftFurnaceBlock.LIT) ? 10 : 0)));

    public static final RegistryObject<Block> SHAFT_FURNACE_PART = BLOCKS.register("shaft_furnace_part",
            () -> new ShaftFurnacePartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0F, 8.0F)
                    .noOcclusion()));

    public static final RegistryObject<Block> COKE_OVEN = BLOCKS.register("coke_oven",
            () -> new CokeOvenBlock(BlockBehaviour.Properties.copy(Blocks.BRICKS)
                    .strength(3.5F, 8.0F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(CokeOvenBlock.LIT) ? 8 : 0)));

    public static final RegistryObject<Block> COKE_OVEN_PART = BLOCKS.register("coke_oven_part",
            () -> new CokeOvenPartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 8.0F)
                    .noOcclusion()));

    public static final RegistryObject<Block> MACHINE_STABILIZER = BLOCKS.register("machine_stabilizer",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.5F, 10.0F)
                    .noOcclusion()));

    public static final RegistryObject<Block> COAL_GENERATOR = BLOCKS.register("coal_generator",
            () -> new CoalGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(CoalGeneratorBlock.LIT) ? 8 : 0)));

    public static final RegistryObject<Block> WATER_PURIFIER = BLOCKS.register("water_purifier",
            () -> new WaterPurifierBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(WaterPurifierBlock.LIT) ? 5 : 0)));

    public static final RegistryObject<Block> OXYGEN_ELECTROLYZER = BLOCKS.register("oxygen_electrolyzer",
            () -> new OxygenElectrolyzerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(OxygenElectrolyzerBlock.LIT) ? 6 : 0)));

    public static final RegistryObject<Block> OXYGEN_FILLER = BLOCKS.register("oxygen_filler",
            () -> new OxygenFillerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(OxygenFillerBlock.LIT) ? 4 : 0)));

    public static final RegistryObject<Block> BIOINCUBATOR = BLOCKS.register("bioincubator",
            () -> new BioincubatorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0F, 8.0F)
                    .lightLevel(state -> state.getValue(BioincubatorBlock.LIT) ? 5 : 0)));
    public static final RegistryObject<Block> SAND_SIEVE = BLOCKS.register("sand_sieve",
            () -> new SandSieveBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_TILES)
                    .strength(2.5F, 6.0F)
                    .noOcclusion()));
    public static final RegistryObject<Block> OXYGEN_PIPE = BLOCKS.register("oxygen_pipe",
            () -> new OxygenPipeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(1.5F, 4.0F)
                    .noOcclusion(), OxygenPipeTier.BASIC));

    public static final RegistryObject<Block> REINFORCED_OXYGEN_PIPE = BLOCKS.register("reinforced_oxygen_pipe",
            () -> new OxygenPipeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2.5F, 6.0F)
                    .noOcclusion(), OxygenPipeTier.REINFORCED));

    public static final RegistryObject<Block> HIGH_FLOW_OXYGEN_PIPE = BLOCKS.register("high_flow_oxygen_pipe",
            () -> new OxygenPipeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 8.0F)
                    .noOcclusion(), OxygenPipeTier.HIGH_FLOW));

    public static final RegistryObject<Block> ENERGY_BUFFER = BLOCKS.register("energy_buffer",
            () -> new EnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0F, 8.0F)));

    public static final RegistryObject<Block> ENERGY_BUFFER_TITAN = BLOCKS.register("energy_buffer_titan",
            () -> new TitanEnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.5F, 10.0F)));

    public static final RegistryObject<Block> ENERGY_BUFFER_ADAMANTIUM = BLOCKS.register("energy_buffer_adamantium",
            () -> new AdamantiumEnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 12.0F)));

    public static final RegistryObject<Block> ENERGY_BUFFER_CREATIVE = BLOCKS.register("energy_buffer_creative",
            () -> new CreativeEnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 12.0F)));

    public static final RegistryObject<Block> BASIC_ENERGY_PIPE = BLOCKS.register("basic_energy_pipe",
            () -> new EnergyPipeBlock(EnergyPipeTier.BASIC,
                    BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    public static final RegistryObject<Block> REINFORCED_ENERGY_PIPE = BLOCKS.register("reinforced_energy_pipe",
            () -> new EnergyPipeBlock(EnergyPipeTier.REINFORCED,
                    BlockBehaviour.Properties.of()
                            .strength(2.5F, 8.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    public static final RegistryObject<Block> HIGH_VOLTAGE_ENERGY_PIPE = BLOCKS.register("high_voltage_energy_pipe",
            () -> new EnergyPipeBlock(EnergyPipeTier.HIGH_VOLTAGE,
                    BlockBehaviour.Properties.of()
                            .strength(3.5F, 10.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()));

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


    /*
     * Legacy block IDs are kept only so older saves can still resolve them.
     * Their BlockItems are intentionally not registered anymore.
     * Current gameplay uses airlock_gate + airlock_control_panel.
     */
    public static final RegistryObject<Block> AIRLOCK_DOOR = BLOCKS.register("airlock_door",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(35.0F, 1200.0F)));

    public static final RegistryObject<Block> AIRLOCK_PANEL = BLOCKS.register("airlock_panel",
            () -> new AirlockPanelBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(15.0F, 1200.0F)));


    public static final RegistryObject<Block> LANOS_DECORATIVE = BLOCKS.register("lanos_decorative",
            () -> new DecorativeLanosBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2.0F, 6.0F)
                    .noOcclusion()));

    public static final RegistryObject<Block> LANOS_ABANDONED = BLOCKS.register("lanos_abandoned",
            () -> new DecorativeLanosBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(2.0F, 6.0F)
                    .noOcclusion()));

    static {
        ITEMS.register("copper_furnace", () -> new BlockItem(COPPER_FURNACE.get(), new Item.Properties()));
        ITEMS.register("shaft_furnace", () -> new BlockItem(SHAFT_FURNACE.get(), new Item.Properties()));
        ITEMS.register("coke_oven", () -> new BlockItem(COKE_OVEN.get(), new Item.Properties()));
        ITEMS.register("machine_stabilizer", () -> new BlockItem(MACHINE_STABILIZER.get(), new Item.Properties()));
        ITEMS.register("coal_generator", () -> new BlockItem(COAL_GENERATOR.get(), new Item.Properties()));
        ITEMS.register("water_purifier", () -> new BlockItem(WATER_PURIFIER.get(), new Item.Properties()));
        ITEMS.register("oxygen_electrolyzer", () -> new BlockItem(OXYGEN_ELECTROLYZER.get(), new Item.Properties()));
        ITEMS.register("oxygen_filler", () -> new BlockItem(OXYGEN_FILLER.get(), new Item.Properties()));
        ITEMS.register("bioincubator", () -> new BlockItem(BIOINCUBATOR.get(), new Item.Properties()));
        // Keeps the former hand-sieve registry id so existing inventories migrate
        // naturally to the new placeable machine.
        ITEMS.register("sand_sieve", () -> new BlockItem(SAND_SIEVE.get(), new Item.Properties()));
        ITEMS.register("oxygen_pipe", () -> new BlockItem(OXYGEN_PIPE.get(), new Item.Properties()));
        ITEMS.register("reinforced_oxygen_pipe", () -> new BlockItem(REINFORCED_OXYGEN_PIPE.get(), new Item.Properties()));
        ITEMS.register("high_flow_oxygen_pipe", () -> new BlockItem(HIGH_FLOW_OXYGEN_PIPE.get(), new Item.Properties()));
        ITEMS.register("energy_buffer", () -> new EnergyBufferBlockItem(ENERGY_BUFFER.get(), new Item.Properties()));
        ITEMS.register("energy_buffer_titan", () -> new EnergyBufferBlockItem(ENERGY_BUFFER_TITAN.get(), new Item.Properties()));
        ITEMS.register("energy_buffer_adamantium", () -> new EnergyBufferBlockItem(ENERGY_BUFFER_ADAMANTIUM.get(), new Item.Properties()));
        ITEMS.register("machine_wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));
        ITEMS.register("energy_buffer_creative", () -> new BlockItem(ENERGY_BUFFER_CREATIVE.get(), new Item.Properties()));
        ITEMS.register("basic_energy_pipe",
                () -> new EnergyPipeBlockItem(BASIC_ENERGY_PIPE.get(), new Item.Properties(),
                        EnergyPipeTier.BASIC.transferPerTick()));
        ITEMS.register("reinforced_energy_pipe",
                () -> new EnergyPipeBlockItem(REINFORCED_ENERGY_PIPE.get(), new Item.Properties(),
                        EnergyPipeTier.REINFORCED.transferPerTick()));
        ITEMS.register("high_voltage_energy_pipe",
                () -> new EnergyPipeBlockItem(HIGH_VOLTAGE_ENERGY_PIPE.get(), new Item.Properties(),
                        EnergyPipeTier.HIGH_VOLTAGE.transferPerTick()));
        ITEMS.register("reinforced_glass", () -> new BlockItem(REINFORCED_GLASS.get(), new Item.Properties()));
        ITEMS.register("dome_frame", () -> new BlockItem(DOME_FRAME.get(), new Item.Properties()));
        ITEMS.register("dome_foundation", () -> new BlockItem(DOME_FOUNDATION.get(), new Item.Properties()));
        ITEMS.register("lanos_decorative", () -> new BlockItem(LANOS_DECORATIVE.get(), new Item.Properties()));
        ITEMS.register("lanos_abandoned", () -> new BlockItem(LANOS_ABANDONED.get(), new Item.Properties()));
    }

    private ModBlocks() {
    }
}
