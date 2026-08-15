package com.wasted.domesurvival.forge.client.itempipe;

import com.wasted.domesurvival.forge.itempipe.FilterRoute;
import com.wasted.domesurvival.forge.itempipe.FilteringItemPipeMenu;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.EnumMap;
import java.util.Map;

public final class FilteringItemPipeScreen extends AbstractContainerScreen<FilteringItemPipeMenu> {
    private static final int ROUTE_X = 142;
    private static final int ROUTE_Y = 42;
    private static final int ROUTE_W = 69;
    private static final int ROUTE_H = 18;
    private static final int ROUTE_GAP_X = 74;
    private static final int ROUTE_GAP_Y = 23;

    private final Map<FilterRoute, Rect> routeRects = new EnumMap<>(FilterRoute.class);
    private int selectedFilter;

    public FilteringItemPipeScreen(FilteringItemPipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 262;
        titleLabelX = 12;
        titleLabelY = 9;
        inventoryLabelX = FilteringItemPipeMenu.PLAYER_INV_X;
        inventoryLabelY = 160;
    }

    @Override
    protected void init() {
        super.init();
        routeRects.clear();
        putRoute(FilterRoute.NORTH, 0, 0);
        putRoute(FilterRoute.SOUTH, 1, 0);
        putRoute(FilterRoute.WEST, 0, 1);
        putRoute(FilterRoute.EAST, 1, 1);
        putRoute(FilterRoute.UP, 0, 2);
        putRoute(FilterRoute.DOWN, 1, 2);
        routeRects.put(FilterRoute.NONE, new Rect(ROUTE_X, ROUTE_Y + 69, 143, ROUTE_H));
    }

    private void putRoute(FilterRoute route, int column, int row) {
        routeRects.put(route, new Rect(
                ROUTE_X + column * ROUTE_GAP_X,
                ROUTE_Y + row * ROUTE_GAP_Y,
                ROUTE_W,
                ROUTE_H
        ));
    }

    private void sendSelectedRoute(FilterRoute route) {
        if (minecraft == null || minecraft.gameMode == null) return;
        int id = FilteringItemPipeMenu.ROUTE_BUTTON_BASE + selectedFilter * 8 + route.ordinal();
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF0B0F11);
        graphics.fill(left + 2, top + 2, left + imageWidth - 2, top + imageHeight - 2, 0xFF20282C);
        graphics.fill(left + 8, top + 28, left + 128, top + 137, 0xFF12181B);
        graphics.fill(left + 10, top + 30, left + 126, top + 135, 0xFF090D0F);

        for (int i = 0; i < ItemPipeBlockEntity.FILTER_SLOTS; i++) {
            Slot slot = menu.slots.get(i);
            int sx = left + slot.x - 1;
            int sy = top + slot.y - 1;
            int outer = i == selectedFilter ? 0xFFB8C0C4 : 0xFF050708;
            graphics.fill(sx - 1, sy - 1, sx + 19, sy + 21, outer);
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF323A3E);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0D1214);
            FilterRoute route = menu.route(i);
            graphics.fill(sx, sy + 18, sx + 18, sy + 21, route.argb());
        }

        graphics.fill(left + 135, top + 28, left + 292, top + 137, 0xFF12181B);
        graphics.fill(left + 137, top + 30, left + 290, top + 135, 0xFF090D0F);

        FilterRoute selectedRoute = menu.route(selectedFilter);
        boolean configured = !menu.filterStack(selectedFilter).isEmpty();
        for (Map.Entry<FilterRoute, Rect> entry : routeRects.entrySet()) {
            drawRouteButton(graphics, entry.getKey(), entry.getValue(),
                    configured && selectedRoute == entry.getKey(), configured);
        }

        graphics.fill(left + 8, top + 153, left + 180, top + 258, 0xFF12181B);
        graphics.fill(left + 10, top + 166, left + 178, top + 256, 0xFF090D0F);
        for (int i = ItemPipeBlockEntity.FILTER_SLOTS; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int sx = left + slot.x - 1;
            int sy = top + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF353D41);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0E1315);
        }
    }

    private void drawRouteButton(GuiGraphics graphics, FilterRoute route, Rect rect,
                                 boolean selected, boolean enabled) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;
        int border = selected ? route.argb() : 0xFF3A4449;
        int fill = enabled ? 0xFF20282C : 0xFF171C1F;
        graphics.fill(x, y, x + rect.w, y + rect.h, border);
        graphics.fill(x + 1, y + 1, x + rect.w - 1, y + rect.h - 1, fill);
        graphics.fill(x + 3, y + 3, x + 7, y + rect.h - 3, route.argb());

        Component label = Component.translatable("gui.domesurvival.item_pipe.color." + route.id());
        String text = font.plainSubstrByWidth(label.getString(), rect.w - 15);
        int color = enabled ? 0xFFD7DDDF : 0xFF667074;
        graphics.drawString(font, text, x + 11, y + 5, color, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String titleText = font.plainSubstrByWidth(title.getString(), imageWidth - 24);
        graphics.drawString(font, titleText, titleLabelX, titleLabelY, 0xFFE4E8EA, false);

        Component hint = Component.translatable("gui.domesurvival.item_pipe.filter_hint_compact");
        String hintText = font.plainSubstrByWidth(hint.getString(), 112);
        graphics.drawString(font, hintText, 12, 20, 0xFF8C979B, false);

        graphics.drawString(font,
                Component.translatable("container.inventory"),
                inventoryLabelX, inventoryLabelY, 0xFFBCC5C8, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!menu.filterStack(selectedFilter).isEmpty()) {
            for (Map.Entry<FilterRoute, Rect> entry : routeRects.entrySet()) {
                Rect rect = entry.getValue();
                double x = leftPos + rect.x;
                double y = topPos + rect.y;
                if (mouseX >= x && mouseX < x + rect.w && mouseY >= y && mouseY < y + rect.h) {
                    sendSelectedRoute(entry.getKey());
                    return true;
                }
            }
        }

        for (int i = 0; i < ItemPipeBlockEntity.FILTER_SLOTS; i++) {
            Slot slot = menu.slots.get(i);
            double sx = leftPos + slot.x - 1;
            double sy = topPos + slot.y - 1;
            if (mouseX >= sx && mouseX < sx + 19 && mouseY >= sy && mouseY < sy + 21) {
                selectedFilter = i;
                break;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private record Rect(int x, int y, int w, int h) { }
}
