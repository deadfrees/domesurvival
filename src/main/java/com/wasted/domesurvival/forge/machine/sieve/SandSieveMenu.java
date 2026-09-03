package com.wasted.domesurvival.forge.machine.sieve;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.SieveMeshItem;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SandSieveMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_END = PLAYER_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    @Nullable private final SandSieveBlockEntity sieve;

    public SandSieveMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, null, new ItemStackHandler(SandSieveBlockEntity.SLOT_COUNT),
                new SimpleContainerData(SandSieveBlockEntity.DATA_COUNT), buffer.readBlockPos());
    }

    public SandSieveMenu(int id, Inventory playerInventory, SandSieveBlockEntity sieve) {
        this(id, playerInventory, sieve, sieve.getInventory(), sieve.getDataAccess(), sieve.getBlockPos());
    }

    private SandSieveMenu(int id, Inventory playerInventory, @Nullable SandSieveBlockEntity sieve,
                          IItemHandler machineInventory, ContainerData data, BlockPos pos) {
        super(ModMenuTypes.SAND_SIEVE.get(), id);
        this.sieve = sieve;
        this.data = data;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        checkContainerDataCount(data, SandSieveBlockEntity.DATA_COUNT);
        addDataSlots(data);

        addSlot(new SlotItemHandler(machineInventory, SandSieveBlockEntity.SLOT_SAND, 65, 61) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return isSand(stack); }
        });
        addSlot(new SlotItemHandler(machineInventory, SandSieveBlockEntity.SLOT_MESH, 103, 61) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof SieveMeshItem;
            }
        });
        for (int i = 0; i < 3; i++) {
            addSlot(new SlotItemHandler(machineInventory, SandSieveBlockEntity.SLOT_OUTPUT_FIRST + i,
                    205 + i * 24, 61) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(playerInventory,
                        column + row * 9 + 9, 51 + column * 22, 137 + row * 22));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, column,
                    51 + column * 22, 203));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SAND_SIEVE.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (isSand(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof SieveMeshItem) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_END) {
            if (!moveItemStackTo(stack, PLAYER_END, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    private static boolean isSand(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(BlockTags.SAND);
    }

    public int progress() { return data.get(SandSieveBlockEntity.DATA_PROGRESS); }
    public int progressMax() { return data.get(SandSieveBlockEntity.DATA_PROGRESS_MAX); }
    public int water() { return data.get(SandSieveBlockEntity.DATA_WATER); }
    public int waterCapacity() { return data.get(SandSieveBlockEntity.DATA_WATER_CAPACITY); }
    public int status() { return data.get(SandSieveBlockEntity.DATA_STATUS); }
    public boolean wetCycle() { return data.get(SandSieveBlockEntity.DATA_WET_CYCLE) != 0; }
}
