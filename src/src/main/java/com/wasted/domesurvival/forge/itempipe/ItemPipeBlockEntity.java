package com.wasted.domesurvival.forge.itempipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class ItemPipeBlockEntity extends BlockEntity {
    public static final int FILTER_SLOTS = 20;
    public static final int FILTER_DATA_COUNT = FILTER_SLOTS + 1;

    private static final String NBT_MODES = "ConnectorModes";
    private static final String NBT_FILTERS = "GhostFilters";
    private static final String NBT_ROUTES = "FilterRoutes";
    private static final String NBT_DEFAULT_ROUTE = "DefaultRoute";
    private static final String NBT_MANUAL_DISCONNECTS = "ManualDisconnects";

    private final byte[] connectorModes = new byte[Direction.values().length];
    private final int[] filterRoutes = new int[FILTER_SLOTS];
    private int defaultRoute = FilterRoute.ANY.ordinal();
    private int filterRevision;
    private byte manualDisconnectMask;

    private final ItemStackHandler ghostFilters = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            filterRevision++;
            markDirtyAndSync();
            ItemPipeNetworkManager.markDirty(level);
        }
    };

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ItemPipeRegistry.BLOCK_ENTITY.get(), pos, state);
        Arrays.fill(connectorModes, (byte) ItemConnectorMode.DISABLED.ordinal());
        Arrays.fill(filterRoutes, FilterRoute.NONE.ordinal());
    }

    public ItemPipeTier tier() {
        if (getBlockState().getBlock() instanceof ItemPipeBlock pipe) return pipe.tier();
        return ItemPipeTier.COPPER;
    }

    public boolean isFiltering() {
        return getBlockState().getBlock() instanceof ItemPipeBlock pipe && pipe.isFiltering();
    }


    public boolean isManuallyDisconnected(Direction side) {
        return (Byte.toUnsignedInt(manualDisconnectMask) & (1 << side.ordinal())) != 0;
    }

    /** @return true when this side is disconnected after toggling. */
    public boolean toggleManualDisconnect(Direction side) {
        manualDisconnectMask = (byte) (Byte.toUnsignedInt(manualDisconnectMask) ^ (1 << side.ordinal()));
        markDirtyAndSync();
        return isManuallyDisconnected(side);
    }

    public ItemConnectorMode getConnectorMode(Direction side) {
        int ordinal = Byte.toUnsignedInt(connectorModes[side.ordinal()]);
        ItemConnectorMode[] modes = ItemConnectorMode.values();
        return ordinal < modes.length ? modes[ordinal] : ItemConnectorMode.DISABLED;
    }

    public void setConnectorMode(Direction side, ItemConnectorMode mode) {
        if (getConnectorMode(side) == mode) return;
        connectorModes[side.ordinal()] = (byte) mode.ordinal();
        markDirtyAndSync();
        ItemPipeNetworkManager.markDirty(level);
    }

    public ItemConnectorMode cycleConnectorMode(Direction side, boolean reverse) {
        ItemConnectorMode current = getConnectorMode(side);
        ItemConnectorMode next = reverse ? current.previous() : current.next();
        setConnectorMode(side, next);
        return next;
    }

    public ItemStackHandler ghostFilters() {
        return ghostFilters;
    }

    public FilterRoute filterRoute(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return FilterRoute.NONE;
        return FilterRoute.filterByIndex(filterRoutes[slot]);
    }

    public void cycleFilterRoute(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        int next = filterRoutes[slot] + 1;
        if (next > FilterRoute.NONE.ordinal()) next = 0;
        setFilterRoute(slot, FilterRoute.filterByIndex(next));
    }

    public void setFilterRoute(int slot, FilterRoute route) {
        if (slot < 0 || slot >= FILTER_SLOTS || route == FilterRoute.ANY) return;
        if (filterRoutes[slot] == route.ordinal()) return;
        filterRoutes[slot] = route.ordinal();
        filterRevision++;
        markDirtyAndSync();
        ItemPipeNetworkManager.markDirty(level);
    }

    public FilterRoute defaultRoute() {
        return FilterRoute.defaultByIndex(defaultRoute);
    }

    public void cycleDefaultRoute() {
        int next = defaultRoute + 1;
        if (next >= FilterRoute.values().length) next = 0;
        if (next == FilterRoute.NONE.ordinal()) next++;
        if (next >= FilterRoute.values().length) next = 0;
        setDefaultRoute(FilterRoute.defaultByIndex(next));
    }

    public void setDefaultRoute(FilterRoute route) {
        if (route == FilterRoute.NONE) return;
        if (defaultRoute == route.ordinal()) return;
        defaultRoute = route.ordinal();
        filterRevision++;
        markDirtyAndSync();
        ItemPipeNetworkManager.markDirty(level);
    }

    public int configuredFilterCount() {
        int count = 0;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!ghostFilters.getStackInSlot(i).isEmpty()) count++;
        }
        return count;
    }

    public boolean allowsFilterExit(Direction direction, ItemStack stack) {
        if (!isFiltering()) return true;

        boolean matched = false;
        boolean allowed = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack filter = ghostFilters.getStackInSlot(i);
            if (filter.isEmpty() || filter.getItem() != stack.getItem()) continue;
            matched = true;
            Direction routed = filterRoute(i).direction();
            if (routed == direction) allowed = true;
        }

        if (matched) return allowed;

        // Unmatched items are intentionally not configured in the GUI.
        // They may use any otherwise valid output.
        return true;
    }

    public int filterRevision() {
        return filterRevision;
    }

    public ContainerData connectorData(Direction side) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getConnectorMode(side).ordinal();
                    case 1 -> tier().itemsPerCycle();
                    case 2 -> tier().cooldownTicks();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0 && value >= 0 && value < ItemConnectorMode.values().length) {
                    setConnectorMode(side, ItemConnectorMode.values()[value]);
                }
            }

            @Override
            public int getCount() { return 3; }
        };
    }

    public ContainerData filterData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index >= 0 && index < FILTER_SLOTS) return filterRoutes[index];
                if (index == FILTER_SLOTS) return defaultRoute;
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < FILTER_SLOTS) {
                    filterRoutes[index] = value;
                } else if (index == FILTER_SLOTS) {
                    defaultRoute = value;
                }
            }

            @Override
            public int getCount() { return FILTER_DATA_COUNT; }
        };
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ItemPipeNetworkManager.registerPipe(this);
    }

    @Override
    public void setRemoved() {
        ItemPipeNetworkManager.unregisterPipe(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByteArray(NBT_MODES, connectorModes);
        tag.put(NBT_FILTERS, ghostFilters.serializeNBT());
        tag.put(NBT_ROUTES, new IntArrayTag(filterRoutes));
        tag.putInt(NBT_DEFAULT_ROUTE, defaultRoute);
        tag.putByte(NBT_MANUAL_DISCONNECTS, manualDisconnectMask);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        byte[] modes = tag.getByteArray(NBT_MODES);
        if (modes.length == connectorModes.length) {
            System.arraycopy(modes, 0, connectorModes, 0, connectorModes.length);
        }

        if (tag.contains(NBT_FILTERS)) ghostFilters.deserializeNBT(tag.getCompound(NBT_FILTERS));

        int[] routes = tag.getIntArray(NBT_ROUTES);
        if (routes.length == FILTER_SLOTS) {
            System.arraycopy(routes, 0, filterRoutes, 0, FILTER_SLOTS);
        }

        if (tag.contains(NBT_DEFAULT_ROUTE)) {
            defaultRoute = tag.getInt(NBT_DEFAULT_ROUTE);
        }
        manualDisconnectMask = tag.getByte(NBT_MANUAL_DISCONNECTS);
        filterRevision++;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }
}
