package com.wasted.domesurvival.forge.storage.tank;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** One shared structure UI opened from any reservoir cell. */
public final class UniversalTankMenu extends AbstractContainerMenu {
    private static final int SIDE_BUTTON_BASE = 100;

    private final Level level;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final UniversalTankBlockEntity tank;

    public UniversalTankMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                null,
                new SimpleContainerData(UniversalTankBlockEntity.DATA_COUNT),
                extraData.readBlockPos()
        );
    }

    public UniversalTankMenu(int containerId, Inventory playerInventory, UniversalTankBlockEntity tank) {
        this(containerId, playerInventory, tank, tank.getDataAccess(), tank.getBlockPos());
    }

    private UniversalTankMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable UniversalTankBlockEntity tank,
            ContainerData data,
            BlockPos blockPos
    ) {
        super(UniversalTankRegistry.UNIVERSAL_TANK_MENU.get(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.access = ContainerLevelAccess.create(level, blockPos);
        this.data = data;
        this.tank = tank;

        checkContainerDataCount(data, UniversalTankBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int sideIndex = id - SIDE_BUTTON_BASE;
        if (sideIndex < 0 || sideIndex >= RelativeSide.values().length) return false;

        if (tank != null) {
            tank.cycleSideMode(RelativeSide.values()[sideIndex]);
        }
        return true;
    }

    public static int sideButtonId(RelativeSide side) {
        return SIDE_BUTTON_BASE + side.ordinal();
    }

    public UniversalTankContentKind getContentKind() {
        return UniversalTankContentKind.byOrdinal(data.get(UniversalTankBlockEntity.DATA_KIND));
    }

    public int getStoredAmount() {
        return Math.max(0, data.get(UniversalTankBlockEntity.DATA_AMOUNT));
    }

    public int getCapacity() {
        return Math.max(0, data.get(UniversalTankBlockEntity.DATA_CAPACITY));
    }

    public int getBlockCount() {
        return Math.max(1, data.get(UniversalTankBlockEntity.DATA_BLOCK_COUNT));
    }

    public int getSizeX() {
        return Math.max(1, data.get(UniversalTankBlockEntity.DATA_SIZE_X));
    }

    public int getSizeY() {
        return Math.max(1, data.get(UniversalTankBlockEntity.DATA_SIZE_Y));
    }

    public int getSizeZ() {
        return Math.max(1, data.get(UniversalTankBlockEntity.DATA_SIZE_Z));
    }

    public boolean usesUnifiedModel() {
        return data.get(UniversalTankBlockEntity.DATA_UNIFIED) != 0;
    }

    public int getFillPercent() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0
                : Math.max(0, Math.min(100, (int) (((long) getStoredAmount() * 100L) / capacity)));
    }

    public SideMode getSideMode(RelativeSide side) {
        Direction worldDirection = UniversalTankBlockEntity.resolveRelativeSide(side, getFacing());
        int ordinal = data.get(
                UniversalTankBlockEntity.DATA_SIDES_START + worldDirection.ordinal()
        );
        SideMode[] values = SideMode.values();
        return ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : SideMode.DISABLED;
    }

    public Direction getFacing() {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof UniversalTankBlockEntity clientTank) {
            return clientTank.getFacing();
        }

        BlockState state = level.getBlockState(blockPos);
        return state.hasProperty(UniversalTankBlock.FACING)
                ? state.getValue(UniversalTankBlock.FACING)
                : Direction.NORTH;
    }

    public FluidStack getClientFluidStack() {
        if (!level.isClientSide || getContentKind() != UniversalTankContentKind.FLUID) {
            return FluidStack.EMPTY;
        }

        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof UniversalTankBlockEntity clientTank)) {
            return FluidStack.EMPTY;
        }

        return clientTank.getVisibleFluidStack();
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, UniversalTankRegistry.UNIVERSAL_TANK.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
