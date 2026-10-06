package com.wasted.domesurvival.forge.machine.organic;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.api.IProcessingMachine;
import com.wasted.domesurvival.forge.machine.api.MachineOperatingState;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.module.IModularMachine;
import com.wasted.domesurvival.forge.machine.module.MachineModuleInventory;
import com.wasted.domesurvival.forge.machine.module.MachineModuleResolver;
import com.wasted.domesurvival.forge.machine.module.MachineModuleType;
import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import com.wasted.domesurvival.forge.recipe.OrganicProcessorRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
    public static final int DATA_SIDES_START = 9;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

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

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

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

    private final java.util.EnumMap<Direction, Port> ports = new java.util.EnumMap<>(Direction.class);

    private int progress;
    private int cycleTicks, cycleEnergy;
    private float animationTick, previousAnimationTick;
    private Direction lastFacing;
    private boolean syncPending;
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
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) {
                return sideConfig.getMode(Direction.values()[index - DATA_SIDES_START]).ordinal();
            }
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public OrganicProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(OrganicProcessorRegistry.ORGANIC_PROCESSOR_BLOCK_ENTITY.get(), pos, state);
        applyDefaultSideConfiguration();
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

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        sideConfig.setMode(RelativeSide.TOP.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.LEFT.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.BACK.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.BOTTOM.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.RIGHT.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.FRONT.resolve(facing), SideMode.DISABLED);
    }

    public static boolean isConfigurableSide(RelativeSide side) {
        return side != RelativeSide.FRONT;
    }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction worldSide = relativeSide.resolve(getMachineFacing());
        SideMode mode = sideConfig.cycleMode(worldSide);
        refreshCapabilities();
        syncPortState(worldSide);
        setChanged();
        return mode;
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(OrganicProcessorBlock.FACING)
                ? state.getValue(OrganicProcessorBlock.FACING)
                : Direction.NORTH;
    }

    public void rotateSideConfiguration(@Nullable Direction previous) {
        Direction facing = getMachineFacing();
        if (previous != null && previous != facing) {
            var modes = new java.util.EnumMap<RelativeSide,SideMode>(RelativeSide.class);
            for (var side : RelativeSide.values()) modes.put(side,sideConfig.getMode(side.resolve(previous)));
            for (var side : RelativeSide.values()) sideConfig.setMode(side.resolve(facing),side==RelativeSide.FRONT?SideMode.DISABLED:modes.get(side));
        }
        sideConfig.setMode(facing,SideMode.DISABLED);
        lastFacing = facing;
        refreshCapabilities();
        syncAllPortStates();
        setChanged();
    }

    public static void clientTick(Level level,BlockPos pos,BlockState state,OrganicProcessorBlockEntity processor) {
        processor.previousAnimationTick = processor.animationTick;
        if (state.getValue(OrganicProcessorBlock.ACTIVE)) processor.animationTick = (processor.animationTick + 1) % 80;
    }
    public float animationPhase(float partialTick) {
        float next = animationTick < previousAnimationTick ? animationTick + 80 : animationTick;
        return (previousAnimationTick + (next-previousAnimationTick)*partialTick)/80F;
    }
    @Override public void setChanged(){super.setChanged();syncPending=true;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}

    public static void serverTick(Level level, BlockPos pos, BlockState state, OrganicProcessorBlockEntity processor) {
        if (processor.lastFacing != processor.getMachineFacing()) processor.rotateSideConfiguration(processor.lastFacing);
        processor.active = false;
        boolean changed = processor.tickProcess();

        if (state.getValue(OrganicProcessorBlock.ACTIVE) != processor.active) {
            level.setBlock(pos, level.getBlockState(pos).setValue(OrganicProcessorBlock.ACTIVE, processor.active), 3);
            changed = true;
        }
        if (changed) processor.setChanged();
        if (processor.syncPending && level.getGameTime()%10L==0) {
            processor.syncPending=false;
            level.sendBlockUpdated(pos,level.getBlockState(pos),level.getBlockState(pos),2);
        }
    }

    private boolean tickProcess() {
        OrganicProcessorRecipe recipe = currentRecipe().orElse(null);
        if (recipe == null || !hasRequiredInput(recipe)) {
            return resetProgressIfNeeded();
        }

        if (activeRecipe == null || !activeRecipe.equals(recipe.getId())) {
            progress = 0;
            cycleTicks = cycleEnergy = 0;
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

        if (cycleTicks == 0) { cycleTicks = ticks; cycleEnergy = totalEnergy; }
        energy.removeEnergyInternal(requiredThisTick);
        progress++;
        active = true;

        if (progress >= ticks) {
            finish(recipe);
            progress = 0;
            cycleTicks = cycleEnergy = 0;
            activeRecipe = null;
        }
        return true;
    }

    private boolean resetProgressIfNeeded() {
        boolean changed = progress != 0 || activeRecipe != null;
        progress = 0;
        cycleTicks = cycleEnergy = 0;
        activeRecipe = null;
        return changed;
    }

    private int modifiedProcessingTicks(OrganicProcessorRecipe recipe) {
        return cycleTicks > 0 ? cycleTicks : Math.max(1, modules.modifiers().applyProcessingTicks(recipe.getProcessingTime()));
    }

    private int modifiedEnergyCost(OrganicProcessorRecipe recipe) {
        return cycleTicks > 0 ? cycleEnergy : Math.max(1, modules.modifiers().applyEnergyCost(recipe.getEnergy()));
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
        // GUI prevents removing an overfilled buffer. Retain FE even for external forced edits.
        energy.setCapacityInternal(Math.max(energy.getEnergyStored(), modules.modifiers().applyBufferCapacity(BASE_ENERGY_CAPACITY)));
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
        tag.putInt("CycleTicks", cycleTicks);
        tag.putInt("CycleEnergy", cycleEnergy);
        tag.putString("PortFacing", getMachineFacing().getName());
        if (activeRecipe != null) tag.putString(NBT_ACTIVE_RECIPE, activeRecipe.toString());
        sideConfig.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        modules.deserializeNBT(tag.getCompound(NBT_MODULES));
        energy.setCapacityInternal(Math.max(tag.getInt(NBT_ENERGY), modules.modifiers().applyBufferCapacity(BASE_ENERGY_CAPACITY)));
        energy.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        if (tag.contains(NBT_WATER)) water.readFromNBT(tag.getCompound(NBT_WATER));
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        cycleTicks = Math.max(0, tag.getInt("CycleTicks"));
        cycleEnergy = Math.max(0, tag.getInt("CycleEnergy"));
        activeRecipe = tag.contains(NBT_ACTIVE_RECIPE)
                ? ResourceLocation.tryParse(tag.getString(NBT_ACTIVE_RECIPE)) : null;
        if (!sideConfig.load(tag)) applyDefaultSideConfiguration();
        rotateSideConfiguration(Direction.byName(tag.getString("PortFacing")));
        invalidateRecipe();
    }

    private boolean isFrontWorldSide(Direction side) {
        return side == getMachineFacing();
    }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof OrganicProcessorBlock)) return;

        PortVisual visual = isFrontWorldSide(direction)
                ? PortVisual.OFF
                : PortVisual.fromMode(sideConfig.getMode(direction));
        var property = OrganicProcessorBlock.portProperty(direction);

        if (state.getValue(property) != visual) {
            level.setBlock(worldPosition, state.setValue(property, visual), 3);
        }
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof OrganicProcessorBlock)) return;

        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction)
                    ? PortVisual.OFF
                    : PortVisual.fromMode(sideConfig.getMode(direction));
            updated = updated.setValue(OrganicProcessorBlock.portProperty(direction), visual);
        }

        if (!updated.equals(state)) {
            level.setBlock(worldPosition, updated, 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncAllPortStates();
    }

    public SideMode sideMode(@Nullable Direction side) {
        return side == null || side == getMachineFacing() ? SideMode.DISABLED : sideConfig.getMode(side);
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,@Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER || cap == ForgeCapabilities.ENERGY || cap == ForgeCapabilities.FLUID_HANDLER) {
            if (isRemoved() || sideMode(side) == SideMode.DISABLED) return LazyOptional.empty();
            Port port = ports.computeIfAbsent(side, Port::new);
            if (cap == ForgeCapabilities.ITEM_HANDLER) return port.items.cast();
            if (sideMode(side) != SideMode.INPUT) return LazyOptional.empty();
            return cap == ForgeCapabilities.ENERGY ? port.power.cast() : port.fluid.cast();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCapabilities() {
        ports.values().forEach(p -> {p.items.invalidate();p.power.invalidate();p.fluid.invalidate();});
        ports.clear();
    }
    @Override public void invalidateCaps(){super.invalidateCaps();refreshCapabilities();}
    @Override public void reviveCaps(){super.reviveCaps();refreshCapabilities();}

    private final class Port implements IItemHandler {
        final Direction side;
        Port(Direction side){this.side=side;}
        boolean input(){return !isRemoved() && sideMode(side)==SideMode.INPUT;}
        boolean output(){return !isRemoved() && sideMode(side)==SideMode.OUTPUT;}
        final LazyOptional<IItemHandler> items=LazyOptional.of(()->this);
        final LazyOptional<IEnergyStorage> power=LazyOptional.of(()->new IEnergyStorage(){
            public int receiveEnergy(int n,boolean sim){int accepted=input()?energy.receiveEnergy(n,sim):0;if(accepted>0&&!sim)setChanged();return accepted;}
            public int extractEnergy(int n,boolean sim){return 0;}
            public boolean canReceive(){return input();} public boolean canExtract(){return false;}
            public int getEnergyStored(){return energy.getEnergyStored();} public int getMaxEnergyStored(){return energy.getMaxEnergyStored();}
        });
        final LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->new IFluidHandler(){
            public int getTanks(){return 1;}
            public FluidStack getFluidInTank(int tank){return tank==0&&input()?water.getFluid().copy():FluidStack.EMPTY;}
            public int getTankCapacity(int tank){return tank==0?water.getCapacity():0;}
            public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&input()&&water.isFluidValid(tank,stack);}
            public int fill(FluidStack stack,FluidAction action){return input()?water.fill(stack,action):0;}
            public FluidStack drain(FluidStack stack,FluidAction action){return FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
        });
        public int getSlots(){return 3;}
        public ItemStack getStackInSlot(int slot){return slot>=0&&slot<3&&(input()&&slot<2||output()&&slot==2)?inventory.getStackInSlot(slot).copy():ItemStack.EMPTY;}
        public boolean isItemValid(int slot,ItemStack stack){return slot>=0&&slot<2&&input()&&inventory.isItemValid(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return slot==2&&output()?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot>=0&&slot<3?inventory.getSlotLimit(slot):0;}
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
