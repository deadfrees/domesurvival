package com.wasted.domesurvival.forge.client.metro;

import com.wasted.domesurvival.forge.metro.network.MetroNetworkService;
import com.wasted.domesurvival.forge.metro.network.MetroNodeClientData;
import com.wasted.domesurvival.forge.metro.network.MetroNodeState;
import com.wasted.domesurvival.forge.metro.network.MetroNodeType;
import com.wasted.domesurvival.forge.metro.network.MetroRouteClientData;
import com.wasted.domesurvival.forge.metro.network.MetroRouteState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

public final class MetroMapRenderer {
    public static final int NODE_RADIUS = 7;

    private MetroMapRenderer() {
    }

    public static void render(GuiGraphics graphics,
                              int x,
                              int y,
                              int width,
                              int height,
                              List<MetroNodeClientData> nodes,
                              List<MetroRouteClientData> routes,
                              Map<String, MetroMapLayout.Point> layout,
                              double zoom,
                              double panX,
                              double panY,
                              String selectedNodeId,
                              String hoveredNodeId) {
        graphics.fill(x, y, x + width, y + height, 0xEE0B1014);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF32404A);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF32404A);

        int centerX = x + width / 2;
        int centerY = y + height / 2;

        for (MetroRouteClientData route : routes) {
            MetroMapLayout.Point a = layout.get(route.nodeA());
            MetroMapLayout.Point b = layout.get(route.nodeB());
            if (a == null || b == null) continue;

            int ax = screenX(centerX, a.x(), zoom, panX);
            int ay = screenY(centerY, a.y(), zoom, panY);
            int bx = screenX(centerX, b.x(), zoom, panX);
            int by = screenY(centerY, b.y(), zoom, panY);

            int color = routeColor(route.state());
            drawLine(graphics, ax, ay, bx, by, color);
        }

        Minecraft minecraft = Minecraft.getInstance();
        for (MetroNodeClientData node : nodes) {
            MetroMapLayout.Point point = layout.get(node.nodeId());
            if (point == null) continue;

            int sx = screenX(centerX, point.x(), zoom, panX);
            int sy = screenY(centerY, point.y(), zoom, panY);
            if (sx < x - 12 || sx > x + width + 12 || sy < y - 12 || sy > y + height + 12) continue;

            boolean selected = node.nodeId().equals(selectedNodeId);
            boolean hovered = node.nodeId().equals(hoveredNodeId);
            int radius = selected ? NODE_RADIUS + 3 : hovered ? NODE_RADIUS + 2 : NODE_RADIUS;
            int color = nodeColor(node);

            graphics.fill(sx - radius, sy - radius, sx + radius + 1, sy + radius + 1, color);
            graphics.fill(sx - radius + 2, sy - radius + 2, sx + radius - 1, sy + radius - 1, 0xFF10171C);

            String shortName = abbreviate(node.displayName(), 18);
            int textWidth = minecraft.font.width(shortName);
            graphics.drawString(
                    minecraft.font,
                    shortName,
                    sx - textWidth / 2,
                    sy + radius + 4,
                    node.state() == MetroNodeState.ACTIVE ? 0xFFE8EDF0 : 0xFF8D969C,
                    false
            );
        }
    }

    public static String hoveredNode(int mouseX,
                                     int mouseY,
                                     int x,
                                     int y,
                                     int width,
                                     int height,
                                     Map<String, MetroMapLayout.Point> layout,
                                     double zoom,
                                     double panX,
                                     double panY) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;

        for (Map.Entry<String, MetroMapLayout.Point> entry : layout.entrySet()) {
            int sx = screenX(centerX, entry.getValue().x(), zoom, panX);
            int sy = screenY(centerY, entry.getValue().y(), zoom, panY);
            double dx = mouseX - sx;
            double dy = mouseY - sy;
            if (dx * dx + dy * dy <= (NODE_RADIUS + 5) * (NODE_RADIUS + 5)) {
                return entry.getKey();
            }
        }
        return "";
    }

    public static void renderTooltip(GuiGraphics graphics,
                                     MetroNodeClientData node,
                                     int mouseX,
                                     int mouseY) {
        if (node == null) return;
        graphics.renderTooltip(
                Minecraft.getInstance().font,
                List.of(
                        Component.literal(node.displayName()),
                        Component.literal(node.type().name() + " | " + node.state().name()),
                        Component.literal(node.dimension()),
                        Component.literal("X " + node.x() + "  Y " + node.y() + "  Z " + node.z())
                ),
                java.util.Optional.empty(),
                mouseX,
                mouseY
        );
    }

    private static int screenX(int centerX, double x, double zoom, double panX) {
        return centerX + (int) Math.round((x + panX) * zoom);
    }

    private static int screenY(int centerY, double y, double zoom, double panY) {
        return centerY + (int) Math.round((y + panY) * zoom);
    }

    private static int nodeColor(MetroNodeClientData node) {
        if (node.state() != MetroNodeState.ACTIVE) return 0xFF69747B;
        if (node.type() == MetroNodeType.DOME
                || MetroNetworkService.DOME_NODE_ID.equals(node.nodeId())) {
            return 0xFF50D6E8;
        }
        return 0xFF75D46B;
    }

    private static int routeColor(MetroRouteState state) {
        return switch (state) {
            case ACTIVE -> 0xFF4FA9C2;
            case DAMAGED -> 0xFFC8A14A;
            case DISABLED -> 0xFF626B70;
        };
    }

    private static String abbreviate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(1, max - 1)) + "\u2026";
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }
}
