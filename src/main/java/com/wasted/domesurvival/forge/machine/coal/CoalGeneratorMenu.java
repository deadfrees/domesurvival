package com.wasted.domesurvival.forge.machine.coal;

import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.block.ModBlocks;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CoalGeneratorMenu extends AbstractContainerMenu {
    private static final int FUEL_SLOT_INDEX = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;
    private static final int SIDE_BUTTON_BASE = 100;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final CoalGeneratorBlockEntity generator;

    public CoalGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, new ItemStackHandler(1),
                new SimpleContainerData(CoalGeneratorBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public CoalGeneratorMenu(int containerId, Inventory playerInventory, CoalGeneratorBlockEntity generator) {
        this(containerId, playerInventory, generator, generator.getInventory(), generator.getDataAccess(), generator.getBlockPos());
    }

    private CoalGeneratorMenu(int containerId, Inventory playerInventory,
                              @Nullable CoalGeneratorBlockEntity generator,
                              IItemHandler fuelInventory, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.COAL_GENERATOR.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.generator = generator;
        checkContainerDataCount(data, CoalGeneratorBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(fuelInventory, 0, 182, 106) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return CoalGeneratorBlockEntity.isValidFuel(stack);
            }
        });
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

    @Override public boolean stillValid(Player player) { return stillValid(access, player, ModBlocks.COAL_GENERATOR.get()); }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == FUEL_SLOT_INDEX) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (CoalGeneratorBlockEntity.isValidFuel(stack)
                && moveItemStackTo(stack, FUEL_SLOT_INDEX, FUEL_SLOT_INDEX + 1, false)) {
            // valid fuel moved into the generator
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
        if (!CoalGeneratorBlockEntity.isConfigurableSide(side)) return false;
        if (generator != null) generator.cycleSideMode(side);
        return true;
    }

    public static int sideButtonId(RelativeSide side) { return SIDE_BUTTON_BASE + side.ordinal(); }

    public int getEnergyStored() { return data.get(CoalGeneratorBlockEntity.DATA_ENERGY); }
    public int getEnergyCapacity() { return data.get(CoalGeneratorBlockEntity.DATA_CAPACITY); }
    public int getBurnTime() { return data.get(CoalGeneratorBlockEntity.DATA_BURN_TIME); }
    public int getMaxBurnTime() { return data.get(CoalGeneratorBlockEntity.DATA_MAX_BURN_TIME); }

    public SideMode getSideMode(RelativeSide side) {
        if (!CoalGeneratorBlockEntity.isConfigurableSide(side)) return SideMode.DISABLED;
        Direction worldDirection = side.resolve(getFacing());
        int ordinal = data.get(CoalGeneratorBlockEntity.DATA_SIDES_START + worldDirection.ordinal());
        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(CoalGeneratorBlock.FACING) ? state.getValue(CoalGeneratorBlock.FACING) : Direction.NORTH;
    }

    public BlockPos getBlockPos() { return blockPos; }
}
