package com.wasted.domesurvival.forge.gas;

import com.wasted.domesurvival.forge.capability.IGasStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Small single-gas tank. A non-empty tank never accepts a different gas type,
 * so mixed-gas states cannot be created by automation.
 */
public final class GasStorage implements IGasStorage {
    private static final String NBT_GAS = "Gas";
    private static final String NBT_AMOUNT = "Amount";

    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;
    private final Predicate<ResourceLocation> validator;
    private final Runnable onChanged;

    @Nullable private ResourceLocation gasType;
    private int amount;

    public GasStorage(int capacity, int maxReceive, int maxExtract,
                      Predicate<ResourceLocation> validator, Runnable onChanged) {
        this.capacity = Math.max(0, capacity);
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
        this.validator = Objects.requireNonNull(validator, "validator");
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
    }

    @Override
    public int receiveGas(ResourceLocation gas, int requested, boolean simulate) {
        if (gas == null || requested <= 0 || !canReceiveGas(gas)) return 0;
        int limit = Math.min(maxReceive, requested);
        int accepted = Math.min(limit, capacity - amount);
        if (accepted <= 0) return 0;
        if (!simulate) {
            if (amount == 0) gasType = gas;
            amount += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public int extractGas(ResourceLocation gas, int requested, boolean simulate) {
        if (gas == null || requested <= 0 || !canExtractGas(gas)) return 0;
        int extracted = Math.min(Math.min(maxExtract, requested), amount);
        if (extracted <= 0) return 0;
        if (!simulate) {
            amount -= extracted;
            if (amount == 0) gasType = null;
            onChanged.run();
        }
        return extracted;
    }

    /** Internal machine production bypasses the external receive-rate policy. */
    public int addInternal(ResourceLocation gas, int requested, boolean simulate) {
        if (gas == null || requested <= 0 || !validator.test(gas)) return 0;
        if (amount > 0 && !gas.equals(gasType)) return 0;
        int accepted = Math.min(requested, capacity - amount);
        if (accepted <= 0) return 0;
        if (!simulate) {
            if (amount == 0) gasType = gas;
            amount += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public @Nullable ResourceLocation getGasType() {
        return gasType;
    }

    @Override public int getGasStored() { return amount; }
    @Override public int getMaxGasStored() { return capacity; }

    @Override
    public boolean canReceiveGas(ResourceLocation gas) {
        return maxReceive > 0
                && validator.test(gas)
                && (amount == 0 || gas.equals(gasType));
    }

    @Override
    public boolean canExtractGas(ResourceLocation gas) {
        return maxExtract > 0 && amount > 0 && gas.equals(gasType);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (gasType != null && amount > 0) {
            tag.putString(NBT_GAS, gasType.toString());
            tag.putInt(NBT_AMOUNT, amount);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        gasType = null;
        amount = 0;
        if (!tag.contains(NBT_GAS) || !tag.contains(NBT_AMOUNT)) return;
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(NBT_GAS));
        int stored = Math.max(0, Math.min(capacity, tag.getInt(NBT_AMOUNT)));
        if (parsed != null && stored > 0 && validator.test(parsed)) {
            gasType = parsed;
            amount = stored;
        }
    }
}
