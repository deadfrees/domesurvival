package com.wasted.domesurvival.forge.metro.network;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.metro.MetroStationRecord;
import com.wasted.domesurvival.forge.metro.MetroStationSavedData;
import com.wasted.domesurvival.forge.metro.MetroRegistry;
import com.wasted.domesurvival.forge.metro.StationState;
import com.wasted.domesurvival.forge.metro.dome.DomeMetroSavedData;
import com.wasted.domesurvival.forge.metro.dome.DomeMetroService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/** Stage-5 event-driven network registration and on-demand route calculation. */
public final class MetroNetworkService {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DOME_NODE_ID = "domesurvival:dome_central";
    private static final double TRAIN_SPEED_BLOCKS_PER_SECOND = 8.0D;

    private MetroNetworkService() {
    }

    /**
     * Startup/dev self-heal. This scans only SavedData records, never world blocks
     * or chunks, and is never called every tick.
     */
    public static SyncResult synchronize(MinecraftServer server) {
        MetroNetworkSavedData network = MetroNetworkSavedData.get(server);
        int nodesBefore = network.nodeCount();
        int routesBefore = network.routeCount();

        ensureDomeNode(server);

        Set<String> expectedStationNodes = new HashSet<>();
        for (MetroStationRecord station : MetroStationSavedData.get(server).all()) {
            if (station.state() != StationState.RESTORED || !station.restored()) continue;
            String nodeId = stationNodeId(station.stationId());
            expectedStationNodes.add(nodeId);
            registerRestoredStation(server, station);
        }

        for (MetroNode node : new ArrayList<>(network.nodes())) {
            if (node.type() == MetroNodeType.RESTORED_STATION && !expectedStationNodes.contains(node.nodeId())) {
                if (network.removeNode(node.nodeId())) {
                    LOGGER.info("Metro network node removed because station is no longer RESTORED: {}", node.nodeId());
                }
            }
        }

        return new SyncResult(
                network.nodeCount() - nodesBefore,
                network.routeCount() - routesBefore,
                network.nodeCount(),
                network.routeCount()
        );
    }

    public static boolean ensureDomeNode(MinecraftServer server) {
        DomeMetroSavedData dome = DomeMetroSavedData.get(server);
        if (!dome.isBuilt() || dome.stationAnchor() == null) return false;

        repairCentralTrainConsole(server, dome);

        MetroNode node = new MetroNode(
                DOME_NODE_ID,
                MetroNodeType.DOME,
                null,
                dome.dimension(),
                dome.stationAnchor(),
                "\u0426\u0435\u043d\u0442\u0440\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0442\u0430\u043d\u0446\u0438\u044f \u041a\u0443\u043f\u043e\u043b\u0430",
                MetroNodeState.ACTIVE
        );

        MetroNetworkSavedData network = MetroNetworkSavedData.get(server);
        boolean changed = network.upsertNode(node);
        if (changed) {
            LOGGER.info("Metro network node registered: {} @ {} {}", node.nodeId(), node.dimension(), node.position().toShortString());
        }
        return changed;
    }

    /**
     * Central-console self-heal.
     *
     * Older Stage-4 worlds saved the console position inside the train. That
     * block can be difficult to see/reach depending on the exact train shell,
     * so Stage 7.1 keeps the original saved console AND installs one explicit
     * passenger console on the open near-side platform.
     *
     * Both positions are deterministic from DomeMetroSavedData. No world scan,
     * station scan or chunk forcing is used.
     */
    public static boolean repairCentralTrainConsole(MinecraftServer server, DomeMetroSavedData dome) {
        if (!dome.isBuilt() || dome.stationAnchor() == null) return false;

        ServerLevel level = server.overworld();
        if (!dome.dimension().equals(level.dimension().location().toString())) return false;

        Block consoleBlock = MetroRegistry.METRO_TRAIN_CONSOLE.get();
        boolean changed = false;

        if (dome.trainConsolePos() != null
                && !level.getBlockState(dome.trainConsolePos()).is(consoleBlock)) {
            level.setBlockAndUpdate(
                    dome.trainConsolePos(),
                    consoleBlock.defaultBlockState()
            );
            LOGGER.info(
                    "Central Dome metro saved train console restored at {}",
                    dome.trainConsolePos().toShortString()
            );
            changed = true;
        }

        BlockPos platformConsole = centralPlatformConsolePos(dome);
        if (platformConsole != null
                && !level.getBlockState(platformConsole).is(consoleBlock)) {
            level.setBlockAndUpdate(
                    platformConsole,
                    consoleBlock.defaultBlockState()
            );
            LOGGER.info(
                    "Central Dome metro platform console installed at {}",
                    platformConsole.toShortString()
            );
            changed = true;
        }

        return changed;
    }

