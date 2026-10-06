package com.wasted.domesurvival.forge.machine.shaft;

import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.machine.module.*;
import java.util.*;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CokeOvenBlockEntity extends BlockEntity implements MenuProvider, IModularMachine {
    public static final int SLOT_COAL = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_COKE = 2;
    public static final int PROCESS_TIME = 1_600;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROGRESS_MAX = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_BURN_TIME_MAX = 3;
    public static final int DATA_COUNT = 12;
    private final UnifiedSideConfig sides = new UnifiedSideConfig();
    private final MachineModuleInventory modules = new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::setChanged);
    private final EnumMap<Direction, LazyOptional<IItemHandler>> ports = new EnumMap<>(Direction.class);
    private Direction lastFacing;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_COAL -> isValidCoal(stack);
                case SLOT_FUEL -> isValidFuel(stack);
                default -> false;
            };
        }

        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    private int progress;
    private int burnTime;
    private int burnTimeMax;
    private int portRepairCooldown;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROGRESS_MAX -> PROCESS_TIME;
                case DATA_BURN_TIME -> burnTime;
                case DATA_BURN_TIME_MAX -> burnTimeMax;
                case 10 -> hasEfficiency() ? 1 : 0;
                case 11 -> status();
                default -> index >= 4 && index < 10 ? sideMode(Direction.values()[index - 4]).ordinal() : 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public CokeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKE_OVEN.get(), pos, state);
        defaults();
        lastFacing = facing();
    }

    @Override public int moduleSlotCount() { return 1; }
    @Override public Set<MachineModuleType> allowedModuleTypes() { return Set.of(MachineModuleType.EFFICIENCY); }
    public MachineModuleInventory getModules() { return modules; }
    public boolean hasEfficiency() {
        return modules != null && modules.isConfigurationValid()
                && modules.getStackInSlot(0).getItem() instanceof MachineModuleItem m
                && m.module().type() == MachineModuleType.EFFICIENCY;
    }
    public int fuelDuration(ItemStack stack) {
        return (int)Math.min(Integer.MAX_VALUE, (long)getFuelBurnTime(stack) * (hasEfficiency() ? 115 : 100) / 100);
    }
    public Direction facing() { return getBlockState().getValue(CokeOvenBlock.FACING); }
    public SideMode sideMode(@Nullable Direction side) {
        return side == null || side == facing() ? SideMode.DISABLED : sides.getMode(side);
    }
    private void defaults() {
        sides.reset();
        // Preserve the old routes when opening an existing world.
        sides.setMode(facing().getClockWise(), SideMode.INPUT);
        sides.setMode(facing().getCounterClockWise(), SideMode.OUTPUT);
        sides.setMode(facing().getOpposite(), SideMode.OUTPUT);
        sides.setMode(Direction.DOWN, SideMode.OUTPUT);
    }
    public void cycleSideMode(RelativeSide side) {
        if (side == RelativeSide.FRONT) return;
        sides.cycleMode(side.resolve(facing()));
        routingChanged();
    }
    public void rotateSideConfiguration(Direction previous) {
        EnumMap<RelativeSide, SideMode> old = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : RelativeSide.values()) old.put(side, sides.getMode(side.resolve(previous)));
        for (RelativeSide side : RelativeSide.values()) sides.setMode(side.resolve(facing()), old.get(side));
        sides.setMode(facing(), SideMode.DISABLED); lastFacing = facing(); routingChanged();
    }
    private void refreshPorts() { ports.values().forEach(LazyOptional::invalidate); ports.clear(); }
    private void routingChanged() {
        refreshPorts(); setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }
    private int status() {
        if (!isValidCoal(inventory.getStackInSlot(SLOT_COAL))) return 0;
        if (!canProcess()) return 3;
        return burnTime > 0 ? 1 : 2;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CokeOvenBlockEntity oven) {
        if (oven.lastFacing != oven.facing()) oven.rotateSideConfiguration(oven.lastFacing);
        if (oven.portRepairCooldown-- <= 0) {
            // Old builds occupied the neighbouring pipe cells with invisible
            // proxy blocks. Remove them so automation can touch the controller
            // directly; sided capabilities below remain strictly one-way.
            CokeOvenBlock.clearLegacyParts(level, pos);
            oven.portRepairCooldown = 40;
        }
        boolean changed = false;
        if (oven.burnTime > 0) {
            oven.burnTime--;
            changed = true;
        }

        if (oven.canProcess()) {
            if (oven.burnTime <= 0) {
                ItemStack fuel = oven.inventory.getStackInSlot(SLOT_FUEL);
                int duration = oven.fuelDuration(fuel);
                if (duration > 0) {
                    oven.consumeOneFuel();
                    oven.burnTime = duration;
                    oven.burnTimeMax = duration;
                    changed = true;
                }
            }
            if (oven.burnTime > 0) {
                oven.progress++;
                changed = true;
                if (oven.progress >= PROCESS_TIME) {
                    oven.finishProcess();
                    oven.progress = 0;
                }
            }
        } else if (!isValidCoal(oven.inventory.getStackInSlot(SLOT_COAL)) && oven.progress != 0) {
            oven.progress = 0;
            changed = true;
        }

        boolean lit = oven.burnTime > 0;
        if (state.getValue(CokeOvenBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(CokeOvenBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) oven.setChanged();

        if (level.getGameTime() % 5L == 0L && oven.exportFinishedCoke(level, pos, state)) {
            oven.setChanged();
        }
    }

    private boolean exportFinishedCoke(Level level, BlockPos controller, BlockState state) {
        for (Direction side : Direction.values()) {
            if (sideMode(side) != SideMode.OUTPUT) continue;
            if (FurnaceOutputTransfer.push(level, inventory, SLOT_COKE, controller.relative(side), side.getOpposite())) return true;
            if (!isValidFuel(inventory.getStackInSlot(SLOT_FUEL))
                    && FurnaceOutputTransfer.push(level, inventory, SLOT_FUEL, controller.relative(side), side.getOpposite())) return true;
        }
        return false;
    }

    public static boolean isValidCoal(ItemStack stack) { return stack.is(Items.COAL); }
    public static boolean isValidFuel(ItemStack stack) { return getFuelBurnTime(stack) > 0; }

    private static int getFuelBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    private boolean canProcess() {
        if (!isValidCoal(inventory.getStackInSlot(SLOT_COAL))) return false;
        ItemStack output = inventory.getStackInSlot(SLOT_COKE);
        return output.isEmpty() || (output.is(ModItems.COAL_COKE.get()) && output.getCount() < output.getMaxStackSize());
    }

    private void finishProcess() {
        if (!canProcess()) return;
        inventory.extractItem(SLOT_COAL, 1, false);
        ItemStack output = inventory.getStackInSlot(SLOT_COKE);
        if (output.isEmpty()) inventory.setStackInSlot(SLOT_COKE, new ItemStack(ModItems.COAL_COKE.get()));
        else inventory.setStackInSlot(SLOT_COKE, output.copyWithCount(output.getCount() + 1));
    }

    private void consumeOneFuel() {
        ItemStack fuel = inventory.getStackInSlot(SLOT_FUEL);
        if (fuel.isEmpty()) return;
        ItemStack remainder = ForgeHooks.getCraftingRemainingItem(fuel.copyWithCount(1));
        fuel.shrink(1);
        if (fuel.isEmpty() && !remainder.isEmpty()) inventory.setStackInSlot(SLOT_FUEL, remainder);
    }

    public ItemStackHandler getInventory() { return inventory; }
    public ContainerData getDataAccess() { return dataAccess; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeMax", burnTimeMax);
        tag.put("Modules", modules.serializeNBT()); sides.save(tag);
        tag.putString("PortFacing", facing().getName());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        progress = Math.max(0, Math.min(PROCESS_TIME - 1, tag.getInt("Progress")));
        burnTime = Math.max(0, tag.getInt("BurnTime"));
        burnTimeMax = Math.max(0, tag.getInt("BurnTimeMax"));
        if (tag.contains("Modules")) modules.deserializeNBT(tag.getCompound("Modules"));
        else modules.setStackInSlot(0, ItemStack.EMPTY);
        if (!sides.load(tag)) defaults();
        Direction savedFacing = Direction.byName(tag.getString("PortFacing"));
        if (savedFacing != null && savedFacing.getAxis().isHorizontal() && savedFacing != facing()) {
            EnumMap<RelativeSide, SideMode> old = new EnumMap<>(RelativeSide.class);
            for (RelativeSide side : RelativeSide.values()) old.put(side, sides.getMode(side.resolve(savedFacing)));
            for (RelativeSide side : RelativeSide.values()) sides.setMode(side.resolve(facing()), old.get(side));
        }
        sides.setMode(facing(), SideMode.DISABLED); lastFacing = facing(); refreshPorts();
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (isRemoved() || sideMode(side) == SideMode.DISABLED) return LazyOptional.empty();
            return ports.computeIfAbsent(side, d -> LazyOptional.of(() -> new Port(d))).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        refreshPorts();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        refreshPorts();
    }

    /** Recheck mode on every operation, including previously cached handlers. */
    private final class Port implements IItemHandler {
        private final Direction side;
        Port(Direction side) { this.side = side; }
        public int getSlots() { return 3; }
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot).copy(); }
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isRemoved() && sideMode(side) == SideMode.INPUT && inventory.isItemValid(slot, stack);
        }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            boolean allowed = slot == SLOT_COKE || slot == SLOT_FUEL && !isValidFuel(inventory.getStackInSlot(slot));
            return !isRemoved() && sideMode(side) == SideMode.OUTPUT && allowed
                    ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.coke_oven"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CokeOvenMenu(containerId, playerInventory, this);
    }
}
