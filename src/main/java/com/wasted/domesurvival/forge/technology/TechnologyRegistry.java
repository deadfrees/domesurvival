package com.wasted.domesurvival.forge.technology;

import com.wasted.domesurvival.forge.quest.QuestProgressFlags;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central technology catalogue. Exact item rules have priority over namespace
 * rules, while raw materials stay available so a locked technology cannot
 * accidentally block the resource chain needed to research it.
 */
public final class TechnologyRegistry {
    private static final Map<ResourceLocation, Technology> EXACT = new LinkedHashMap<>();
    private static final Map<String, Technology> NAMESPACE_DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, List<String>> MATERIAL_PREFIXES = new LinkedHashMap<>();

    private static final Technology POWER_START = technology(
            "power_start", "Основы энергетики", "POWER_PROGRAM_STARTED");
    private static final Technology PULSE_MATRIX = technology(
            "pulse_matrix", "Импульсная матрица", "PULSE_MATRIX_AVAILABLE");
    private static final Technology FIRST_POWER = technology(
            "first_power", "Первая энергосеть", "FIRST_POWERED_MACHINE_ONLINE");
    private static final Technology POWER_TRANSMISSION = technology(
            "power_transmission", "Передача энергии", "POWER_TRANSMISSION_TECH_KNOWN");
    private static final Technology DOME_POWER = technology(
            "dome_power", "Энергетическая инфраструктура купола", "DOME_POWER_ONLINE");
    private static final Technology WATER_PURIFICATION = technology(
            "water_purification", "Очистка воды", "WATER_PURIFICATION_TECH_KNOWN");
    private static final Technology OXYGEN_ELECTROLYSIS = technology(
            "oxygen_electrolysis", "Кислородный электролиз", "OXYGEN_ELECTROLYSIS_TECH_KNOWN");
    private static final Technology OXYGEN_DISTRIBUTION = technology(
            "oxygen_distribution", "Распределение кислорода", "OXYGEN_DISTRIBUTION_TECH_KNOWN");
    private static final Technology OXYGEN_FILLING = technology(
            "oxygen_filling", "Заправка кислорода", "OXYGEN_FILLING_TECH_KNOWN");
    private static final Technology PORTABLE_OXYGEN = technology(
            "portable_oxygen", "Переносное кислородное оборудование", "PORTABLE_OXYGEN_TECH_KNOWN");
    private static final Technology SETTLEMENT = technology(
            "settlement", "Инфраструктура поселения", "SETTLEMENT_ESTABLISHED");
    private static final Technology FOOD = technology(
            "food", "Пищевая промышленность", "FOOD_SYSTEM_ESTABLISHED");
    private static final Technology INDUSTRY = technology(
            "industry", "Промышленное производство", "INDUSTRY_STAGE_1");
    private static final Technology HEAVY_INDUSTRY = technology(
            "heavy_industry", "Тяжёлая промышленность", "HEAVY_INDUSTRY_STARTED");
    private static final Technology AUTOMATION = technology(
            "automation", "Промышленная автоматизация", "AUTOMATION_ONLINE");
    private static final Technology DEEP_MINING = technology(
            "deep_mining", "Глубинная добыча", "DEEP_MINING_STAGE_1");
    private static final Technology SPACE = technology(
            "space", "Космическая программа", "SPACE_PROGRAM_STARTED");

