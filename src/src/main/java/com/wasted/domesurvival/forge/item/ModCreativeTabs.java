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

    /*
     * Stable thematic order for the Dome Survival tab. Transport pipes remain
     * one contiguous row; machine families and multiblock modules stay grouped.
     */
    private static final List<String> DISPLAY_ORDER = List.of(
            // Row 1: all transport pipes.
            "basic_energy_pipe",
            "reinforced_energy_pipe",
            "high_voltage_energy_pipe",
            "basic_fluid_pipe",
            "reinforced_fluid_pipe",
            "high_pressure_fluid_pipe",
            "oxygen_pipe",
            "reinforced_oxygen_pipe",
            "high_flow_oxygen_pipe",

            // Row 2: machines and energy storage.
            "coal_generator",
            "copper_furnace",
            "water_purifier",
            "oxygen_electrolyzer",
            "oxygen_filler",
            "oxygen_complex_air_intake",
            "oxygen_complex_filtration",
            "oxygen_complex_compression",
            "oxygen_complex_output",
            "energy_buffer",
            "energy_buffer_titan",
            "energy_buffer_adamantium",
            "energy_buffer_creative",

            // Row 3: bulk storage, logistics and core components.
            "universal_tank",
            "copper_hopper",
            "steel_hopper",
            "desh_hopper",
            "hopper_upgrade_vanilla_to_copper",
            "hopper_upgrade_copper_to_steel",
            "hopper_upgrade_steel_to_desh",
            "machine_stabilizer",
            "pulse_matrix",

            // Row 4: tools, filters and portable oxygen equipment.
            "machine_wrench",
            "airlock_binding_key",
            "water_filter_cartridge",
            "improved_water_filter",
            "industrial_water_filter",
            "oxygen_mask",
            "small_oxygen_tank",
            "medium_oxygen_tank",
            "large_oxygen_tank",

            // Row 5: suit and authored dome/airlock blocks.
            "surface_suit_helmet",
            "surface_suit_chestplate",
            "surface_suit_leggings",
            "surface_suit_boots",
            "airlock_gate",
            "airlock_control_panel",

            // Remaining authored/legacy blocks.
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
                                        .sorted(CreativeItemOrder.COMPARATOR)
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
