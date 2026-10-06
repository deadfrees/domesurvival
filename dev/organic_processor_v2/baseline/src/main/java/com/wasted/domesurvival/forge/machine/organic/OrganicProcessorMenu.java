package com.wasted.domesurvival.forge.machine.organic;

import com.wasted.domesurvival.forge.machine.module.MachineModuleItem;
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

public final class OrganicProcessorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private static final int SIDE_BUTTON_BASE = 100;
    public static final int MODULE_PANEL_OPEN_BUTTON_ID = 200;
    public static final int MODULE_PANEL_CLOSE_BUTTON_ID = 201;
    public static final int MODULE_SLOT_X = 288;
    public static final int MODULE_SLOT_0_Y = 50;
    public static final int MODULE_SLOT_1_Y = 80;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final OrganicProcessorBlockEntity processor;
    private boolean modulePanelOpen;

    public OrganicProcessorMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, null, new ItemStackHandler(3), new ItemStackHandler(2),
                new SimpleContainerData(OrganicProcessorBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public OrganicProcessorMenu(int id, Inventory playerInventory, OrganicProcessorBlockEntity processor) {
        this(id, playerInventory, processor, processor.getInventory(), processor.getModules(),
                processor.getDataAccess(), processor.getBlockPos());
    }

    private OrganicProcessorMenu(int id, Inventory playerInventory,
                                 @Nullable OrganicProcessorBlockEntity processor,
                                 IItemHandler machine, IItemHandler modules,
                                 ContainerData data, BlockPos pos) {
        super(OrganicProcessorRegistry.ORGANIC_PROCESSOR_MENU.get(), id);
        this.level = playerInventory.player.level();
        this.blockPos = pos;
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = data;
        this.processor = processor;

        checkContainerDataCount(data, OrganicProcessorBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(machine, OrganicProcessorBlockEntity.SLOT_PRIMARY, 55, 58));
        addSlot(new SlotItemHandler(machine, OrganicProcessorBlockEntity.SLOT_ADDITIVE, 55, 88));
        addSlot(outputSlot(machine, OrganicProcessorBlockEntity.SLOT_OUTPUT, 166, 73));

        // Upgrade slots are only active while their dedicated side drawer is open.
        addSlot(moduleSlot(modules, 0, MODULE_SLOT_X, MODULE_SLOT_0_Y));
        addSlot(moduleSlot(modules, 1, MODULE_SLOT_X, MODULE_SLOT_1_Y));

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
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        };
    }

    private SlotItemHandler moduleSlot(IItemHandler handler, int slot, int x, int y) {
        return new SlotItemHandler(handler, slot, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof MachineModuleItem && super.mayPlace(stack);
            }

            @Override
            public boolean isActive() {
                return modulePanelOpen;
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, OrganicProcessorRegistry.ORGANIC_PROCESSOR.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == MODULE_PANEL_OPEN_BUTTON_ID) {
            modulePanelOpen = true;
            return true;
        }
        if (id == MODULE_PANEL_CLOSE_BUTTON_ID) {
            modulePanelOpen = false;
            return true;
        }

        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) return false;

        RelativeSide side = RelativeSide.values()[sideIndex];
        if (!OrganicProcessorBlockEntity.isConfigurableSide(side)) return false;

        if (processor != null) processor.cycleSideMode(side);
        return true;
    }

    public static int sideButtonId(RelativeSide side) {
        return SIDE_BUTTON_BASE + side.ordinal();
    }

    public void setModulePanelOpen(boolean open) {
        modulePanelOpen = open;
    }

    public boolean isModulePanelOpen() {
        return modulePanelOpen;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof MachineModuleItem) {
            if (!modulePanelOpen || !moveItemStackTo(stack, 3, 5, false)) return ItemStack.EMPTY;
        } else if (moveItemStackTo(stack, 0, 2, false)) {
            // Inserted into one of the two recipe inputs.
        } else if (index >= PLAYER_START && index < PLAYER_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    public int energyStored() { return data.get(OrganicProcessorBlockEntity.DATA_ENERGY); }
    public int energyCapacity() { return data.get(OrganicProcessorBlockEntity.DATA_ENERGY_CAPACITY); }
    public int waterStored() { return data.get(OrganicProcessorBlockEntity.DATA_WATER); }
    public int waterCapacity() { return data.get(OrganicProcessorBlockEntity.DATA_WATER_CAPACITY); }
    public int progress() { return data.get(OrganicProcessorBlockEntity.DATA_PROGRESS); }
    public int progressMax() { return data.get(OrganicProcessorBlockEntity.DATA_PROGRESS_MAX); }
    public int status() { return data.get(OrganicProcessorBlockEntity.DATA_STATUS); }
    public int recipeEnergy() { return data.get(OrganicProcessorBlockEntity.DATA_RECIPE_ENERGY); }
    public int waterRequired() { return data.get(OrganicProcessorBlockEntity.DATA_WATER_REQUIRED); }

    public SideMode getSideMode(RelativeSide side) {
        if (!OrganicProcessorBlockEntity.isConfigurableSide(side)) return SideMode.DISABLED;
        Direction worldDirection = side.resolve(getFacing());
        int ordinal = data.get(OrganicProcessorBlockEntity.DATA_SIDES_START + worldDirection.ordinal());
        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(OrganicProcessorBlock.FACING)
                ? state.getValue(OrganicProcessorBlock.FACING)
                : Direction.NORTH;
    }
}
