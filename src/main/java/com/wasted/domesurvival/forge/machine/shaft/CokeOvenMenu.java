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

public final class CokeOvenMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 3;
    private static final int PLAYER_START = 3;
    private static final int PLAYER_END = 30;
    private static final int HOTBAR_START = 30;
    private static final int HOTBAR_END = 39;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public CokeOvenMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, new ItemStackHandler(MACHINE_SLOTS),
                new SimpleContainerData(CokeOvenBlockEntity.DATA_COUNT),
                ContainerLevelAccess.create(playerInventory.player.level(), extraData.readBlockPos()));
    }

    public CokeOvenMenu(int id, Inventory playerInventory, CokeOvenBlockEntity oven) {
        this(id, playerInventory, oven.getInventory(), oven.getDataAccess(),
                ContainerLevelAccess.create(playerInventory.player.level(), oven.getBlockPos()));
    }

    private CokeOvenMenu(int id, Inventory playerInventory, IItemHandler inventory,
                         ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.COKE_OVEN.get(), id);
        this.access = access;
        this.data = data;
        checkContainerDataCount(data, CokeOvenBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(inventory, CokeOvenBlockEntity.SLOT_COAL, 56, 17) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return CokeOvenBlockEntity.isValidCoal(stack); }
        });
        addSlot(new SlotItemHandler(inventory, CokeOvenBlockEntity.SLOT_FUEL, 56, 53) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return CokeOvenBlockEntity.isValidFuel(stack); }
        });
        addSlot(new SlotItemHandler(inventory, CokeOvenBlockEntity.SLOT_COKE, 116, 35) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, column + row * 9 + 9,
                    8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, column, 8 + column * 18, 142));
    }

    @Override public boolean stillValid(Player player) { return stillValid(access, player, ModBlocks.COKE_OVEN.get()); }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (CokeOvenBlockEntity.isValidCoal(stack)
                && moveItemStackTo(stack, CokeOvenBlockEntity.SLOT_COAL, CokeOvenBlockEntity.SLOT_COAL + 1, false)) {
            // coal input
        } else if (CokeOvenBlockEntity.isValidFuel(stack)
                && moveItemStackTo(stack, CokeOvenBlockEntity.SLOT_FUEL, CokeOvenBlockEntity.SLOT_FUEL + 1, false)) {
            // heat fuel
        } else if (index >= PLAYER_START && index < PLAYER_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    public int getProgressPixels() {
        int max = data.get(CokeOvenBlockEntity.DATA_PROGRESS_MAX);
        return max <= 0 ? 0 : data.get(CokeOvenBlockEntity.DATA_PROGRESS) * 24 / max;
    }

    public int getBurnPixels() {
        int max = data.get(CokeOvenBlockEntity.DATA_BURN_TIME_MAX);
        return max <= 0 ? 0 : data.get(CokeOvenBlockEntity.DATA_BURN_TIME) * 13 / max;
    }
}
