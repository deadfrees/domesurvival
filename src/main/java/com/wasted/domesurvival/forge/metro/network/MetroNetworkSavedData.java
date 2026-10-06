package com.wasted.domesurvival.forge.metro.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stage-5 server-side source of truth for metro nodes and routes.
 *
 * No world/chunk scans are performed here. The graph is updated only by
 * restoration/build events, startup synchronization and explicit dev commands.
 */
public final class MetroNetworkSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_metro_network";
    public static final int CURRENT_VERSION = 1;

    private final Map<String, MetroNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, MetroRoute> routes = new LinkedHashMap<>();
    private int networkVersion = CURRENT_VERSION;

    public static MetroNetworkSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                MetroNetworkSavedData::load,
                MetroNetworkSavedData::new,
                DATA_NAME
        );
    }

    public static MetroNetworkSavedData load(CompoundTag tag) {
        MetroNetworkSavedData data = new MetroNetworkSavedData();
        data.networkVersion = Math.max(0, tag.getInt("NetworkVersion"));

        ListTag nodeList = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodeList.size(); i++) {
            CompoundTag entry = nodeList.getCompound(i);
            String nodeId = entry.getString("NodeId");
            String dimension = entry.getString("Dimension");
            if (nodeId.isBlank() || dimension.isBlank() || !entry.contains("Position", Tag.TAG_LONG)) continue;

            UUID stationId = entry.hasUUID("StationId") ? entry.getUUID("StationId") : null;
            MetroNode node = new MetroNode(
                    nodeId,
                    MetroNodeType.byName(entry.getString("Type")),
                    stationId,
                    dimension,
                    BlockPos.of(entry.getLong("Position")),
                    entry.getString("DisplayName"),
                    MetroNodeState.byName(entry.getString("State"))
            );
            data.nodes.put(node.nodeId(), node);
        }

        ListTag routeList = tag.getList("Routes", Tag.TAG_COMPOUND);
        for (int i = 0; i < routeList.size(); i++) {
            CompoundTag entry = routeList.getCompound(i);
            if (!entry.hasUUID("RouteId")) continue;
            String nodeA = entry.getString("NodeA");
            String nodeB = entry.getString("NodeB");
            if (nodeA.isBlank() || nodeB.isBlank()) continue;

            MetroRoute route = new MetroRoute(
                    entry.getUUID("RouteId"),
                    nodeA,
                    nodeB,
                    Math.max(0.0D, entry.getDouble("Distance")),
                    Math.max(1, entry.getInt("TravelTimeTicks")),
                    MetroRouteState.byName(entry.getString("State"))
            );
            if (data.nodes.containsKey(nodeA) && data.nodes.containsKey(nodeB)) {
                data.routes.put(route.routeId(), route);
            }
        }

        if (data.networkVersion < CURRENT_VERSION) {
            data.networkVersion = CURRENT_VERSION;
            data.setDirty();
        }
        return data;
    }

    public int networkVersion() {
        return networkVersion;
    }

    @Nullable
    public MetroNode node(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<MetroNode> nodes() {
        ArrayList<MetroNode> result = new ArrayList<>(nodes.values());
        result.sort(Comparator.comparing(MetroNode::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    public List<MetroRoute> routes() {
        ArrayList<MetroRoute> result = new ArrayList<>(routes.values());
        result.sort(Comparator.comparing(route -> route.routeId().toString()));
        return List.copyOf(result);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int routeCount() {
        return routes.size();
    }

    public boolean upsertNode(MetroNode node) {
        MetroNode previous = nodes.get(node.nodeId());
        if (node.equals(previous)) return false;
        nodes.put(node.nodeId(), node);
        setDirty();
        return true;
    }

    public boolean removeNode(String nodeId) {
        if (nodes.remove(nodeId) == null) return false;
        routes.entrySet().removeIf(entry -> {
            MetroRoute route = entry.getValue();
            return route.nodeA().equals(nodeId) || route.nodeB().equals(nodeId);
        });
        setDirty();
        return true;
    }

    @Nullable
    public MetroRoute routeBetween(String nodeA, String nodeB) {
        for (MetroRoute route : routes.values()) {
            if (route.connects(nodeA, nodeB)) return route;
        }
        return null;
    }

    public RouteUpdate ensureRoute(String nodeA, String nodeB, double distance, int travelTimeTicks) {
        if (nodeA.equals(nodeB) || !nodes.containsKey(nodeA) || !nodes.containsKey(nodeB)) {
            return new RouteUpdate(null, false);
        }

        MetroRoute existing = routeBetween(nodeA, nodeB);
        if (existing != null) {
            MetroRoute updated = existing.withMetrics(distance, travelTimeTicks);
            if (!updated.equals(existing)) {
                routes.put(existing.routeId(), updated);
                setDirty();
            }
            return new RouteUpdate(updated, false);
        }

        MetroRoute created = new MetroRoute(
                UUID.randomUUID(),
                nodeA,
                nodeB,
                distance,
                travelTimeTicks,
                MetroRouteState.ACTIVE
        );
        routes.put(created.routeId(), created);
        setDirty();
        return new RouteUpdate(created, true);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NetworkVersion", CURRENT_VERSION);

        ListTag nodeList = new ListTag();
        for (MetroNode node : nodes.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("NodeId", node.nodeId());
            entry.putString("Type", node.type().name());
            if (node.stationId() != null) entry.putUUID("StationId", node.stationId());
            entry.putString("Dimension", node.dimension());
            entry.putLong("Position", node.position().asLong());
            entry.putString("DisplayName", node.displayName());
            entry.putString("State", node.state().name());
            nodeList.add(entry);
        }
        tag.put("Nodes", nodeList);

        ListTag routeList = new ListTag();
        for (MetroRoute route : routes.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("RouteId", route.routeId());
            entry.putString("NodeA", route.nodeA());
            entry.putString("NodeB", route.nodeB());
            entry.putDouble("Distance", route.distance());
            entry.putInt("TravelTimeTicks", route.travelTimeTicks());
            entry.putString("State", route.state().name());
            routeList.add(entry);
        }
        tag.put("Routes", routeList);
        return tag;
    }

    public record RouteUpdate(@Nullable MetroRoute route, boolean created) {
    }
}
