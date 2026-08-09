package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
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

    private ModItems() {
    }
}
