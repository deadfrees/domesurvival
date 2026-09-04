package com.wasted.domesurvival.forge.machine.filter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FilterRegenerationMenu extends AbstractContainerMenu {
    private static final int FILTER_SLOT_INDEX = 0;
    private static final int MEDIA_SLOT_INDEX = 1;
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_INVENTORY_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public FilterRegenerationMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, new ItemStackHandler(2),
                new SimpleContainerData(FilterRegenerationBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public FilterRegenerationMenu(int containerId, Inventory playerInventory,
                                  FilterRegenerationBlockEntity station) {
        this(containerId, playerInventory, station, station.getInventory(), station.getDataAccess(), station.getBlockPos());
    }

    private FilterRegenerationMenu(int containerId, Inventory playerInventory,
                                   @Nullable FilterRegenerationBlockEntity station,
                                   IItemHandler machineInventory, ContainerData data, BlockPos blockPos) {
        super(FilterRegenerationRegistry.FILTER_REGENERATION_MENU.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), blockPos);
        this.data = data;
        checkContainerDataCount(data, FilterRegenerationBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(machineInventory, 0, 64, 62) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return FilterRegenerationBlockEntity.isEligibleFilter(stack);
            }
        });
        addSlot(new SlotItemHandler(machineInventory, 1, 100, 62) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return FilterRegenerationBlockEntity.isRegenerationMedia(stack);
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        12 + column * 22,
                        132 + row * 22
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(
                    playerInventory,
                    column,
                    12 + column * 22,
                    198
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index == FILTER_SLOT_INDEX || index == MEDIA_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (FilterRegenerationBlockEntity.isEligibleFilter(stack)
                && moveItemStackTo(stack, FILTER_SLOT_INDEX, FILTER_SLOT_INDEX + 1, false)) {
            // Moved into filter slot.
        } else if (FilterRegenerationBlockEntity.isRegenerationMedia(stack)
                && moveItemStackTo(stack, MEDIA_SLOT_INDEX, MEDIA_SLOT_INDEX + 1, false)) {
            // Moved into media slot.
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    public int energyStored() { return data.get(FilterRegenerationBlockEntity.DATA_ENERGY); }
    public int energyCapacity() { return data.get(FilterRegenerationBlockEntity.DATA_CAPACITY); }
    public int progress() { return data.get(FilterRegenerationBlockEntity.DATA_PROGRESS); }
    public int progressMax() { return data.get(FilterRegenerationBlockEntity.DATA_MAX_PROGRESS); }
    public int status() { return data.get(FilterRegenerationBlockEntity.DATA_STATUS); }
    public int regenerationCycles() { return data.get(FilterRegenerationBlockEntity.DATA_REGEN_CYCLES); }
    public int maxRegenerationCycles() { return data.get(FilterRegenerationBlockEntity.DATA_MAX_REGEN_CYCLES); }
}
