package com.wasted.domesurvival.forge.machine.crusher;

import com.wasted.domesurvival.forge.machine.module.MachineModuleItem;
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

public final class IndustrialCrusherMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT = 0;
    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public IndustrialCrusherMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, new ItemStackHandler(3), new ItemStackHandler(2),
                new SimpleContainerData(IndustrialCrusherBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public IndustrialCrusherMenu(int id, Inventory playerInventory, IndustrialCrusherBlockEntity crusher) {
        this(id, playerInventory, crusher.getInventory(), crusher.getModules(), crusher.getDataAccess(), crusher.getBlockPos());
    }

    private IndustrialCrusherMenu(int id, Inventory playerInventory, IItemHandler machine, IItemHandler modules,
                                  ContainerData data, BlockPos pos) {
        super(IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER_MENU.get(), id);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        checkContainerDataCount(data, IndustrialCrusherBlockEntity.DATA_COUNT);
        addDataSlots(data);

        // 220x266 industrial layout; player grid deliberately mirrors CoalGeneratorMenu.
        addSlot(new SlotItemHandler(machine, 0, 55, 63));
        addSlot(outputSlot(machine, 1, 166, 53));
        addSlot(outputSlot(machine, 2, 166, 83));
        addSlot(moduleSlot(modules, 0, 86, 118));
        addSlot(moduleSlot(modules, 1, 116, 118));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        14 + col * 22,
                        161 + row * 22
                ));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 14 + col * 22, 229));
        }
    }

    private static SlotItemHandler outputSlot(IItemHandler handler, int slot, int x, int y) {
        return new SlotItemHandler(handler, slot, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        };
    }

    private static SlotItemHandler moduleSlot(IItemHandler handler, int slot, int x, int y) {
        return new SlotItemHandler(handler, slot, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof MachineModuleItem && super.mayPlace(stack);
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof MachineModuleItem) {
            if (!moveItemStackTo(stack, 3, 5, false)) {
                return ItemStack.EMPTY;
            }
        } else if (moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
            // Valid crusher input; the ItemStackHandler performs recipe validation.
        } else if (index >= PLAYER_START && index < PLAYER_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    public int energyStored() { return data.get(IndustrialCrusherBlockEntity.DATA_ENERGY); }
    public int energyCapacity() { return data.get(IndustrialCrusherBlockEntity.DATA_CAPACITY); }
    public int progress() { return data.get(IndustrialCrusherBlockEntity.DATA_PROGRESS); }
    public int progressMax() { return data.get(IndustrialCrusherBlockEntity.DATA_MAX_PROGRESS); }
    public int recipeEnergy() { return data.get(IndustrialCrusherBlockEntity.DATA_RECIPE_ENERGY); }
    public int status() { return data.get(IndustrialCrusherBlockEntity.DATA_STATUS); }
}
