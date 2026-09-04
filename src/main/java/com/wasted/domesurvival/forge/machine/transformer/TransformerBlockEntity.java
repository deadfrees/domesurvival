package com.wasted.domesurvival.forge.machine.transformer;

import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TransformerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 100_000;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_MODE = 2;
    public static final int DATA_INPUT_RATE = 3;
    public static final int DATA_OUTPUT_RATE = 4;
    public static final int DATA_INPUT_THIS_TICK = 5;
    public static final int DATA_OUTPUT_THIS_TICK = 6;
    public static final int DATA_COUNT = 7;

    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_MODE = "Mode";

    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, 0, 0);

    private TransformerMode mode = TransformerMode.LV_TO_MV;
    private long budgetGameTime = Long.MIN_VALUE;
    private int receivedThisTick;
    private int extractedThisTick;

    private final IEnergyStorage inputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return receiveLimited(maxReceive, simulate);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IEnergyStorage outputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return extractLimited(maxExtract, simulate);
        }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private final IEnergyStorage combinedView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return receiveLimited(maxReceive, simulate);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return extractLimited(maxExtract, simulate);
        }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> inputCapability = LazyOptional.of(() -> inputView);
    private LazyOptional<IEnergyStorage> outputCapability = LazyOptional.of(() -> outputView);
    private LazyOptional<IEnergyStorage> combinedCapability = LazyOptional.of(() -> combinedView);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            refreshBudgets();
            return switch (index) {
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_CAPACITY -> energyStorage.getMaxEnergyStored();
                case DATA_MODE -> mode.ordinal();
                case DATA_INPUT_RATE -> mode.inputRate();
                case DATA_OUTPUT_RATE -> mode.outputRate();
                case DATA_INPUT_THIS_TICK -> receivedThisTick;
                case DATA_OUTPUT_THIS_TICK -> extractedThisTick;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(TransformerRegistry.TRANSFORMER_BLOCK_ENTITY.get(), pos, state);
    }

    private int receiveLimited(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) return 0;
        refreshBudgets();
        int remainingRate = Math.max(0, mode.inputRate() - receivedThisTick);
        int freeSpace = Math.max(0, energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored());
        int accepted = Math.min(maxReceive, Math.min(remainingRate, freeSpace));
        if (!simulate && accepted > 0) {
            int stored = energyStorage.addEnergyInternal(accepted);
            receivedThisTick += stored;
            if (stored > 0) setChanged();
            return stored;
        }
        return accepted;
    }

    private int extractLimited(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) return 0;
        refreshBudgets();
        int remainingRate = Math.max(0, mode.outputRate() - extractedThisTick);
        int available = energyStorage.getEnergyStored();
        int extracted = Math.min(maxExtract, Math.min(remainingRate, available));
        if (!simulate && extracted > 0) {
            int removed = energyStorage.removeEnergyInternal(extracted);
            extractedThisTick += removed;
            if (removed > 0) setChanged();
            return removed;
        }
        return extracted;
    }

    private void refreshBudgets() {
        long gameTime = level == null ? 0L : level.getGameTime();
        if (budgetGameTime != gameTime) {
            budgetGameTime = gameTime;
            receivedThisTick = 0;
            extractedThisTick = 0;
        }
    }

    public TransformerMode cycleMode() {
        mode = mode.next();
        receivedThisTick = 0;
        extractedThisTick = 0;
        setChanged();
        return mode;
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(TransformerBlock.FACING)
                ? state.getValue(TransformerBlock.FACING)
                : Direction.NORTH;
    }

    public Direction getInputSide() {
        return getMachineFacing().getOpposite();
    }

    public Direction getOutputSide() {
        return getMachineFacing();
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(NBT_MODE, mode.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        mode = TransformerMode.fromOrdinal(tag.getInt(NBT_MODE));
        budgetGameTime = Long.MIN_VALUE;
        receivedThisTick = 0;
        extractedThisTick = 0;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            if (side == null) return combinedCapability.cast();
            if (side == getInputSide()) return inputCapability.cast();
            if (side == getOutputSide()) return outputCapability.cast();
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCapability.invalidate();
        outputCapability.invalidate();
        combinedCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inputCapability = LazyOptional.of(() -> inputView);
        outputCapability = LazyOptional.of(() -> outputView);
        combinedCapability = LazyOptional.of(() -> combinedView);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Силовой трансформатор");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TransformerMenu(containerId, playerInventory, this);
    }
}
