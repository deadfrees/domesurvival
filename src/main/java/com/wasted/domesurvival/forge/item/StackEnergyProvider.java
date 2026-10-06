package com.wasted.domesurvival.forge.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight Forge Energy provider backed directly by the owning ItemStack NBT.
 * The energy value therefore follows ordinary stack copies, container sync and saves
 * without a second serialization channel.
 */
public final class StackEnergyProvider implements ICapabilityProvider {
    public static final String ENERGY_KEY = "domesurvival_energy";

    private final LazyOptional<IEnergyStorage> energy;

    public StackEnergyProvider(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
        this.energy = LazyOptional.of(() -> new StackEnergyStorage(
                stack,
                Math.max(1, capacity),
                Math.max(0, maxReceive),
                Math.max(0, maxExtract)
        ));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
    }

    public static int getStored(ItemStack stack, int capacity) {
        if (stack.isEmpty() || capacity <= 0) return 0;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ENERGY_KEY)) return 0;
        return Math.max(0, Math.min(capacity, tag.getInt(ENERGY_KEY)));
    }

    public static int consumeInternal(ItemStack stack, int capacity, int amount, boolean simulate) {
        if (stack.isEmpty() || capacity <= 0 || amount <= 0) return 0;
        int stored = getStored(stack, capacity);
        int extracted = Math.min(stored, amount);
        if (!simulate && extracted > 0) {
            setStored(stack, capacity, stored - extracted);
        }
        return extracted;
    }

    private static void setStored(ItemStack stack, int capacity, int amount) {
        stack.getOrCreateTag().putInt(ENERGY_KEY, Math.max(0, Math.min(capacity, amount)));
    }

    private static final class StackEnergyStorage implements IEnergyStorage {
        private final ItemStack stack;
        private final int capacity;
        private final int maxReceive;
        private final int maxExtract;

        private StackEnergyStorage(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
            this.stack = stack;
            this.capacity = capacity;
            this.maxReceive = maxReceive;
            this.maxExtract = maxExtract;
        }

        @Override
        public int receiveEnergy(int requested, boolean simulate) {
            if (requested <= 0 || maxReceive <= 0) return 0;
            int stored = getEnergyStored();
            int accepted = Math.min(Math.min(requested, maxReceive), capacity - stored);
            if (!simulate && accepted > 0) setEnergy(stored + accepted);
            return Math.max(0, accepted);
        }

        @Override
        public int extractEnergy(int requested, boolean simulate) {
            if (requested <= 0 || maxExtract <= 0) return 0;
            int stored = getEnergyStored();
            int extracted = Math.min(Math.min(requested, maxExtract), stored);
            if (!simulate && extracted > 0) setEnergy(stored - extracted);
            return Math.max(0, extracted);
        }

        @Override
        public int getEnergyStored() {
            return StackEnergyProvider.getStored(stack, capacity);
        }

        @Override public int getMaxEnergyStored() { return capacity; }
        @Override public boolean canExtract() { return maxExtract > 0; }
        @Override public boolean canReceive() { return maxReceive > 0; }

        private void setEnergy(int amount) {
            StackEnergyProvider.setStored(stack, capacity, amount);
        }
    }
}
