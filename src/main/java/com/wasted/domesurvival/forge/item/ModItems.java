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
/** Early engineering components kept local so Thermal stays progression-locked. */
    public static final RegistryObject<Item> STEEL_GEAR = engineeringComponent("steel_gear");
    public static final RegistryObject<Item> TIN_GEAR = engineeringComponent("tin_gear");
    public static final RegistryObject<Item> LEAD_GEAR = engineeringComponent("lead_gear");
    public static final RegistryObject<Item> NICKEL_GEAR = engineeringComponent("nickel_gear");

    /** Self-contained early metallurgy materials; no external machine is required to obtain them. */
    public static final RegistryObject<Item> COAL_COKE = ITEMS.register(
            "coal_coke",
            () -> new CoalCokeItem(new Item.Properties().stacksTo(64))
    );
    public static final RegistryObject<Item> STEEL_INGOT = engineeringComponent("steel_ingot");
    public static final RegistryObject<Item> SLAG = engineeringComponent("slag");

    /** Replaceable meshes for the placeable sand sifter. */
    public static final RegistryObject<Item> FIBER_SIEVE_MESH = ITEMS.register(
            "fiber_sieve_mesh",
            () -> new SieveMeshItem(SieveMeshItem.Tier.FIBER,
                    new Item.Properties().stacksTo(1).durability(64))
    );
    public static final RegistryObject<Item> COPPER_SIEVE_MESH = ITEMS.register(
            "copper_sieve_mesh",
            () -> new SieveMeshItem(SieveMeshItem.Tier.COPPER,
                    new Item.Properties().stacksTo(1).durability(192))
    );
    public static final RegistryObject<Item> STEEL_SIEVE_MESH = ITEMS.register(
            "steel_sieve_mesh",
            () -> new SieveMeshItem(SieveMeshItem.Tier.STEEL,
                    new Item.Properties().stacksTo(1).durability(384))
    );

    /** Viable pre-catastrophe animal genomes recovered from the genetic archive. */
    public static final RegistryObject<Item> CHICKEN_CRYOCAPSULE = ITEMS.register(
            "chicken_cryocapsule",
            () -> new CryocapsuleItem(new net.minecraft.resources.ResourceLocation("minecraft", "chicken"), false, new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE))
    );
    public static final RegistryObject<Item> SHEEP_CRYOCAPSULE = ITEMS.register(
            "sheep_cryocapsule",
            () -> new CryocapsuleItem(new net.minecraft.resources.ResourceLocation("minecraft", "sheep"), false, new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE))
    );
    public static final RegistryObject<Item> COW_CRYOCAPSULE = ITEMS.register(
            "cow_cryocapsule",
            () -> new CryocapsuleItem(new net.minecraft.resources.ResourceLocation("minecraft", "cow"), false, new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE))
    );

    /** Damaged pig genome; preserved for a later research stage. */
    public static final RegistryObject<Item> DAMAGED_PIG_CRYOCAPSULE = ITEMS.register(
            "damaged_pig_cryocapsule",
            () -> new CryocapsuleItem(new net.minecraft.resources.ResourceLocation("minecraft", "pig"), true, new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON))
    );

    /** Data-driven unidentified sample used by generated and fixed-map loot layers. */
    public static final RegistryObject<Item> BIO_MODULE = ITEMS.register(
            "bio_module",
            () -> new BioModuleItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE))
    );

    /** Universal consumables for restoring damaged biological modules. */
    public static final RegistryObject<Item> BIO_REPAIR_KIT = engineeringComponent("bio_repair_kit");
    public static final RegistryObject<Item> BIOGEL = ITEMS.register(
            "biogel",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON))
    );
    public static final RegistryObject<Item> NUTRIENT_MIX = ITEMS.register(
            "nutrient_mix",
            () -> new Item(new Item.Properties().stacksTo(16))
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

    private static RegistryObject<Item> engineeringComponent(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(64)));
    }

    private ModItems() {
    }
}