    /**
     * Authored Stage-4 central hall:
     * local R=+6, F=-2, Y=+2 is open walking space on the near platform,
     * outside the train body and away from structural pillars.
     */
    @Nullable
    public static BlockPos centralPlatformConsolePos(DomeMetroSavedData dome) {
        if (!dome.isBuilt() || dome.stationAnchor() == null) return null;

        Direction forward = dome.forward();
        if (!forward.getAxis().isHorizontal()) forward = Direction.WEST;
        Direction right = forward.getClockWise();

        return dome.stationAnchor()
                .relative(right, 6)
                .relative(forward.getOpposite(), 2)
                .above(2);
    }

    public static boolean registerRestoredStation(MinecraftServer server, MetroStationRecord station) {
        if (station == null || station.state() != StationState.RESTORED || !station.restored()) return false;

        MetroNetworkSavedData network = MetroNetworkSavedData.get(server);
        String nodeId = stationNodeId(station.stationId());
        MetroNode node = new MetroNode(
                nodeId,
                MetroNodeType.RESTORED_STATION,
                station.stationId(),
                station.dimension(),
                station.anchor(),
                station.stationName(),
                MetroNodeState.ACTIVE
        );

        boolean changed = network.upsertNode(node);
        if (changed) {
            LOGGER.info("Metro station registered in network: {} {} @ {} {}",
                    station.stationName(), nodeId, station.dimension(), station.anchor().toShortString());
        }

        ensureDomeNode(server);
        MetroNode domeNode = network.node(DOME_NODE_ID);
        if (domeNode != null && domeNode.state() == MetroNodeState.ACTIVE
                && node.state() == MetroNodeState.ACTIVE
                && domeNode.dimension().equals(node.dimension())) {
            double distance = horizontalDistance(domeNode, node);
            int travelTime = travelTimeTicks(distance);
            MetroNetworkSavedData.RouteUpdate update = network.ensureRoute(
                    domeNode.nodeId(),
                    node.nodeId(),
                    distance,
                    travelTime
            );
            if (update.created() && update.route() != null) {
                LOGGER.info("Metro route created: {} <-> {} distance={} travelTimeTicks={}",
                        domeNode.nodeId(), node.nodeId(), Math.round(distance), travelTime);
            }
        }

        return changed;
    }

    public static void onStationRestored(MinecraftServer server, MetroStationRecord station) {
        registerRestoredStation(server, station);
    }

    public static void onStationReset(MinecraftServer server, UUID stationId) {
        String nodeId = stationNodeId(stationId);
        if (MetroNetworkSavedData.get(server).removeNode(nodeId)) {
            LOGGER.info("Metro station removed from network after reset: {}", nodeId);
        }
    }

