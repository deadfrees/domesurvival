package com.wasted.domesurvival.forge.machine.water;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.WaterFilterItem;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import com.wasted.domesurvival.forge.sound.MachineAmbientSoundService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WaterPurifierBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_ENERGY_INPUT_PER_TICK = 200;
    /** Fallback values used only while no cartridge is installed. */
    public static final int ENERGY_PER_TICK = ModItems.BASIC_FILTER_ENERGY_PER_TICK;
    public static final int PROCESS_TICKS = ModItems.BASIC_FILTER_PROCESS_TICKS;
    public static final int RAW_TANK_CAPACITY = 4_000;
    public static final int PURIFIED_TANK_CAPACITY = 4_000;
    public static final int RAW_WATER_PER_CYCLE = 250;
    public static final int PURIFIED_WATER_PER_CYCLE = 200;
    private static final int MAX_FLUID_OUTPUT_PER_TICK = 100;

    public static final int SLOT_WATER_BUCKET = 0;
    public static final int SLOT_FILTER = 1;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_RAW_WATER = 2;
    public static final int DATA_RAW_CAPACITY = 3;
    public static final int DATA_PURIFIED_WATER = 4;
    public static final int DATA_PURIFIED_CAPACITY = 5;
    public static final int DATA_PROGRESS = 6;
    public static final int DATA_PROGRESS_MAX = 7;
    public static final int DATA_STATUS = 8;
    public static final int DATA_SIDES_START = 9;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_NO_WATER = 2;
    public static final int STATUS_NO_FILTER = 3;
    public static final int STATUS_NO_ENERGY = 4;
    public static final int STATUS_OUTPUT_FULL = 5;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_RAW_TANK = "RawTank";
    private static final String NBT_PURIFIED_TANK = "PurifiedTank";
    private static final String NBT_PROGRESS = "Progress";

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_WATER_BUCKET -> stack.is(Items.WATER_BUCKET);
                case SLOT_FILTER -> stack.getItem() instanceof WaterFilterItem;
                default -> false;
            };
        }
        @Override protected void onContentsChanged(int slot) {
            if (slot == SLOT_FILTER) {
                WaterPurifierBlockEntity.this.progress = 0;
            }
            setChanged();
        }
    };

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT_PER_TICK, 0);

    private final FluidTank rawWaterTank = new FluidTank(RAW_TANK_CAPACITY, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final FluidTank purifiedWaterTank = new FluidTank(PURIFIED_TANK_CAPACITY, stack -> stack.getFluid().isSame(ModFluids.PURIFIED_WATER.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { int accepted = energyStorage.receiveEnergy(maxReceive, simulate); if (!simulate && accepted > 0) setChanged(); return accepted; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IFluidHandler rawFluidInputView = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return rawWaterTank.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return RAW_TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return rawWaterTank.isFluidValid(0, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return rawWaterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    private final IFluidHandler purifiedFluidOutputView = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return purifiedWaterTank.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return PURIFIED_TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return purifiedWaterTank.drain(resource, action); }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return purifiedWaterTank.drain(maxDrain, action); }
    };

    private final IFluidHandler combinedFluidView = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return tank == 0 ? rawWaterTank.getFluidInTank(0) : purifiedWaterTank.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? RAW_TANK_CAPACITY : PURIFIED_TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return tank == 0 && rawWaterTank.isFluidValid(0, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return rawWaterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return purifiedWaterTank.drain(resource, action); }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return purifiedWaterTank.drain(maxDrain, action); }
    };

    private final IItemHandler itemInputView = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return inventory.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return inventory.isItemValid(slot, stack); }
    };

    private final IItemHandler itemOutputView = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_WATER_BUCKET || !inventory.getStackInSlot(slot).is(Items.BUCKET)) return ItemStack.EMPTY;
            return inventory.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    private final IItemHandler combinedItemView = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return itemInputView.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return itemOutputView.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return inventory.isItemValid(slot, stack); }
    };

    private LazyOptional<IEnergyStorage> energyInputCapability = LazyOptional.of(() -> energyInputView);
    private LazyOptional<IFluidHandler> rawFluidInputCapability = LazyOptional.of(() -> rawFluidInputView);
    private LazyOptional<IFluidHandler> purifiedFluidOutputCapability = LazyOptional.of(() -> purifiedFluidOutputView);
    private LazyOptional<IFluidHandler> combinedFluidCapability = LazyOptional.of(() -> combinedFluidView);
    private LazyOptional<IItemHandler> itemInputCapability = LazyOptional.of(() -> itemInputView);
    private LazyOptional<IItemHandler> itemOutputCapability = LazyOptional.of(() -> itemOutputView);
    private LazyOptional<IItemHandler> combinedItemCapability = LazyOptional.of(() -> combinedItemView);

    private int progress;
    private int status = STATUS_IDLE;
    private int ambientSoundTick;

    private final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int index) {
            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_CAPACITY) return energyStorage.getMaxEnergyStored();
            if (index == DATA_RAW_WATER) return rawWaterTank.getFluidAmount();
            if (index == DATA_RAW_CAPACITY) return rawWaterTank.getCapacity();
            if (index == DATA_PURIFIED_WATER) return purifiedWaterTank.getFluidAmount();
            if (index == DATA_PURIFIED_CAPACITY) return purifiedWaterTank.getCapacity();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_PROGRESS_MAX) return currentProcessTicks();
            if (index == DATA_STATUS) return status;
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) return sideConfig.getMode(Direction.values()[index - DATA_SIDES_START]).ordinal();
            return 0;
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public WaterPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PURIFIER.get(), pos, state);
        applyDefaultSideConfiguration();
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        for (RelativeSide relative : RelativeSide.values()) {
            if (relative == RelativeSide.FRONT) {
                sideConfig.setMode(relative.resolve(facing), SideMode.DISABLED);
            } else {
                sideConfig.setMode(relative.resolve(facing), SideMode.BOTH);
            }
        }
    }

    public static boolean isConfigurableSide(RelativeSide side) { return side != RelativeSide.FRONT; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterPurifierBlockEntity purifier) {
        purifier.syncAllPortStates();
        boolean changed = purifier.consumeWaterBucketIfPossible();
        int newStatus = purifier.calculateStatus();

        if (newStatus == STATUS_RUNNING) {
            int energyPerTick = purifier.currentEnergyPerTick();
            int processTicks = purifier.currentProcessTicks();
            int removed = purifier.energyStorage.removeEnergyInternal(energyPerTick);
            if (removed == energyPerTick) {
                purifier.progress++;
                changed = true;
                if (purifier.progress >= processTicks) {
                    purifier.finishCycle();
                    purifier.progress = 0;
                }
            }
        } else if (purifier.progress != 0 && newStatus != STATUS_NO_ENERGY) {
            purifier.progress = 0;
            changed = true;
        }

        changed |= purifier.pushPurifiedWaterToNeighbors();

        purifier.status = purifier.calculateStatus();
        boolean shouldBeLit = purifier.status == STATUS_RUNNING;
        purifier.ambientSoundTick = MachineAmbientSoundService.tick(
                level, pos, shouldBeLit, purifier.ambientSoundTick,
                MachineAmbientSoundService.MachineType.WATER_PURIFIER
        );
        if (state.getValue(WaterPurifierBlock.LIT) != shouldBeLit) {
            level.setBlock(pos, state.setValue(WaterPurifierBlock.LIT, shouldBeLit), 3);
            changed = true;
        }
        if (changed) purifier.setChanged();
    }

    private boolean pushPurifiedWaterToNeighbors() {
        if (level == null || level.isClientSide || purifiedWaterTank.isEmpty()) return false;
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            if (isFrontWorldSide(direction)) continue;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) continue;
            LazyOptional<IFluidHandler> opt = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite());
            if (!opt.isPresent()) continue;
            IFluidHandler handler = opt.orElse(null);
            if (handler == null) continue;
            FluidStack preview = purifiedWaterTank.drain(MAX_FLUID_OUTPUT_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
            if (preview.isEmpty()) continue;
            int accepted = handler.fill(preview, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack extracted = purifiedWaterTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            if (extracted.isEmpty()) continue;
            int filled = handler.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
            if (filled < extracted.getAmount()) {
                purifiedWaterTank.fill(new FluidStack(extracted.getFluid(), extracted.getAmount() - filled), IFluidHandler.FluidAction.EXECUTE);
            }
            changed = true;
            if (purifiedWaterTank.isEmpty()) break;
        }
        return changed;
    }

    private int calculateStatus() {
        if (!hasUsableFilter()) return STATUS_NO_FILTER;
        if (rawWaterTank.getFluidAmount() < RAW_WATER_PER_CYCLE) return STATUS_NO_WATER;
        if (purifiedWaterTank.getCapacity() - purifiedWaterTank.getFluidAmount() < PURIFIED_WATER_PER_CYCLE) return STATUS_OUTPUT_FULL;
        if (energyStorage.getEnergyStored() < currentEnergyPerTick()) return STATUS_NO_ENERGY;
        return STATUS_RUNNING;
    }

    private boolean consumeWaterBucketIfPossible() {
        ItemStack waterBucket = inventory.getStackInSlot(SLOT_WATER_BUCKET);
        if (!waterBucket.is(Items.WATER_BUCKET)) return false;
        int accepted = rawWaterTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE);
        if (accepted < 1000) return false;
        rawWaterTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        inventory.setStackInSlot(SLOT_WATER_BUCKET, new ItemStack(Items.BUCKET));
        return true;
    }

    private boolean hasUsableFilter() {
        ItemStack filter = inventory.getStackInSlot(SLOT_FILTER);
        return filter.getItem() instanceof WaterFilterItem
                && filter.getDamageValue() < filter.getMaxDamage();
    }

    private WaterFilterItem currentFilterItem() {
        ItemStack filter = inventory.getStackInSlot(SLOT_FILTER);
        return filter.getItem() instanceof WaterFilterItem item ? item : null;
    }

    private int currentProcessTicks() {
        WaterFilterItem filter = currentFilterItem();
        return filter != null ? filter.processTicks() : PROCESS_TICKS;
    }

    private int currentEnergyPerTick() {
        WaterFilterItem filter = currentFilterItem();
        return filter != null ? filter.energyPerTick() : ENERGY_PER_TICK;
    }

    private void finishCycle() {
        rawWaterTank.drain(RAW_WATER_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
        purifiedWaterTank.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(), PURIFIED_WATER_PER_CYCLE), IFluidHandler.FluidAction.EXECUTE);
        damageFilter();
    }

    private void damageFilter() {
        ItemStack filter = inventory.getStackInSlot(SLOT_FILTER);
        if (filter.isEmpty()) return;
        int nextDamage = filter.getDamageValue() + 1;
        if (nextDamage >= filter.getMaxDamage()) inventory.setStackInSlot(SLOT_FILTER, ItemStack.EMPTY);
        else { filter.setDamageValue(nextDamage); inventory.setStackInSlot(SLOT_FILTER, filter); }
    }

    public ItemStackHandler getInventory() { return inventory; }
    public ContainerData getDataAccess() { return dataAccess; }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction worldSide = relativeSide.resolve(getMachineFacing());
        SideMode mode = sideConfig.cycleMode(worldSide);
        refreshCapabilities(); syncPortState(worldSide); setChanged(); return mode;
    }

    private boolean isFrontWorldSide(Direction side) { return side == getMachineFacing(); }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof WaterPurifierBlock)) return;
        PortVisual visual = isFrontWorldSide(direction) ? PortVisual.OFF : PortVisual.fromMode(sideConfig.getMode(direction));
        var property = WaterPurifierBlock.portProperty(direction);
        if (state.getValue(property) != visual) level.setBlock(worldPosition, state.setValue(property, visual), 3);
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof WaterPurifierBlock)) return;
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction) ? PortVisual.OFF : PortVisual.fromMode(sideConfig.getMode(direction));
            updated = updated.setValue(WaterPurifierBlock.portProperty(direction), visual);
        }
        if (!updated.equals(state)) level.setBlock(worldPosition, updated, 3);
    }

    @Override public void onLoad() { super.onLoad(); syncAllPortStates(); }
    public Direction getMachineFacing() { BlockState state = getBlockState(); return state.hasProperty(WaterPurifierBlock.FACING) ? state.getValue(WaterPurifierBlock.FACING) : Direction.NORTH; }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.put(NBT_RAW_TANK, rawWaterTank.writeToNBT(new CompoundTag()));
        tag.put(NBT_PURIFIED_TANK, purifiedWaterTank.writeToNBT(new CompoundTag()));
        tag.putInt(NBT_PROGRESS, progress);
        sideConfig.save(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        rawWaterTank.readFromNBT(tag.getCompound(NBT_RAW_TANK));
        purifiedWaterTank.readFromNBT(tag.getCompound(NBT_PURIFIED_TANK));
        progress = Math.max(0, Math.min(currentProcessTicks() - 1, tag.getInt(NBT_PROGRESS)));
        if (!sideConfig.load(tag)) applyDefaultSideConfiguration();
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);
        status = calculateStatus();
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            // Machine cables may connect to every non-front face. The unified side
            // panel remains a visual/routing preference, but never hides FE capability.
            if (side == null || !isFrontWorldSide(side)) return energyInputCapability.cast();
            return LazyOptional.empty();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // Expose one deterministic duplex fluid view on every non-front face:
            // fill -> raw water tank, drain -> purified water tank. This avoids
            // pipe mods failing to connect because a saved side mode was OUTPUT.
            if (side == null || !isFrontWorldSide(side)) return combinedFluidCapability.cast();
            return LazyOptional.empty();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) return combinedItemCapability.cast();
            if (isFrontWorldSide(side)) return LazyOptional.empty();
            boolean input = sideConfig.allowsInput(side);
            boolean output = sideConfig.allowsOutput(side);
            if (input && output) return combinedItemCapability.cast();
            if (input) return itemInputCapability.cast();
            if (output) return itemOutputCapability.cast();
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    private void refreshCapabilities() {
        energyInputCapability.invalidate(); rawFluidInputCapability.invalidate(); purifiedFluidOutputCapability.invalidate(); combinedFluidCapability.invalidate(); itemInputCapability.invalidate(); itemOutputCapability.invalidate(); combinedItemCapability.invalidate();
        energyInputCapability = LazyOptional.of(() -> energyInputView);
        rawFluidInputCapability = LazyOptional.of(() -> rawFluidInputView);
        purifiedFluidOutputCapability = LazyOptional.of(() -> purifiedFluidOutputView);
        combinedFluidCapability = LazyOptional.of(() -> combinedFluidView);
        itemInputCapability = LazyOptional.of(() -> itemInputView);
        itemOutputCapability = LazyOptional.of(() -> itemOutputView);
        combinedItemCapability = LazyOptional.of(() -> combinedItemView);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); energyInputCapability.invalidate(); rawFluidInputCapability.invalidate(); purifiedFluidOutputCapability.invalidate(); combinedFluidCapability.invalidate(); itemInputCapability.invalidate(); itemOutputCapability.invalidate(); combinedItemCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); refreshCapabilities(); }
    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.water_purifier"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) { return new WaterPurifierMenu(containerId, playerInventory, this); }
}
