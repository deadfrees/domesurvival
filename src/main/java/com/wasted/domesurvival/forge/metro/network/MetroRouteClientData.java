package com.wasted.domesurvival.forge.metro.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record MetroRouteClientData(
        UUID routeId,
        String nodeA,
        String nodeB,
        double distance,
        int travelTimeTicks,
        MetroRouteState state
) {
    public static MetroRouteClientData from(MetroRoute route) {
        return new MetroRouteClientData(
                route.routeId(),
                route.nodeA(),
                route.nodeB(),
                route.distance(),
                route.travelTimeTicks(),
                route.state()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(routeId);
        buf.writeUtf(nodeA, 512);
        buf.writeUtf(nodeB, 512);
        buf.writeDouble(distance);
        buf.writeVarInt(travelTimeTicks);
        buf.writeEnum(state);
    }

    public static MetroRouteClientData read(FriendlyByteBuf buf) {
        return new MetroRouteClientData(
                buf.readUUID(),
                buf.readUtf(512),
                buf.readUtf(512),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readEnum(MetroRouteState.class)
        );
    }

    public boolean connects(String first, String second) {
        return (nodeA.equals(first) && nodeB.equals(second))
                || (nodeA.equals(second) && nodeB.equals(first));
    }

    public String other(String nodeId) {
        if (nodeA.equals(nodeId)) return nodeB;
        if (nodeB.equals(nodeId)) return nodeA;
        return "";
    }
}
