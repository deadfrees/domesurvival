package com.wasted.domesurvival.forge.machine.alloy;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.api.IProcessingMachine;
import com.wasted.domesurvival.forge.machine.api.MachineOperatingState;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.recipe.AlloyEnricherRecipe;
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

public final class AlloyEnricherBlockEntity extends BlockEntity
        implements net.minecraft.world.MenuProvider, IProcessingMachine {
    public static final int ENERGY_CAPACITY = 80_000;
    public static final int MAX_RECEIVE = 256;
    public static final int NEOFLUX_CAPACITY = 4_000;

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_NEOFLUX = 2;
    public static final int DATA_NEOFLUX_CAPACITY = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_PROGRESS_MAX = 5;
    public static final int DATA_STATUS = 6;
    public static final int DATA_RECIPE_ENERGY = 7;
    public static final int DATA_FLUID_REQUIRED = 8;
    public static final int DATA_COUNT = 9;

    public static final int READY = 0;
    public static final int ENRICHING = 1;
    public static final int NO_ENERGY = 2;
    public static final int NO_RECIPE = 3;
    public static final int NOT_ENOUGH_INPUT = 4;
    public static final int NO_NEOFLUX = 5;
    public static final int OUTPUT_FULL = 6;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_NEOFLUX = "Neoflux";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE_RECIPE = "ActiveRecipe";

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_INPUT && isValidInput(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_INPUT) {
                invalidateRecipe();
                progress = 0;
                activeRecipe = null;
            }
            setChanged();
        }
    };

    private final MachineEnergyStorage energy =
            new MachineEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0);

    private final FluidTank neoflux = new FluidTank(
            NEOFLUX_CAPACITY,
            stack -> stack.getFluid().isSame(ModFluids.NEOFLUX.get())
    ) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IItemHandler fullItems = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == SLOT_INPUT ? inventory.insertItem(SLOT_INPUT, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? inventory.extractItem(SLOT_OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_INPUT && inventory.isItemValid(SLOT_INPUT, stack);
        }
    };

    private final IItemHandler inputItems = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) {
            return slot == 0 ? inventory.getStackInSlot(SLOT_INPUT) : ItemStack.EMPTY;
        }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == 0 ? inventory.insertItem(SLOT_INPUT, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(SLOT_INPUT); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && inventory.isItemValid(SLOT_INPUT, stack);
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
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return neoflux.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return neoflux.getCapacity(); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return neoflux.isFluidValid(0, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return neoflux.fill(resource, action); }
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

    private ItemStack cachedInput = ItemStack.EMPTY;
    private Optional<AlloyEnricherRecipe> cachedRecipe = Optional.empty();
    private boolean recipeCacheValid;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            AlloyEnricherRecipe recipe = currentRecipe().orElse(null);
            if (index == DATA_ENERGY) return energy.getEnergyStored();
            if (index == DATA_ENERGY_CAPACITY) return energy.getMaxEnergyStored();
            if (index == DATA_NEOFLUX) return neoflux.getFluidAmount();
            if (index == DATA_NEOFLUX_CAPACITY) return neoflux.getCapacity();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_PROGRESS_MAX) return recipe == null ? 0 : recipe.getProcessingTime();
            if (index == DATA_STATUS) return status(recipe);
            if (index == DATA_RECIPE_ENERGY) return recipe == null ? 0 : recipe.getEnergy();
            if (index == DATA_FLUID_REQUIRED) return recipe == null ? 0 : recipe.getFluidInput().getAmount();
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public AlloyEnricherBlockEntity(BlockPos pos, BlockState state) {
        super(AlloyEnricherRegistry.ALLOY_ENRICHER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlloyEnricherBlockEntity enricher) {
        enricher.active = false;
        boolean changed = enricher.tickProcess();

        if (state.getValue(AlloyEnricherBlock.ACTIVE) != enricher.active) {
            level.setBlock(pos, state.setValue(AlloyEnricherBlock.ACTIVE, enricher.active), 3);
            changed = true;
        }
        if (changed) enricher.setChanged();
    }

    private boolean tickProcess() {
        AlloyEnricherRecipe recipe = currentRecipe().orElse(null);
        if (recipe == null || !hasRequiredInput(recipe)) {
            return resetProgressIfNeeded();
        }

        if (activeRecipe == null || !activeRecipe.equals(recipe.getId())) {
            progress = 0;
            activeRecipe = recipe.getId();
        }

        if (!hasRequiredFluid(recipe) || !canAcceptOutput(recipe.getResult())) {
            return false;
        }

        int ticks = recipe.getProcessingTime();
        int totalEnergy = recipe.getEnergy();
        int requiredThisTick = energyStep(totalEnergy, ticks);
        if (energy.getEnergyStored() < requiredThisTick) {
            return false;
        }

        energy.removeEnergyInternal(requiredThisTick);
        progress++;
        active = true;

        if (progress >= ticks) {
            if (finish(recipe)) {
                progress = 0;
                activeRecipe = null;
            }
        }
        return true;
    }

    private boolean finish(AlloyEnricherRecipe recipe) {
        if (!hasRequiredInput(recipe) || !hasRequiredFluid(recipe) || !canAcceptOutput(recipe.getResult())) {
            return false;
        }

        FluidStack request = recipe.getFluidInput();
        FluidStack simulated = neoflux.drain(request, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.getAmount() != request.getAmount()) {
            return false;
        }

        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        input.shrink(recipe.getInputCount());
        inventory.setStackInSlot(SLOT_INPUT, input);
        neoflux.drain(request, IFluidHandler.FluidAction.EXECUTE);
        mergeOutput(recipe.getResult());
        return true;
    }

    private int energyStep(int totalEnergy, int totalTicks) {
        int safeTicks = Math.max(1, totalTicks);
        int beforeProgress = Math.min(progress, safeTicks);
        int afterProgress = Math.min(progress + 1, safeTicks);
        int before = (int) ((long) totalEnergy * beforeProgress / safeTicks);
        int after = (int) ((long) totalEnergy * afterProgress / safeTicks);
        return Math.max(0, after - before);
    }

    private boolean resetProgressIfNeeded() {
        boolean changed = progress != 0 || activeRecipe != null;
        progress = 0;
        activeRecipe = null;
        return changed;
    }

    private boolean hasRequiredInput(AlloyEnricherRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        return recipe.acceptsIngredient(input) && input.getCount() >= recipe.getInputCount();
    }

    private boolean hasRequiredFluid(AlloyEnricherRecipe recipe) {
        FluidStack required = recipe.getFluidInput();
        FluidStack stored = neoflux.getFluid();
        return !stored.isEmpty()
                && stored.getFluid().isSame(required.getFluid())
                && stored.getAmount() >= required.getAmount();
    }

    private boolean canAcceptOutput(ItemStack addition) {
        if (addition.isEmpty()) return true;
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        int slotLimit = inventory.getSlotLimit(SLOT_OUTPUT);
        if (current.isEmpty()) return addition.getCount() <= Math.min(slotLimit, addition.getMaxStackSize());
        if (!ItemStack.isSameItemSameTags(current, addition)) return false;
        int limit = Math.min(slotLimit, current.getMaxStackSize());
        return current.getCount() + addition.getCount() <= limit;
    }

    private void mergeOutput(ItemStack addition) {
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, addition.copy());
        } else {
            current.grow(addition.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, current);
        }
    }

    private Optional<AlloyEnricherRecipe> currentRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return Optional.empty();

        if (recipeCacheValid && ItemStack.isSameItemSameTags(cachedInput, input)) {
            return cachedRecipe;
        }

        cachedRecipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ALLOY_ENRICHER_TYPE.get())
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
                .getAllRecipesFor(ModRecipes.ALLOY_ENRICHER_TYPE.get())
                .stream()
                .anyMatch(recipe -> recipe.acceptsIngredient(stack));
    }

    private void invalidateRecipe() {
        recipeCacheValid = false;
        cachedInput = ItemStack.EMPTY;
        cachedRecipe = Optional.empty();
    }

    private int status(@Nullable AlloyEnricherRecipe recipe) {
        if (recipe == null) return inventory.getStackInSlot(SLOT_INPUT).isEmpty() ? READY : NO_RECIPE;
        if (!hasRequiredInput(recipe)) return NOT_ENOUGH_INPUT;
        if (!canAcceptOutput(recipe.getResult())) return OUTPUT_FULL;
        if (!hasRequiredFluid(recipe)) return NO_NEOFLUX;
        int requiredThisTick = energyStep(recipe.getEnergy(), recipe.getProcessingTime());
        if (energy.getEnergyStored() < requiredThisTick) return NO_ENERGY;
        return progress > 0 ? ENRICHING : READY;
    }

    @Override
    public MachineOperatingState operatingState() {
        return switch (status(currentRecipe().orElse(null))) {
            case ENRICHING -> MachineOperatingState.WORKING;
            case NO_ENERGY -> MachineOperatingState.NO_ENERGY;
            case OUTPUT_FULL -> MachineOperatingState.OUTPUT_BLOCKED;
            case NO_RECIPE, NOT_ENOUGH_INPUT, NO_NEOFLUX -> MachineOperatingState.NO_RECIPE;
            default -> MachineOperatingState.IDLE;
        };
    }

    @Override public int progressTicks() { return progress; }
    @Override public int requiredTicks() { return currentRecipe().map(AlloyEnricherRecipe::getProcessingTime).orElse(0); }

    public ItemStackHandler getInventory() { return inventory; }
    public ContainerData getDataAccess() { return data; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_ENERGY, energy.getEnergyStored());
        CompoundTag fluidTag = new CompoundTag();
        neoflux.writeToNBT(fluidTag);
        tag.put(NBT_NEOFLUX, fluidTag);
        tag.putInt(NBT_PROGRESS, progress);
        if (activeRecipe != null) tag.putString(NBT_ACTIVE_RECIPE, activeRecipe.toString());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        energy.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        if (tag.contains(NBT_NEOFLUX)) neoflux.readFromNBT(tag.getCompound(NBT_NEOFLUX));
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
        return Component.translatable("block.domesurvival.alloy_enricher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AlloyEnricherMenu(id, inventory, this);
    }
}
