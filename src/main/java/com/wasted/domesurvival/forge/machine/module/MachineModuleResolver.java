package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface MachineModuleResolver {
    @Nullable
    MachineModule resolve(ItemStack stack);
}
