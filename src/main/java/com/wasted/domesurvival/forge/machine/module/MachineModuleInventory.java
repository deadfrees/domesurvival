package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Small validated inventory for machine upgrades. It does not own gameplay effects;
 * machines read the installed logical modules and apply their own balanced modifiers.
 */
public final class MachineModuleInventory extends ItemStackHandler {
    private final IModularMachine machine;
    private final MachineModuleResolver resolver;

    public MachineModuleInventory(IModularMachine machine, MachineModuleResolver resolver) {
        super(Math.max(0, machine.moduleSlotCount()));
        this.machine = machine;
        this.resolver = resolver;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        MachineModule candidate = resolver.resolve(stack);
        if (candidate == null || !machine.allowsModule(candidate)) {
            return false;
        }

        for (int i = 0; i < getSlots(); i++) {
            if (i == slot) {
                continue;
            }
            ItemStack installedStack = getStackInSlot(i);
            if (installedStack.isEmpty()) {
                continue;
            }
            MachineModule installed = resolver.resolve(installedStack);
            if (installed != null && !machine.allowsCombination(candidate, installed)) {
                return false;
            }
        }
        return true;
    }
}
