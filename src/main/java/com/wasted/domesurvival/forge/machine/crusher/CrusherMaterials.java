package com.wasted.domesurvival.forge.machine.crusher;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Materials produced by the industrial crushing stage. */
public final class CrusherMaterials {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);

    public static final RegistryObject<Item> CRUSHED_IRON_ORE = register("crushed_iron_ore");
    public static final RegistryObject<Item> CRUSHED_COPPER_ORE = register("crushed_copper_ore");
    public static final RegistryObject<Item> CRUSHED_GOLD_ORE = register("crushed_gold_ore");

    private static RegistryObject<Item> register(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(64)));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private CrusherMaterials() {
    }
}
