package com.wasted.domesurvival.forge.machine.water;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.WaterFilterItem;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WaterPurifierMenu extends AbstractContainerMenu {
    private static final int WATER_SLOT_INDEX = 0;
    private static final int FILTER_SLOT_INDEX = 1;
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_INVENTORY_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;
    private static final int SIDE_BUTTON_BASE = 100;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final WaterPurifierBlockEntity purifier;

    public WaterPurifierMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, new ItemStackHandler(2),
                new SimpleContainerData(WaterPurifierBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public WaterPurifierMenu(int containerId, Inventory playerInventory, WaterPurifierBlockEntity purifier) {
        this(containerId, playerInventory, purifier, purifier.getInventory(), purifier.getDataAccess(), purifier.getBlockPos());
    }

    private WaterPurifierMenu(int containerId, Inventory playerInventory,
                              @Nullable WaterPurifierBlockEntity purifier,
                              IItemHandler machineInventory, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.WATER_PURIFIER.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.purifier = purifier;
        checkContainerDataCount(data, WaterPurifierBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(machineInventory, WaterPurifierBlockEntity.SLOT_WATER_BUCKET, 150, 140) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.is(Items.WATER_BUCKET); }
        });
        addSlot(new SlotItemHandler(machineInventory, WaterPurifierBlockEntity.SLOT_FILTER, 182, 140) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.getItem() instanceof WaterFilterItem; }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory, column + row * 9 + 9, 14 + column * 22, 182 + row * 22));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, column, 14 + column * 22, 250));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WATER_PURIFIER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == WATER_SLOT_INDEX || index == FILTER_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.WATER_BUCKET)
                && moveItemStackTo(stack, WATER_SLOT_INDEX, WATER_SLOT_INDEX + 1, false)) {
            // moved to water input
        } else if (stack.getItem() instanceof WaterFilterItem
                && moveItemStackTo(stack, FILTER_SLOT_INDEX, FILTER_SLOT_INDEX + 1, false)) {
            // moved to filter input
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) return false;
        RelativeSide side = RelativeSide.values()[sideIndex];
        if (!WaterPurifierBlockEntity.isConfigurableSide(side)) return false;
        if (purifier != null) purifier.cycleSideMode(side);
        return true;
    }

    public static int sideButtonId(RelativeSide side) { return SIDE_BUTTON_BASE + side.ordinal(); }

    public int getEnergyStored() { return data.get(WaterPurifierBlockEntity.DATA_ENERGY); }
    public int getEnergyCapacity() { return data.get(WaterPurifierBlockEntity.DATA_CAPACITY); }
    public int getRawWater() { return data.get(WaterPurifierBlockEntity.DATA_RAW_WATER); }
    public int getRawCapacity() { return data.get(WaterPurifierBlockEntity.DATA_RAW_CAPACITY); }
    public int getPurifiedWater() { return data.get(WaterPurifierBlockEntity.DATA_PURIFIED_WATER); }
    public int getPurifiedCapacity() { return data.get(WaterPurifierBlockEntity.DATA_PURIFIED_CAPACITY); }
    public int getProgress() { return data.get(WaterPurifierBlockEntity.DATA_PROGRESS); }
    public int getProgressMax() { return data.get(WaterPurifierBlockEntity.DATA_PROGRESS_MAX); }
    public int getStatus() { return data.get(WaterPurifierBlockEntity.DATA_STATUS); }

    public SideMode getSideMode(RelativeSide side) {
        if (!WaterPurifierBlockEntity.isConfigurableSide(side)) return SideMode.DISABLED;
        Direction worldDirection = side.resolve(getFacing());
        int ordinal = data.get(WaterPurifierBlockEntity.DATA_SIDES_START + worldDirection.ordinal());
        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(WaterPurifierBlock.FACING) ? state.getValue(WaterPurifierBlock.FACING) : Direction.NORTH;
    }
}
