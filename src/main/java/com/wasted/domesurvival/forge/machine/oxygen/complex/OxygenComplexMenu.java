package com.wasted.domesurvival.forge.machine.oxygen.complex;

import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
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

public final class OxygenComplexMenu extends AbstractContainerMenu {
    public static final int FILTER_SLOT_INDEX = 0;
    public static final int FILTER_SLOT_X = 127;
    public static final int FILTER_SLOT_Y = 100;

    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    private static final int PLAYER_X = 20;
    private static final int PLAYER_Y = 228;
    private static final int HOTBAR_Y = 286;

    private static final int SIDE_BUTTON_BASE = 100;

    private final ContainerData data;
    private final BlockPos controllerPos;
    private final ContainerLevelAccess access;
    @Nullable private final OxygenComplexBlockEntity controller;

    public OxygenComplexMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                null,
                new ItemStackHandler(1),
                new SimpleContainerData(OxygenComplexBlockEntity.DATA_COUNT),
                extraData.readBlockPos()
        );
    }

    public OxygenComplexMenu(int containerId, Inventory playerInventory, OxygenComplexBlockEntity controller) {
        this(
                containerId,
                playerInventory,
                controller,
                controller.getFilterInventory(),
                controller.getDataAccess(),
                controller.getBlockPos()
        );
    }

    private OxygenComplexMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable OxygenComplexBlockEntity controller,
            IItemHandler filterInventory,
            ContainerData data,
            BlockPos controllerPos
    ) {
        super(OxygenComplexRegistry.MENU.get(), containerId);
        this.data = data;
        this.controllerPos = controllerPos;
        this.controller = controller;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), controllerPos);

        checkContainerDataCount(data, OxygenComplexBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(filterInventory, 0, FILTER_SLOT_X, FILTER_SLOT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return OxygenComplexFilters.isAirFilter(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        inventory,
                        column + row * 9 + 9,
                        PLAYER_X + column * 18,
                        PLAYER_Y + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(
                    inventory,
                    column,
                    PLAYER_X + column * 18,
                    HOTBAR_Y
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, OxygenComplexRegistry.OUTPUT.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        RelativeSide side = sideFromButtonId(buttonId);
        if (side == null
                || !OxygenComplexPortLayout.isPhysicalPort(side)
                || controller == null
                || !controller.isController()) {
            return false;
        }

        controller.cycleSideMode(side);
        broadcastChanges();
        return true;
    }

    public static int sideButtonId(RelativeSide side) {
        return SIDE_BUTTON_BASE + side.ordinal();
    }

    @Nullable
    private static RelativeSide sideFromButtonId(int buttonId) {
        int ordinal = buttonId - SIDE_BUTTON_BASE;
        RelativeSide[] values = RelativeSide.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index == FILTER_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START && index < HOTBAR_END) {
            if (OxygenComplexFilters.isAirFilter(stack)) {
                if (!moveItemStackTo(stack, FILTER_SLOT_INDEX, FILTER_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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

        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return result;
    }

    public BlockPos getControllerPos() { return controllerPos; }
    @Nullable public OxygenComplexBlockEntity getController() { return controller; }

    public int getEnergy() { return data.get(OxygenComplexBlockEntity.DATA_ENERGY); }
    public int getEnergyCapacity() { return data.get(OxygenComplexBlockEntity.DATA_ENERGY_CAPACITY); }
    public int getCollectedAir() { return data.get(OxygenComplexBlockEntity.DATA_COLLECTED_AIR); }
    public int getCollectedAirCapacity() { return data.get(OxygenComplexBlockEntity.DATA_COLLECTED_AIR_CAPACITY); }
    public int getFilteredAir() { return data.get(OxygenComplexBlockEntity.DATA_FILTERED_AIR); }
    public int getFilteredAirCapacity() { return data.get(OxygenComplexBlockEntity.DATA_FILTERED_AIR_CAPACITY); }
    public int getCompressedFeed() { return data.get(OxygenComplexBlockEntity.DATA_COMPRESSED); }
    public int getCompressedFeedCapacity() { return data.get(OxygenComplexBlockEntity.DATA_COMPRESSED_CAPACITY); }
    public int getOxygen() { return data.get(OxygenComplexBlockEntity.DATA_OXYGEN); }
    public int getOxygenCapacity() { return data.get(OxygenComplexBlockEntity.DATA_OXYGEN_CAPACITY); }
    public OxygenComplexStatus getStatus() { return OxygenComplexStatus.byOrdinal(data.get(OxygenComplexBlockEntity.DATA_STATUS)); }
    public int getCurrentEnergyUse() { return data.get(OxygenComplexBlockEntity.DATA_CURRENT_FE_T); }
    public boolean isFormed() { return data.get(OxygenComplexBlockEntity.DATA_FORMED) != 0; }
    public boolean isIntakeActive() { return data.get(OxygenComplexBlockEntity.DATA_INTAKE_ACTIVE) != 0; }
    public boolean isFilterActive() { return data.get(OxygenComplexBlockEntity.DATA_FILTER_ACTIVE) != 0; }
    public boolean isCompressionActive() { return data.get(OxygenComplexBlockEntity.DATA_COMPRESS_ACTIVE) != 0; }
    public boolean isOutputActive() { return data.get(OxygenComplexBlockEntity.DATA_OUTPUT_ACTIVE) != 0; }
    public boolean hasAtmosphere() { return data.get(OxygenComplexBlockEntity.DATA_ATMOSPHERE) != 0; }
    public boolean hasAirFilter() { return data.get(OxygenComplexBlockEntity.DATA_FILTER_PRESENT) != 0; }

    public int getAirFilterDamage() { return data.get(OxygenComplexBlockEntity.DATA_FILTER_DAMAGE); }
    public int getAirFilterMaxDamage() { return data.get(OxygenComplexBlockEntity.DATA_FILTER_MAX_DAMAGE); }
    public int getAirFilterRemaining() {
        return Math.max(0, getAirFilterMaxDamage() - getAirFilterDamage());
    }

    public SideMode getSideMode(RelativeSide side) {
        if (!OxygenComplexPortLayout.isPhysicalPort(side)) {
            return SideMode.DISABLED;
        }
        int index = switch (side) {
            case TOP -> OxygenComplexBlockEntity.DATA_SIDE_TOP;
            case BOTTOM -> OxygenComplexBlockEntity.DATA_SIDE_BOTTOM;
            case FRONT -> OxygenComplexBlockEntity.DATA_SIDE_FRONT;
            case BACK -> OxygenComplexBlockEntity.DATA_SIDE_BACK;
            case LEFT -> OxygenComplexBlockEntity.DATA_SIDE_LEFT;
            case RIGHT -> OxygenComplexBlockEntity.DATA_SIDE_RIGHT;
        };

        int ordinal = data.get(index);
        SideMode[] values = SideMode.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SideMode.DISABLED;
        }

        SideMode mode = values[ordinal];
        return mode == SideMode.BOTH ? SideMode.OUTPUT : mode;
    }
}
