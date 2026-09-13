package com.wasted.domesurvival.forge.machine.side;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/** Strict one-way capability adapters used by machine connector faces. */
public final class CapabilityViews {
    public static IItemHandler inputItems(IItemHandler delegate) {
        return new IItemHandler() {
            @Override public int getSlots() { return delegate.getSlots(); }
            @Override public @NotNull ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(slot); }
            @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return delegate.insertItem(slot, stack, simulate);
            }
            @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return delegate.isItemValid(slot, stack);
            }
        };
    }

    public static IItemHandler outputItems(IItemHandler delegate) {
        return new IItemHandler() {
            @Override public int getSlots() { return delegate.getSlots(); }
            @Override public @NotNull ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(slot); }
            @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return stack;
            }
            @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return delegate.extractItem(slot, amount, simulate);
            }
            @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
        };
    }

    public static IFluidHandler inputFluid(IFluidHandler delegate) {
        return new IFluidHandler() {
            @Override public int getTanks() { return delegate.getTanks(); }
            @Override public @NotNull FluidStack getFluidInTank(int tank) { return delegate.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return delegate.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                return delegate.isFluidValid(tank, stack);
            }
            @Override public int fill(FluidStack resource, FluidAction action) {
                return delegate.fill(resource, action);
            }
            @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }
            @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    public static IFluidHandler outputFluid(IFluidHandler delegate) {
        return new IFluidHandler() {
            @Override public int getTanks() { return delegate.getTanks(); }
            @Override public @NotNull FluidStack getFluidInTank(int tank) { return delegate.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return delegate.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return false; }
            @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
            @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                return delegate.drain(resource, action);
            }
            @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                return delegate.drain(maxDrain, action);
            }
        };
    }

    private CapabilityViews() {
    }
}
