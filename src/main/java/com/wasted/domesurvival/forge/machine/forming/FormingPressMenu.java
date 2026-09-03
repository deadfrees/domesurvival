package com.wasted.domesurvival.forge.machine.forming;

import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FormingPressMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT_INDEX = 0;
    private static final int OUTPUT_SLOT_INDEX = 1;
    private static final int PLAYER_INVENTORY_START = 2;
    private static final int PLAYER_INVENTORY_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;
    private static final int SIDE_BUTTON_BASE = 100;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final FormingPressBlockEntity press;

    public FormingPressMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, new ItemStackHandler(2),
                new SimpleContainerData(FormingPressBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public FormingPressMenu(int containerId, Inventory playerInventory, FormingPressBlockEntity press) {
        this(containerId, playerInventory, press, press.getInventory(), press.getDataAccess(), press.getBlockPos());
    }

    private FormingPressMenu(int containerId, Inventory playerInventory,
                             @Nullable FormingPressBlockEntity press,
                             IItemHandler machineInventory, ContainerData data, BlockPos blockPos) {
        super(FormingPressRegistry.FORMING_PRESS_MENU.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.press = press;
        checkContainerDataCount(data, FormingPressBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(machineInventory, 0, 64, 62) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return FormingPressBlockEntity.isValidFormingInput(level, stack);
            }
        });
        addSlot(new SlotItemHandler(machineInventory, 1, 136, 62) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
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
        return stillValid(access, player, FormingPressRegistry.FORMING_PRESS.get());
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
        ItemStack result = stack.copy();

        if (index == INPUT_SLOT_INDEX || index == OUTPUT_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (FormingPressBlockEntity.isValidFormingInput(level, stack)
                && moveItemStackTo(stack, INPUT_SLOT_INDEX, INPUT_SLOT_INDEX + 1, false)) {
            // Moved into machine input.
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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
        return result;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) {
            return false;
        }

        RelativeSide side = RelativeSide.values()[sideIndex];
        if (!FormingPressBlockEntity.isConfigurableSide(side)) {
            return false;
        }

        if (press != null) {
            press.cycleSideMode(side);
        }
        return true;
    }

    public static int sideButtonId(RelativeSide side) {
        return SIDE_BUTTON_BASE + side.ordinal();
    }

    public int energyStored() { return data.get(FormingPressBlockEntity.DATA_ENERGY); }
    public int energyCapacity() { return data.get(FormingPressBlockEntity.DATA_CAPACITY); }
    public int progress() { return data.get(FormingPressBlockEntity.DATA_PROGRESS); }
    public int progressMax() { return data.get(FormingPressBlockEntity.DATA_MAX_PROGRESS); }
    public int recipeEnergy() { return data.get(FormingPressBlockEntity.DATA_RECIPE_ENERGY); }
    public int status() { return data.get(FormingPressBlockEntity.DATA_STATUS); }

    public SideMode getSideMode(RelativeSide side) {
        if (!FormingPressBlockEntity.isConfigurableSide(side)) {
            return SideMode.DISABLED;
        }
        Direction worldDirection = side.resolve(getFacing());
        int ordinal = data.get(FormingPressBlockEntity.DATA_SIDES_START + worldDirection.ordinal());
        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(FormingPressBlock.FACING)
                ? state.getValue(FormingPressBlock.FACING)
                : Direction.NORTH;
    }
}
