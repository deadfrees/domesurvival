package com.wasted.domesurvival.forge.machine.module;

import java.util.Set;

/**
 * Common contract implemented by machines that accept DomeSurvival modules.
 */
public interface IModularMachine {
    int moduleSlotCount();

    Set<MachineModuleType> allowedModuleTypes();

    default boolean allowsModule(MachineModule module) {
        return module != null && allowedModuleTypes().contains(module.type());
    }

    default boolean allowsCombination(MachineModule first, MachineModule second) {
        return first != null
                && second != null
                && allowsModule(first)
                && allowsModule(second)
                && !first.conflictsWith(second);
    }
}
