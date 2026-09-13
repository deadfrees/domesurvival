package com.wasted.domesurvival.forge.machine.module;

import java.util.Set;

/**
 * Common contract implemented by machines that accept DomeSurvival modules.
 */
public interface IModularMachine {
    int moduleSlotCount();

    Set<MachineModuleType> allowedModuleTypes();

    /**
     * Meaningful choices are the default: module families do not stack unless a
     * specific machine explicitly opts into it.
     */
    default int maxModulesOfType(MachineModuleType type) {
        return 1;
    }

    default boolean allowsModule(MachineModule module) {
        return module != null
                && maxModulesOfType(module.type()) > 0
                && allowedModuleTypes().contains(module.type());
    }

    default boolean allowsCombination(MachineModule first, MachineModule second) {
        return first != null
                && second != null
                && allowsModule(first)
                && allowsModule(second)
                && !first.conflictsWith(second);
    }
}
