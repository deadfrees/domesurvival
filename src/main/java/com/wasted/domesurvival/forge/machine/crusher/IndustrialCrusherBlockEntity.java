package com.wasted.domesurvival.forge.machine.crusher;

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

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_MAX_PROGRESS = 3;
    public static final int DATA_RECIPE_ENERGY = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_COUNT = 6;

    public static final int READY = 0;
    public static final int CRUSHING = 1;
    public static final int NO_ENERGY = 2;
    public static final int NO_RECIPE = 3;
    public static final int OUTPUT_FULL = 4;
    public static final int NOT_ENOUGH_INPUT = 5;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_MODULES = "Modules";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE_RECIPE = "ActiveRecipe";

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && isValidInput(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0) {
                invalidateRecipe();
            }
            setChanged();
        }
    };

    private final MachineEnergyStorage energy =
            new MachineEnergyStorage(BASE_CAPACITY, MAX_RECEIVE, 0);

    private final MachineModuleInventory modules =
            new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);

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
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyInput);

    private int progress;
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
            if (index == DATA_MAX_PROGRESS) return recipe == null ? 0 : modifiedProcessingTicks(recipe);
            if (index == DATA_RECIPE_ENERGY) return recipe == null ? 0 : modifiedEnergyCost(recipe);
            if (index == DATA_STATUS) return status(recipe);
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public IndustrialCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public int moduleSlotCount() {
        return 2;
    }

    @Override
    public Set<MachineModuleType> allowedModuleTypes() {
        // Every accepted module has an immediate effect on this machine.
        return Set.of(
                MachineModuleType.EFFICIENCY,
                MachineModuleType.OVERDRIVE,
                MachineModuleType.BUFFER
        );
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IndustrialCrusherBlockEntity crusher) {
        crusher.active = false;
        boolean changed = crusher.tickProcess();

        if (state.getValue(IndustrialCrusherBlock.ACTIVE) != crusher.active) {
            level.setBlock(pos, state.setValue(IndustrialCrusherBlock.ACTIVE, crusher.active), 3);
            changed = true;
        }
        if (changed) {
            crusher.setChanged();
        }
    }

    private boolean tickProcess() {
        IndustrialCrusherRecipe recipe = currentRecipe().orElse(null);
        if (recipe == null) {
            return resetProgressIfNeeded();
        }

        if (!hasRequiredInput(recipe)) {
            return resetProgressIfNeeded();
        }

        if (activeRecipe == null || !activeRecipe.equals(recipe.getId())) {
            progress = 0;
            activeRecipe = recipe.getId();
        }

        // Reserve space for both possible outputs before spending energy. This is
        // intentionally conservative: a full byproduct slot can never make an item vanish.
        if (!canAccept(1, recipe.getResult()) || !canAccept(2, recipe.getByproduct())) {
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

    private int modifiedProcessingTicks(IndustrialCrusherRecipe recipe) {
        return Math.max(1, modules.modifiers().applyProcessingTicks(recipe.getProcessingTime()));
    }

    private int modifiedEnergyCost(IndustrialCrusherRecipe recipe) {
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

    private void finish(IndustrialCrusherRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(0);
        if (!hasRequiredInput(recipe)) {
            return;
        }

        input.shrink(recipe.getInputCount());
        inventory.setStackInSlot(0, input);
        merge(1, recipe.getResult());

        if (level != null
                && !recipe.getByproduct().isEmpty()
                && level.random.nextInt(10_000) < recipe.getByproductChancePerTenThousand()) {
            merge(2, recipe.getByproduct());
        }
    }

    private void merge(int slot, ItemStack addition) {
        if (addition.isEmpty()) {
            return;
        }

        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            inventory.setStackInSlot(slot, addition.copy());
            return;
        }

        current.grow(addition.getCount());
        inventory.setStackInSlot(slot, current);
    }

    private boolean canAccept(int slot, ItemStack addition) {
        if (addition.isEmpty()) {
            return true;
        }

        int slotLimit = Math.min(inventory.getSlotLimit(slot), addition.getMaxStackSize());
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            return addition.getCount() <= slotLimit;
        }
        if (!ItemStack.isSameItemSameTags(current, addition)) {
            return false;
        }

        int combinedLimit = Math.min(inventory.getSlotLimit(slot), current.getMaxStackSize());
        return current.getCount() + addition.getCount() <= combinedLimit;
    }

    private boolean hasRequiredInput(IndustrialCrusherRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(0);
        return recipe != null
                && recipe.acceptsIngredient(input)
                && input.getCount() >= recipe.getInputCount();
    }

    private Optional<IndustrialCrusherRecipe> currentRecipe() {
        if (level == null) {
            return Optional.empty();
        }

        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        if (recipeCacheValid && ItemStack.isSameItemSameTags(cachedInput, input)) {
            return cachedRecipe;
        }

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
        if (recipe == null) {
            return inventory.getStackInSlot(0).isEmpty() ? READY : NO_RECIPE;
        }
        if (!hasRequiredInput(recipe)) {
            return NOT_ENOUGH_INPUT;
        }
        if (!canAccept(1, recipe.getResult()) || !canAccept(2, recipe.getByproduct())) {
            return OUTPUT_FULL;
        }

        int ticks = modifiedProcessingTicks(recipe);
        int totalEnergy = modifiedEnergyCost(recipe);
        if (energy.getEnergyStored() < energyStep(totalEnergy, ticks)) {
            return NO_ENERGY;
        }
        return progress > 0 ? CRUSHING : READY;
    }

    private void modulesChanged() {
        energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_CAPACITY));
        progress = 0;
        activeRecipe = null;
        setChanged();
    }

    @Override
    public MachineOperatingState operatingState() {
        IndustrialCrusherRecipe recipe = currentRecipe().orElse(null);
        return switch (status(recipe)) {
            case CRUSHING -> MachineOperatingState.WORKING;
            case NO_ENERGY -> MachineOperatingState.NO_ENERGY;
            case NO_RECIPE, NOT_ENOUGH_INPUT -> MachineOperatingState.NO_RECIPE;
            case OUTPUT_FULL -> MachineOperatingState.OUTPUT_BLOCKED;
            default -> MachineOperatingState.IDLE;
        };
    }

    @Override
    public int progressTicks() {
        return progress;
    }

    @Override
    public int requiredTicks() {
        return currentRecipe().map(this::modifiedProcessingTicks).orElse(0);
    }

    public ItemStackHandler getInventory() { return inventory; }
    public MachineModuleInventory getModules() { return modules; }
    public ContainerData getDataAccess() { return data; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.put(NBT_MODULES, modules.serializeNBT());
        tag.putInt(NBT_ENERGY, energy.getEnergyStored());
        tag.putInt(NBT_PROGRESS, progress);
        if (activeRecipe != null) {
            tag.putString(NBT_ACTIVE_RECIPE, activeRecipe.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        modules.deserializeNBT(tag.getCompound(NBT_MODULES));
        energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_CAPACITY));
        energy.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        activeRecipe = tag.contains(NBT_ACTIVE_RECIPE)
                ? ResourceLocation.tryParse(tag.getString(NBT_ACTIVE_RECIPE))
                : null;
        invalidateRecipe();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCap = LazyOptional.of(() -> items);
        energyCap = LazyOptional.of(() -> energyInput);
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
