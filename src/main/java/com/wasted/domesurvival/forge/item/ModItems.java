package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);

    /**
     * Technical V3.2 test balance: two minutes of tank oxygen at 1 O2/sec.
     * Recipes/progression are intentionally deferred until filling infrastructure is added.
     */
    public static final int SMALL_TANK_CAPACITY = 120;
    public static final int MEDIUM_TANK_CAPACITY = 360;
    public static final int LARGE_TANK_CAPACITY = 720;

    public static final int BASIC_FILTER_PROCESS_TICKS = 200;
    public static final int IMPROVED_FILTER_PROCESS_TICKS = 160;
    public static final int INDUSTRIAL_FILTER_PROCESS_TICKS = 120;

    public static final int BASIC_FILTER_ENERGY_PER_TICK = 15;
    public static final int IMPROVED_FILTER_ENERGY_PER_TICK = 19;
    public static final int INDUSTRIAL_FILTER_ENERGY_PER_TICK = 25;

    /** Core electronic control component used by machine recipes. */
    public static final RegistryObject<Item> PULSE_MATRIX = ITEMS.register(
            "pulse_matrix",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> WATER_FILTER_CARTRIDGE = ITEMS.register(
            "water_filter_cartridge",
            () -> new WaterFilterItem(
                    BASIC_FILTER_PROCESS_TICKS, BASIC_FILTER_ENERGY_PER_TICK,
                    new Item.Properties().durability(64)
            )
    );

    public static final RegistryObject<Item> IMPROVED_WATER_FILTER = ITEMS.register(
            "improved_water_filter",
            () -> new WaterFilterItem(
                    IMPROVED_FILTER_PROCESS_TICKS, IMPROVED_FILTER_ENERGY_PER_TICK,
                    new Item.Properties().durability(160)
            )
    );

    public static final RegistryObject<Item> INDUSTRIAL_WATER_FILTER = ITEMS.register(
            "industrial_water_filter",
            () -> new AirFilterItem(320, new Item.Properties().durability(320))
    );
public static final RegistryObject<Item> OXYGEN_MASK = ITEMS.register(
            "oxygen_mask",
            () -> new OxygenMaskItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> SMALL_OXYGEN_TANK = ITEMS.register(
            "small_oxygen_tank",
            () -> new OxygenTankItem(
                    SMALL_TANK_CAPACITY,
                    new Item.Properties().stacksTo(1)
            )
    );

    public static final RegistryObject<Item> MEDIUM_OXYGEN_TANK = ITEMS.register(
            "medium_oxygen_tank",
            () -> new OxygenTankItem(
                    MEDIUM_TANK_CAPACITY,
                    new Item.Properties().stacksTo(1)
            )
    );

    public static final RegistryObject<Item> LARGE_OXYGEN_TANK = ITEMS.register(
            "large_oxygen_tank",
            () -> new OxygenTankItem(
                    LARGE_TANK_CAPACITY,
                    new Item.Properties().stacksTo(1)
            )
    );

    public static final RegistryObject<Item> SURFACE_SUIT_HELMET = ITEMS.register(
            "surface_suit_helmet",
            () -> new SurfaceSuitItem(ArmorItem.Type.HELMET, new Item.Properties())
    );

    public static final RegistryObject<Item> SURFACE_SUIT_CHESTPLATE = ITEMS.register(
            "surface_suit_chestplate",
            () -> new SurfaceSuitItem(ArmorItem.Type.CHESTPLATE, new Item.Properties())
    );

    public static final RegistryObject<Item> SURFACE_SUIT_LEGGINGS = ITEMS.register(
            "surface_suit_leggings",
            () -> new SurfaceSuitItem(ArmorItem.Type.LEGGINGS, new Item.Properties())
    );

    public static final RegistryObject<Item> SURFACE_SUIT_BOOTS = ITEMS.register(
            "surface_suit_boots",
            () -> new SurfaceSuitItem(ArmorItem.Type.BOOTS, new Item.Properties())
    );

    /** Separate painting item backed by Dome Survival painting variants. */
    public static final RegistryObject<Item> MEMORY_PAINTING = ITEMS.register(
            "memory_painting",
            () -> new MemoryPaintingItem(new Item.Properties().stacksTo(64))
    );
    private ModItems() {
    }
}
