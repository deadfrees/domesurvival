package com.wasted.domesurvival.forge.machine.energy;

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
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Energy Buffer menu using the same side-button protocol as the generator. */
public final class TitanEnergyBufferMenu extends AbstractContainerMenu implements EnergyTransferRateMenu {
    private static final int SIDE_BUTTON_BASE = 100;
    private static final int CHARGE_SLOT = 0;
    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private boolean sidePanelOpen;
    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final TitanEnergyBufferBlockEntity buffer;

    public TitanEnergyBufferMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, new ItemStackHandler(1),
                new SimpleContainerData(TitanEnergyBufferBlockEntity.DATA_COUNT),
                extraData.readBlockPos());
    }

    public TitanEnergyBufferMenu(int containerId, Inventory playerInventory, TitanEnergyBufferBlockEntity buffer) {
        this(containerId, playerInventory, buffer, buffer.getChargeInventory(), buffer.getDataAccess(), buffer.getBlockPos());
    }

    private TitanEnergyBufferMenu(int containerId, Inventory playerInventory,
                             @Nullable TitanEnergyBufferBlockEntity buffer,
                             IItemHandler chargeInventory, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.ENERGY_BUFFER_TITAN.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.buffer = buffer;
        checkContainerDataCount(data, TitanEnergyBufferBlockEntity.DATA_COUNT);
        // Vanilla DataSlot packets carry signed shorts: split every full value explicitly.
        for (int i=0;i<data.getCount();i++) {
            final int index=i;
            for (int part=0;part<2;part++) {
                final int shift=part*16;
                addDataSlot(new net.minecraft.world.inventory.DataSlot() {
                    public int get() { return (data.get(index) >>> shift) & 0xFFFF; }
                    public void set(int value) { data.set(index, (data.get(index) & ~(0xFFFF << shift)) | ((value & 0xFFFF) << shift)); }
                });
            }
        }
        addSlot(new SlotItemHandler(chargeInventory, 0, 178, 47) {
            @Override public boolean isActive() { return !sidePanelOpen; }
            @Override public boolean mayPlace(ItemStack stack) { return isActive() && EnergyItemCharging.isChargeable(stack) && super.mayPlace(stack); }
            @Override public boolean mayPickup(Player player) { return isActive() && super.mayPickup(player); }
        });
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;
        if (id == 201 || id == 202) { sidePanelOpen = id == 202; return true; }
        if (!sidePanelOpen) return false;
        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) return false;

        RelativeSide side = RelativeSide.values()[sideIndex];
        if (!TitanEnergyBufferBlockEntity.isConfigurableSide(side)) return false;

        if (buffer != null) {
            buffer.cycleSideMode(side);
        }
        return true;
    }

    public boolean isSidePanelOpen() { return sidePanelOpen; }
    public void setSidePanelOpen(boolean value) { sidePanelOpen=value; }

    public static int sideButtonId(RelativeSide side) {
        return SIDE_BUTTON_BASE + side.ordinal();
    }

    public int getEnergyStored() {
        return data.get(TitanEnergyBufferBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(TitanEnergyBufferBlockEntity.DATA_CAPACITY);
    }

    private int combineRate(int lowIndex, int highIndex) {
        return (data.get(lowIndex) & 0xFFFF) | ((data.get(highIndex) & 0xFFFF) << 16);
    }

    @Override
    public int getInputPerTick() {
        return combineRate(TitanEnergyBufferBlockEntity.DATA_INPUT_RATE_LOW, TitanEnergyBufferBlockEntity.DATA_INPUT_RATE_HIGH);
    }

    @Override
    public int getOutputPerTick() {
        return combineRate(TitanEnergyBufferBlockEntity.DATA_OUTPUT_RATE_LOW, TitanEnergyBufferBlockEntity.DATA_OUTPUT_RATE_HIGH);
    }

    @Override
    public int getMaxInputPerTick() {
        return TitanEnergyBufferBlockEntity.MAX_RECEIVE_PER_TICK;
    }

    @Override
    public int getMaxOutputPerTick() {
        return TitanEnergyBufferBlockEntity.MAX_OUTPUT_PER_TICK;
    }

    public SideMode getSideMode(RelativeSide side) {
        if (!TitanEnergyBufferBlockEntity.isConfigurableSide(side)) return SideMode.DISABLED;
        Direction worldDirection = side.resolve(getFacing());
        int ordinal = data.get(TitanEnergyBufferBlockEntity.DATA_SIDES_START + worldDirection.ordinal());
        SideMode[] modes = SideMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(TitanEnergyBufferBlock.FACING)
                ? state.getValue(TitanEnergyBufferBlock.FACING)
                : Direction.NORTH;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ENERGY_BUFFER_TITAN.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == CHARGE_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (!sidePanelOpen && EnergyItemCharging.isChargeable(stack)) {
            if (!moveItemStackTo(stack, CHARGE_SLOT, CHARGE_SLOT + 1, false)) return ItemStack.EMPTY;
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
}
