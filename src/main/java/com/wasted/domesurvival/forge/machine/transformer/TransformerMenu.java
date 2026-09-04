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
    private static final int MODE_BUTTON = 0;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final TransformerBlockEntity transformer;

    public TransformerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null,
                new SimpleContainerData(TransformerBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public TransformerMenu(int containerId, Inventory playerInventory, TransformerBlockEntity transformer) {
        this(containerId, playerInventory, transformer, transformer.getDataAccess(), transformer.getBlockPos());
    }

    private TransformerMenu(int containerId, Inventory playerInventory,
                            @Nullable TransformerBlockEntity transformer,
                            ContainerData data, BlockPos blockPos) {
        super(TransformerRegistry.TRANSFORMER_MENU.get(), containerId);
        this.transformer = transformer;
        this.data = data;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), blockPos);
        checkContainerDataCount(data, TransformerBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, TransformerRegistry.TRANSFORMER.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != MODE_BUTTON) return false;
        if (transformer != null) transformer.cycleMode();
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public static int modeButtonId() {
        return MODE_BUTTON;
    }

    public int energyStored() { return data.get(TransformerBlockEntity.DATA_ENERGY); }
    public int energyCapacity() { return data.get(TransformerBlockEntity.DATA_CAPACITY); }
    public TransformerMode mode() { return TransformerMode.fromOrdinal(data.get(TransformerBlockEntity.DATA_MODE)); }
    public int inputRate() { return data.get(TransformerBlockEntity.DATA_INPUT_RATE); }
    public int outputRate() { return data.get(TransformerBlockEntity.DATA_OUTPUT_RATE); }
    public int inputThisTick() { return data.get(TransformerBlockEntity.DATA_INPUT_THIS_TICK); }
    public int outputThisTick() { return data.get(TransformerBlockEntity.DATA_OUTPUT_THIS_TICK); }
}
