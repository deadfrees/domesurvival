package com.wasted.domesurvival.forge.hopper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class TieredHopperMenu extends AbstractContainerMenu {
    private final Container container;
    private final int containerSize;
    private final int containerRows;

    public TieredHopperMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                clientContainer(playerInventory, extraData)
        );
    }

    public TieredHopperMenu(int containerId, Inventory playerInventory, Container container) {
        super(HopperRegistryEvents.TIERED_HOPPER_MENU.get(), containerId);

        this.container = container;
        this.containerSize = container.getContainerSize();
        this.containerRows = (containerSize + 8) / 9;

        container.startOpen(playerInventory.player);

        int remaining = containerSize;
        int slotIndex = 0;

        for (int row = 0; row < containerRows; row++) {
            int rowSlots = Math.min(9, remaining);
            int startX = 8 + (9 - rowSlots) * 9;
            int y = 18 + row * 18;

            for (int column = 0; column < rowSlots; column++) {
                addSlot(new Slot(container, slotIndex++, startX + column * 18, y));
            }

            remaining -= rowSlots;
        }

        int playerInventoryY = 50 + (containerRows - 1) * 18;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        playerInventoryY + row * 18
                ));
            }
        }

        int hotbarY = playerInventoryY + 58;
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    hotbarY
            ));
        }
    }

    private static Container clientContainer(Inventory playerInventory, FriendlyByteBuf data) {
        BlockEntity blockEntity = playerInventory.player.level()
                .getBlockEntity(data.readBlockPos());

        if (blockEntity instanceof TieredHopperBlockEntity hopper) {
            return hopper;
        }

        // This fallback is only used if the client chunk has not produced the BE yet.
        // The smallest size keeps construction safe until the server sync closes/reopens.
        return new SimpleContainer(HopperTier.COPPER.slots());
    }

    public int getContainerSize() {
        return containerSize;
    }

    public int getContainerRows() {
        return containerRows;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();
        ItemStack original = sourceStack.copy();

        if (index < containerSize) {
            if (!moveItemStackTo(sourceStack, containerSize, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(sourceStack, 0, containerSize, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (sourceStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, sourceStack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
