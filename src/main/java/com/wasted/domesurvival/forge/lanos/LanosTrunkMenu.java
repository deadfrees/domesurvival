package com.wasted.domesurvival.forge.lanos;

import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.wasted.domesurvival.forge.block.ModBlocks;

public final class LanosTrunkMenu extends AbstractContainerMenu {
    private final Container trunk;

    public LanosTrunkMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, clientContainer(inventory, data));
    }

    public LanosTrunkMenu(int containerId, Inventory inventory, Container trunk) {
        super(ModMenuTypes.LANOS_TRUNK.get(), containerId);
        this.trunk = trunk;
        trunk.startOpen(inventory.player);

        for (int column = 0; column < LanosTrunkBlockEntity.SIZE; column++) {
            addSlot(new Slot(trunk, column, 35 + column * 18, 35));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 79 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 137));
        }
    }

    private static Container clientContainer(Inventory inventory, FriendlyByteBuf data) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(data.readBlockPos());
        return blockEntity instanceof LanosTrunkBlockEntity trunk
                ? trunk
                : new SimpleContainer(LanosTrunkBlockEntity.SIZE);
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(trunk instanceof LanosTrunkBlockEntity blockEntity)) {
            return true;
        }
        BlockEntity current = player.level().getBlockEntity(blockEntity.getBlockPos());
        return current == blockEntity
                && (player.level().getBlockState(blockEntity.getBlockPos()).is(ModBlocks.LANOS_DECORATIVE.get())
                    || player.level().getBlockState(blockEntity.getBlockPos()).is(ModBlocks.LANOS_ABANDONED.get()))
                && player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5D,
                        blockEntity.getBlockPos().getY() + 0.5D,
                        blockEntity.getBlockPos().getZ() + 0.5D) <= 100.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < LanosTrunkBlockEntity.SIZE) {
            if (!moveItemStackTo(source, LanosTrunkBlockEntity.SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, 0, LanosTrunkBlockEntity.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        trunk.stopOpen(player);
    }
}
