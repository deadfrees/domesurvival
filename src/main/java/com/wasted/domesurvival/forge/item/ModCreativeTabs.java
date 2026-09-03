package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated Dome Survival creative tab with a stable player-facing order.
 *
 * <p>The tab remains registry-driven: every item in the domesurvival namespace
 * still appears automatically. Authored items get a fixed thematic rank while
 * future unknown items are appended alphabetically at the end.</p>
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    private static final ResourceLocation TAB_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "items");
    private static final ResourceLocation PREFERRED_ICON =
            new ResourceLocation(DomeSurvival.MOD_ID, "reinforced_glass");

    /** Stable thematic order: materials, life support, power, technology, biology, misc. */
    private static final List<String> DISPLAY_ORDER = List.of(
            // 1. Base materials and components.
            "steel_ingot",
            "coal_coke",
            "slag",
            "steel_gear",
            "tin_gear",
            "lead_gear",
            "nickel_gear",
            "pulse_matrix",
            "machine_stabilizer",
            "water_filter_cartridge",
            "improved_water_filter",
            "industrial_water_filter",
            "hopper_upgrade_vanilla_to_copper",
            "hopper_upgrade_copper_to_steel",
            "hopper_upgrade_steel_to_desh",

            // 2. Oxygen and life-support equipment.
            "water_purifier",
            "oxygen_electrolyzer",
            "oxygen_filler",
            "oxygen_complex_air_intake",
            "oxygen_complex_filtration",
            "oxygen_complex_compression",
            "oxygen_complex_output",
            "oxygen_mask",
            "small_oxygen_tank",
            "medium_oxygen_tank",
            "large_oxygen_tank",
            "oxygen_pipe",
            "reinforced_oxygen_pipe",
            "high_flow_oxygen_pipe",

            // 3. Power generation, storage and transport.
            "coal_generator",
            "energy_buffer",
            "energy_buffer_titan",
            "energy_buffer_adamantium",
            "energy_buffer_creative",
            "basic_energy_pipe",
            "reinforced_energy_pipe",
            "high_voltage_energy_pipe",

            // 4. Tanks, processing machines and technical logistics.
            "universal_tank",
            "basic_fluid_pipe",
            "reinforced_fluid_pipe",
            "high_pressure_fluid_pipe",
            "copper_furnace",
            "coke_oven",
            "shaft_furnace",
            "copper_hopper",
            "steel_hopper",
            "desh_hopper",
            "copper_item_pipe",
            "steel_item_pipe",
            "desh_item_pipe",
            "filtering_item_pipe",
            "service_pass_through",

            // 5. Fauna restoration.
            "bioincubator",
            "bio_repair_kit",
            "biogel",
            "nutrient_mix",
            "chicken_cryocapsule",
            "sheep_cryocapsule",
            "cow_cryocapsule",
            "damaged_pig_cryocapsule",

            // 6. Tools, suit, construction and decorative content.
            "sand_sieve",
            "fiber_sieve_mesh",
            "copper_sieve_mesh",
            "steel_sieve_mesh",
            "machine_wrench",
            "airlock_binding_key",
            "memory_painting",
            "surface_suit_helmet",
            "surface_suit_chestplate",
            "surface_suit_leggings",
            "surface_suit_boots",
            "airlock_gate",
            "airlock_control_panel",

            "lanos_decorative",
            "lanos_abandoned"
    );

    private static final Map<String, Integer> DISPLAY_RANK = createDisplayRank();

    private ModCreativeTabs() {
    }

    @SubscribeEvent
    public static void registerCreativeTab(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(
                TAB_ID,
                CreativeModeTab.builder()
                        .title(Component.literal("Dome Survival"))
                        .icon(ModCreativeTabs::createIcon)
                        .displayItems((parameters, output) ->
                                ForgeRegistries.ITEMS.getValues().stream()
                                        .filter(ModCreativeTabs::isDomeSurvivalItem)
                                        .sorted(Comparator.comparingInt(ModCreativeTabs::displayRank)
                                                .thenComparing(ModCreativeTabs::registryPath))
                                        .forEach(output::accept))
                        .build()
        ));
    }

    private static boolean isDomeSurvivalItem(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !DomeSurvival.MOD_ID.equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();

        // Legacy airlock items are removed. Dome construction blocks remain
        // registered and obtainable through /give, but stay out of creative.
        return !"airlock_door".equals(path)
                && !"airlock_panel".equals(path)
                && !"bio_module".equals(path)
                && !"reinforced_glass".equals(path)
                && !"dome_frame".equals(path)
                && !"dome_foundation".equals(path);
    }

    private static String registryPath(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "" : id.getPath();
    }

    private static int displayRank(Item item) {
        return DISPLAY_RANK.getOrDefault(registryPath(item), Integer.MAX_VALUE);
    }

    private static Map<String, Integer> createDisplayRank() {
        HashMap<String, Integer> ranks = new HashMap<>(DISPLAY_ORDER.size() * 2);
        for (int i = 0; i < DISPLAY_ORDER.size(); i++) {
            ranks.put(DISPLAY_ORDER.get(i), i);
        }
        return Map.copyOf(ranks);
    }

    private static ItemStack createIcon() {
        Item preferred = ForgeRegistries.ITEMS.getValue(PREFERRED_ICON);
        return new ItemStack(preferred == null || preferred == Items.AIR
                ? Items.GLASS
                : preferred);
    }
}
