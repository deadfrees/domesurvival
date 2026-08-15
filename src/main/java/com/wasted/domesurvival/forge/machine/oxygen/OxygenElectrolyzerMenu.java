package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.block.ModBlocks;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OxygenElectrolyzerMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_END = 27;
    private static final int HOTBAR_START = 27;
    private static final int HOTBAR_END = 36;
    private static final int SIDE_BUTTON_BASE = 100;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final OxygenElectrolyzerBlockEntity electrolyzer;

    public OxygenElectrolyzerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null,
                new SimpleContainerData(OxygenElectrolyzerBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public OxygenElectrolyzerMenu(int containerId, Inventory playerInventory, OxygenElectrolyzerBlockEntity electrolyzer) {
        this(containerId, playerInventory, electrolyzer, electrolyzer.getDataAccess(), electrolyzer.getBlockPos());
    }

    private OxygenElectrolyzerMenu(int containerId, Inventory playerInventory,
                                   @Nullable OxygenElectrolyzerBlockEntity electrolyzer,
                                   ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.OXYGEN_ELECTROLYZER.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.electrolyzer = electrolyzer;
        checkContainerDataCount(data, OxygenElectrolyzerBlockEntity.DATA_COUNT);
        addDataSlots(data);
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(inv, column + row * 9 + 9,
                        14 + column * 22, 161 + row * 22));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(inv, column, 14 + column * 22, 229));
        }
    }

    @Override public boolean stillValid(Player player) { return stillValid(access, player, ModBlocks.OXYGEN_ELECTROLYZER.get()); }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) return false;
        RelativeSide side = RelativeSide.values()[sideIndex];
        if (!OxygenElectrolyzerBlockEntity.isConfigurableSide(side)) return false;
        if (electrolyzer != null) electrolyzer.cycleSideMode(side);
        return true;
    }

    public static int sideButtonId(RelativeSide side) { return SIDE_BUTTON_BASE + side.ordinal(); }
    public int getEnergyStored() { return data.get(OxygenElectrolyzerBlockEntity.DATA_ENERGY); }
    public int getEnergyCapacity() { return data.get(OxygenElectrolyzerBlockEntity.DATA_ENERGY_CAPACITY); }
    public int getWater() { return data.get(OxygenElectrolyzerBlockEntity.DATA_WATER); }
    public int getWaterCapacity() { return data.get(OxygenElectrolyzerBlockEntity.DATA_WATER_CAPACITY); }
    public int getOxygen() { return data.get(OxygenElectrolyzerBlockEntity.DATA_OXYGEN); }
    public int getOxygenCapacity() { return data.get(OxygenElectrolyzerBlockEntity.DATA_OXYGEN_CAPACITY); }
    public int getProgress() { return data.get(OxygenElectrolyzerBlockEntity.DATA_PROGRESS); }
    public int getProgressMax() { return data.get(OxygenElectrolyzerBlockEntity.DATA_PROGRESS_MAX); }
    public int getStatus() { return data.get(OxygenElectrolyzerBlockEntity.DATA_STATUS); }

    public SideMode getSideMode(RelativeSide side) {
        if (!OxygenElectrolyzerBlockEntity.isConfigurableSide(side)) return SideMode.DISABLED;
        Direction worldDirection = side.resolve(getFacing());
        int ordinal = data.get(OxygenElectrolyzerBlockEntity.DATA_SIDES_START + worldDirection.ordinal());
        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(OxygenElectrolyzerBlock.FACING) ? state.getValue(OxygenElectrolyzerBlock.FACING) : Direction.NORTH;
    }
}
