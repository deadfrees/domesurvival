package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Immutable logical description of a machine module.
 */
public record MachineModule(
        ResourceLocation id,
        MachineModuleType type,
        Set<MachineModuleType> incompatibleWith,
        MachineModuleModifiers modifiers
) {
    public MachineModule {
        if (id == null || type == null || modifiers == null) {
            throw new IllegalArgumentException("Machine module fields cannot be null");
        }
        incompatibleWith = Set.copyOf(incompatibleWith);
    }

    public boolean conflictsWith(MachineModule other) {
        return other != null && (
                incompatibleWith.contains(other.type())
                        || other.incompatibleWith().contains(type)
        );
    }
}
