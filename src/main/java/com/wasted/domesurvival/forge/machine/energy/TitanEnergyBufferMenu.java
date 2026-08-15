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

/** Energy Buffer menu using the same side-button protocol as the generator. */
public final class TitanEnergyBufferMenu extends AbstractContainerMenu implements EnergyTransferRateMenu {
    private static final int SIDE_BUTTON_BASE = 100;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final TitanEnergyBufferBlockEntity buffer;

    public TitanEnergyBufferMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null,
                new SimpleContainerData(TitanEnergyBufferBlockEntity.DATA_COUNT),
                extraData.readBlockPos());
    }

    public TitanEnergyBufferMenu(int containerId, Inventory playerInventory, TitanEnergyBufferBlockEntity buffer) {
        this(containerId, playerInventory, buffer, buffer.getDataAccess(), buffer.getBlockPos());
    }

    private TitanEnergyBufferMenu(int containerId, Inventory playerInventory,
                             @Nullable TitanEnergyBufferBlockEntity buffer,
                             ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.ENERGY_BUFFER_TITAN.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.buffer = buffer;
        checkContainerDataCount(data, TitanEnergyBufferBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) return false;

        RelativeSide side = RelativeSide.values()[sideIndex];
        if (!TitanEnergyBufferBlockEntity.isConfigurableSide(side)) return false;

        if (buffer != null) {
            buffer.cycleSideMode(side);
        }
        return true;
    }

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
        return ItemStack.EMPTY;
    }
}
