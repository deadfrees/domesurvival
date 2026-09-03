package com.wasted.domesurvival.forge.dome;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * Manifest of blocks that represent quest-built development, not the initial camp.
 * NPC entities, architecture, vegetation and starter decoration are deliberately preserved.
 */
public final class StarterDomeInitialState {
    private static final Set<String> VANILLA_PROGRESS_BLOCKS = Set.of(
            "crafting_table",
            "stonecutter",
            "chest",
            "trapped_chest",
            "barrel",
            "furnace",
            "blast_furnace",
            "smoker",
            "composter",
            "redstone_lamp"
    );

    private static final Set<String> DOME_PROGRESS_BLOCKS = Set.of(
            "coal_generator",
            "energy_buffer",
            "energy_buffer_titan",
            "energy_buffer_adamantium",
            "energy_buffer_creative",
            "basic_energy_pipe",
            "reinforced_energy_pipe",
            "high_voltage_energy_pipe",
            "water_purifier",
            "oxygen_electrolyzer",
            "oxygen_filler",
            "oxygen_pipe",
            "reinforced_oxygen_pipe",
            "high_flow_oxygen_pipe",
            "bioincubator",
            "coke_oven",
            "coke_oven_part",
            "shaft_furnace",
            "shaft_furnace_part",
            "metal_furnace"
    );

    private StarterDomeInitialState() {
    }

    public static boolean shouldRemove(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getBlock() instanceof BedBlock
                || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof FarmBlock
                || state.getBlock() instanceof ComposterBlock) {
            return true;
        }

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        if ("minecraft".equals(id.getNamespace())) {
            return VANILLA_PROGRESS_BLOCKS.contains(path);
        }
        if ("domesurvival".equals(id.getNamespace()) && DOME_PROGRESS_BLOCKS.contains(path)) {
            return true;
        }

        // Existing late-game machines from integrated mods must not leak into a fresh campaign.
        return path.contains("generator")
                || path.contains("dynamo")
                || path.contains("energy_cell")
                || path.contains("power_cell")
                || path.endsWith("_cable")
                || path.endsWith("_conduit")
                || path.endsWith("_machine")
                || path.contains("crusher")
                || path.contains("smelter")
                || path.contains("reactor")
                || (path.contains("copper") && path.contains("furnace"));
    }
}
