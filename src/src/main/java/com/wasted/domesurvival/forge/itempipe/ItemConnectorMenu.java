package com.wasted.domesurvival.forge.itempipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class ItemConnectorMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Direction side;
    private final ContainerData data;

    public ItemConnectorMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), Direction.from3DDataValue(buf.readUnsignedByte()));
    }

    public ItemConnectorMenu(int containerId, Inventory inventory, BlockPos pos, Direction side) {
        super(ItemPipeRegistry.CONNECTOR_MENU.get(), containerId);
        this.pos = pos;
        this.side = side;

        if (inventory.player.level().getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            this.data = pipe.connectorData(side);
        } else {
            this.data = new SimpleContainerData(3);
        }
        addDataSlots(data);
    }

    public BlockPos pos() { return pos; }
    public Direction side() { return side; }
    public ItemConnectorMode mode() {
        int value = data.get(0);
        return value >= 0 && value < ItemConnectorMode.values().length
                ? ItemConnectorMode.values()[value]
                : ItemConnectorMode.DISABLED;
    }
    public int itemsPerCycle() { return data.get(1); }
    public int cooldownTicks() { return data.get(2); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= ItemConnectorMode.values().length) return false;
        data.set(0, id);
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
                && player.level().getBlockState(pos).getBlock() instanceof ItemPipeBlock;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
