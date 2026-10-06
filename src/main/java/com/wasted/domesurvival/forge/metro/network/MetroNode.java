package com.wasted.domesurvival.forge.metro.network;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record MetroNode(
        String nodeId,
        MetroNodeType type,
        @Nullable UUID stationId,
        String dimension,
        BlockPos position,
        String displayName,
        MetroNodeState state
) {
    public MetroNode {
        if (nodeId == null || nodeId.isBlank()) throw new IllegalArgumentException("nodeId");
        if (type == null) type = MetroNodeType.RESTORED_STATION;
        if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("dimension");
        if (position == null) throw new IllegalArgumentException("position");
        if (displayName == null || displayName.isBlank()) displayName = nodeId;
        if (state == null) state = MetroNodeState.ACTIVE;
        position = position.immutable();
    }
}
