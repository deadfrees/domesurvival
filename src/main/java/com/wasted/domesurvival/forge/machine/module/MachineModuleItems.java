package com.wasted.domesurvival.forge.machine.module;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Physical item registrations for the shared logical machine modules. */
public final class MachineModuleItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DomeSurvival.MOD_ID);

    public static final RegistryObject<Item> EFFICIENCY = register("efficiency_module", MachineModuleCatalog.EFFICIENCY);
    public static final RegistryObject<Item> OVERDRIVE = register("overdrive_module", MachineModuleCatalog.OVERDRIVE);
    public static final RegistryObject<Item> BUFFER = register("buffer_module", MachineModuleCatalog.BUFFER);
    public static final RegistryObject<Item> AUTOMATION = register("automation_module", MachineModuleCatalog.AUTOMATION);
    public static final RegistryObject<Item> EMERGENCY_PROTECTION = register("emergency_protection_module", MachineModuleCatalog.EMERGENCY_PROTECTION);
    public static final RegistryObject<Item> COMMUNICATION = register("communication_module", MachineModuleCatalog.COMMUNICATION);

    private static RegistryObject<Item> register(String id, MachineModule module) {
        return ITEMS.register(id, () -> new MachineModuleItem(module, new Item.Properties().stacksTo(1)));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private MachineModuleItems() {
    }
}
