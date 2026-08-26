package com.wasted.domesurvival.forge.machine.shaft;

import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import com.wasted.domesurvival.forge.item.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShaftFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_IRON = 0;
    public static final int SLOT_COKE = 1;
    public static final int SLOT_STEEL = 2;
    public static final int SLOT_SLAG = 3;
    public static final int PROCESS_TIME = 1_600;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROGRESS_MAX = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_BURN_TIME_MAX = 3;
    public static final int DATA_COUNT = 4;

    private static final TagKey<Item> IRON_INGOTS = ItemTags.create(new ResourceLocation("forge", "ingots/iron"));
    private static final TagKey<Item> COAL_COKE = ItemTags.create(new ResourceLocation("forge", "coal_coke"));
    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_BURN_TIME = "BurnTime";
    private static final String NBT_BURN_TIME_MAX = "BurnTimeMax";

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_IRON -> isValidIron(stack);
                case SLOT_COKE -> isValidCoke(stack);
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> fullCapability = LazyOptional.of(() -> inventory);
    private LazyOptional<IItemHandler> ironCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_IRON, SLOT_IRON + 1));
    private LazyOptional<IItemHandler> cokeCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_COKE, SLOT_COKE + 1));
    private LazyOptional<IItemHandler> outputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_STEEL, SLOT_SLAG + 1));

    private int progress;
    private int burnTime;
    private int burnTimeMax;

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

    public ShaftFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT_FURNACE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShaftFurnaceBlockEntity furnace) {
        boolean changed = false;

        if (furnace.burnTime > 0) {
            furnace.burnTime--;
            changed = true;
        }

        if (furnace.canProcess()) {
            if (furnace.burnTime <= 0) {
                int duration = getCokeBurnTime(furnace.inventory.getStackInSlot(SLOT_COKE));
                if (duration > 0) {
                    furnace.consumeOneCoke();
                    furnace.burnTime = duration;
                    furnace.burnTimeMax = duration;
                    changed = true;
                }
            }

            if (furnace.burnTime > 0) {
                furnace.progress++;
                changed = true;
                if (furnace.progress >= PROCESS_TIME) {
                    furnace.finishProcess();
                    furnace.progress = 0;
                }
            } else if (furnace.progress != 0) {
                furnace.progress = 0;
                changed = true;
            }
        } else if (furnace.progress != 0) {
            furnace.progress = 0;
            changed = true;
        }

        boolean lit = furnace.burnTime > 0;
        if (state.getValue(ShaftFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(ShaftFurnaceBlock.LIT, lit), 3);
            changed = true;
        }
        BlockState upperState = level.getBlockState(pos.above());
        if (upperState.is(state.getBlock())
                && upperState.getValue(ShaftFurnaceBlock.HALF) == DoubleBlockHalf.UPPER
                && upperState.getValue(ShaftFurnaceBlock.LIT) != lit) {
            level.setBlock(pos.above(), upperState.setValue(ShaftFurnaceBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) furnace.setChanged();
    }

    public static boolean isValidIron(ItemStack stack) {
        return !stack.isEmpty() && stack.is(IRON_INGOTS);
    }

    public static boolean isValidCoke(ItemStack stack) {
        return !stack.isEmpty() && stack.is(COAL_COKE);
    }

    private static int getCokeBurnTime(ItemStack stack) {
        if (!isValidCoke(stack)) return 0;
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    private boolean canProcess() {
        if (!isValidIron(inventory.getStackInSlot(SLOT_IRON))) return false;
        ItemStack steel = getSteelResult();
        ItemStack slag = getSlagResult();
        return !steel.isEmpty() && !slag.isEmpty()
                && canAccept(SLOT_STEEL, steel)
                && canAccept(SLOT_SLAG, slag);
    }

    private boolean canAccept(int slot, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(current, result)
                && current.getCount() + result.getCount() <= Math.min(current.getMaxStackSize(), inventory.getSlotLimit(slot));
    }

    private void finishProcess() {
        if (!canProcess()) return;
        inventory.extractItem(SLOT_IRON, 1, false);
        addResult(SLOT_STEEL, getSteelResult());
        addResult(SLOT_SLAG, getSlagResult());
    }

    private void addResult(int slot, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) inventory.setStackInSlot(slot, result.copy());
        else current.grow(result.getCount());
    }

    private static ItemStack getSteelResult() {
        return new ItemStack(ModItems.STEEL_INGOT.get());
    }

    private static ItemStack getSlagResult() {
        return new ItemStack(ModItems.SLAG.get());
    }

    private void consumeOneCoke() {
        ItemStack fuel = inventory.getStackInSlot(SLOT_COKE);
        if (!fuel.isEmpty()) fuel.shrink(1);
    }

    public ItemStackHandler getInventory() { return inventory; }
    public ContainerData getDataAccess() { return dataAccess; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_PROGRESS, progress);
        tag.putInt(NBT_BURN_TIME, burnTime);
        tag.putInt(NBT_BURN_TIME_MAX, burnTimeMax);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        burnTime = Math.max(0, tag.getInt(NBT_BURN_TIME));
        burnTimeMax = Math.max(0, tag.getInt(NBT_BURN_TIME_MAX));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) return fullCapability.cast();
            if (side == Direction.UP) return ironCapability.cast();
            if (side == Direction.DOWN) return outputCapability.cast();
            return cokeCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fullCapability.invalidate();
        ironCapability.invalidate();
        cokeCapability.invalidate();
        outputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fullCapability = LazyOptional.of(() -> inventory);
        ironCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_IRON, SLOT_IRON + 1));
        cokeCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_COKE, SLOT_COKE + 1));
        outputCapability = LazyOptional.of(() -> new RangedWrapper(inventory, SLOT_STEEL, SLOT_SLAG + 1));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.shaft_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ShaftFurnaceMenu(containerId, playerInventory, this);
    }
}
