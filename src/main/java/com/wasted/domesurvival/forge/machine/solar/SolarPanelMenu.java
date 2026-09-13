package com.wasted.domesurvival.forge.machine.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SolarPanelMenu extends AbstractContainerMenu {
    private final Level level;
    private final BlockPos blockPos;
    private final ContainerData data;
    @Nullable
    private final SolarPanelBlockEntity panel;

    public SolarPanelMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        this(
                containerId,
                playerInventory,
                null,
                new SimpleContainerData(SolarPanelBlockEntity.DATA_COUNT),
                extraData.readBlockPos()
        );
    }

    public SolarPanelMenu(
            int containerId,
            Inventory playerInventory,
            SolarPanelBlockEntity panel
    ) {
        this(
                containerId,
                playerInventory,
                panel,
                panel.getDataAccess(),
                panel.getBlockPos()
        );
    }

    private SolarPanelMenu(
            int containerId,
            Inventory playerInventory,
            @Nullable SolarPanelBlockEntity panel,
            ContainerData data,
            BlockPos blockPos
    ) {
        super(SolarPanelRegistry.menuType(), containerId);
        this.level = playerInventory.player.level();
        this.blockPos = blockPos;
        this.data = data;
        this.panel = panel;
        checkContainerDataCount(data, SolarPanelBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(level.getBlockState(blockPos).getBlock() instanceof SolarPanelBlock)) {
            return false;
        }

        double dx = player.getX() - (blockPos.getX() + 0.5D);
        double dy = player.getY() - (blockPos.getY() + 0.5D);
        double dz = player.getZ() - (blockPos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= 64.0D;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int getEnergyStored() {
        return data.get(SolarPanelBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(SolarPanelBlockEntity.DATA_CAPACITY);
    }

    public int getGenerationPerTick() {
        return data.get(SolarPanelBlockEntity.DATA_GENERATION);
    }

    public int getMaxOutputPerTick() {
        return data.get(SolarPanelBlockEntity.DATA_MAX_OUTPUT);
    }

    public SolarPanelState getSolarState() {
        return SolarPanelState.fromNetworkId(data.get(SolarPanelBlockEntity.DATA_STATE));
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Nullable
    public SolarPanelBlockEntity getPanel() {
        return panel;
    }
}
