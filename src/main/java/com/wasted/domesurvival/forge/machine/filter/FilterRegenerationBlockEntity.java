package com.wasted.domesurvival.forge.machine.filter;

import com.wasted.domesurvival.forge.item.WaterFilterItem;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexFilters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FilterRegenerationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_INPUT_PER_TICK = 128;
    public static final int ENERGY_PER_TICK = 20;
    public static final int PROCESSING_TICKS = 200;
    public static final int MAX_REGENERATION_CYCLES = 8;

    public static final int STATUS_READY = 0;
    public static final int STATUS_REGENERATING = 1;
    public static final int STATUS_NO_ENERGY = 2;
    public static final int STATUS_NO_FILTER = 3;
    public static final int STATUS_NO_MEDIA = 4;
    public static final int STATUS_FILTER_HEALTHY = 5;
    public static final int STATUS_EXHAUSTED = 6;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_MAX_PROGRESS = 3;
    public static final int DATA_STATUS = 4;
    public static final int DATA_REGEN_CYCLES = 5;
    public static final int DATA_MAX_REGEN_CYCLES = 6;
    public static final int DATA_COUNT = 7;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_PROGRESS = "Progress";
    private static final String FILTER_NBT_REGEN_CYCLES = "DomeRegenCycles";

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> isEligibleFilter(stack);
                case 1 -> isRegenerationMedia(stack);
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0 && progress != 0) progress = 0;
            setChanged();
        }
    };

    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, MAX_INPUT_PER_TICK, 0);

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = energyStorage.receiveEnergy(maxReceive, simulate);
            if (!simulate && accepted > 0) setChanged();
            return accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyInputView);

    private int progress;
    private boolean processingThisTick;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            ItemStack filter = inventory.getStackInSlot(0);
            return switch (index) {
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_CAPACITY -> energyStorage.getMaxEnergyStored();
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> PROCESSING_TICKS;
                case DATA_STATUS -> getStatus(filter);
                case DATA_REGEN_CYCLES -> getRegenerationCycles(filter);
                case DATA_MAX_REGEN_CYCLES -> MAX_REGENERATION_CYCLES;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public FilterRegenerationBlockEntity(BlockPos pos, BlockState state) {
        super(FilterRegenerationRegistry.FILTER_REGENERATION_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  FilterRegenerationBlockEntity station) {
        station.processingThisTick = false;
        boolean changed = station.tickRegeneration();

        if (state.getValue(FilterRegenerationBlock.ACTIVE) != station.processingThisTick) {
            level.setBlock(pos, state.setValue(FilterRegenerationBlock.ACTIVE, station.processingThisTick), 3);
            changed = true;
        }

        if (changed) station.setChanged();
    }

    private boolean tickRegeneration() {
        ItemStack filter = inventory.getStackInSlot(0);
        if (!canRegenerateFilter(filter)) {
            return resetProgressIfNeeded();
        }

        if (!isRegenerationMedia(inventory.getStackInSlot(1))) {
            return false;
        }

        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) {
            return false;
        }

        energyStorage.removeEnergyInternal(ENERGY_PER_TICK);
        progress++;
        processingThisTick = true;

        if (progress >= PROCESSING_TICKS) {
            finishRegeneration();
            progress = 0;
        }
        return true;
    }

    private boolean resetProgressIfNeeded() {
        if (progress == 0) return false;
        progress = 0;
        return true;
    }

    private void finishRegeneration() {
        ItemStack current = inventory.getStackInSlot(0);
        if (!canRegenerateFilter(current) || !isRegenerationMedia(inventory.getStackInSlot(1))) return;

        ItemStack repaired = current.copy();
        int maxDamage = Math.max(1, repaired.getMaxDamage());
        int repairAmount = Math.max(1, maxDamage / 4);
        repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - repairAmount));
        setRegenerationCycles(repaired, getRegenerationCycles(repaired) + 1);
        inventory.setStackInSlot(0, repaired);

        ItemStack media = inventory.getStackInSlot(1).copy();
        media.shrink(1);
        inventory.setStackInSlot(1, media);
    }

    private int getStatus(ItemStack filter) {
        if (!isEligibleFilter(filter)) return STATUS_NO_FILTER;
        if (filter.getDamageValue() <= 0) return STATUS_FILTER_HEALTHY;
        if (getRegenerationCycles(filter) >= MAX_REGENERATION_CYCLES) return STATUS_EXHAUSTED;
        if (!isRegenerationMedia(inventory.getStackInSlot(1))) return STATUS_NO_MEDIA;
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return STATUS_NO_ENERGY;
        return progress > 0 ? STATUS_REGENERATING : STATUS_READY;
    }

    public static boolean isEligibleFilter(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isDamageableItem()
                && (stack.getItem() instanceof WaterFilterItem || OxygenComplexFilters.isAirFilter(stack));
    }

    public static boolean isRegenerationMedia(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.CHARCOAL);
    }

    public static boolean canRegenerateFilter(ItemStack stack) {
        return isEligibleFilter(stack)
                && stack.getDamageValue() > 0
                && getRegenerationCycles(stack) < MAX_REGENERATION_CYCLES;
    }

    public static int getRegenerationCycles(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return 0;
        return Math.max(0, stack.getTag().getInt(FILTER_NBT_REGEN_CYCLES));
    }

    private static void setRegenerationCycles(ItemStack stack, int cycles) {
        stack.getOrCreateTag().putInt(
                FILTER_NBT_REGEN_CYCLES,
                Math.max(0, Math.min(MAX_REGENERATION_CYCLES, cycles))
        );
    }

    public ItemStackHandler getInventory() { return inventory; }
    public ContainerData getDataAccess() { return dataAccess; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(NBT_PROGRESS, progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        progress = Math.max(0, Math.min(PROCESSING_TICKS - 1, tag.getInt(NBT_PROGRESS)));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> inventory);
        energyCapability = LazyOptional.of(() -> energyInputView);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Станция регенерации фильтров");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FilterRegenerationMenu(containerId, playerInventory, this);
    }
}