    static {
        exact(POWER_START,
                "domesurvival:machine_stabilizer",
                "domesurvival:pulse_matrix",
                "domesurvival:steel_gear",
                "domesurvival:tin_gear",
                "domesurvival:lead_gear",
                "domesurvival:nickel_gear",
                "domesurvival:airlock_binding_key",
                "domesurvival:airlock_control_panel",
                "domesurvival:airlock_gate",
                "immersiveengineering:hammer",
                "domesurvival:coke_oven",
                "domesurvival:shaft_furnace");
        exact(PULSE_MATRIX, "domesurvival:coal_generator");
        exact(FIRST_POWER, "domesurvival:basic_energy_pipe");
        exact(POWER_TRANSMISSION, "domesurvival:energy_buffer");

        exact(DOME_POWER,
                "domesurvival:machine_wrench",
                "domesurvival:water_filter_cartridge",
                "domesurvival:basic_fluid_pipe",
                "domesurvival:universal_tank",
                "immersiveengineering:wirecutter",
                "immersiveengineering:fluid_pipe",
                "immersiveengineering:fluid_pump",
                "immersiveengineering:voltmeter",
                "mekanism:steel_casing",
                "mekanism:metallurgic_infuser",
                "mekanism:basic_control_circuit",
                "mekanism:basic_mechanical_pipe",
                "mekanism:electrolytic_core",
                "mekanism:electrolytic_separator");

        exact(WATER_PURIFICATION, "domesurvival:water_purifier");
        exact(OXYGEN_ELECTROLYSIS,
                "domesurvival:oxygen_electrolyzer",
                "domesurvival:oxygen_pipe");
        exact(OXYGEN_DISTRIBUTION, "domesurvival:reinforced_oxygen_pipe");
        exact(OXYGEN_FILLING, "domesurvival:oxygen_filler");
        exact(PORTABLE_OXYGEN,
                "domesurvival:small_oxygen_tank",
                "domesurvival:oxygen_mask");

        exact(SETTLEMENT,
                "domesurvival:copper_furnace",
                "domesurvival:copper_hopper",
                "domesurvival:copper_item_pipe");
        exact(INDUSTRY,
                "domesurvival:steel_hopper",
                "domesurvival:steel_item_pipe",
                "domesurvival:service_pass_through",
                "domesurvival:improved_water_filter");
        exact(AUTOMATION, "domesurvival:filtering_item_pipe");
        exact(DEEP_MINING, "domesurvival:large_oxygen_tank");
        exact(technology("oxygen_infrastructure", "Стационарные кислородные резервы", "OXYGEN_INFRASTRUCTURE_READY"),
                "domesurvival:medium_oxygen_tank");
        exact(technology("desh", "Обработка деша", "DESH_TECH_AVAILABLE"),
                "domesurvival:desh_hopper",
                "domesurvival:desh_item_pipe",
                "domesurvival:high_flow_oxygen_pipe",
                "domesurvival:high_pressure_fluid_pipe",
                "domesurvival:energy_buffer_titan");
        exact(HEAVY_INDUSTRY,
                "domesurvival:industrial_water_filter",
                "domesurvival:high_voltage_energy_pipe",
                "domesurvival:reinforced_energy_pipe",
                "domesurvival:reinforced_fluid_pipe",
                "domesurvival:oxygen_complex_controller",
                "domesurvival:oxygen_complex_casing",
                "domesurvival:oxygen_complex_intake",
                "domesurvival:oxygen_complex_output");
        exact(technology("reality_edge", "Технологии границы реальности", "REALITY_EDGE_REACHED"),
                "domesurvival:energy_buffer_adamantium");

        namespace("immersiveengineering", HEAVY_INDUSTRY);
        namespace("mekanism", HEAVY_INDUSTRY);
        namespace("mekanismgenerators", HEAVY_INDUSTRY);
        namespace("mekanismtools", HEAVY_INDUSTRY);
        namespace("thermal", HEAVY_INDUSTRY);
        namespace("enderio", AUTOMATION);
        namespace("ad_astra", SPACE);
        namespace("sophisticatedbackpacks", SETTLEMENT);
        namespace("ironchest", SETTLEMENT);
        namespace("waystones", technology("field_logistics", "Полевая логистика", "FIELD_AIRLOCK_READY"));
        namespace("farmersdelight", FOOD);
        namespace("brewinandchewin", FOOD);

        materialPrefixes("mekanism",
                "ingot_", "nugget_", "block_", "raw_", "dust_", "dirty_dust_",
                "clump_", "shard_", "crystal_", "alloy_", "enriched_", "pellet_",
                "salt", "fluorite", "osmium", "tin", "lead", "uranium", "bronze", "steel");
        materialPrefixes("immersiveengineering",
                "ingot_", "nugget_", "plate_", "dust_", "wire_", "stick_", "component_",
                "treated_wood", "coal_coke", "coke", "slag", "hemp", "fiber_", "fabric_");
        materialPrefixes("thermal",
                "ingot_", "nugget_", "dust_", "plate_", "gear_", "raw_", "block_",
                "*_ingot", "*_nugget", "*_dust", "*_plate", "*_gear", "*_block",
                "apatite", "cinnabar", "sulfur", "niter");
    }

    private TechnologyRegistry() {
    }

    public static Optional<Technology> requiredFor(ResourceLocation itemId) {
        Technology exact = EXACT.get(itemId);
        if (exact != null) {
            return Optional.of(exact);
        }

        Technology namespaceRule = NAMESPACE_DEFAULTS.get(itemId.getNamespace());
        if (namespaceRule == null || isMaterialExempt(itemId)) {
            return Optional.empty();
        }
        return Optional.of(namespaceRule);
    }

    public static Collection<Technology> all() {
        Map<String, Technology> unique = new LinkedHashMap<>();
        EXACT.values().forEach(technology -> unique.put(technology.id(), technology));
        NAMESPACE_DEFAULTS.values().forEach(technology -> unique.put(technology.id(), technology));
        return Collections.unmodifiableCollection(unique.values());
    }

    public static List<Technology> technologiesForFlag(String flag) {
        List<Technology> result = new ArrayList<>();
        for (Technology technology : all()) {
            if (technology.requiredFlag().equals(flag)) {
                result.add(technology);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Technology technology(String id, String title, String flag) {
        if (!QuestProgressFlags.isKnown(flag)) {
            throw new IllegalStateException("Unknown technology progress flag: " + flag);
        }
        return new Technology(id, title, flag);
    }

    private static void exact(Technology technology, String... itemIds) {
        for (String itemId : itemIds) {
            ResourceLocation id = new ResourceLocation(itemId);
            Technology previous = EXACT.put(id, technology);
            if (previous != null) {
                throw new IllegalStateException("Duplicate technology rule for " + id);
            }
        }
    }

    private static void namespace(String namespace, Technology technology) {
        NAMESPACE_DEFAULTS.put(namespace, technology);
    }

    private static void materialPrefixes(String namespace, String... prefixes) {
        MATERIAL_PREFIXES.put(namespace, List.of(prefixes));
    }

    private static boolean isMaterialExempt(ResourceLocation itemId) {
        List<String> prefixes = MATERIAL_PREFIXES.get(itemId.getNamespace());
        if (prefixes == null) {
            return false;
        }
        String path = itemId.getPath();
        return prefixes.stream().anyMatch(pattern -> pattern.startsWith("*")
                ? path.endsWith(pattern.substring(1))
                : path.startsWith(pattern));
    }

    public record Technology(String id, String title, String requiredFlag) {
    }
}
