package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Small validated inventory for machine upgrades.
 *
 * <p>Effects are cached and rebuilt only when the inventory changes, so machines do
 * not need to resolve module ItemStacks every tick.</p>
 */
public final class MachineModuleInventory extends ItemStackHandler {
    private final IModularMachine machine;
    private final MachineModuleResolver resolver;
    private final Runnable changeListener;

    private MachineModuleModifiers cachedModifiers = MachineModuleModifiers.IDENTITY;
    private boolean configurationValid = true;

    public MachineModuleInventory(IModularMachine machine, MachineModuleResolver resolver) {
        this(machine, resolver, () -> { });
    }

    public MachineModuleInventory(
            IModularMachine machine,
            MachineModuleResolver resolver,
            Runnable changeListener
    ) {
        super(Math.max(0, machine.moduleSlotCount()));
        this.machine = machine;
        this.resolver = resolver;
        this.changeListener = changeListener == null ? () -> { } : changeListener;
        rebuildCachedState();
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

        int sameType = 0;
        for (int i = 0; i < getSlots(); i++) {
            if (i == slot) {
                continue;
            }
            ItemStack installedStack = getStackInSlot(i);
            if (installedStack.isEmpty()) {
                continue;
            }
            MachineModule installed = resolver.resolve(installedStack);
            if (installed == null) {
                continue;
            }
            if (installed.type() == candidate.type()) {
                sameType++;
            }
            if (!machine.allowsCombination(candidate, installed)) {
                return false;
            }
        }

        return sameType < machine.maxModulesOfType(candidate.type());
    }

    public MachineModuleModifiers modifiers() {
        return cachedModifiers;
    }

    public boolean isConfigurationValid() {
        return configurationValid;
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        rebuildCachedState();
        changeListener.run();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        rebuildCachedState();
    }

    private void rebuildCachedState() {
        MachineModuleModifiers result = MachineModuleModifiers.IDENTITY;
        boolean valid = true;
        Map<MachineModuleType, Integer> counts = new EnumMap<>(MachineModuleType.class);
        List<MachineModule> accepted = new ArrayList<>(getSlots());

        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            MachineModule module = resolver.resolve(stack);
            if (module == null || !machine.allowsModule(module)) {
                valid = false;
                continue;
            }

            int count = counts.getOrDefault(module.type(), 0);
            if (count >= machine.maxModulesOfType(module.type())) {
                valid = false;
                continue;
            }

            boolean conflicts = false;
            for (MachineModule installed : accepted) {
                if (!machine.allowsCombination(module, installed)) {
                    conflicts = true;
                    break;
                }
            }
            if (conflicts) {
                valid = false;
                continue;
            }

            counts.put(module.type(), count + 1);
            accepted.add(module);
            result = result.combine(module.modifiers());
        }

        cachedModifiers = result;
        configurationValid = valid;
    }
}
