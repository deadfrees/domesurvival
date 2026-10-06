package com.wasted.domesurvival.forge.client.metro;

import com.wasted.domesurvival.forge.metro.network.MetroNetworkService;
import com.wasted.domesurvival.forge.metro.network.MetroNodeClientData;
import com.wasted.domesurvival.forge.metro.network.MetroNodeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MetroMapLayout {
    private MetroMapLayout() {
    }

    public static Map<String, Point> radial(List<MetroNodeClientData> nodes) {
        LinkedHashMap<String, Point> result = new LinkedHashMap<>();

        MetroNodeClientData dome = null;
        ArrayList<MetroNodeClientData> stations = new ArrayList<>();
        for (MetroNodeClientData node : nodes) {
            if (node.type() == MetroNodeType.DOME
                    || MetroNetworkService.DOME_NODE_ID.equals(node.nodeId())) {
                dome = node;
            } else {
                stations.add(node);
            }
        }

        stations.sort(Comparator
                .comparing(MetroNodeClientData::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MetroNodeClientData::nodeId));

        if (dome != null) {
            result.put(dome.nodeId(), new Point(0.0D, 0.0D));
        }

        int count = stations.size();
        if (count == 0) return result;

        if (count == 1) {
            result.put(stations.get(0).nodeId(), new Point(92.0D, 0.0D));
            return result;
        }

        double radius = Math.max(78.0D, Math.min(138.0D, 54.0D + count * 10.0D));
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2.0D + (Math.PI * 2.0D * i / count);
            result.put(
                    stations.get(i).nodeId(),
                    new Point(Math.cos(angle) * radius, Math.sin(angle) * radius)
            );
        }

        return result;
    }

    public record Point(double x, double y) {
    }
}
