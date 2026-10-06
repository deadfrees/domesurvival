package com.wasted.domesurvival.forge.metro.network;

import net.minecraft.network.FriendlyByteBuf;

public record MetroNodeClientData(
        String nodeId,
        String displayName,
        MetroNodeType type,
        MetroNodeState state,
        String dimension,
        int x,
        int y,
        int z
) {
    public static MetroNodeClientData from(MetroNode node) {
        return new MetroNodeClientData(
                node.nodeId(),
                node.displayName(),
                node.type(),
                node.state(),
                node.dimension(),
                node.position().getX(),
                node.position().getY(),
                node.position().getZ()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(nodeId, 512);
        buf.writeUtf(displayName, 512);
        buf.writeEnum(type);
        buf.writeEnum(state);
        buf.writeUtf(dimension, 512);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public static MetroNodeClientData read(FriendlyByteBuf buf) {
        return new MetroNodeClientData(
                buf.readUtf(512),
                buf.readUtf(512),
                buf.readEnum(MetroNodeType.class),
                buf.readEnum(MetroNodeState.class),
                buf.readUtf(512),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }
}
