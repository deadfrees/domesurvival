package com.wasted.domesurvival.forge.material;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Manufactured intermediates for DomeSurvival technology progression. */
public final class TechComponents {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);

    public static final RegistryObject<Item> COPPER_ROD = component("copper_rod");
    public static final RegistryObject<Item> COPPER_WIRE = component("copper_wire");
    public static final RegistryObject<Item> COPPER_TUBE = component("copper_tube");
    public static final RegistryObject<Item> TIN_TUBE = component("tin_tube");
    public static final RegistryObject<Item> STEEL_PLATE = component("steel_plate");
    public static final RegistryObject<Item> STEEL_ROD = component("steel_rod");
    public static final RegistryObject<Item> STEEL_WIRE = component("steel_wire");
    public static final RegistryObject<Item> STEEL_TUBE = component("steel_tube");
    public static final RegistryObject<Item> NICKEL_PLATE = component("nickel_plate");
    public static final RegistryObject<Item> NICKEL_TUBE = component("nickel_tube");
    public static final RegistryObject<Item> SILVER_ROD = component("silver_rod");
    public static final RegistryObject<Item> SILVER_WIRE = component("silver_wire");
    public static final RegistryObject<Item> GOTEIUM_PLATE = component("goteium_plate");
    public static final RegistryObject<Item> GOTEIUM_GEAR = component("goteium_gear");
    public static final RegistryObject<Item> VOLTARIUM_PLATE = component("voltarium_plate");
    public static final RegistryObject<Item> VOLTARIUM_GEAR = component("voltarium_gear");
    public static final RegistryObject<Item> VOLTARIUM_ROD = component("voltarium_rod");
    public static final RegistryObject<Item> VOLTARIUM_WIRE = component("voltarium_wire");

    private static RegistryObject<Item> component(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(64)));
    }

    private TechComponents() {
    }
}
