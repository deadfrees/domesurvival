package com.wasted.domesurvival.forge.itempipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class FilteringItemPipeMenu extends AbstractContainerMenu {
    public static final int FILTER_SLOT_START_X = 16;
    public static final int FILTER_SLOT_START_Y = 42;
    public static final int FILTER_SLOT_PITCH_X = 22;
    public static final int FILTER_SLOT_PITCH_Y = 22;

    public static final int PLAYER_INV_X = 16;
    public static final int PLAYER_INV_Y = 176;
    public static final int HOTBAR_Y = 234;

    public static final int ROUTE_BUTTON_BASE = 1000;
    public static final int DEFAULT_BUTTON_BASE = 2000;

    private final BlockPos pos;
    private final ItemStackHandler filters;
    private final ContainerData data;

    public FilteringItemPipeMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos());
    }

    public FilteringItemPipeMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ItemPipeRegistry.FILTER_MENU.get(), containerId);
        this.pos = pos;

        if (inventory.player.level().getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe
                && pipe.isFiltering()) {
            this.filters = pipe.ghostFilters();
            this.data = pipe.filterData();
        } else {
            this.filters = new ItemStackHandler(ItemPipeBlockEntity.FILTER_SLOTS);
            this.data = new SimpleContainerData(ItemPipeBlockEntity.FILTER_DATA_COUNT);
            this.data.set(ItemPipeBlockEntity.FILTER_SLOTS, FilterRoute.ANY.ordinal());
        }

        for (int i = 0; i < ItemPipeBlockEntity.FILTER_SLOTS; i++) {
            int column = i % 5;
            int row = i / 5;
            addSlot(new GhostFilterSlot(
                    filters,
                    i,
                    FILTER_SLOT_START_X + column * FILTER_SLOT_PITCH_X,
                    FILTER_SLOT_START_Y + row * FILTER_SLOT_PITCH_Y
            ));
        }

        // Real player inventory is intentionally present: pick any item onto the
        // cursor and click a ghost slot. The item is copied, never consumed.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        inventory,
                        col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18
                ));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    public BlockPos pos() { return pos; }
    public FilterRoute route(int index) { return FilterRoute.filterByIndex(data.get(index)); }
    public FilterRoute defaultRoute() { return FilterRoute.defaultByIndex(data.get(ItemPipeBlockEntity.FILTER_SLOTS)); }
    public ItemStack filterStack(int index) {
        return index >= 0 && index < ItemPipeBlockEntity.FILTER_SLOTS
                ? filters.getStackInSlot(index)
                : ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < ItemPipeBlockEntity.FILTER_SLOTS
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
            if (button == 1) {
                filters.setStackInSlot(slotId, ItemStack.EMPTY);
            } else {
                ItemStack sample = getCarried();
                if (sample.isEmpty()) sample = player.getMainHandItem();
                if (!sample.isEmpty()) {
                    filters.setStackInSlot(slotId, sample.copyWithCount(1));
                }
            }
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player.level().getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) || !pipe.isFiltering()) {
            return false;
        }

        if (id >= ROUTE_BUTTON_BASE
                && id < ROUTE_BUTTON_BASE + ItemPipeBlockEntity.FILTER_SLOTS * 8) {
            int packed = id - ROUTE_BUTTON_BASE;
            int slot = packed / 8;
            int routeOrdinal = packed % 8;
            if (slot >= 0 && slot < ItemPipeBlockEntity.FILTER_SLOTS
                    && routeOrdinal >= 0 && routeOrdinal <= FilterRoute.NONE.ordinal()) {
                pipe.setFilterRoute(slot, FilterRoute.values()[routeOrdinal]);
                broadcastChanges();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
                && player.level().getBlockState(pos).getBlock() instanceof ItemPipeBlock pipe
                && pipe.isFiltering();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class GhostFilterSlot extends SlotItemHandler {
        private GhostFilterSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
