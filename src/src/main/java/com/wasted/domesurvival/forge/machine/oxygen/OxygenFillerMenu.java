package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import com.wasted.domesurvival.forge.oxygen.room.SealedRoomManager;
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

public final class OxygenFillerMenu extends AbstractContainerMenu {
    private static final int TANK_SLOT_INDEX = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;
    private static final int MODE_BUTTON_ID = 50;
    private static final int SIDE_BUTTON_BASE = 100;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final OxygenFillerBlockEntity filler;

    public OxygenFillerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, new ItemStackHandler(1),
                new SimpleContainerData(OxygenFillerBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public OxygenFillerMenu(int containerId, Inventory playerInventory, OxygenFillerBlockEntity filler) {
        this(containerId, playerInventory, filler, filler.getInventory(), filler.getDataAccess(), filler.getBlockPos());
    }

    private OxygenFillerMenu(int containerId, Inventory playerInventory,
                             @Nullable OxygenFillerBlockEntity filler,
                             IItemHandler machineInventory, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.OXYGEN_FILLER.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.filler = filler;
        checkContainerDataCount(data, OxygenFillerBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(machineInventory, OxygenFillerBlockEntity.SLOT_TANK, 102, 60) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof OxygenTankItem;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(inv, column + row * 9 + 9,
                        14 + column * 22, 177 + row * 22));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(inv, column,
                    14 + column * 22, 249));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.OXYGEN_FILLER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == TANK_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof OxygenTankItem
                && moveItemStackTo(stack, TANK_SLOT_INDEX, TANK_SLOT_INDEX + 1, false)) {
            // moved to filler slot
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
        if (id == MODE_BUTTON_ID) {
            if (filler != null) filler.cycleOperatingMode();
            return true;
        }

        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) return false;
        RelativeSide side = RelativeSide.values()[sideIndex];
        if (!OxygenFillerBlockEntity.isConfigurableSide(side)) return false;
        if (filler != null) filler.cycleSideMode(side);
        return true;
    }

    public static int modeButtonId() {
        return MODE_BUTTON_ID;
    }

    public static int sideButtonId(RelativeSide side) {
        return SIDE_BUTTON_BASE + side.ordinal();
    }

    public int getEnergyStored() { return data.get(OxygenFillerBlockEntity.DATA_ENERGY); }
    public int getEnergyCapacity() { return data.get(OxygenFillerBlockEntity.DATA_ENERGY_CAPACITY); }
    public int getOxygen() { return data.get(OxygenFillerBlockEntity.DATA_OXYGEN); }
    public int getOxygenCapacity() { return data.get(OxygenFillerBlockEntity.DATA_OXYGEN_CAPACITY); }
    public int getTankOxygen() { return data.get(OxygenFillerBlockEntity.DATA_TANK_OXYGEN); }
    public int getTankCapacity() { return data.get(OxygenFillerBlockEntity.DATA_TANK_CAPACITY); }
    public int getStatus() { return data.get(OxygenFillerBlockEntity.DATA_STATUS); }
    public OxygenFillerMode getOperatingMode() {
        return OxygenFillerMode.byOrdinal(data.get(OxygenFillerBlockEntity.DATA_MODE));
    }

    public SealedRoomManager.RoomState getRoomState() {
        return SealedRoomManager.RoomState.byOrdinal(data.get(OxygenFillerBlockEntity.DATA_ROOM_STATE));
    }

    public int getRoomVolume() {
        return data.get(OxygenFillerBlockEntity.DATA_ROOM_VOLUME);
    }

    public int getRoomOxygen() {
        return data.get(OxygenFillerBlockEntity.DATA_ROOM_OXYGEN);
    }

    public int getRoomOxygenRequired() {
        return data.get(OxygenFillerBlockEntity.DATA_ROOM_OXYGEN_REQUIRED);
    }

    public int getRoomPressurePercent() {
        int required = getRoomOxygenRequired();
        if (required <= 0) return 0;
        return Math.max(0, Math.min(100, (int) (((long) getRoomOxygen() * 100L) / required)));
    }

    public SideMode getSideMode(RelativeSide side) {
        if (!OxygenFillerBlockEntity.isConfigurableSide(side)) return SideMode.DISABLED;
        Direction worldDirection = side.resolve(getFacing());
        int ordinal = data.get(OxygenFillerBlockEntity.DATA_SIDES_START + worldDirection.ordinal());
        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(OxygenFillerBlock.FACING)
                ? state.getValue(OxygenFillerBlock.FACING) : Direction.NORTH;
    }
}
