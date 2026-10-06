package com.wasted.domesurvival.forge.metro.network;

import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class MetroNetworkMenu extends AbstractContainerMenu {
    private static final int MAX_NODES = 256;
    private static final int MAX_ROUTES = 512;

    private final BlockPos consolePos;
    private final List<MetroNodeClientData> nodes;
    private final List<MetroRouteClientData> routes;

    public MetroNetworkMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(
                containerId,
                inventory,
                buf.readBlockPos(),
                readNodes(buf),
                readRoutes(buf)
        );
    }

    public MetroNetworkMenu(int containerId,
                            Inventory inventory,
                            BlockPos consolePos,
                            MetroNetworkSavedData network) {
        this(
                containerId,
                inventory,
                consolePos,
                network.nodes().stream().map(MetroNodeClientData::from).toList(),
                network.routes().stream().map(MetroRouteClientData::from).toList()
        );
    }

    private MetroNetworkMenu(int containerId,
                             Inventory inventory,
                             BlockPos consolePos,
                             List<MetroNodeClientData> nodes,
                             List<MetroRouteClientData> routes) {
        super(ModMenuTypes.METRO_NETWORK.get(), containerId);
        this.consolePos = consolePos.immutable();
        this.nodes = List.copyOf(nodes);
        this.routes = List.copyOf(routes);
    }

    public static void writeOpenData(FriendlyByteBuf buf,
                                     BlockPos consolePos,
                                     MetroNetworkSavedData network) {
        buf.writeBlockPos(consolePos);
        List<MetroNode> nodes = network.nodes();
        buf.writeVarInt(Math.min(nodes.size(), MAX_NODES));
        for (int i = 0; i < Math.min(nodes.size(), MAX_NODES); i++) {
            MetroNodeClientData.from(nodes.get(i)).write(buf);
        }

        List<MetroRoute> routes = network.routes();
        buf.writeVarInt(Math.min(routes.size(), MAX_ROUTES));
        for (int i = 0; i < Math.min(routes.size(), MAX_ROUTES); i++) {
            MetroRouteClientData.from(routes.get(i)).write(buf);
        }
    }

    private static List<MetroNodeClientData> readNodes(FriendlyByteBuf buf) {
        int count = Math.min(Math.max(0, buf.readVarInt()), MAX_NODES);
        ArrayList<MetroNodeClientData> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) result.add(MetroNodeClientData.read(buf));
        return result;
    }

    private static List<MetroRouteClientData> readRoutes(FriendlyByteBuf buf) {
        int count = Math.min(Math.max(0, buf.readVarInt()), MAX_ROUTES);
        ArrayList<MetroRouteClientData> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) result.add(MetroRouteClientData.read(buf));
        return result;
    }

    public List<MetroNodeClientData> nodes() {
        return nodes;
    }

    public List<MetroRouteClientData> routes() {
        return routes;
    }

    public BlockPos consolePos() {
        return consolePos;
    }

    @Override
    public boolean stillValid(Player player) {
        double dx = player.getX() - (consolePos.getX() + 0.5D);
        double dy = player.getY() - (consolePos.getY() + 0.5D);
        double dz = player.getZ() - (consolePos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= 64.0D;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
