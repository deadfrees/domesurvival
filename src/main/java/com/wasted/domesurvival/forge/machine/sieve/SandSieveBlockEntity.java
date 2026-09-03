package com.wasted.domesurvival.forge.machine.sieve;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.item.SieveMeshItem;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SandSieveBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_SAND = 0;
    public static final int SLOT_MESH = 1;
    public static final int SLOT_OUTPUT_FIRST = 2;
    public static final int SLOT_OUTPUT_LAST = 4;
    public static final int SLOT_COUNT = 5;

    public static final int WATER_CAPACITY = 2_000;
    public static final int WET_WATER_PER_CYCLE = 250;
    public static final int DRY_SAND_PER_CYCLE = 1;
    public static final int WET_SAND_PER_CYCLE = 4;
    public static final int DRY_PROCESS_TICKS = 80;
    public static final int WET_PROCESS_TICKS = 60;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_READY_DRY = 1;
    public static final int STATUS_READY_WET = 2;
    public static final int STATUS_RUNNING_DRY = 3;
    public static final int STATUS_RUNNING_WET = 4;
    public static final int STATUS_NO_SAND = 5;
    public static final int STATUS_NO_MESH = 6;
    public static final int STATUS_OUTPUT_BLOCKED = 7;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROGRESS_MAX = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_WATER_CAPACITY = 3;
    public static final int DATA_STATUS = 4;
    public static final int DATA_WET_CYCLE = 5;
    public static final int DATA_COUNT = 6;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_WATER = "PurifiedWater";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_WET = "WetCycle";
    private static final String NBT_CYCLE_STARTED_AT = "CycleStartedAt";

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_SAND) return isSand(stack);
            if (slot == SLOT_MESH) return stack.getItem() instanceof SieveMeshItem;
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChangedAndSync();
        }
    };

    private final FluidTank waterTank = new FluidTank(WATER_CAPACITY, SandSieveBlockEntity::isAcceptedWater) {
        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (!isAcceptedWater(resource)) return 0;
            // Both ordinary and purified water are supported by pipes. Internally
            // they are normalized so a bucket can top up a tank filled by a pipe.
            return super.fill(new FluidStack(Fluids.WATER, resource.getAmount()), action);
        }

        @Override
        protected void onContentsChanged() {
            setChangedAndSync();
        }
    };

    private LazyOptional<IItemHandler> inputCapability = LazyOptional.of(
            () -> new RangedWrapper(inventory, SLOT_SAND, SLOT_MESH + 1));
    private LazyOptional<IItemHandler> outputCapability = LazyOptional.of(
            () -> new RangedWrapper(inventory, SLOT_OUTPUT_FIRST, SLOT_OUTPUT_LAST + 1));
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> waterTank);

    private int progress;
    private boolean wetCycle;
    private long cycleStartedAt;
    private int status = STATUS_IDLE;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROGRESS_MAX -> wetCycle ? WET_PROCESS_TICKS : DRY_PROCESS_TICKS;
                case DATA_WATER -> waterTank.getFluidAmount();
                case DATA_WATER_CAPACITY -> waterTank.getCapacity();
                case DATA_STATUS -> status;
                case DATA_WET_CYCLE -> wetCycle ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public SandSieveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAND_SIEVE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SandSieveBlockEntity sieve) {
        boolean wasRunning = sieve.progress > 0;
        if (wasRunning) {
            if (!sieve.hasCycleInputs()) {
                sieve.progress = 0;
                sieve.setActive(false);
                sieve.setChangedAndSync();
            } else {
                sieve.progress++;
                if (sieve.progress >= (sieve.wetCycle ? WET_PROCESS_TICKS : DRY_PROCESS_TICKS)) {
                    sieve.finishCycle();
                    sieve.progress = 0;
                    sieve.setActive(false);
                }
                sieve.setChanged();
            }
        }
        if (level.getGameTime() % 4L == 0L) sieve.pushOutputsDown();
        sieve.status = sieve.calculateStatus();
        if (wasRunning != (sieve.progress > 0)) sieve.setChangedAndSync();
    }

    public boolean tryStartCycle() {
        if (level == null || level.isClientSide || progress > 0) return false;
        status = calculateStatus();
        if (status != STATUS_READY_DRY && status != STATUS_READY_WET) return false;
        wetCycle = status == STATUS_READY_WET;
        cycleStartedAt = level.getGameTime();
        progress = 1;
        status = wetCycle ? STATUS_RUNNING_WET : STATUS_RUNNING_DRY;
        setActive(true);
        setChangedAndSync();
        return true;
    }

    private int calculateStatus() {
        if (progress > 0) return wetCycle ? STATUS_RUNNING_WET : STATUS_RUNNING_DRY;
        ItemStack sand = inventory.getStackInSlot(SLOT_SAND);
        if (!isSand(sand)) return STATUS_NO_SAND;
        if (!(inventory.getStackInSlot(SLOT_MESH).getItem() instanceof SieveMeshItem)) return STATUS_NO_MESH;
        if (!outputsEmpty()) return STATUS_OUTPUT_BLOCKED;
        if (waterTank.getFluidAmount() >= WET_WATER_PER_CYCLE && sand.getCount() >= WET_SAND_PER_CYCLE) {
            return STATUS_READY_WET;
        }
        return STATUS_READY_DRY;
    }

    private boolean hasCycleInputs() {
        ItemStack sand = inventory.getStackInSlot(SLOT_SAND);
        int required = wetCycle ? WET_SAND_PER_CYCLE : DRY_SAND_PER_CYCLE;
        return isSand(sand) && sand.getCount() >= required
                && inventory.getStackInSlot(SLOT_MESH).getItem() instanceof SieveMeshItem
                && outputsEmpty();
    }

    private boolean outputsEmpty() {
        for (int slot = SLOT_OUTPUT_FIRST; slot <= SLOT_OUTPUT_LAST; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    private void pushOutputsDown() {
        if (level == null) return;
        BlockEntity target = level.getBlockEntity(worldPosition.below());
        if (target == null) return;
        target.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(handler -> {
            for (int sourceSlot = SLOT_OUTPUT_FIRST; sourceSlot <= SLOT_OUTPUT_LAST; sourceSlot++) {
                ItemStack source = inventory.getStackInSlot(sourceSlot);
                if (source.isEmpty()) continue;
                ItemStack remainder = source.copy();
                for (int targetSlot = 0; targetSlot < handler.getSlots() && !remainder.isEmpty(); targetSlot++) {
                    remainder = handler.insertItem(targetSlot, remainder, false);
                }
                if (remainder.getCount() != source.getCount()) {
                    inventory.setStackInSlot(sourceSlot, remainder);
                }
            }
        });
    }

    private void finishCycle() {
        if (level == null || !hasCycleInputs()) return;
        SieveMeshItem mesh = (SieveMeshItem) inventory.getStackInSlot(SLOT_MESH).getItem();
        inventory.extractItem(SLOT_SAND, wetCycle ? WET_SAND_PER_CYCLE : DRY_SAND_PER_CYCLE, false);
        if (wetCycle) waterTank.drain(WET_WATER_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);

        ListBuilder results = wetCycle ? wetResults(mesh.tier()) : dryResults(mesh.tier());
        int slot = SLOT_OUTPUT_FIRST;
        for (ItemStack result : results.values) {
            if (!result.isEmpty() && slot <= SLOT_OUTPUT_LAST) inventory.setStackInSlot(slot++, result);
        }
        damageMesh();
        setChangedAndSync();
    }

    private ListBuilder dryResults(SieveMeshItem.Tier tier) {
        ListBuilder result = new ListBuilder();
        SieveDropTable.Chances chances = SieveDropTable.dry(tier);
        addRolledResult(result, chances, level.random.nextInt(100));
        return result;
    }

    private ListBuilder wetResults(SieveMeshItem.Tier tier) {
        ListBuilder result = new ListBuilder();
        SieveDropTable.Chances chances = SieveDropTable.wet(tier);
        int clayCount = level.random.nextInt(100) < chances.clayPercent() ? 2 : 1;
        result.add(new ItemStack(Items.CLAY_BALL, clayCount));
        addRolledResult(result, chances, level.random.nextInt(100), false);
        return result;
    }

    private static void addRolledResult(ListBuilder result, SieveDropTable.Chances chances, int roll) {
        addRolledResult(result, chances, roll, true);
    }

    private static void addRolledResult(ListBuilder result, SieveDropTable.Chances chances,
                                        int roll, boolean includeClay) {
        if (includeClay) {
            if (roll < chances.clayPercent()) {
                result.add(new ItemStack(Items.CLAY_BALL));
                return;
            }
            roll -= chances.clayPercent();
        }
        if (roll < chances.flintPercent()) {
            result.add(new ItemStack(Items.FLINT));
            return;
        }
        roll -= chances.flintPercent();
        if (roll < chances.boneMealPercent()) {
            result.add(new ItemStack(Items.BONE_MEAL));
            return;
        }
        roll -= chances.boneMealPercent();
        if (roll < chances.rawCopperPercent()) {
            result.add(new ItemStack(Items.RAW_COPPER));
            return;
        }
        roll -= chances.rawCopperPercent();
        if (roll < chances.ironNuggetPercent()) {
            result.add(new ItemStack(Items.IRON_NUGGET));
            return;
        }
        roll -= chances.ironNuggetPercent();
        if (roll < chances.goldNuggetPercent()) {
            result.add(new ItemStack(Items.GOLD_NUGGET));
            return;
        }
        roll -= chances.goldNuggetPercent();
        if (roll < chances.redstonePercent()) {
            result.add(new ItemStack(Items.REDSTONE));
        }
    }

    private void damageMesh() {
        ItemStack stack = inventory.getStackInSlot(SLOT_MESH);
        if (!(stack.getItem() instanceof SieveMeshItem)) return;
        int damage = stack.getDamageValue() + 1;
        if (damage >= stack.getMaxDamage()) inventory.setStackInSlot(SLOT_MESH, ItemStack.EMPTY);
        else {
            stack.setDamageValue(damage);
            inventory.setStackInSlot(SLOT_MESH, stack);
        }
    }

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(SandSieveBlock.ACTIVE) && state.getValue(SandSieveBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(SandSieveBlock.ACTIVE, active), 3);
        }
    }

    private static boolean isSand(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(BlockTags.SAND);
    }

    private static boolean isAcceptedWater(FluidStack stack) {
        return !stack.isEmpty() && (stack.getFluid().isSame(Fluids.WATER)
                || stack.getFluid().isSame(ModFluids.PURIFIED_WATER.get()));
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public ItemStackHandler getInventory() { return inventory; }
    public FluidTank getWaterTank() { return waterTank; }
    public ContainerData getDataAccess() { return dataAccess; }
    public boolean hasSandForRendering() { return isSand(inventory.getStackInSlot(SLOT_SAND)); }
    public ItemStack meshForRendering() { return inventory.getStackInSlot(SLOT_MESH); }
    public float animationProgress(float partialTick) {
        int duration = wetCycle ? WET_PROCESS_TICKS : DRY_PROCESS_TICKS;
        if (level == null) return 0.0F;
        if (cycleStartedAt <= 0L) {
            return (float) Math.max(0.0D, Math.min(1.0D, progress / (double) Math.max(1, duration)));
        }
        double elapsed = level.getGameTime() + partialTick - cycleStartedAt;
        return (float) Math.max(0.0D, Math.min(1.0D, elapsed / Math.max(1, duration)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        CompoundTag water = new CompoundTag();
        waterTank.writeToNBT(water);
        tag.put(NBT_WATER, water);
        tag.putInt(NBT_PROGRESS, progress);
        tag.putBoolean(NBT_WET, wetCycle);
        tag.putLong(NBT_CYCLE_STARTED_AT, cycleStartedAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        waterTank.readFromNBT(tag.getCompound(NBT_WATER));
        if (!waterTank.getFluid().isEmpty()
                && waterTank.getFluid().getFluid().isSame(ModFluids.PURIFIED_WATER.get())) {
            waterTank.setFluid(new FluidStack(Fluids.WATER, waterTank.getFluidAmount()));
        }
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        wetCycle = tag.getBoolean(NBT_WET);
        cycleStartedAt = tag.getLong(NBT_CYCLE_STARTED_AT);
        status = calculateStatus();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                      @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return side == Direction.DOWN ? outputCapability.cast() : inputCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCapability.invalidate();
        outputCapability.invalidate();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_SAND, SLOT_MESH + 1));
        outputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_OUTPUT_FIRST, SLOT_OUTPUT_LAST + 1));
        fluidCapability = LazyOptional.of(() -> waterTank);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.sand_sieve"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SandSieveMenu(containerId, inventory, this);
    }

    private static final class ListBuilder {
        private final java.util.ArrayList<ItemStack> values = new java.util.ArrayList<>(3);
        private void add(ItemStack stack) { values.add(stack); }
    }
}
