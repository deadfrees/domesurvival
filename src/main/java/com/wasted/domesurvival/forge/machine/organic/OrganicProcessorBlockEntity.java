package com.wasted.domesurvival.forge.machine.organic;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.api.IProcessingMachine;
import com.wasted.domesurvival.forge.machine.api.MachineOperatingState;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.module.IModularMachine;
import com.wasted.domesurvival.forge.machine.module.MachineModuleInventory;
import com.wasted.domesurvival.forge.machine.module.MachineModuleResolver;
import com.wasted.domesurvival.forge.machine.module.MachineModuleType;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import com.wasted.domesurvival.forge.recipe.OrganicProcessorRecipe;
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

public final class OrganicProcessorBlockEntity extends BlockEntity
        implements net.minecraft.world.MenuProvider, IModularMachine, IProcessingMachine {
    public static final int BASE_ENERGY_CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 256;
    public static final int WATER_CAPACITY = 4_000;

    public static final int SLOT_PRIMARY = 0;
    public static final int SLOT_ADDITIVE = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_WATER_CAPACITY = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_PROGRESS_MAX = 5;
    public static final int DATA_STATUS = 6;
    public static final int DATA_RECIPE_ENERGY = 7;
    public static final int DATA_WATER_REQUIRED = 8;
    public static final int DATA_COUNT = 9;

    public static final int READY = 0;
    public static final int PROCESSING = 1;
    public static final int NO_ENERGY = 2;
    public static final int NO_RECIPE = 3;
    public static final int NOT_ENOUGH_INPUT = 4;
    public static final int NO_WATER = 5;
    public static final int OUTPUT_FULL = 6;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_MODULES = "Modules";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_WATER = "PurifiedWater";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE_RECIPE = "ActiveRecipe";

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_PRIMARY) return isValidPrimary(stack);
            if (slot == SLOT_ADDITIVE) return isValidAdditive(stack);
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_PRIMARY || slot == SLOT_ADDITIVE) {
                invalidateRecipe();
                progress = 0;
                activeRecipe = null;
            }
            setChanged();
        }
    };

    private final MachineEnergyStorage energy =
            new MachineEnergyStorage(BASE_ENERGY_CAPACITY, MAX_RECEIVE, 0);

    private final FluidTank water = new FluidTank(
            WATER_CAPACITY,
            stack -> stack.getFluid().isSame(ModFluids.PURIFIED_WATER.get())
    ) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final MachineModuleInventory modules =
            new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);

    private final IItemHandler fullItems = new IItemHandler() {
        @Override public int getSlots() { return 3; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == SLOT_PRIMARY || slot == SLOT_ADDITIVE
                    ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    };

    private final IItemHandler inputItems = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot >= 0 && slot < 2 ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < 2 && inventory.isItemValid(slot, stack);
        }
    };

    private final IItemHandler outputItems = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) {
            return slot == 0 ? inventory.getStackInSlot(SLOT_OUTPUT) : ItemStack.EMPTY;
        }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? inventory.extractItem(SLOT_OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(SLOT_OUTPUT); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    private final IEnergyStorage energyInput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = energy.receiveEnergy(maxReceive, simulate);
            if (!simulate && accepted > 0) setChanged();
            return accepted;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IFluidHandler fluidInput = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return water.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return water.getCapacity(); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return water.isFluidValid(0, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return water.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    private LazyOptional<IItemHandler> fullItemCap = LazyOptional.of(() -> fullItems);
    private LazyOptional<IItemHandler> inputItemCap = LazyOptional.of(() -> inputItems);
    private LazyOptional<IItemHandler> outputItemCap = LazyOptional.of(() -> outputItems);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyInput);
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluidInput);

    private int progress;
    private boolean active;
    @Nullable private ResourceLocation activeRecipe;

    private ItemStack cachedPrimary = ItemStack.EMPTY;
    private ItemStack cachedAdditive = ItemStack.EMPTY;
    private Optional<OrganicProcessorRecipe> cachedRecipe = Optional.empty();
    private boolean recipeCacheValid;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            OrganicProcessorRecipe recipe = currentRecipe().orElse(null);
            if (index == DATA_ENERGY) return energy.getEnergyStored();
            if (index == DATA_ENERGY_CAPACITY) return energy.getMaxEnergyStored();
            if (index == DATA_WATER) return water.getFluidAmount();
            if (index == DATA_WATER_CAPACITY) return water.getCapacity();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_PROGRESS_MAX) return recipe == null ? 0 : modifiedProcessingTicks(recipe);
            if (index == DATA_STATUS) return status(recipe);
            if (index == DATA_RECIPE_ENERGY) return recipe == null ? 0 : modifiedEnergyCost(recipe);
            if (index == DATA_WATER_REQUIRED) return recipe == null ? 0 : recipe.getWaterMb();
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public OrganicProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(OrganicProcessorRegistry.ORGANIC_PROCESSOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public int moduleSlotCount() {
        return 2;
    }

    @Override
    public Set<MachineModuleType> allowedModuleTypes() {
        return Set.of(
                MachineModuleType.EFFICIENCY,
                MachineModuleType.OVERDRIVE,
                MachineModuleType.BUFFER
        );
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OrganicProcessorBlockEntity processor) {
        processor.active = false;
        boolean changed = processor.tickProcess();

        if (state.getValue(OrganicProcessorBlock.ACTIVE) != processor.active) {
            level.setBlock(pos, state.setValue(OrganicProcessorBlock.ACTIVE, processor.active), 3);
            changed = true;
        }
        if (changed) processor.setChanged();
    }

    private boolean tickProcess() {
        OrganicProcessorRecipe recipe = currentRecipe().orElse(null);
        if (recipe == null || !hasRequiredInput(recipe)) {
            return resetProgressIfNeeded();
        }

        if (activeRecipe == null || !activeRecipe.equals(recipe.getId())) {
            progress = 0;
            activeRecipe = recipe.getId();
        }

        if (water.getFluidAmount() < recipe.getWaterMb() || !canAcceptOutput(recipe.getResult())) {
            return false;
        }

        int ticks = modifiedProcessingTicks(recipe);
        int totalEnergy = modifiedEnergyCost(recipe);
        int requiredThisTick = energyStep(totalEnergy, ticks);
        if (energy.getEnergyStored() < requiredThisTick) {
            return false;
        }

        energy.removeEnergyInternal(requiredThisTick);
        progress++;
        active = true;

        if (progress >= ticks) {
            finish(recipe);
            progress = 0;
            activeRecipe = null;
        }
        return true;
    }

    private boolean resetProgressIfNeeded() {
        boolean changed = progress != 0 || activeRecipe != null;
        progress = 0;
        activeRecipe = null;
        return changed;
    }

    private int modifiedProcessingTicks(OrganicProcessorRecipe recipe) {
        return Math.max(1, modules.modifiers().applyProcessingTicks(recipe.getProcessingTime()));
    }

    private int modifiedEnergyCost(OrganicProcessorRecipe recipe) {
        return Math.max(1, modules.modifiers().applyEnergyCost(recipe.getEnergy()));
    }

    private int energyStep(int totalEnergy, int totalTicks) {
        int safeTicks = Math.max(1, totalTicks);
        int beforeProgress = Math.min(progress, safeTicks);
        int afterProgress = Math.min(progress + 1, safeTicks);
        int before = (int) ((long) totalEnergy * beforeProgress / safeTicks);
        int after = (int) ((long) totalEnergy * afterProgress / safeTicks);
        return Math.max(0, after - before);
    }

    private void finish(OrganicProcessorRecipe recipe) {
        if (!hasRequiredInput(recipe)
                || water.getFluidAmount() < recipe.getWaterMb()
                || !canAcceptOutput(recipe.getResult())) {
            return;
        }

        ItemStack primary = inventory.getStackInSlot(SLOT_PRIMARY);
        ItemStack additive = inventory.getStackInSlot(SLOT_ADDITIVE);
        primary.shrink(recipe.getPrimaryCount());
        additive.shrink(recipe.getAdditiveCount());
        inventory.setStackInSlot(SLOT_PRIMARY, primary);
        inventory.setStackInSlot(SLOT_ADDITIVE, additive);
        water.drain(recipe.getWaterMb(), IFluidHandler.FluidAction.EXECUTE);
        mergeOutput(recipe.getResult());
    }

    private void mergeOutput(ItemStack addition) {
        if (addition.isEmpty()) return;
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, addition.copy());
        } else {
            current.grow(addition.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, current);
        }
    }

    private boolean canAcceptOutput(ItemStack addition) {
        if (addition.isEmpty()) return true;
        int slotLimit = Math.min(inventory.getSlotLimit(SLOT_OUTPUT), addition.getMaxStackSize());
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) return addition.getCount() <= slotLimit;
        if (!ItemStack.isSameItemSameTags(current, addition)) return false;
        int combinedLimit = Math.min(inventory.getSlotLimit(SLOT_OUTPUT), current.getMaxStackSize());
        return current.getCount() + addition.getCount() <= combinedLimit;
    }

    private boolean hasRequiredInput(OrganicProcessorRecipe recipe) {
        ItemStack primary = inventory.getStackInSlot(SLOT_PRIMARY);
        ItemStack additive = inventory.getStackInSlot(SLOT_ADDITIVE);
        return recipe.acceptsPrimary(primary)
                && recipe.acceptsAdditive(additive)
                && primary.getCount() >= recipe.getPrimaryCount()
                && additive.getCount() >= recipe.getAdditiveCount();
    }

    private Optional<OrganicProcessorRecipe> currentRecipe() {
        if (level == null) return Optional.empty();

        ItemStack primary = inventory.getStackInSlot(SLOT_PRIMARY);
        ItemStack additive = inventory.getStackInSlot(SLOT_ADDITIVE);
        if (primary.isEmpty() || additive.isEmpty()) return Optional.empty();

        if (recipeCacheValid
                && ItemStack.isSameItemSameTags(cachedPrimary, primary)
                && ItemStack.isSameItemSameTags(cachedAdditive, additive)) {
            return cachedRecipe;
        }

        cachedRecipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ORGANIC_PROCESSOR_TYPE.get())
                .stream()
                .filter(recipe -> recipe.acceptsPrimary(primary) && recipe.acceptsAdditive(additive))
                .findFirst();
        cachedPrimary = primary.copyWithCount(1);
        cachedAdditive = additive.copyWithCount(1);
        recipeCacheValid = true;
        return cachedRecipe;
    }

    private boolean isValidPrimary(ItemStack stack) {
        return level != null
                && !stack.isEmpty()
                && level.getRecipeManager().getAllRecipesFor(ModRecipes.ORGANIC_PROCESSOR_TYPE.get())
                .stream().anyMatch(recipe -> recipe.acceptsPrimary(stack));
    }

    private boolean isValidAdditive(ItemStack stack) {
        return level != null
                && !stack.isEmpty()
                && level.getRecipeManager().getAllRecipesFor(ModRecipes.ORGANIC_PROCESSOR_TYPE.get())
                .stream().anyMatch(recipe -> recipe.acceptsAdditive(stack));
    }

    private void invalidateRecipe() {
        recipeCacheValid = false;
        cachedPrimary = ItemStack.EMPTY;
        cachedAdditive = ItemStack.EMPTY;
        cachedRecipe = Optional.empty();
    }

    private int status(@Nullable OrganicProcessorRecipe recipe) {
        if (recipe == null) return inventory.getStackInSlot(SLOT_PRIMARY).isEmpty()
                && inventory.getStackInSlot(SLOT_ADDITIVE).isEmpty() ? READY : NO_RECIPE;
        if (!hasRequiredInput(recipe)) return NOT_ENOUGH_INPUT;
        if (water.getFluidAmount() < recipe.getWaterMb()) return NO_WATER;
        if (!canAcceptOutput(recipe.getResult())) return OUTPUT_FULL;

        int ticks = modifiedProcessingTicks(recipe);
        int totalEnergy = modifiedEnergyCost(recipe);
        if (energy.getEnergyStored() < energyStep(totalEnergy, ticks)) return NO_ENERGY;
        return progress > 0 ? PROCESSING : READY;
    }

    private void modulesChanged() {
        energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_ENERGY_CAPACITY));
        progress = 0;
        activeRecipe = null;
        setChanged();
    }

    @Override
    public MachineOperatingState operatingState() {
        return switch (status(currentRecipe().orElse(null))) {
            case PROCESSING -> MachineOperatingState.WORKING;
            case NO_ENERGY -> MachineOperatingState.NO_ENERGY;
            case OUTPUT_FULL -> MachineOperatingState.OUTPUT_BLOCKED;
            case NO_RECIPE, NOT_ENOUGH_INPUT, NO_WATER -> MachineOperatingState.NO_RECIPE;
            default -> MachineOperatingState.IDLE;
        };
    }

    @Override public int progressTicks() { return progress; }
    @Override public int requiredTicks() { return currentRecipe().map(this::modifiedProcessingTicks).orElse(0); }

    public ItemStackHandler getInventory() { return inventory; }
    public MachineModuleInventory getModules() { return modules; }
    public ContainerData getDataAccess() { return data; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.put(NBT_MODULES, modules.serializeNBT());
        tag.putInt(NBT_ENERGY, energy.getEnergyStored());
        CompoundTag waterTag = new CompoundTag();
        water.writeToNBT(waterTag);
        tag.put(NBT_WATER, waterTag);
        tag.putInt(NBT_PROGRESS, progress);
        if (activeRecipe != null) tag.putString(NBT_ACTIVE_RECIPE, activeRecipe.toString());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        modules.deserializeNBT(tag.getCompound(NBT_MODULES));
        energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_ENERGY_CAPACITY));
        energy.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        if (tag.contains(NBT_WATER)) water.readFromNBT(tag.getCompound(NBT_WATER));
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        activeRecipe = tag.contains(NBT_ACTIVE_RECIPE)
                ? ResourceLocation.tryParse(tag.getString(NBT_ACTIVE_RECIPE)) : null;
        invalidateRecipe();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return side == Direction.DOWN ? LazyOptional.empty() : fluidCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) return fullItemCap.cast();
            return side == Direction.DOWN ? outputItemCap.cast() : inputItemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fullItemCap.invalidate();
        inputItemCap.invalidate();
        outputItemCap.invalidate();
        energyCap.invalidate();
        fluidCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fullItemCap = LazyOptional.of(() -> fullItems);
        inputItemCap = LazyOptional.of(() -> inputItems);
        outputItemCap = LazyOptional.of(() -> outputItems);
        energyCap = LazyOptional.of(() -> energyInput);
        fluidCap = LazyOptional.of(() -> fluidInput);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.organic_processor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OrganicProcessorMenu(id, inventory, this);
    }
}
