package com.wasted.domesurvival.forge.machine.transformer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TransformerMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_END = 27;
    private static final int HOTBAR_START = 27;
    private static final int HOTBAR_END = 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public TransformerMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        this(
                containerId,
                playerInventory,
                new SimpleContainerData(TransformerBlockEntity.DATA_COUNT),
                extraData.readBlockPos()
        );
    }

    public TransformerMenu(
            int containerId,
            Inventory playerInventory,
            TransformerBlockEntity transformer
    ) {
        this(
                containerId,
                playerInventory,
                transformer.getDataAccess(),
                transformer.getBlockPos()
        );
    }

    private TransformerMenu(
            int containerId,
            Inventory playerInventory,
            ContainerData data,
            BlockPos blockPos
    ) {
        super(TransformerRegistry.TRANSFORMER_MENU.get(), containerId);
        this.data = data;
        this.access = ContainerLevelAccess.create(
                playerInventory.player.level(),
                blockPos
        );

        checkContainerDataCount(data, TransformerBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        14 + column * 22,
                        161 + row * 22
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(
                    playerInventory,
                    column,
                    14 + column * 22,
                    229
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                TransformerRegistry.TRANSFORMER.get()
        );
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!moveItemStackTo(
                    stack,
                    PLAYER_INVENTORY_START,
                    PLAYER_INVENTORY_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    public int energyStored() {
        return data.get(TransformerBlockEntity.DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(TransformerBlockEntity.DATA_CAPACITY);
    }

    @Nullable
    public TransformerMode mode() {
        return TransformerMode.fromOrdinalOrNull(
                data.get(TransformerBlockEntity.DATA_MODE)
        );
    }

    public int inputRate() {
        return data.get(TransformerBlockEntity.DATA_INPUT_RATE);
    }

    public int outputRate() {
        return data.get(TransformerBlockEntity.DATA_OUTPUT_RATE);
    }

    public int inputThisTick() {
        return data.get(TransformerBlockEntity.DATA_INPUT_THIS_TICK);
    }

    public int outputThisTick() {
        return data.get(TransformerBlockEntity.DATA_OUTPUT_THIS_TICK);
    }

    public boolean active() {
        return data.get(TransformerBlockEntity.DATA_ACTIVE) != 0;
    }

    public boolean validTopology() {
        return data.get(TransformerBlockEntity.DATA_VALID) != 0;
    }
}
