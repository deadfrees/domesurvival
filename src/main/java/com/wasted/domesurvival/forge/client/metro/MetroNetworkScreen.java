package com.wasted.domesurvival.forge.client.metro;

import com.wasted.domesurvival.forge.metro.network.MetroNetworkMenu;
import com.wasted.domesurvival.forge.metro.network.MetroNodeClientData;
import com.wasted.domesurvival.forge.metro.network.MetroRouteClientData;
import com.wasted.domesurvival.forge.network.MetroTravelRequestPacket;
import com.wasted.domesurvival.forge.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.HashMap;
import java.util.Map;

public final class MetroNetworkScreen extends AbstractContainerScreen<MetroNetworkMenu> {
    private static final int MAP_X = 12;
    private static final int MAP_Y = 30;
    private static final int MAP_W = 360;
    private static final int MAP_H = 160;

    private final Map<String, MetroMapLayout.Point> layout;
    private final Map<String, MetroNodeClientData> nodeById = new HashMap<>();

    private String selectedNodeId = "";
    private String hoveredNodeId = "";
    private double zoom = 1.0D;
    private double panX;
    private double panY;
    private boolean dragging;
    private Button travelButton;

    public MetroNetworkScreen(MetroNetworkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 384;
        this.imageHeight = 270;
        this.layout = MetroMapLayout.radial(menu.nodes());
        for (MetroNodeClientData node : menu.nodes()) nodeById.put(node.nodeId(), node);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(
                Component.literal("\u0421\u0431\u0440\u043e\u0441 \u0432\u0438\u0434\u0430"),
                button -> {
                    zoom = 1.0D;
                    panX = 0.0D;
                    panY = 0.0D;
                }
        ).bounds(leftPos + 12, topPos + 238, 92, 20).build());

        travelButton = addRenderableWidget(Button.builder(
                Component.literal("\u25b6 \u041f\u041e\u0415\u0425\u0410\u0422\u042c"),
                button -> requestTravel()
        ).bounds(leftPos + 130, topPos + 238, 124, 20).build());
        travelButton.active = false;

        addRenderableWidget(Button.builder(
                Component.literal("\u0417\u0430\u043a\u0440\u044b\u0442\u044c"),
                button -> onClose()
        ).bounds(leftPos + imageWidth - 82, topPos + 238, 70, 20).build());
    }

    private void requestTravel() {
        if (selectedNodeId.isBlank()) return;
        if (travelButton != null) travelButton.active = false;

        // Per spec the client sends only destinationNodeId. Current station,
        // route, station state and active-session state are all verified server-side.
        ModNetwork.CHANNEL.sendToServer(new MetroTravelRequestPacket(selectedNodeId));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int mapX = leftPos + MAP_X;
        int mapY = topPos + MAP_Y;
        if (mouseX >= mapX && mouseX < mapX + MAP_W && mouseY >= mapY && mouseY < mapY + MAP_H) {
            hoveredNodeId = MetroMapRenderer.hoveredNode(
                    mouseX, mouseY,
                    mapX, mapY, MAP_W, MAP_H,
                    layout, zoom, panX, panY
            );
        } else {
            hoveredNodeId = "";
        }

        MetroMapRenderer.render(
                graphics,
                mapX,
                mapY,
                MAP_W,
                MAP_H,
                menu.nodes(),
                menu.routes(),
                layout,
                zoom,
                panX,
                panY,
                selectedNodeId,
                hoveredNodeId
        );

        renderSelection(graphics);

        MetroNodeClientData hovered = nodeById.get(hoveredNodeId);
        if (hovered != null) {
            MetroMapRenderer.renderTooltip(graphics, hovered, mouseX, mouseY);
        }

        if (travelButton != null && travelButton.visible) {
            travelButton.active = !selectedNodeId.isBlank();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF20A0E11);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 26, 0xFF151F25);
        graphics.fill(leftPos + 1, topPos + 26, leftPos + imageWidth - 1, topPos + 27, 0xFF35515D);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                "\u0421\u0415\u0422\u042c \u041c\u0415\u0422\u0420\u041e \u00b7 DOMESURVIVAL",
                12,
                9,
                0xFFBFEAF2,
                false
        );

        String status = "\u0423\u0437\u043b\u043e\u0432: " + menu.nodes().size()
                + "  |  \u041c\u0430\u0440\u0448\u0440\u0443\u0442\u043e\u0432: " + menu.routes().size()
                + "  |  \u041c\u0430\u0441\u0448\u0442\u0430\u0431: " + (int) Math.round(zoom * 100.0D) + "%";
        graphics.drawString(font, status, 12, 198, 0xFF9BAAB2, false);
    }

    private void renderSelection(GuiGraphics graphics) {
        MetroNodeClientData selected = nodeById.get(selectedNodeId);
        if (selected == null) {
            graphics.drawString(
                    font,
                    "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u0442\u0430\u043d\u0446\u0438\u044e, \u0437\u0430\u0442\u0435\u043c \u043d\u0430\u0436\u043c\u0438\u0442\u0435 \u00ab\u041f\u041e\u0415\u0425\u0410\u0422\u042c\u00bb.",
                    leftPos + 12,
                    topPos + 214,
                    0xFFB7C0C5,
                    false
            );
            return;
        }

        String line = selected.displayName();
        MetroRouteClientData route = routeFromDome(selected.nodeId());
        if (route != null) {
            line += "  |  " + Math.round(route.distance()) + " \u0431\u043b."
                    + "  |  " + Math.max(1, route.travelTimeTicks() / 20) + " \u0441"
                    + "  |  " + route.state().name();
        } else {
            line += "  |  " + selected.state().name();
        }

        graphics.drawString(
                font,
                abbreviate(line, 58),
                leftPos + 12,
                topPos + 214,
                0xFFE5EBEE,
                false
        );
    }

    private MetroRouteClientData routeFromDome(String nodeId) {
        for (MetroRouteClientData route : menu.routes()) {
            if (route.connects("domesurvival:dome_central", nodeId)) return route;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mapX = leftPos + MAP_X;
            int mapY = topPos + MAP_Y;
            if (mouseX >= mapX && mouseX < mapX + MAP_W && mouseY >= mapY && mouseY < mapY + MAP_H) {
                String hovered = MetroMapRenderer.hoveredNode(
                        (int) mouseX, (int) mouseY,
                        mapX, mapY, MAP_W, MAP_H,
                        layout, zoom, panX, panY
                );
                if (!hovered.isBlank()) {
                    selectedNodeId = hovered;
                    if (travelButton != null) travelButton.active = true;
                    return true;
                }
                dragging = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX,
                                double mouseY,
                                int button,
                                double dragX,
                                double dragY) {
        if (dragging && button == 0) {
            panX += dragX / zoom;
            panY += dragY / zoom;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int mapX = leftPos + MAP_X;
        int mapY = topPos + MAP_Y;
        if (mouseX >= mapX && mouseX < mapX + MAP_W && mouseY >= mapY && mouseY < mapY + MAP_H) {
            zoom = Math.max(0.65D, Math.min(2.20D, zoom + delta * 0.10D));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static String abbreviate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(1, max - 1)) + "\u2026";
    }
}
