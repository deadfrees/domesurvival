package com.wasted.domesurvival.forge.machine.crusher;

import com.wasted.domesurvival.forge.capability.IGasStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.gas.GasStorage;
import com.wasted.domesurvival.forge.gas.ModGases;
import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.api.IProcessingMachine;
import com.wasted.domesurvival.forge.machine.api.MachineOperatingState;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.module.IModularMachine;
import com.wasted.domesurvival.forge.machine.module.MachineModuleInventory;
import com.wasted.domesurvival.forge.machine.module.MachineModuleResolver;
import com.wasted.domesurvival.forge.machine.module.MachineModuleType;
import com.wasted.domesurvival.forge.recipe.IndustrialCrusherRecipe;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class IndustrialCrusherBlockEntity extends BlockEntity
        implements net.minecraft.world.MenuProvider, IModularMachine, IProcessingMachine {
    public static final int BASE_CAPACITY = 40_000;
    public static final int MAX_RECEIVE = 256;
    public static final int PROCESS_CAPACITY = 4_000;
    public static final int GAS_CAPACITY = PROCESS_CAPACITY;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_MAX_PROGRESS = 3;
    public static final int DATA_RECIPE_ENERGY = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_GAS = 6;
    public static final int DATA_GAS_CAPACITY = 7;
    public static final int DATA_PROCESS_TYPE = 8;
    public static final int DATA_COUNT = 9;

    public static final int PROCESS_EMPTY = 0;
    public static final int PROCESS_MINERAL_GAS = 1;
    public static final int PROCESS_NEOFLUX = 2;

    public static final int READY = 0;
    public static final int CRUSHING = 1;
    public static final int NO_ENERGY = 2;
    public static final int NO_RECIPE = 3;
    public static final int OUTPUT_FULL = 4;
    public static final int NOT_ENOUGH_INPUT = 5;
    public static final int PROCESS_TANK_BLOCKED = 6;
    public static final int GAS_FULL = PROCESS_TANK_BLOCKED;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_MODULES = "Modules";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE_RECIPE = "ActiveRecipe";
    private static final String NBT_CYCLE_TICKS = "CycleTicks";
    private static final String NBT_CYCLE_ENERGY = "CycleEnergy";
    private static final String NBT_GAS_TANK = "GasTank";
    private static final String NBT_FLUID_TANK = "FluidTank";

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && isValidInput(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0) invalidateRecipe();
            setChanged();
        }
    };

    private final MachineEnergyStorage energy =
            new MachineEnergyStorage(BASE_CAPACITY, MAX_RECEIVE, 0);

    private final MachineModuleInventory modules =
            new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);

    /** Crusher gas buffer is production-only externally: pipes may drain it but never fill it. */
    private final GasStorage gasTank = new GasStorage(
            PROCESS_CAPACITY,
            0,
            Integer.MAX_VALUE,
            ModGases.MINERAL_GAS::equals,
            this::setChanged
    );

    /** Neoflux shares the Crusher's logical process tank with Mineral Gas. */
    private final FluidTank fluidTank = new FluidTank(
            PROCESS_CAPACITY,
            stack -> stack.getFluid() == ModFluids.NEOFLUX.get()
    ) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IItemHandler items = new IItemHandler() {
        @Override public int getSlots() { return 3; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == 0 ? inventory.insertItem(0, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 || slot == 2 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && inventory.isItemValid(0, stack);
        }
    };

    private final IEnergyStorage energyInput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = energy.receiveEnergy(amount, simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IGasStorage gasOutput = new IGasStorage() {
        @Override public int receiveGas(ResourceLocation gas, int maxReceive, boolean simulate) { return 0; }
        @Override public int extractGas(ResourceLocation gas, int maxExtract, boolean simulate) {
            return gasTank.extractGas(gas, maxExtract, simulate);
        }
        @Override public @Nullable ResourceLocation getGasType() { return gasTank.getGasType(); }
        @Override public int getGasStored() { return gasTank.getGasStored(); }
        @Override public int getMaxGasStored() { return gasTank.getMaxGasStored(); }
        @Override public boolean canReceiveGas(ResourceLocation gas) { return false; }
        @Override public boolean canExtractGas(ResourceLocation gas) { return gasTank.canExtractGas(gas); }
    };

    /** Forge fluid capability is output-only; automation may drain Neoflux but cannot inject it. */
    private final IFluidHandler fluidOutput = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fluidTank.getFluid().copy() : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? fluidTank.getCapacity() : 0; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return fluidTank.drain(resource, action);
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return fluidTank.drain(maxDrain, action);
        }
    };

    private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyInput);
    private LazyOptional<IGasStorage> gasCap = LazyOptional.of(() -> gasOutput);
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluidOutput);

    private int progress;
    private int cycleTicks;
    private int cycleEnergy;
    private boolean active;
    @Nullable private ResourceLocation activeRecipe;

    private ItemStack cachedInput = ItemStack.EMPTY;
    private Optional<IndustrialCrusherRecipe> cachedRecipe = Optional.empty();
    private boolean recipeCacheValid;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            IndustrialCrusherRecipe recipe = currentRecipe().orElse(null);
            if (index == DATA_ENERGY) return energy.getEnergyStored();
            if (index == DATA_CAPACITY) return energy.getMaxEnergyStored();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_MAX_PROGRESS) return recipe == null ? 0 : processingTicks(recipe);
            if (index == DATA_RECIPE_ENERGY) return recipe == null ? 0 : processingEnergy(recipe);
            if (index == DATA_STATUS) return status(recipe);
            if (index == DATA_GAS) return processStored();
            if (index == DATA_GAS_CAPACITY) return PROCESS_CAPACITY;
            if (index == DATA_PROCESS_TYPE) return processType();
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public IndustrialCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override public int moduleSlotCount() { return 2; }

    @Override
    public Set<MachineModuleType> allowedModuleTypes() {
        return Set.of(MachineModuleType.EFFICIENCY, MachineModuleType.OVERDRIVE, MachineModuleType.BUFFER);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IndustrialCrusherBlockEntity crusher) {
        crusher.active = false;
        boolean changed = crusher.tickProcess();

        if (state.getValue(IndustrialCrusherBlock.ACTIVE) != crusher.active) {
            level.setBlock(pos, state.setValue(IndustrialCrusherBlock.ACTIVE, crusher.active), 3);
            changed = true;
        }
        if (changed) crusher.setChanged();
    }

    private boolean tickProcess() {
        IndustrialCrusherRecipe recipe = currentRecipe().orElse(null);
        if (recipe == null || !hasRequiredInput(recipe)) return resetProgressIfNeeded();

        if (activeRecipe == null || !activeRecipe.equals(recipe.getId())) {
            progress = 0;
            cycleTicks = 0;
            cycleEnergy = 0;
            activeRecipe = recipe.getId();
        }

        ensureCycleSnapshot(recipe);

        // No FE is consumed while either item output or the complete process output is blocked.
        if (!canAcceptItemOutputs(recipe) || !canAcceptProcessOutput(recipe)) return false;

        int ticks = processingTicks(recipe);
        int totalEnergy = processingEnergy(recipe);
        int requiredThisTick = energyStep(totalEnergy, ticks);
        if (energy.getEnergyStored() < requiredThisTick) return false;

        // Completion gets an explicit second validation immediately before the final FE debit.
        if (progress + 1 >= ticks
                && (!hasRequiredInput(recipe) || !canAcceptItemOutputs(recipe) || !canAcceptProcessOutput(recipe))) {
            return false;
        }

        energy.removeEnergyInternal(requiredThisTick);
        progress++;
        active = true;

        if (progress >= ticks) {
            commitRecipe(recipe);
            progress = 0;
            cycleTicks = 0;
            cycleEnergy = 0;
            activeRecipe = null;
        }
        return true;
    }

    private void ensureCycleSnapshot(IndustrialCrusherRecipe recipe) {
        if (progress == 0 || cycleTicks <= 0 || cycleEnergy <= 0) {
            cycleTicks = Math.max(1, modules.modifiers().applyProcessingTicks(recipe.getProcessingTime()));
            cycleEnergy = Math.max(1, modules.modifiers().applyEnergyCost(recipe.getEnergy()));
        }
    }

    private boolean hasCycle(IndustrialCrusherRecipe recipe) {
        return activeRecipe != null && activeRecipe.equals(recipe.getId()) && cycleTicks > 0 && cycleEnergy > 0;
    }

    private int processingTicks(IndustrialCrusherRecipe recipe) {
        return hasCycle(recipe)
                ? cycleTicks
                : Math.max(1, modules.modifiers().applyProcessingTicks(recipe.getProcessingTime()));
    }

    private int processingEnergy(IndustrialCrusherRecipe recipe) {
        return hasCycle(recipe)
                ? cycleEnergy
                : Math.max(1, modules.modifiers().applyEnergyCost(recipe.getEnergy()));
    }

    private boolean resetProgressIfNeeded() {
        boolean changed = progress != 0 || activeRecipe != null || cycleTicks != 0 || cycleEnergy != 0;
        progress = 0;
        cycleTicks = 0;
        cycleEnergy = 0;
        activeRecipe = null;
        return changed;
    }

    private int energyStep(int totalEnergy, int totalTicks) {
        int safeTicks = Math.max(1, totalTicks);
        int beforeProgress = Math.min(progress, safeTicks);
        int afterProgress = Math.min(progress + 1, safeTicks);
        int before = (int) ((long) totalEnergy * beforeProgress / safeTicks);
        int after = (int) ((long) totalEnergy * afterProgress / safeTicks);
        return Math.max(0, after - before);
    }

    /** Called only after simulation has proven every output fits in the same server tick. */
    private void commitRecipe(IndustrialCrusherRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(0);
        boolean createByproduct = level != null
                && !recipe.getByproduct().isEmpty()
                && level.random.nextInt(10_000) < recipe.getByproductChancePerTenThousand();

        input.shrink(recipe.getInputCount());
        inventory.setStackInSlot(0, input);
        merge(1, recipe.getResult());
        if (createByproduct) merge(2, recipe.getByproduct());

        if (recipe.hasGasResult()) {
            gasTank.addInternal(recipe.getGasResult(), recipe.getGasAmount(), false);
        } else if (recipe.hasFluidResult()) {
            fluidTank.fill(recipe.getFluidResult(), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void merge(int slot, ItemStack addition) {
        if (addition.isEmpty()) return;
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            inventory.setStackInSlot(slot, addition.copy());
            return;
        }
        current.grow(addition.getCount());
        inventory.setStackInSlot(slot, current);
    }

    private boolean canAcceptItemOutputs(IndustrialCrusherRecipe recipe) {
        // Reserve the byproduct slot even for a probabilistic output. This deliberately
        // prefers blocking over ever rolling an item that has nowhere to go.
        return canAccept(1, recipe.getResult()) && canAccept(2, recipe.getByproduct());
    }

    private boolean canAcceptProcessOutput(IndustrialCrusherRecipe recipe) {
        if (recipe.hasGasResult()) {
            ResourceLocation gas = recipe.getGasResult();
            return gas != null && gasTank.addInternal(gas, recipe.getGasAmount(), true) == recipe.getGasAmount();
        }

        if (recipe.hasFluidResult()) {
            FluidStack fluid = recipe.getFluidResult();
            return fluidTank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount();
        }

        return true;
    }

    private int processStored() {
        IndustrialCrusherRecipe recipe = currentRecipe().orElse(null);
        if (recipe != null && recipe.hasGasResult()) return gasTank.getGasStored();
        if (recipe != null && recipe.hasFluidResult()) return fluidTank.getFluidAmount();
        if (!fluidTank.isEmpty()) return fluidTank.getFluidAmount();
        return gasTank.getGasStored();
    }

    private int processType() {
        IndustrialCrusherRecipe recipe = currentRecipe().orElse(null);
        if (recipe != null && recipe.hasGasResult()) return PROCESS_MINERAL_GAS;
        if (recipe != null && recipe.hasFluidResult()) return PROCESS_NEOFLUX;
        if (!fluidTank.isEmpty()) return PROCESS_NEOFLUX;
        if (gasTank.getGasStored() > 0) return PROCESS_MINERAL_GAS;
        return PROCESS_EMPTY;
    }

    private boolean canAccept(int slot, ItemStack addition) {
        if (addition.isEmpty()) return true;
        int slotLimit = Math.min(inventory.getSlotLimit(slot), addition.getMaxStackSize());
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) return addition.getCount() <= slotLimit;
        if (!ItemStack.isSameItemSameTags(current, addition)) return false;
        int combinedLimit = Math.min(inventory.getSlotLimit(slot), current.getMaxStackSize());
        return current.getCount() + addition.getCount() <= combinedLimit;
    }

    private boolean hasRequiredInput(IndustrialCrusherRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(0);
        return recipe != null && recipe.acceptsIngredient(input) && input.getCount() >= recipe.getInputCount();
    }

    private Optional<IndustrialCrusherRecipe> currentRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return Optional.empty();

        if (recipeCacheValid && ItemStack.isSameItemSameTags(cachedInput, input)) return cachedRecipe;

        cachedRecipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.INDUSTRIAL_CRUSHER_TYPE.get())
                .stream()
                .filter(recipe -> recipe.acceptsIngredient(input))
                .findFirst();
        cachedInput = input.copyWithCount(1);
        recipeCacheValid = true;
        return cachedRecipe;
    }

    private boolean isValidInput(ItemStack stack) {
        return level != null
                && !stack.isEmpty()
                && level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.INDUSTRIAL_CRUSHER_TYPE.get())
                .stream()
                .anyMatch(recipe -> recipe.acceptsIngredient(stack));
    }

    private void invalidateRecipe() {
        recipeCacheValid = false;
        cachedInput = ItemStack.EMPTY;
        cachedRecipe = Optional.empty();
    }

    private int status(@Nullable IndustrialCrusherRecipe recipe) {
        if (recipe == null) return inventory.getStackInSlot(0).isEmpty() ? READY : NO_RECIPE;
        if (!hasRequiredInput(recipe)) return NOT_ENOUGH_INPUT;
        if (!canAcceptItemOutputs(recipe)) return OUTPUT_FULL;
        if (!canAcceptProcessOutput(recipe)) return PROCESS_TANK_BLOCKED;

        int ticks = processingTicks(recipe);
        int totalEnergy = processingEnergy(recipe);
        if (energy.getEnergyStored() < energyStep(totalEnergy, ticks)) return NO_ENERGY;
        return progress > 0 ? CRUSHING : READY;
    }

    private void modulesChanged() {
        // Buffer capacity changes immediately. Speed/energy modifiers are snapshotted
        // per cycle, so swapping a module cannot reset progress or double-charge FE.
        energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_CAPACITY));
        setChanged();
    }

    @Override
    public MachineOperatingState operatingState() {
        IndustrialCrusherRecipe recipe = currentRecipe().orElse(null);
        return switch (status(recipe)) {
            case CRUSHING -> MachineOperatingState.WORKING;
            case NO_ENERGY -> MachineOperatingState.NO_ENERGY;
            case NO_RECIPE, NOT_ENOUGH_INPUT -> MachineOperatingState.NO_RECIPE;
            case OUTPUT_FULL, PROCESS_TANK_BLOCKED -> MachineOperatingState.OUTPUT_BLOCKED;
            default -> MachineOperatingState.IDLE;
        };
    }

    @Override public int progressTicks() { return progress; }
    @Override public int requiredTicks() { return currentRecipe().map(this::processingTicks).orElse(0); }

    public ItemStackHandler getInventory() { return inventory; }
    public MachineModuleInventory getModules() { return modules; }
    public ContainerData getDataAccess() { return data; }
    public IGasStorage getGasStorage() { return gasOutput; }
    public IFluidHandler getFluidStorage() { return fluidOutput; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.put(NBT_MODULES, modules.serializeNBT());
        tag.putInt(NBT_ENERGY, energy.getEnergyStored());
        tag.putInt(NBT_PROGRESS, progress);
        tag.putInt(NBT_CYCLE_TICKS, cycleTicks);
        tag.putInt(NBT_CYCLE_ENERGY, cycleEnergy);
        tag.put(NBT_GAS_TANK, gasTank.serializeNBT());
        tag.put(NBT_FLUID_TANK, fluidTank.writeToNBT(new CompoundTag()));
        if (activeRecipe != null) tag.putString(NBT_ACTIVE_RECIPE, activeRecipe.toString());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        modules.deserializeNBT(tag.getCompound(NBT_MODULES));
        energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_CAPACITY));
        energy.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        cycleTicks = Math.max(0, tag.getInt(NBT_CYCLE_TICKS));
        cycleEnergy = Math.max(0, tag.getInt(NBT_CYCLE_ENERGY));
        gasTank.deserializeNBT(tag.getCompound(NBT_GAS_TANK));
        fluidTank.readFromNBT(tag.getCompound(NBT_FLUID_TANK));
        activeRecipe = tag.contains(NBT_ACTIVE_RECIPE)
                ? ResourceLocation.tryParse(tag.getString(NBT_ACTIVE_RECIPE))
                : null;
        invalidateRecipe();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (cap == ModCapabilities.GAS) return gasCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        energyCap.invalidate();
        gasCap.invalidate();
        fluidCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCap = LazyOptional.of(() -> items);
        energyCap = LazyOptional.of(() -> energyInput);
        gasCap = LazyOptional.of(() -> gasOutput);
        fluidCap = LazyOptional.of(() -> fluidOutput);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.industrial_crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IndustrialCrusherMenu(id, inventory, this);
    }
}
