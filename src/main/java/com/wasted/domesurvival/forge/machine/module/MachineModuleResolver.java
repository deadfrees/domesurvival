package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface MachineModuleResolver {
    MachineModuleResolver STANDARD = stack -> {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.getItem() instanceof MachineModuleItem moduleItem ? moduleItem.module() : null;
    };

    @Nullable
    MachineModule resolve(ItemStack stack);
}
