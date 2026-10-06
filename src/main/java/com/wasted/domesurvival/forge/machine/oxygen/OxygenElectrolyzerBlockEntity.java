package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.module.*;
import java.util.*;
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

public final class OxygenElectrolyzerBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, IModularMachine {
    public static final int ENERGY_CAPACITY = 30_000;
    public static final int MAX_ENERGY_INPUT_PER_TICK = 64;
    /** Unmodified cycle balance. */
    public static final int ENERGY_PER_TICK = 12;
    public static final int PROCESS_TICKS = 200;
    public static final int WATER_TANK_CAPACITY = 4_000;
    public static final int OXYGEN_CAPACITY = 4_000;
    public static final int WATER_PER_CYCLE = 200;
    public static final int OXYGEN_PER_CYCLE = 96;
    public static final int MAX_OXYGEN_OUTPUT_PER_TICK = 120;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_WATER_CAPACITY = 3;
    public static final int DATA_OXYGEN = 4;
    public static final int DATA_OXYGEN_CAPACITY = 5;
    public static final int DATA_PROGRESS = 6;
    public static final int DATA_PROGRESS_MAX = 7;
    public static final int DATA_STATUS = 8;
    public static final int DATA_SIDES_START = 9;
    public static final int DATA_COUNT = DATA_SIDES_START + 7;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_NO_WATER = 2;
    public static final int STATUS_NO_ENERGY = 3;
    public static final int STATUS_OUTPUT_FULL = 4;

    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_WATER = "PurifiedWater";
    private static final String NBT_OXYGEN = "Oxygen";
    private static final String NBT_PROGRESS = "Progress";

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT_PER_TICK, 0);

    private final FluidTank waterTank = new FluidTank(WATER_TANK_CAPACITY, stack -> stack.getFluid().isSame(ModFluids.PURIFIED_WATER.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final OxygenStorage oxygenStorage = new OxygenStorage(OXYGEN_CAPACITY, 0, MAX_OXYGEN_OUTPUT_PER_TICK);

    private final EnumMap<Direction, Port> ports = new EnumMap<>(Direction.class);
    private Direction lastFacing;
    private final MachineModuleInventory modules = new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);
    private int cycleTicks, cycleEnergy;
    @Override public int moduleSlotCount() { return 2; }
    @Override public Set<MachineModuleType> allowedModuleTypes() { return Set.of(MachineModuleType.BUFFER, MachineModuleType.EFFICIENCY, MachineModuleType.OVERDRIVE); }
    public MachineModuleInventory getModules() { return modules; }
    private void modulesChanged() { energyStorage.setCapacityInternal(modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY)); setChanged(); }
    private float animationTick, previousAnimationTick;
    public void clientAnimationTick() { previousAnimationTick = animationTick; if (getBlockState().getValue(OxygenElectrolyzerBlock.LIT)) animationTick = (animationTick + 1) % 80; }
    public float animationPhase(float partial) { float next = animationTick < previousAnimationTick ? animationTick + 80 : animationTick; return (previousAnimationTick + (next - previousAnimationTick) * partial) / 80F; }
    public int waterAmount() { return waterTank.getFluidAmount(); }
    public int oxygenAmount() { return oxygenStorage.getOxygenStored(); }
    private int nextEnergyCost() { int ticks = currentProcessTicks(), total = currentCycleEnergy(); return (int)((long)total * (progress + 1) / ticks - (long)total * progress / ticks); }

    private int progress;
    private int status = STATUS_IDLE;
    private int ambientSoundTick;
    private boolean syncPending;
    @Override public void setChanged() { super.setChanged(); syncPending=true; }

    private final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int index) {
            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_ENERGY_CAPACITY) return energyStorage.getMaxEnergyStored();
            if (index == DATA_WATER) return waterTank.getFluidAmount();
            if (index == DATA_WATER_CAPACITY) return waterTank.getCapacity();
            if (index == DATA_OXYGEN) return oxygenStorage.getOxygenStored();
            if (index == DATA_OXYGEN_CAPACITY) return oxygenStorage.getMaxOxygenStored();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_PROGRESS_MAX) return currentProcessTicks();
            if (index == DATA_STATUS) return status;
            if (index == 15) return currentCycleEnergy();

            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) return sideMode(Direction.values()[index - DATA_SIDES_START]).ordinal();
            return 0;
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public OxygenElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_ELECTROLYZER.get(), pos, state);
        applyDefaultSideConfiguration(); lastFacing = getMachineFacing();
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        sideConfig.setMode(Direction.UP, SideMode.INPUT);
        sideConfig.setMode(facing.getCounterClockWise(), SideMode.INPUT);
        sideConfig.setMode(Direction.DOWN, SideMode.OUTPUT);
        sideConfig.setMode(facing.getClockWise(), SideMode.OUTPUT);
        sideConfig.setMode(facing.getOpposite(), SideMode.OUTPUT);
    }

    public static boolean isConfigurableSide(RelativeSide side) { return side != RelativeSide.FRONT; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenElectrolyzerBlockEntity machine) {
        if (machine.lastFacing != machine.getMachineFacing()) machine.rotateSideConfiguration(machine.lastFacing);
        machine.syncAllPortStates();
        boolean changed = false;
        int newStatus = machine.calculateStatus();

        if (newStatus == STATUS_RUNNING) {
            if (machine.cycleTicks == 0) {
                machine.cycleTicks = machine.currentProcessTicks();
                machine.cycleEnergy = machine.modules.modifiers().applyEnergyCost(PROCESS_TICKS * ENERGY_PER_TICK);
            }
            int energyPerTick = machine.nextEnergyCost();
            int processTicks = machine.currentProcessTicks();
            int removed = machine.energyStorage.removeEnergyInternal(energyPerTick);
            if (removed == energyPerTick) {
                machine.progress++;
                changed = true;
                if (machine.progress >= processTicks) {
                    machine.finishCycle();
                    machine.progress = 0; machine.cycleTicks = machine.cycleEnergy = 0;
                }
            }
        }



        machine.status = machine.calculateStatus();
        boolean shouldBeLit = newStatus == STATUS_RUNNING;
        machine.ambientSoundTick = MachineAmbientSoundService.tick(
                level, pos, shouldBeLit, machine.ambientSoundTick,
                MachineAmbientSoundService.MachineType.OXYGEN_ELECTROLYZER
        );
        BlockState current = machine.getBlockState();
        if (current.getValue(OxygenElectrolyzerBlock.LIT) != shouldBeLit) {
            level.setBlock(pos, current.setValue(OxygenElectrolyzerBlock.LIT, shouldBeLit), 3);
            changed = true;
        }
        if (changed) machine.setChanged();
        if (machine.syncPending && level.getGameTime() % 10 == 0) {
            level.sendBlockUpdated(pos, machine.getBlockState(), machine.getBlockState(), 2);machine.syncPending=false;
        }
    }

    private int calculateStatus() {
        if (waterTank.getFluidAmount() < WATER_PER_CYCLE) return STATUS_NO_WATER;
        if (oxygenStorage.getMaxOxygenStored() - oxygenStorage.getOxygenStored() < OXYGEN_PER_CYCLE) return STATUS_OUTPUT_FULL;
        if (energyStorage.getEnergyStored() < nextEnergyCost()) return STATUS_NO_ENERGY;
        return STATUS_RUNNING;
    }

    private int currentProcessTicks() { return cycleTicks > 0 ? cycleTicks : modules.modifiers().applyProcessingTicks(PROCESS_TICKS); }
    private int currentCycleEnergy() { return cycleTicks > 0 ? cycleEnergy : modules.modifiers().applyEnergyCost(PROCESS_TICKS * ENERGY_PER_TICK); }
    private void finishCycle() {
        waterTank.drain(WATER_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
        oxygenStorage.addInternal(OXYGEN_PER_CYCLE);
    }

    public ContainerData getDataAccess() { return dataAccess; }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction worldSide = relativeSide.resolve(getMachineFacing());
        SideMode mode = sideConfig.cycleMode(worldSide);
        routingChanged(); return mode;
    }

    public SideMode sideMode(@Nullable Direction side) { return side == null || side == getMachineFacing() ? SideMode.DISABLED : sideConfig.getMode(side); }
    public void rotateSideConfiguration(Direction previous) {
        EnumMap<RelativeSide, SideMode> old = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : RelativeSide.values()) old.put(side, sideConfig.getMode(side.resolve(previous)));
        for (RelativeSide side : RelativeSide.values()) sideConfig.setMode(side.resolve(getMachineFacing()), old.get(side));
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED); lastFacing = getMachineFacing(); routingChanged();
    }
    private void routingChanged() { refreshCapabilities(); syncAllPortStates(); setChanged(); if (level != null) { level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); level.updateNeighborsAt(worldPosition,getBlockState().getBlock()); } }
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

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.put(NBT_WATER, waterTank.writeToNBT(new CompoundTag()));
        tag.putInt(NBT_OXYGEN, oxygenAmount());
        tag.putInt(NBT_PROGRESS, progress);
        sideConfig.save(tag);
        tag.put("Modules", modules.serializeNBT()); tag.putInt("CycleTicks", cycleTicks); tag.putInt("CycleEnergy", cycleEnergy); tag.putString("PortFacing", getMachineFacing().getName());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Modules")) modules.deserializeNBT(tag.getCompound("Modules")); else for (int i=0;i<2;i++) modules.setStackInSlot(i,ItemStack.EMPTY);
        modulesChanged(); energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        waterTank.readFromNBT(tag.getCompound(NBT_WATER));
        oxygenStorage.setStoredInternal(tag.getInt(NBT_OXYGEN));
        cycleTicks = Math.max(0, tag.getInt("CycleTicks")); cycleEnergy = Math.max(0, tag.getInt("CycleEnergy"));
        // A legacy unfinished cycle retains its original timing/cost.
        if (cycleTicks == 0 && tag.getInt(NBT_PROGRESS) > 0) { cycleTicks = PROCESS_TICKS; cycleEnergy = PROCESS_TICKS * ENERGY_PER_TICK; }
        progress = Math.max(0, Math.min(currentProcessTicks() - 1, tag.getInt(NBT_PROGRESS)));
        if (!sideConfig.load(tag)) applyDefaultSideConfiguration();
        // Older BOTH defaults were saved as five OUTPUT modes despite accepting FE/water.
        // Migrate only that exact legacy default; preserve custom side choices.
        if (!tag.contains("PortFacing") && Arrays.stream(Direction.values()).filter(d->d!=getMachineFacing()).allMatch(d->sideConfig.getMode(d)==SideMode.OUTPUT)) applyDefaultSideConfiguration();
        Direction savedFacing = Direction.byName(tag.getString("PortFacing"));
        if (savedFacing != null && savedFacing.getAxis().isHorizontal() && savedFacing != getMachineFacing()) {
            EnumMap<RelativeSide,SideMode> old=new EnumMap<>(RelativeSide.class);
            for (RelativeSide side:RelativeSide.values()) old.put(side,sideConfig.getMode(side.resolve(savedFacing)));
            for (RelativeSide side:RelativeSide.values()) sideConfig.setMode(side.resolve(getMachineFacing()),old.get(side));
        }
        for (Direction side:Direction.values()) if (sideConfig.getMode(side)==SideMode.BOTH) sideConfig.setMode(side,side==Direction.UP||side==getMachineFacing().getCounterClockWise()?SideMode.INPUT:SideMode.OUTPUT);
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED); lastFacing=getMachineFacing(); refreshCapabilities();
        status = calculateStatus();
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap==ForgeCapabilities.ENERGY || cap==ForgeCapabilities.FLUID_HANDLER || cap==com.wasted.domesurvival.forge.capability.ModCapabilities.OXYGEN) {
            if (isRemoved() || sideMode(side)==SideMode.DISABLED) return LazyOptional.empty();
            Port port=ports.computeIfAbsent(side,Port::new);
            if (cap==com.wasted.domesurvival.forge.capability.ModCapabilities.OXYGEN) return sideMode(side)==SideMode.OUTPUT?port.oxygen.cast():LazyOptional.empty();
            if (sideMode(side)!=SideMode.INPUT) return LazyOptional.empty();
            return cap==ForgeCapabilities.ENERGY?port.energy.cast():port.fluid.cast();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCapabilities() { ports.values().forEach(p->{p.energy.invalidate();p.fluid.invalidate();p.oxygen.invalidate();});ports.clear(); }
    @Override public void invalidateCaps() { super.invalidateCaps();refreshCapabilities(); }
    @Override public void reviveCaps() { super.reviveCaps();refreshCapabilities(); }
    private final class Port implements IFluidHandler {
        final Direction side; Port(Direction side) { this.side=side; }
        boolean input() { return !isRemoved()&&sideMode(side)==SideMode.INPUT; }
        boolean output() { return !isRemoved()&&sideMode(side)==SideMode.OUTPUT; }
        final LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->this);
        final LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->new IEnergyStorage() {
            public int receiveEnergy(int amount,boolean simulate) { int n=input()?energyStorage.receiveEnergy(amount,simulate):0;if(n>0&&!simulate)setChanged();return n; }
            public int extractEnergy(int amount,boolean simulate) { return 0; }
            public int getEnergyStored() { return energyStorage.getEnergyStored(); }
            public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
            public boolean canReceive() { return input(); }public boolean canExtract() { return false; }
        });
        final LazyOptional<com.wasted.domesurvival.forge.capability.IOxygenStorage> oxygen=LazyOptional.of(()->new com.wasted.domesurvival.forge.capability.IOxygenStorage() {
            public int receiveOxygen(int amount,boolean simulate) { return 0; }
            public int extractOxygen(int amount,boolean simulate) { int n=output()?oxygenStorage.extractOxygen(amount,simulate):0;if(n>0&&!simulate)setChanged();return n; }
            public int getOxygenStored() { return oxygenAmount(); }public int getMaxOxygenStored() { return OXYGEN_CAPACITY; }
            public boolean canReceive() { return false; }public boolean canExtract() { return output(); }
        });
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return input()&&tank==0?waterTank.getFluid().copy():FluidStack.EMPTY; }
        public int getTankCapacity(int tank) { return tank==0?WATER_TANK_CAPACITY:0; }
        public boolean isFluidValid(int tank,FluidStack stack) { return input()&&tank==0&&waterTank.isFluidValid(0,stack); }
        public int fill(FluidStack stack,FluidAction action) { return input()?waterTank.fill(stack,action):0; }
        public FluidStack drain(FluidStack stack,FluidAction action) { return FluidStack.EMPTY; }
        public FluidStack drain(int amount,FluidAction action) { return FluidStack.EMPTY; }
    }
    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.oxygen_electrolyzer"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) { return new OxygenElectrolyzerMenu(containerId, playerInventory, this); }
}
