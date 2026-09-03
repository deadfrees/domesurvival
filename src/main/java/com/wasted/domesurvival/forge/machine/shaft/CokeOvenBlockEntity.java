package com.wasted.domesurvival.forge.machine.shaft;

import com.wasted.domesurvival.forge.item.ModItems;
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
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CokeOvenBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COAL = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_COKE = 2;
    public static final int PROCESS_TIME = 2_250;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROGRESS_MAX = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_BURN_TIME_MAX = 3;
    public static final int DATA_COUNT = 4;

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

    private LazyOptional<IItemHandler> inputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_COAL, SLOT_FUEL + 1));
    private LazyOptional<IItemHandler> outputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_COKE, SLOT_COKE + 1));

    private int progress;
    private int burnTime;
    private int burnTimeMax;
    private boolean legacyPartsChecked;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROGRESS_MAX -> PROCESS_TIME;
                case DATA_BURN_TIME -> burnTime;
                case DATA_BURN_TIME_MAX -> burnTimeMax;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public CokeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKE_OVEN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CokeOvenBlockEntity oven) {
        if (!oven.legacyPartsChecked) {
            CokeOvenBlock.clearLegacyParts(level, pos);
            oven.legacyPartsChecked = true;
        }
        boolean changed = false;
        if (oven.burnTime > 0) {
            oven.burnTime--;
            changed = true;
        }

        if (oven.canProcess()) {
            if (oven.burnTime <= 0) {
                ItemStack fuel = oven.inventory.getStackInSlot(SLOT_FUEL);
                int duration = getFuelBurnTime(fuel);
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
            } else if (oven.progress != 0) {
                oven.progress = 0;
                changed = true;
            }
        } else if (oven.progress != 0) {
            oven.progress = 0;
            changed = true;
        }

        boolean lit = oven.burnTime > 0;
        if (state.getValue(CokeOvenBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(CokeOvenBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) oven.setChanged();
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
        else output.grow(1);
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
    LazyOptional<IItemHandler> getInputPortCapability() { return inputCapability; }
    LazyOptional<IItemHandler> getOutputPortCapability() { return outputCapability; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeMax", burnTimeMax);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        progress = Math.max(0, tag.getInt("Progress"));
        burnTime = Math.max(0, tag.getInt("BurnTime"));
        burnTimeMax = Math.max(0, tag.getInt("BurnTimeMax"));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            Direction facing = getBlockState().getValue(CokeOvenBlock.FACING);
            if (side == facing.getCounterClockWise() || side == facing.getOpposite()) {
                return inputCapability.cast();
            }
            if (side == facing.getClockWise() || side == Direction.DOWN) {
                return outputCapability.cast();
            }
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCapability.invalidate();
        outputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_COAL, SLOT_FUEL + 1));
        outputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_COKE, SLOT_COKE + 1));
    }

    @Override public Component getDisplayName() { return Component.translatable("block.domesurvival.coke_oven"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CokeOvenMenu(containerId, playerInventory, this);
    }
}
