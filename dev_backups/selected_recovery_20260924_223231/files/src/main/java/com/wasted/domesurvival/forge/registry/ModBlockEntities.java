package com.wasted.domesurvival.forge.registry;

import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.CreativeEnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.copper.CopperFurnaceBlockEntity;
import com.wasted.domesurvival.forge.machine.coal.CoalGeneratorBlockEntity;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceBlockEntity;
import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnacePartBlockEntity;
import com.wasted.domesurvival.forge.machine.shaft.CokeOvenBlockEntity;
import com.wasted.domesurvival.forge.machine.shaft.CokeOvenPartBlockEntity;
import com.wasted.domesurvival.forge.machine.water.WaterPurifierBlockEntity;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenElectrolyzerBlockEntity;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerBlockEntity;
import com.wasted.domesurvival.forge.machine.bio.BioincubatorBlockEntity;
import com.wasted.domesurvival.forge.machine.sieve.SandSieveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DomeSurvival.MOD_ID);
public static final RegistryObject<BlockEntityType<CopperFurnaceBlockEntity>> COPPER_FURNACE =
            BLOCK_ENTITY_TYPES.register(
                    "copper_furnace",
                    () -> BlockEntityType.Builder.of(
                            CopperFurnaceBlockEntity::new,
                            ModBlocks.COPPER_FURNACE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR =
            BLOCK_ENTITY_TYPES.register(
                    "coal_generator",
                    () -> BlockEntityType.Builder.of(
                            CoalGeneratorBlockEntity::new,
                            ModBlocks.COAL_GENERATOR.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<ShaftFurnaceBlockEntity>> SHAFT_FURNACE =
            BLOCK_ENTITY_TYPES.register(
                    "shaft_furnace",
                    () -> BlockEntityType.Builder.of(
                            ShaftFurnaceBlockEntity::new,
                            ModBlocks.SHAFT_FURNACE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<ShaftFurnacePartBlockEntity>> SHAFT_FURNACE_PART =
            BLOCK_ENTITY_TYPES.register(
                    "shaft_furnace_part",
                    () -> BlockEntityType.Builder.of(
                            ShaftFurnacePartBlockEntity::new,
                            ModBlocks.SHAFT_FURNACE_PART.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<CokeOvenBlockEntity>> COKE_OVEN =
            BLOCK_ENTITY_TYPES.register(
                    "coke_oven",
                    () -> BlockEntityType.Builder.of(
                            CokeOvenBlockEntity::new,
                            ModBlocks.COKE_OVEN.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<CokeOvenPartBlockEntity>> COKE_OVEN_PART =
            BLOCK_ENTITY_TYPES.register(
                    "coke_oven_part",
                    () -> BlockEntityType.Builder.of(
                            CokeOvenPartBlockEntity::new,
                            ModBlocks.COKE_OVEN_PART.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<WaterPurifierBlockEntity>> WATER_PURIFIER =
            BLOCK_ENTITY_TYPES.register(
                    "water_purifier",
                    () -> BlockEntityType.Builder.of(
                            WaterPurifierBlockEntity::new,
                            ModBlocks.WATER_PURIFIER.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<OxygenElectrolyzerBlockEntity>> OXYGEN_ELECTROLYZER =
            BLOCK_ENTITY_TYPES.register(
                    "oxygen_electrolyzer",
                    () -> BlockEntityType.Builder.of(
                            OxygenElectrolyzerBlockEntity::new,
                            ModBlocks.OXYGEN_ELECTROLYZER.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<OxygenFillerBlockEntity>> OXYGEN_FILLER =
            BLOCK_ENTITY_TYPES.register(
                    "oxygen_filler",
                    () -> BlockEntityType.Builder.of(
                            OxygenFillerBlockEntity::new,
                            ModBlocks.OXYGEN_FILLER.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<BioincubatorBlockEntity>> BIOINCUBATOR =
            BLOCK_ENTITY_TYPES.register(
                    "bioincubator",
                    () -> BlockEntityType.Builder.of(
                            BioincubatorBlockEntity::new,
                            ModBlocks.BIOINCUBATOR.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<SandSieveBlockEntity>> SAND_SIEVE =
            BLOCK_ENTITY_TYPES.register(
                    "sand_sieve",
                    () -> BlockEntityType.Builder.of(
                            SandSieveBlockEntity::new,
                            ModBlocks.SAND_SIEVE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<EnergyBufferBlockEntity>> ENERGY_BUFFER =
            BLOCK_ENTITY_TYPES.register(
                    "energy_buffer",
                    () -> BlockEntityType.Builder.of(
                            EnergyBufferBlockEntity::new,
                            ModBlocks.ENERGY_BUFFER.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<TitanEnergyBufferBlockEntity>> ENERGY_BUFFER_TITAN =
            BLOCK_ENTITY_TYPES.register(
                    "energy_buffer_titan",
                    () -> BlockEntityType.Builder.of(
                            TitanEnergyBufferBlockEntity::new,
                            ModBlocks.ENERGY_BUFFER_TITAN.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<AdamantiumEnergyBufferBlockEntity>> ENERGY_BUFFER_ADAMANTIUM =
            BLOCK_ENTITY_TYPES.register(
                    "energy_buffer_adamantium",
                    () -> BlockEntityType.Builder.of(
                            AdamantiumEnergyBufferBlockEntity::new,
                            ModBlocks.ENERGY_BUFFER_ADAMANTIUM.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<CreativeEnergyBufferBlockEntity>> ENERGY_BUFFER_CREATIVE =
            BLOCK_ENTITY_TYPES.register(
                    "energy_buffer_creative",
                    () -> BlockEntityType.Builder.of(
                            CreativeEnergyBufferBlockEntity::new,
                            ModBlocks.ENERGY_BUFFER_CREATIVE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<EnergyPipeBlockEntity>> ENERGY_PIPE =
            BLOCK_ENTITY_TYPES.register(
                    "energy_pipe",
                    () -> BlockEntityType.Builder.of(
                            EnergyPipeBlockEntity::new,
                            ModBlocks.BASIC_ENERGY_PIPE.get(),
                            ModBlocks.REINFORCED_ENERGY_PIPE.get(),
                            ModBlocks.HIGH_VOLTAGE_ENERGY_PIPE.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }
}
