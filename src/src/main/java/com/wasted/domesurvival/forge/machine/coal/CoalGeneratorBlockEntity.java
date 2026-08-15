package com.wasted.domesurvival.forge.machine.coal;

import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.ResourceChannel;
import com.wasted.domesurvival.forge.machine.side.SideConfig;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CoalGeneratorBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int GENERATION_PER_TICK = 20;
    public static final int MAX_OUTPUT_PER_TICK = 80;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_MAX_BURN_TIME = 3;
    public static final int DATA_SIDES_START = 4;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_BURN_TIME = "BurnTime";
    private static final String NBT_MAX_BURN_TIME = "MaxBurnTime";

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && isValidFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, 0, MAX_OUTPUT_PER_TICK);

    private final IItemHandler itemInputView = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return inventory.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return inventory.isItemValid(slot, stack); }
    };

    private final IItemHandler itemOutputView = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty() || isValidFuel(stack)) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    private final IItemHandler itemCombinedView = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return itemInputView.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemOutputView.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return inventory.isItemValid(slot, stack); }
    };

    private final IEnergyStorage energyOutputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return energyStorage.extractEnergy(maxExtract, simulate); }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private LazyOptional<IItemHandler> itemInputCapability = LazyOptional.of(() -> itemInputView);
    private LazyOptional<IItemHandler> itemOutputCapability = LazyOptional.of(() -> itemOutputView);
    private LazyOptional<IItemHandler> itemCombinedCapability = LazyOptional.of(() -> itemCombinedView);
    private LazyOptional<IEnergyStorage> energyOutputCapability = LazyOptional.of(() -> energyOutputView);

    private int burnTime;
    private int maxBurnTime;
    private int ambientSoundTick;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_CAPACITY) return energyStorage.getMaxEnergyStored();
            if (index == DATA_BURN_TIME) return burnTime;
            if (index == DATA_MAX_BURN_TIME) return maxBurnTime;
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) {
                Direction direction = Direction.values()[index - DATA_SIDES_START];
                return sideConfig.getMode(direction).ordinal();
            }
            return 0;
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR.get(), pos, state);
        applyDefaultSideConfiguration();
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        sideConfig.setMode(RelativeSide.TOP.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.BOTTOM.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.LEFT.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.RIGHT.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.BACK.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.FRONT.resolve(facing), SideMode.DISABLED);
    }

    public static boolean isConfigurableSide(RelativeSide side) {
        return side != RelativeSide.FRONT;
    }

    private boolean isFrontWorldSide(Direction side) {
        return side == getMachineFacing();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoalGeneratorBlockEntity generator) {
        boolean changed = false;
        boolean hasGenerationSpace = generator.energyStorage.getEnergyStored() < generator.energyStorage.getMaxEnergyStored();

        if (generator.burnTime > 0 && hasGenerationSpace) {
            int generated = generator.energyStorage.addEnergyInternal(GENERATION_PER_TICK);
            if (generated > 0) {
                generator.burnTime--;
                changed = true;
            }
        }

        if (generator.burnTime <= 0 && hasGenerationSpace) {
            ItemStack fuel = generator.inventory.getStackInSlot(0);
            int burnDuration = getFuelBurnTime(fuel);
            if (burnDuration > 0) {
                generator.burnTime = burnDuration;
                generator.maxBurnTime = burnDuration;
                generator.consumeOneFuel();
                changed = true;
            }
        }

        if (generator.pushEnergyToNeighbors(level, pos) > 0) {
            changed = true;
        }

        boolean shouldBeLit = generator.burnTime > 0;
        generator.ambientSoundTick = MachineAmbientSoundService.tick(
                level, pos, shouldBeLit, generator.ambientSoundTick,
                MachineAmbientSoundService.MachineType.COAL_GENERATOR
        );
        if (state.getValue(CoalGeneratorBlock.LIT) != shouldBeLit) {
            level.setBlock(pos, state.setValue(CoalGeneratorBlock.LIT, shouldBeLit), 3);
            changed = true;
        }
        if (changed) generator.setChanged();
    }

    public static boolean isValidFuel(@NotNull ItemStack stack) {
        return getFuelBurnTime(stack) > 0;
    }

    private static int getFuelBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        // Uses the same Forge furnace-fuel hook as normal smelting, so compatible modded
        // furnace fuels work automatically without hard-coded mod integration.
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    private void consumeOneFuel() {
        ItemStack fuel = inventory.getStackInSlot(0);
        if (fuel.isEmpty()) return;
        ItemStack remainder = ForgeHooks.getCraftingRemainingItem(fuel.copyWithCount(1));
        fuel.shrink(1);
        if (fuel.isEmpty() && !remainder.isEmpty()) {
            inventory.setStackInSlot(0, remainder);
        } else {
            inventory.setStackInSlot(0, fuel);
        }
    }

    private int pushEnergyToNeighbors(Level level, BlockPos pos) {
        int totalTransferred = 0;
        for (Direction direction : Direction.values()) {
            if (isFrontWorldSide(direction) || !sideConfig.allowsOutput(direction)) continue;
            int remainingOutput = MAX_OUTPUT_PER_TICK - totalTransferred;
            if (remainingOutput <= 0 || energyStorage.getEnergyStored() <= 0) break;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) continue;
            IEnergyStorage target = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (target == null || !target.canReceive()) continue;
            int available = energyStorage.extractEnergy(remainingOutput, true);
            int acceptedSimulation = target.receiveEnergy(available, true);
            if (acceptedSimulation <= 0) continue;
            int extracted = energyStorage.extractEnergy(acceptedSimulation, false);
            int acceptedActual = target.receiveEnergy(extracted, false);
            if (acceptedActual < extracted) energyStorage.addEnergyInternal(extracted - acceptedActual);
            totalTransferred += acceptedActual;
        }
        return totalTransferred;
    }

    public ItemStackHandler getInventory() { return inventory; }
    public ContainerData getDataAccess() { return dataAccess; }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction worldSide = relativeSide.resolve(getMachineFacing());
        SideMode mode = sideConfig.cycleMode(worldSide);
        refreshCapabilities();
        syncPortState(worldSide);
        setChanged();
        return mode;
    }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof CoalGeneratorBlock)) return;
        PortVisual visual = isFrontWorldSide(direction) ? PortVisual.OFF : PortVisual.fromMode(sideConfig.getMode(direction));
        var property = CoalGeneratorBlock.portProperty(direction);
        if (state.getValue(property) != visual) {
            level.setBlock(worldPosition, state.setValue(property, visual), 3);
        }
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof CoalGeneratorBlock)) return;
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction) ? PortVisual.OFF : PortVisual.fromMode(sideConfig.getMode(direction));
            updated = updated.setValue(CoalGeneratorBlock.portProperty(direction), visual);
        }
        if (!updated.equals(state)) level.setBlock(worldPosition, updated, 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncAllPortStates();
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(CoalGeneratorBlock.FACING) ? state.getValue(CoalGeneratorBlock.FACING) : Direction.NORTH;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(NBT_BURN_TIME, burnTime);
        tag.putInt(NBT_MAX_BURN_TIME, maxBurnTime);
        sideConfig.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        burnTime = Math.max(0, tag.getInt(NBT_BURN_TIME));
        maxBurnTime = Math.max(0, tag.getInt(NBT_MAX_BURN_TIME));

        if (!sideConfig.load(tag)) {
            migrateLegacySideConfiguration(tag);
        }
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);
    }

    private void migrateLegacySideConfiguration(CompoundTag tag) {
        applyDefaultSideConfiguration();
        if (!tag.contains("SideConfig")) return;
        SideConfig legacy = new SideConfig(ResourceChannel.ENERGY);
        legacy.load(tag);
        Direction facing = getMachineFacing();
        for (RelativeSide side : new RelativeSide[]{RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.BACK}) {
            Direction world = side.resolve(facing);
            if (legacy.allowsOutput(ResourceChannel.ENERGY, world)) sideConfig.setMode(world, SideMode.OUTPUT);
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) return itemCombinedCapability.cast();
            if (isFrontWorldSide(side)) return LazyOptional.empty();
            if (sideConfig.allowsInput(side)) return itemInputCapability.cast();
            if (sideConfig.allowsOutput(side)) return itemOutputCapability.cast();
            return LazyOptional.empty();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            if (side == null || (!isFrontWorldSide(side) && sideConfig.allowsOutput(side))) {
                return energyOutputCapability.cast();
            }
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    private void refreshCapabilities() {
        itemInputCapability.invalidate();
        itemOutputCapability.invalidate();
        energyOutputCapability.invalidate();
        itemInputCapability = LazyOptional.of(() -> itemInputView);
        itemOutputCapability = LazyOptional.of(() -> itemOutputView);
        energyOutputCapability = LazyOptional.of(() -> energyOutputView);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemInputCapability.invalidate();
        itemOutputCapability.invalidate();
        itemCombinedCapability.invalidate();
        energyOutputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemInputCapability = LazyOptional.of(() -> itemInputView);
        itemOutputCapability = LazyOptional.of(() -> itemOutputView);
        itemCombinedCapability = LazyOptional.of(() -> itemCombinedView);
        energyOutputCapability = LazyOptional.of(() -> energyOutputView);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.coal_generator"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoalGeneratorMenu(containerId, playerInventory, this);
    }
}
