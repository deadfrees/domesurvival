package com.wasted.domesurvival.forge.metro.network;

import java.util.UUID;

public record MetroRoute(
        UUID routeId,
        String nodeA,
        String nodeB,
        double distance,
        int travelTimeTicks,
        MetroRouteState state
) {
    public MetroRoute {
        if (routeId == null) routeId = UUID.randomUUID();
        if (nodeA == null || nodeA.isBlank()) throw new IllegalArgumentException("nodeA");
        if (nodeB == null || nodeB.isBlank()) throw new IllegalArgumentException("nodeB");
        distance = Math.max(0.0D, distance);
        travelTimeTicks = Math.max(1, travelTimeTicks);
        if (state == null) state = MetroRouteState.ACTIVE;
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

    public MetroRoute withMetrics(double distance, int travelTimeTicks) {
        return new MetroRoute(routeId, nodeA, nodeB, distance, travelTimeTicks, state);
    }
}
