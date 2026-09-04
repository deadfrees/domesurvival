package com.wasted.domesurvival.forge.machine.forming;

import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import com.wasted.domesurvival.forge.recipe.FormingPressRecipe;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
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

import java.util.Optional;

public final class FormingPressBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_INPUT_PER_TICK = 128;

    public static final int STATUS_READY = 0;
    public static final int STATUS_FORMING = 1;
    public static final int STATUS_NO_ENERGY = 2;
    public static final int STATUS_NO_RECIPE = 3;
    public static final int STATUS_OUTPUT_FULL = 4;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_MAX_PROGRESS = 3;
    public static final int DATA_RECIPE_ENERGY = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_OPERATION = 6;
    public static final int DATA_SIDES_START = 7;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_RECIPE = "ActiveRecipe";
    private static final String NBT_OPERATION = "Operation";

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && isValidFormingInput(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0) invalidateRecipeCache();
            setChanged();
        }
    };

    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, MAX_INPUT_PER_TICK, 0);

    private final IItemHandler itemInputView = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == 0 ? inventory.insertItem(0, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && inventory.isItemValid(0, stack);
        }
    };

    private final IItemHandler itemOutputView = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? inventory.extractItem(1, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    private final IItemHandler itemCombinedView = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return itemInputView.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemOutputView.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return itemInputView.isItemValid(slot, stack);
        }
    };

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = energyStorage.receiveEnergy(maxReceive, simulate);
            if (!simulate && accepted > 0) {
                setChanged();
            }
            return accepted;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IItemHandler> itemInputCapability = LazyOptional.of(() -> itemInputView);
    private LazyOptional<IItemHandler> itemOutputCapability = LazyOptional.of(() -> itemOutputView);
    private LazyOptional<IItemHandler> itemCombinedCapability = LazyOptional.of(() -> itemCombinedView);
    private LazyOptional<IEnergyStorage> energyInputCapability = LazyOptional.of(() -> energyInputView);

    private int progress;
    private boolean processingThisTick;
    private FormingOperation selectedOperation = FormingOperation.PRESS;
    @Nullable private ResourceLocation activeRecipeId;

    @Nullable private RecipeManager cachedRecipeManager;
    private ItemStack cachedRecipeInput = ItemStack.EMPTY;
    private FormingOperation cachedRecipeOperation = FormingOperation.PRESS;
    private Optional<FormingPressRecipe> cachedRecipe = Optional.empty();
    private boolean recipeCacheValid;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            FormingPressRecipe recipe = getCurrentRecipe().orElse(null);
            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_CAPACITY) return energyStorage.getMaxEnergyStored();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_MAX_PROGRESS) return recipe == null ? 0 : recipe.getProcessingTime();
            if (index == DATA_RECIPE_ENERGY) return recipe == null ? 0 : recipe.getEnergy();
            if (index == DATA_STATUS) return getStatus(recipe);
            if (index == DATA_OPERATION) return selectedOperation.ordinal();
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) {
                Direction direction = Direction.values()[index - DATA_SIDES_START];
                return sideConfig.getMode(direction).ordinal();
            }
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public FormingPressBlockEntity(BlockPos pos, BlockState state) {
        super(FormingPressRegistry.FORMING_PRESS_BLOCK_ENTITY.get(), pos, state);
        applyDefaultSideConfiguration();
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        sideConfig.setMode(RelativeSide.TOP.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.BACK.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.RIGHT.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.FRONT.resolve(facing), SideMode.DISABLED);
    }

    public static boolean isConfigurableSide(RelativeSide side) {
        return side != RelativeSide.FRONT;
    }

    private boolean isFrontWorldSide(Direction side) {
        return side == getMachineFacing();
    }

    /**
     * The rear connector is a guaranteed Forge Energy input, matching the original GOTEICRAFT
     * machine layout. Other faces configured as INPUT remain valid FE inputs for automation.
     * Keeping the rear independent from saved generic side modes also repairs old worlds where
     * a stale side configuration could make the press invisible to EnderIO/Mekanism/Thermal cables.
     */
    private boolean isEnergyInputSide(@Nullable Direction side) {
        if (side == null) return true;
        if (isFrontWorldSide(side)) return false;
        Direction rear = RelativeSide.BACK.resolve(getMachineFacing());
        return side == rear || sideConfig.allowsInput(side);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FormingPressBlockEntity press) {
        press.processingThisTick = false;
        boolean changed = press.tickProcessing();
        if (state.getValue(FormingPressBlock.ACTIVE) != press.processingThisTick) {
            level.setBlock(pos, state.setValue(FormingPressBlock.ACTIVE, press.processingThisTick), 3);
            changed = true;
        }
        if (changed) press.setChanged();
    }

    private boolean tickProcessing() {
        Optional<FormingPressRecipe> recipeOptional = getCurrentRecipe();
        if (recipeOptional.isEmpty()) {
            if (progress != 0 || activeRecipeId != null) {
                progress = 0;
                activeRecipeId = null;
                return true;
            }
            return false;
        }

        FormingPressRecipe recipe = recipeOptional.get();
        if (activeRecipeId == null || !activeRecipeId.equals(recipe.getId())) {
            progress = 0;
            activeRecipeId = recipe.getId();
        }
        if (!canAcceptResult(recipe)) return false;

        int requiredEnergy = getEnergyForNextTick(recipe);
        if (energyStorage.getEnergyStored() < requiredEnergy) return false;

        energyStorage.removeEnergyInternal(requiredEnergy);
        progress++;
        processingThisTick = true;
        if (progress >= recipe.getProcessingTime()) {
            finishRecipe(recipe);
            progress = 0;
            activeRecipeId = null;
        }
        return true;
    }

    private int getEnergyForNextTick(FormingPressRecipe recipe) {
        int totalTicks = Math.max(1, recipe.getProcessingTime());
        int completedBefore = Math.min(progress, totalTicks);
        int completedAfter = Math.min(progress + 1, totalTicks);
        int before = (int) ((long) recipe.getEnergy() * completedBefore / totalTicks);
        int after = (int) ((long) recipe.getEnergy() * completedAfter / totalTicks);
        return Math.max(0, after - before);
    }

    private void finishRecipe(FormingPressRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty() || input.getCount() < recipe.getInputCount()) return;

        ItemStack result = recipe.getResult();
        ItemStack output = inventory.getStackInSlot(1);
        input.shrink(recipe.getInputCount());
        inventory.setStackInSlot(0, input);
        if (output.isEmpty()) {
            inventory.setStackInSlot(1, result);
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(1, output);
        }
    }

    public boolean isValidFormingInput(@NotNull ItemStack stack) {
        return isValidFormingInput(level, stack);
    }

    public static boolean isValidFormingInput(@Nullable Level level, @NotNull ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.FORMING_TYPE.get())
                .stream()
                .anyMatch(recipe -> recipe.acceptsIngredient(stack));
    }

    private Optional<FormingPressRecipe> getCurrentRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return Optional.empty();

        RecipeManager recipeManager = level.getRecipeManager();
        if (recipeCacheValid
                && cachedRecipeManager == recipeManager
                && cachedRecipeOperation == selectedOperation
                && ItemStack.isSameItemSameTags(cachedRecipeInput, input)) {
            return cachedRecipe;
        }

        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, input);
        Optional<FormingPressRecipe> found = recipeManager
                .getRecipesFor(ModRecipes.FORMING_TYPE.get(), container, level)
                .stream()
                .filter(recipe -> recipe.getOperation() == selectedOperation)
                .findFirst();

        cachedRecipeManager = recipeManager;
        cachedRecipeInput = input.copyWithCount(1);
        cachedRecipeOperation = selectedOperation;
        cachedRecipe = found;
        recipeCacheValid = true;
        return found;
    }

    private void invalidateRecipeCache() {
        recipeCacheValid = false;
        cachedRecipeManager = null;
        cachedRecipeInput = ItemStack.EMPTY;
        cachedRecipe = Optional.empty();
    }

    private boolean canAcceptResult(FormingPressRecipe recipe) {
        ItemStack result = recipe.getResult();
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        int max = Math.min(output.getMaxStackSize(), inventory.getSlotLimit(1));
        return output.getCount() + result.getCount() <= max;
    }

    private int getStatus(@Nullable FormingPressRecipe recipe) {
        if (recipe == null) {
            return inventory.getStackInSlot(0).isEmpty() ? STATUS_READY : STATUS_NO_RECIPE;
        }
        if (!canAcceptResult(recipe)) return STATUS_OUTPUT_FULL;
        if (energyStorage.getEnergyStored() < getEnergyForNextTick(recipe)) return STATUS_NO_ENERGY;
        return progress > 0 ? STATUS_FORMING : STATUS_READY;
    }

    public FormingOperation getSelectedOperation() {
        return selectedOperation;
    }

    public FormingOperation cycleOperation() {
        return setSelectedOperation(selectedOperation.next());
    }

    public FormingOperation setSelectedOperation(FormingOperation operation) {
        FormingOperation sanitized = operation == null ? FormingOperation.PRESS : operation;
        if (selectedOperation != sanitized) {
            selectedOperation = sanitized;
            progress = 0;
            activeRecipeId = null;
            invalidateRecipeCache();
            setChanged();
        }
        return selectedOperation;
    }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction direction = relativeSide.resolve(getMachineFacing());
        SideMode mode = sideConfig.cycleMode(direction);
        refreshCapabilities();
        syncPortState(direction);
        setChanged();
        return mode;
    }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof FormingPressBlock)) return;
        PortVisual visual = isFrontWorldSide(direction)
                ? PortVisual.OFF
                : PortVisual.fromMode(sideConfig.getMode(direction));
        var property = FormingPressBlock.portProperty(direction);
        if (state.getValue(property) != visual) {
            level.setBlock(worldPosition, state.setValue(property, visual), 3);
        }
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof FormingPressBlock)) return;
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction)
                    ? PortVisual.OFF
                    : PortVisual.fromMode(sideConfig.getMode(direction));
            updated = updated.setValue(FormingPressBlock.portProperty(direction), visual);
        }
        if (!updated.equals(state)) level.setBlock(worldPosition, updated, 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        invalidateRecipeCache();
        syncAllPortStates();
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(FormingPressBlock.FACING)
                ? state.getValue(FormingPressBlock.FACING)
                : Direction.NORTH;
    }

    public ItemStackHandler getInventory() { return inventory; }
    public ContainerData getDataAccess() { return dataAccess; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(NBT_PROGRESS, progress);
        tag.putString(NBT_OPERATION, selectedOperation.getSerializedName());
        if (activeRecipeId != null) tag.putString(NBT_RECIPE, activeRecipeId.toString());
        sideConfig.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        selectedOperation = FormingOperation.fromSerializedName(tag.getString(NBT_OPERATION));
        activeRecipeId = tag.contains(NBT_RECIPE)
                ? ResourceLocation.tryParse(tag.getString(NBT_RECIPE))
                : null;
        invalidateRecipeCache();
        if (!sideConfig.load(tag)) applyDefaultSideConfiguration();
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);
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
            return isEnergyInputSide(side) ? energyInputCapability.cast() : LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    private void refreshCapabilities() {
        itemInputCapability.invalidate();
        itemOutputCapability.invalidate();
        itemCombinedCapability.invalidate();
        energyInputCapability.invalidate();
        itemInputCapability = LazyOptional.of(() -> itemInputView);
        itemOutputCapability = LazyOptional.of(() -> itemOutputView);
        itemCombinedCapability = LazyOptional.of(() -> itemCombinedView);
        energyInputCapability = LazyOptional.of(() -> energyInputView);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemInputCapability.invalidate();
        itemOutputCapability.invalidate();
        itemCombinedCapability.invalidate();
        energyInputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemInputCapability = LazyOptional.of(() -> itemInputView);
        itemOutputCapability = LazyOptional.of(() -> itemOutputView);
        itemCombinedCapability = LazyOptional.of(() -> itemCombinedView);
        energyInputCapability = LazyOptional.of(() -> energyInputView);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.forming_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FormingPressMenu(containerId, playerInventory, this);
    }
}
