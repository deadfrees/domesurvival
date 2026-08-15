package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.capability.IOxygenStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.fluid.ModFluids;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OxygenElectrolyzerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 30_000;
    public static final int MAX_ENERGY_INPUT_PER_TICK = 240;
    public static final int ENERGY_PER_TICK = 12;
    public static final int PROCESS_TICKS = 200;
    public static final int WATER_TANK_CAPACITY = 4_000;
    public static final int OXYGEN_CAPACITY = 4_000;
    public static final int WATER_PER_CYCLE = 200;
    public static final int OXYGEN_PER_CYCLE = 96;
    public static final int MAX_OXYGEN_OUTPUT_PER_TICK = 120;

    public static final int DATA_ENERGY = 0, DATA_ENERGY_CAPACITY = 1, DATA_WATER = 2, DATA_WATER_CAPACITY = 3,
            DATA_OXYGEN = 4, DATA_OXYGEN_CAPACITY = 5, DATA_PROGRESS = 6, DATA_PROGRESS_MAX = 7, DATA_STATUS = 8,
            DATA_SIDES_START = 9, DATA_COUNT = DATA_SIDES_START + 6;

    public static final int STATUS_IDLE = 0, STATUS_RUNNING = 1, STATUS_NO_WATER = 2, STATUS_NO_ENERGY = 3, STATUS_OUTPUT_FULL = 4;
    private static final String NBT_ENERGY = "Energy", NBT_WATER = "PurifiedWater", NBT_OXYGEN = "Oxygen", NBT_PROGRESS = "Progress";

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();
    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT_PER_TICK, 0);
    private final FluidTank waterTank = new FluidTank(WATER_TANK_CAPACITY, stack -> stack.getFluid().isSame(ModFluids.PURIFIED_WATER.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final OxygenStorage oxygenStorage = new OxygenStorage(OXYGEN_CAPACITY, 0, MAX_OXYGEN_OUTPUT_PER_TICK);

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { int accepted = energyStorage.receiveEnergy(maxReceive, simulate); if (!simulate && accepted > 0) setChanged(); return accepted; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };
    private final IFluidHandler fluidInputView = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return WATER_TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return waterTank.isFluidValid(0, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return waterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private final IOxygenStorage oxygenOutputView = new IOxygenStorage() {
        @Override public int receiveOxygen(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractOxygen(int maxExtract, boolean simulate) { int extracted = oxygenStorage.extractOxygen(maxExtract, simulate); if (!simulate && extracted > 0) setChanged(); return extracted; }
        @Override public int getOxygenStored() { return oxygenStorage.getOxygenStored(); }
        @Override public int getMaxOxygenStored() { return oxygenStorage.getMaxOxygenStored(); }
        @Override public boolean canReceive() { return false; }
        @Override public boolean canExtract() { return true; }
    };
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyInputView);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidInputView);
    private LazyOptional<IOxygenStorage> oxygenCapability = LazyOptional.of(() -> oxygenOutputView);
    private int progress, status = STATUS_IDLE;
    private int ambientSoundTick;

    private final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int index) {
            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_ENERGY_CAPACITY) return energyStorage.getMaxEnergyStored();
            if (index == DATA_WATER) return waterTank.getFluidAmount();
            if (index == DATA_WATER_CAPACITY) return waterTank.getCapacity();
            if (index == DATA_OXYGEN) return oxygenStorage.getOxygenStored();
            if (index == DATA_OXYGEN_CAPACITY) return oxygenStorage.getMaxOxygenStored();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_PROGRESS_MAX) return PROCESS_TICKS;
            if (index == DATA_STATUS) return status;
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) return sideConfig.getMode(Direction.values()[index - DATA_SIDES_START]).ordinal();
            return 0;
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public OxygenElectrolyzerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.OXYGEN_ELECTROLYZER.get(), pos, state); applyDefaultSideConfiguration(); }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        for (RelativeSide relative : RelativeSide.values()) {
            sideConfig.setMode(relative.resolve(facing), relative == RelativeSide.FRONT ? SideMode.DISABLED : SideMode.BOTH);
        }
    }

    public static boolean isConfigurableSide(RelativeSide side) { return side != RelativeSide.FRONT; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenElectrolyzerBlockEntity machine) {
        machine.syncAllPortStates();
        int newStatus = machine.calculateStatus();
        boolean changed = false;
        if (newStatus == STATUS_RUNNING) {
            int removed = machine.energyStorage.removeEnergyInternal(ENERGY_PER_TICK);
            if (removed == ENERGY_PER_TICK) {
                machine.progress++; changed = true;
                if (machine.progress >= PROCESS_TICKS) {
                    machine.waterTank.drain(WATER_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
                    machine.oxygenStorage.addInternal(OXYGEN_PER_CYCLE);
                    machine.progress = 0;
                }
            }
        } else if (machine.progress != 0 && newStatus != STATUS_NO_ENERGY) { machine.progress = 0; changed = true; }
        machine.status = machine.calculateStatus();
        boolean shouldBeLit = machine.status == STATUS_RUNNING;
        machine.ambientSoundTick = MachineAmbientSoundService.tick(
                level, pos, shouldBeLit, machine.ambientSoundTick,
                MachineAmbientSoundService.MachineType.OXYGEN_ELECTROLYZER
        );
        if (state.getValue(OxygenElectrolyzerBlock.LIT) != shouldBeLit) { level.setBlock(pos, state.setValue(OxygenElectrolyzerBlock.LIT, shouldBeLit), 3); changed = true; }
        if (changed) machine.setChanged();
    }

    private int calculateStatus() {
        if (waterTank.getFluidAmount() < WATER_PER_CYCLE) return STATUS_NO_WATER;
        if (oxygenStorage.getMaxOxygenStored() - oxygenStorage.getOxygenStored() < OXYGEN_PER_CYCLE) return STATUS_OUTPUT_FULL;
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return STATUS_NO_ENERGY;
        return STATUS_RUNNING;
    }

    public ContainerData getDataAccess() { return dataAccess; }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction worldSide = relativeSide.resolve(getMachineFacing());
        SideMode mode = sideConfig.cycleMode(worldSide); refreshCapabilities(); syncPortState(worldSide); setChanged(); return mode;
    }

    private boolean isFrontWorldSide(Direction side) { return side == getMachineFacing(); }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof OxygenElectrolyzerBlock)) return;
        PortVisual visual = isFrontWorldSide(direction) ? PortVisual.OFF : PortVisual.fromMode(sideConfig.getMode(direction));
        var property = OxygenElectrolyzerBlock.portProperty(direction);
        if (state.getValue(property) != visual) level.setBlock(worldPosition, state.setValue(property, visual), 3);
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof OxygenElectrolyzerBlock)) return;
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction) ? PortVisual.OFF : PortVisual.fromMode(sideConfig.getMode(direction));
            updated = updated.setValue(OxygenElectrolyzerBlock.portProperty(direction), visual);
        }
        if (!updated.equals(state)) level.setBlock(worldPosition, updated, 3);
    }

    @Override public void onLoad() { super.onLoad(); syncAllPortStates(); }
    public Direction getMachineFacing() { BlockState state = getBlockState(); return state.hasProperty(OxygenElectrolyzerBlock.FACING) ? state.getValue(OxygenElectrolyzerBlock.FACING) : Direction.NORTH; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored()); tag.put(NBT_WATER, waterTank.writeToNBT(new CompoundTag())); tag.putInt(NBT_OXYGEN, oxygenStorage.getOxygenStored()); tag.putInt(NBT_PROGRESS, progress); sideConfig.save(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY)); waterTank.readFromNBT(tag.getCompound(NBT_WATER)); oxygenStorage.setStoredInternal(tag.getInt(NBT_OXYGEN)); progress = Math.max(0, Math.min(PROCESS_TICKS - 1, tag.getInt(NBT_PROGRESS))); if (!sideConfig.load(tag)) applyDefaultSideConfiguration(); sideConfig.setMode(getMachineFacing(), SideMode.DISABLED); status = calculateStatus(); }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            if (side == null || !isFrontWorldSide(side)) return energyCapability.cast();
            return LazyOptional.empty();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == null || !isFrontWorldSide(side)) return fluidCapability.cast();
            return LazyOptional.empty();
        }
        if (cap == ModCapabilities.OXYGEN) {
            if (side == null || (!isFrontWorldSide(side) && sideConfig.allowsOutput(side))) return oxygenCapability.cast();
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }
    private void refreshCapabilities() { energyCapability.invalidate(); fluidCapability.invalidate(); oxygenCapability.invalidate(); energyCapability = LazyOptional.of(() -> energyInputView); fluidCapability = LazyOptional.of(() -> fluidInputView); oxygenCapability = LazyOptional.of(() -> oxygenOutputView); }
    @Override public void invalidateCaps() { super.invalidateCaps(); energyCapability.invalidate(); fluidCapability.invalidate(); oxygenCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); refreshCapabilities(); }
    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.oxygen_electrolyzer"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) { return new OxygenElectrolyzerMenu(containerId, playerInventory, this); }
}
