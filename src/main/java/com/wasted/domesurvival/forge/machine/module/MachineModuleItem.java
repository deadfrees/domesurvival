package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.world.item.Item;

/**
 * Physical item wrapper for a logical machine module definition.
 */
public final class MachineModuleItem extends Item {
    private final MachineModule module;

    public MachineModuleItem(MachineModule module, Properties properties) {
        super(properties);
        if (module == null) {
            throw new IllegalArgumentException("module");
        }
        this.module = module;
    }

    public MachineModule module() {
        return module;
    }
}