    public static String stationNodeId(UUID stationId) {
        return "domesurvival:station/" + stationId.toString().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static MetroNode resolveNode(MinecraftServer server, String token) {
        if (token == null || token.isBlank()) return null;
        MetroNetworkSavedData network = MetroNetworkSavedData.get(server);
        String value = token.trim();

        if ("dome".equalsIgnoreCase(value)
                || "dome_central".equalsIgnoreCase(value)
                || DOME_NODE_ID.equalsIgnoreCase(value)) {
            return network.node(DOME_NODE_ID);
        }

        MetroNode direct = network.node(value);
        if (direct != null) return direct;

        try {
            MetroNode byUuid = network.node(stationNodeId(UUID.fromString(value)));
            if (byUuid != null) return byUuid;
        } catch (IllegalArgumentException ignored) {
        }

        for (MetroNode node : network.nodes()) {
            if (node.displayName().equalsIgnoreCase(value)) return node;
        }
        return null;
    }

    @Nullable
    public static PathResult findPath(MinecraftServer server, String fromId, String toId) {
        MetroNetworkSavedData data = MetroNetworkSavedData.get(server);
        MetroNode from = data.node(fromId);
        MetroNode to = data.node(toId);
        if (from == null || to == null
                || from.state() != MetroNodeState.ACTIVE
                || to.state() != MetroNodeState.ACTIVE) return null;

        if (from.nodeId().equals(to.nodeId())) {
            return new PathResult(List.of(from), List.of(), 0.0D, 0);
        }

        Map<String, Integer> cost = new HashMap<>();
        Map<String, String> previousNode = new HashMap<>();
        Map<String, UUID> previousRoute = new HashMap<>();
        Map<UUID, MetroRoute> routeById = new HashMap<>();
        for (MetroRoute route : data.routes()) routeById.put(route.routeId(), route);

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator.comparingInt(QueueEntry::cost));
        cost.put(from.nodeId(), 0);
        queue.add(new QueueEntry(from.nodeId(), 0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (current.cost() != cost.getOrDefault(current.nodeId(), Integer.MAX_VALUE)) continue;
            if (current.nodeId().equals(to.nodeId())) break;

            for (MetroRoute route : data.routes()) {
                if (route.state() != MetroRouteState.ACTIVE) continue;
                String nextId = route.other(current.nodeId());
                if (nextId.isBlank()) continue;

                MetroNode next = data.node(nextId);
                if (next == null || next.state() != MetroNodeState.ACTIVE) continue;

                int nextCost = current.cost() + route.travelTimeTicks();
                if (nextCost < cost.getOrDefault(nextId, Integer.MAX_VALUE)) {
                    cost.put(nextId, nextCost);
                    previousNode.put(nextId, current.nodeId());
                    previousRoute.put(nextId, route.routeId());
                    queue.add(new QueueEntry(nextId, nextCost));
                }
            }
        }

        if (!cost.containsKey(to.nodeId())) return null;

        ArrayList<MetroNode> reversedNodes = new ArrayList<>();
        ArrayList<MetroRoute> reversedRoutes = new ArrayList<>();
        String cursor = to.nodeId();
        reversedNodes.add(to);

        while (!cursor.equals(from.nodeId())) {
            UUID routeId = previousRoute.get(cursor);
            String previous = previousNode.get(cursor);
            if (routeId == null || previous == null) return null;

            MetroRoute route = routeById.get(routeId);
            MetroNode node = data.node(previous);
            if (route == null || node == null) return null;

            reversedRoutes.add(route);
            reversedNodes.add(node);
            cursor = previous;
        }

        java.util.Collections.reverse(reversedNodes);
        java.util.Collections.reverse(reversedRoutes);

        double distance = 0.0D;
        for (MetroRoute route : reversedRoutes) distance += route.distance();

        return new PathResult(
                List.copyOf(reversedNodes),
                List.copyOf(reversedRoutes),
                distance,
                cost.get(to.nodeId())
        );
    }

    private static double horizontalDistance(MetroNode first, MetroNode second) {
        long dx = (long) first.position().getX() - second.position().getX();
        long dz = (long) first.position().getZ() - second.position().getZ();
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    private static int travelTimeTicks(double distance) {
        double seconds = Math.max(5.0D, distance / TRAIN_SPEED_BLOCKS_PER_SECOND);
        return Math.max(100, Math.min(2400, (int) Math.ceil(seconds * 20.0D)));
    }

    private record QueueEntry(String nodeId, int cost) {
    }

    public record PathResult(
            List<MetroNode> nodes,
            List<MetroRoute> routes,
            double distance,
            int travelTimeTicks
    ) {
    }

    public record SyncResult(int nodeDelta, int routeDelta, int nodeCount, int routeCount) {
    }
}
