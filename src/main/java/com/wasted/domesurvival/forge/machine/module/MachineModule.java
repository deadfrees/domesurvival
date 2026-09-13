package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Immutable logical description of a machine module.
 */
public record MachineModule(
        ResourceLocation id,
        MachineModuleType type,
        Set<MachineModuleType> incompatibleWith
) {
    public MachineModule {
        incompatibleWith = Set.copyOf(incompatibleWith);
    }

    public boolean conflictsWith(MachineModule other) {
        return incompatibleWith.contains(other.type())
                || other.incompatibleWith().contains(type);
    }
}
