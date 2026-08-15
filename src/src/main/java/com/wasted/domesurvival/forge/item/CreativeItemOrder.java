package com.wasted.domesurvival.forge.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.Map;

/**
 * Stable visual grouping for the Dome Survival creative tab.
 *
 * <p>This does not alter registries or recipes. It only changes display order.
 * Unknown/future items remain visible and fall back to alphabetical ordering.</p>
 */
public final class CreativeItemOrder {
    private static final Map<String, Integer> EXPLICIT_ORDER = Map.ofEntries(
            // Machines / machine structures.
            Map.entry("copper_furnace", 100),
            Map.entry("machine_stabilizer", 110),
            Map.entry("coal_generator", 120),
            Map.entry("water_purifier", 130),
            Map.entry("oxygen_electrolyzer", 140),
            Map.entry("oxygen_filler", 150),
            Map.entry("energy_buffer", 160),
            Map.entry("energy_buffer_titan", 161),
            Map.entry("energy_buffer_adamantium", 162),
            Map.entry("energy_buffer_creative", 163),
            Map.entry("universal_tank", 170),
            Map.entry("service_pass_through", 180),
            Map.entry("oxygen_complex_air_intake", 190),
            Map.entry("oxygen_complex_filtration", 191),
            Map.entry("oxygen_complex_compression", 192),
            Map.entry("oxygen_complex_output", 193),
            Map.entry("airlock_control_panel", 200),
            Map.entry("airlock_gate", 201),

            // ALL pipe families together.
            Map.entry("basic_energy_pipe", 300),
            Map.entry("reinforced_energy_pipe", 301),
            Map.entry("high_voltage_energy_pipe", 302),

            Map.entry("basic_fluid_pipe", 310),
            Map.entry("reinforced_fluid_pipe", 311),
            Map.entry("high_pressure_fluid_pipe", 312),

            Map.entry("oxygen_pipe", 320),
            Map.entry("reinforced_oxygen_pipe", 321),
            Map.entry("high_flow_oxygen_pipe", 322),

            Map.entry("copper_item_pipe", 330),
            Map.entry("steel_item_pipe", 331),
            Map.entry("desh_item_pipe", 332),
            Map.entry("filtering_item_pipe", 333),

            // Logistics / upgrades.
            Map.entry("copper_hopper", 400),
            Map.entry("steel_hopper", 401),
            Map.entry("desh_hopper", 402),
            Map.entry("hopper_upgrade_vanilla_to_copper", 410),
            Map.entry("hopper_upgrade_copper_to_steel", 411),
            Map.entry("hopper_upgrade_steel_to_desh", 412),

            // Tools.
            Map.entry("machine_wrench", 500),
            Map.entry("airlock_binding_key", 501)
    );

    public static final Comparator<Item> COMPARATOR =
            Comparator.comparingInt(CreativeItemOrder::order)
                    .thenComparing(CreativeItemOrder::registryPath);

    private CreativeItemOrder() {
    }

    private static int order(Item item) {
        return EXPLICIT_ORDER.getOrDefault(registryPath(item), 1000);
    }

    private static String registryPath(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "" : id.getPath();
    }
}
