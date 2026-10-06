package com.wasted.domesurvival.forge.machine.alloy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public final class AlloyEnricherMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 2;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_START = PLAYER_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public AlloyEnricherMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, new ItemStackHandler(2),
                new SimpleContainerData(AlloyEnricherBlockEntity.DATA_COUNT), extraData.readBlockPos());
    }

    public AlloyEnricherMenu(int id, Inventory playerInventory, AlloyEnricherBlockEntity enricher) {
        this(id, playerInventory, enricher.getInventory(), enricher.getDataAccess(), enricher.getBlockPos());
    }

    private AlloyEnricherMenu(int id, Inventory playerInventory, IItemHandler machine,
                              ContainerData data, BlockPos pos) {
        super(AlloyEnricherRegistry.ALLOY_ENRICHER_MENU.get(), id);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        checkContainerDataCount(data, AlloyEnricherBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(machine, AlloyEnricherBlockEntity.SLOT_INPUT, 63, 72));
        addSlot(new SlotItemHandler(machine, AlloyEnricherBlockEntity.SLOT_OUTPUT, 157, 72) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory, col + row * 9 + 9,
                        14 + col * 22, 161 + row * 22
                ));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 14 + col * 22, 229));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, AlloyEnricherRegistry.ALLOY_ENRICHER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (moveItemStackTo(stack, 0, 1, false)) {
            // Server-side ItemStackHandler validates alloy-enricher inputs.
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

    public int energyStored() { return data.get(AlloyEnricherBlockEntity.DATA_ENERGY); }
    public int energyCapacity() { return data.get(AlloyEnricherBlockEntity.DATA_ENERGY_CAPACITY); }
    public int neofluxStored() { return data.get(AlloyEnricherBlockEntity.DATA_NEOFLUX); }
    public int neofluxCapacity() { return data.get(AlloyEnricherBlockEntity.DATA_NEOFLUX_CAPACITY); }
    public int progress() { return data.get(AlloyEnricherBlockEntity.DATA_PROGRESS); }
    public int progressMax() { return data.get(AlloyEnricherBlockEntity.DATA_PROGRESS_MAX); }
    public int status() { return data.get(AlloyEnricherBlockEntity.DATA_STATUS); }
    public int recipeEnergy() { return data.get(AlloyEnricherBlockEntity.DATA_RECIPE_ENERGY); }
    public int fluidRequired() { return data.get(AlloyEnricherBlockEntity.DATA_FLUID_REQUIRED); }
}
