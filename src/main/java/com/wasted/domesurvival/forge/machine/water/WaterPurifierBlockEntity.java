package com.wasted.domesurvival.forge.machine.water;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.module.*;
import java.util.*;
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

public final class WaterPurifierBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, IModularMachine {
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_ENERGY_INPUT_PER_TICK = 64;
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
    public static final int DATA_COUNT = DATA_SIDES_START + 8;

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
                cycleTicks = cycleEnergy = 0;
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

    private final EnumMap<Direction, Port> ports = new EnumMap<>(Direction.class);
    private Direction lastFacing;
    private final MachineModuleInventory modules = new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);
    private int cycleTicks, cycleEnergy;
    @Override public int moduleSlotCount() { return 2; }
    @Override public Set<MachineModuleType> allowedModuleTypes() { return Set.of(MachineModuleType.BUFFER, MachineModuleType.EFFICIENCY, MachineModuleType.OVERDRIVE); }
    public MachineModuleInventory getModules() { return modules; }
    private void modulesChanged() { energyStorage.setCapacityInternal(modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY)); setChanged(); }
    private float animationTick, previousAnimationTick;
    public void clientAnimationTick() { previousAnimationTick = animationTick; if (getBlockState().getValue(WaterPurifierBlock.LIT)) animationTick = (animationTick + 1) % 80; }
    public float pumpAngle(float partial) { float next = animationTick < previousAnimationTick ? animationTick + 80 : animationTick; return (previousAnimationTick + (next - previousAnimationTick) * partial) * 4.5F; }
    public int rawAmount() { return rawWaterTank.getFluidAmount(); }
    public int purifiedAmount() { return purifiedWaterTank.getFluidAmount(); }
    private int nextEnergyCost() { int ticks = currentProcessTicks(), total = currentCycleEnergy(); return (int)((long)total * (progress + 1) / ticks - (long)total * progress / ticks); }

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
            if (index == 15) return currentCycleEnergy();
            if (index == 16) return hasUsableFilter() ? inventory.getStackInSlot(SLOT_FILTER).getMaxDamage() - inventory.getStackInSlot(SLOT_FILTER).getDamageValue() : 0;
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) return sideMode(Direction.values()[index - DATA_SIDES_START]).ordinal();
            return 0;
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public WaterPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PURIFIER.get(), pos, state);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterPurifierBlockEntity purifier) {
        if (purifier.lastFacing != purifier.getMachineFacing()) purifier.rotateSideConfiguration(purifier.lastFacing);
        purifier.syncAllPortStates();
        boolean changed = purifier.consumeWaterBucketIfPossible();
        int newStatus = purifier.calculateStatus();

        if (newStatus == STATUS_RUNNING) {
            if (purifier.cycleTicks == 0) {
                purifier.cycleTicks = purifier.currentProcessTicks();
                purifier.cycleEnergy = purifier.modules.modifiers().applyEnergyCost(purifier.baseCycleEnergy());
            }
            int energyPerTick = purifier.nextEnergyCost();
            int processTicks = purifier.currentProcessTicks();
            int removed = purifier.energyStorage.removeEnergyInternal(energyPerTick);
            if (removed == energyPerTick) {
                purifier.progress++;
                changed = true;
                if (purifier.progress >= processTicks) {
                    purifier.finishCycle();
                    purifier.progress = 0; purifier.cycleTicks = purifier.cycleEnergy = 0;
                }
            }
        }

        changed |= purifier.pushPurifiedWaterToNeighbors();

        purifier.status = purifier.calculateStatus();
        boolean shouldBeLit = newStatus == STATUS_RUNNING;
        purifier.ambientSoundTick = MachineAmbientSoundService.tick(
                level, pos, shouldBeLit, purifier.ambientSoundTick,
                MachineAmbientSoundService.MachineType.WATER_PURIFIER
        );
        BlockState current = purifier.getBlockState();
        if (current.getValue(WaterPurifierBlock.LIT) != shouldBeLit) {
            level.setBlock(pos, current.setValue(WaterPurifierBlock.LIT, shouldBeLit), 3);
            changed = true;
        }
        if (changed) { purifier.setChanged(); if (level.getGameTime() % 10 == 0) level.sendBlockUpdated(pos, purifier.getBlockState(), purifier.getBlockState(), 2); }
    }

    private boolean pushPurifiedWaterToNeighbors() {
        if (level == null || level.isClientSide || purifiedWaterTank.isEmpty()) return false;
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            if (sideMode(direction) != SideMode.OUTPUT) continue;
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
        if (energyStorage.getEnergyStored() < nextEnergyCost()) return STATUS_NO_ENERGY;
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
        return cycleTicks > 0 ? cycleTicks : modules.modifiers().applyProcessingTicks(filter != null ? filter.processTicks() : PROCESS_TICKS);
    }

    private int baseCycleEnergy() { WaterFilterItem filter = currentFilterItem(); return filter == null ? PROCESS_TICKS * ENERGY_PER_TICK : filter.processTicks() * filter.energyPerTick(); }
    private int currentCycleEnergy() { return cycleTicks > 0 ? cycleEnergy : modules.modifiers().applyEnergyCost(baseCycleEnergy()); }

    private void finishCycle() {
        rawWaterTank.drain(RAW_WATER_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
        purifiedWaterTank.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(), PURIFIED_WATER_PER_CYCLE), IFluidHandler.FluidAction.EXECUTE);
        damageFilter();
    }

    private void damageFilter() {
        ItemStack filter = inventory.getStackInSlot(SLOT_FILTER);
        if (filter.isEmpty()) return;
        int maxDamage = Math.max(1, filter.getMaxDamage());
        int nextDamage = Math.min(maxDamage, filter.getDamageValue() + 1);
        if (nextDamage != filter.getDamageValue()) {
            // Keep an exhausted cartridge in the slot so its NBT (including the
            // regeneration counter) survives and the cartridge can be regenerated.
            filter.setDamageValue(nextDamage);
            inventory.setStackInSlot(SLOT_FILTER, filter);
        }
    }

    public ItemStackHandler getInventory() { return inventory; }
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
        tag.put("Modules", modules.serializeNBT()); tag.putInt("CycleTicks", cycleTicks); tag.putInt("CycleEnergy", cycleEnergy); tag.putString("PortFacing", getMachineFacing().getName());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        if (tag.contains("Modules")) modules.deserializeNBT(tag.getCompound("Modules")); else for (int i=0;i<2;i++) modules.setStackInSlot(i,ItemStack.EMPTY);
        modulesChanged(); energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        rawWaterTank.readFromNBT(tag.getCompound(NBT_RAW_TANK));
        purifiedWaterTank.readFromNBT(tag.getCompound(NBT_PURIFIED_TANK));
        cycleTicks = Math.max(0, tag.getInt("CycleTicks")); cycleEnergy = Math.max(0, tag.getInt("CycleEnergy"));
        // A legacy unfinished cycle retains its original cartridge timing/cost.
        if (cycleTicks == 0 && tag.getInt(NBT_PROGRESS) > 0 && currentFilterItem() != null) { cycleTicks = currentFilterItem().processTicks(); cycleEnergy = baseCycleEnergy(); }
        progress = Math.max(0, Math.min(currentProcessTicks() - 1, tag.getInt(NBT_PROGRESS)));
        if (!sideConfig.load(tag)) applyDefaultSideConfiguration();
        // Older purifier defaults exposed every resource despite five OUTPUT modes.
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
        if (cap==ForgeCapabilities.ENERGY || cap==ForgeCapabilities.FLUID_HANDLER || cap==ForgeCapabilities.ITEM_HANDLER) {
            if (isRemoved() || sideMode(side)==SideMode.DISABLED) return LazyOptional.empty();
            Port port=ports.computeIfAbsent(side,Port::new);
            if (cap==ForgeCapabilities.ENERGY) return sideMode(side)==SideMode.INPUT?port.energy.cast():LazyOptional.empty();
            return cap==ForgeCapabilities.FLUID_HANDLER?port.fluid.cast():port.items.cast();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCapabilities() { ports.values().forEach(p->{p.energy.invalidate();p.fluid.invalidate();p.items.invalidate();});ports.clear(); }
    @Override public void invalidateCaps() { super.invalidateCaps();refreshCapabilities(); }
    @Override public void reviveCaps() { super.reviveCaps();refreshCapabilities(); }
    private final class Port implements IEnergyStorage, IFluidHandler, IItemHandler {
        final Direction side; Port(Direction side) { this.side=side; }
        final LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->this);
        final LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->this);
        final LazyOptional<IItemHandler> items=LazyOptional.of(()->this);
        boolean input() { return !isRemoved()&&sideMode(side)==SideMode.INPUT; }
        boolean output() { return !isRemoved()&&sideMode(side)==SideMode.OUTPUT; }
        public int receiveEnergy(int amount,boolean simulate) { int accepted=input()?energyStorage.receiveEnergy(amount,simulate):0; if(accepted>0&&!simulate)setChanged();return accepted; }
        public int extractEnergy(int amount,boolean simulate) { return 0; }
        public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        public boolean canReceive() { return input(); } public boolean canExtract() { return false; }
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return (input()?rawWaterTank.getFluid():output()?purifiedWaterTank.getFluid():FluidStack.EMPTY).copy(); }
        public int getTankCapacity(int tank) { return input()?RAW_TANK_CAPACITY:PURIFIED_TANK_CAPACITY; }
        public boolean isFluidValid(int tank,FluidStack stack) { return input()&&rawWaterTank.isFluidValid(0,stack); }
        public int fill(FluidStack stack,FluidAction action) { return input()?rawWaterTank.fill(stack,action):0; }
        public FluidStack drain(FluidStack stack,FluidAction action) { return output()?purifiedWaterTank.drain(stack,action):FluidStack.EMPTY; }
        public FluidStack drain(int amount,FluidAction action) { return output()?purifiedWaterTank.drain(amount,action):FluidStack.EMPTY; }
        public int getSlots() { return 2; }
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot).copy(); }
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        public boolean isItemValid(int slot,ItemStack stack) { return input()&&inventory.isItemValid(slot,stack); }
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack; }
        public ItemStack extractItem(int slot,int amount,boolean simulate) {
            ItemStack stack=inventory.getStackInSlot(slot);
            boolean used=slot==SLOT_WATER_BUCKET&&stack.is(Items.BUCKET)||slot==SLOT_FILTER&&stack.getItem() instanceof WaterFilterItem&&stack.getDamageValue()>=stack.getMaxDamage();
            return output()&&used?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;
        }
    }
    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.water_purifier"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) { return new WaterPurifierMenu(containerId, playerInventory, this); }
}
