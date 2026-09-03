package com.wasted.domesurvival.forge.machine.shaft;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
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

public final class ShaftFurnaceMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = 4;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ShaftFurnaceMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(MACHINE_SLOT_COUNT),
                new SimpleContainerData(ShaftFurnaceBlockEntity.DATA_COUNT),
                ContainerLevelAccess.create(playerInventory.player.level(), extraData.readBlockPos()));
    }

    public ShaftFurnaceMenu(int containerId, Inventory playerInventory, ShaftFurnaceBlockEntity furnace) {
        this(containerId, playerInventory, furnace.getInventory(), furnace.getDataAccess(),
                ContainerLevelAccess.create(playerInventory.player.level(), furnace.getBlockPos()));
    }

    private ShaftFurnaceMenu(int containerId, Inventory playerInventory, IItemHandler inventory,
                             ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.SHAFT_FURNACE.get(), containerId);
        this.access = access;
        this.data = data;
        checkContainerDataCount(data, ShaftFurnaceBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(inventory, ShaftFurnaceBlockEntity.SLOT_IRON, 26, 54) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return ShaftFurnaceBlockEntity.isValidIron(stack); }
        });
        addSlot(new SlotItemHandler(inventory, ShaftFurnaceBlockEntity.SLOT_COKE, 62, 54) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return ShaftFurnaceBlockEntity.isValidCoke(stack); }
        });
        addSlot(new OutputSlot(inventory, ShaftFurnaceBlockEntity.SLOT_STEEL, 178, 42));
        addSlot(new OutputSlot(inventory, ShaftFurnaceBlockEntity.SLOT_SLAG, 178, 72));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory, column + row * 9 + 9, 14 + column * 22, 161 + row * 22));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, column, 14 + column * 22, 229));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SHAFT_FURNACE.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (ShaftFurnaceBlockEntity.isValidIron(stack)
                && moveItemStackTo(stack, ShaftFurnaceBlockEntity.SLOT_IRON, ShaftFurnaceBlockEntity.SLOT_IRON + 1, false)) {
            // moved to iron input
        } else if (ShaftFurnaceBlockEntity.isValidCoke(stack)
                && moveItemStackTo(stack, ShaftFurnaceBlockEntity.SLOT_COKE, ShaftFurnaceBlockEntity.SLOT_COKE + 1, false)) {
            // moved to coke input
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

    public int getProgressPixels() {
        int max = data.get(ShaftFurnaceBlockEntity.DATA_PROGRESS_MAX);
        return max <= 0 ? 0 : data.get(ShaftFurnaceBlockEntity.DATA_PROGRESS) * 24 / max;
    }

    public int getBurnPixels() {
        int max = data.get(ShaftFurnaceBlockEntity.DATA_BURN_TIME_MAX);
        return max <= 0 ? 0 : data.get(ShaftFurnaceBlockEntity.DATA_BURN_TIME) * 13 / max;
    }

    public int getProgress() { return data.get(ShaftFurnaceBlockEntity.DATA_PROGRESS); }
    public int getProgressMax() { return data.get(ShaftFurnaceBlockEntity.DATA_PROGRESS_MAX); }
    public int getBurnTime() { return data.get(ShaftFurnaceBlockEntity.DATA_BURN_TIME); }
    public int getBurnTimeMax() { return data.get(ShaftFurnaceBlockEntity.DATA_BURN_TIME_MAX); }
    public boolean isWorking() { return getBurnTime() > 0 && getProgress() > 0; }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(IItemHandler inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
    }
}
