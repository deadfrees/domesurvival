package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexMenu;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * V64.2D production GUI for the Oxygen Reclamation Station.
 *
 * Layout rules:
 * - the four process cards never overlap the inventory;
 * - the air-filter slot is a real menu Slot inside the filtration card;
 * - the player inventory uses the exact menu slot coordinates;
 * - the connector panel occupies the lower-right service area;
 * - all visible UI text is Russian.
 */
public final class OxygenComplexScreen extends AbstractContainerScreen<OxygenComplexMenu> {
    private static final String GUI_REVISION = "V64.2K.1";
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 326;

    private static final int HEADER_X = 6;
    private static final int HEADER_Y = 5;
    private static final int HEADER_W = 348;
    private static final int HEADER_H = 25;

    private static final int CARD_Y = 35;
    private static final int CARD_W = 78;
    private static final int CARD_H = 104;
    private static final int[] CARD_X = {10, 97, 184, 271};

    private static final int ENERGY_X = 10;
    private static final int ENERGY_Y = 145;
    private static final int ENERGY_W = 165;
    private static final int ENERGY_H = 18;

    private static final int OXYGEN_X = 185;
    private static final int OXYGEN_Y = 145;
    private static final int OXYGEN_W = 165;
    private static final int OXYGEN_H = 18;

    private static final int DIAG_X = 10;
    private static final int DIAG_Y = 169;
    private static final int DIAG_W = 340;
    private static final int DIAG_H = 25;

    private static final int INVENTORY_PANEL_X = 8;
    private static final int INVENTORY_PANEL_Y = 202;
    private static final int INVENTORY_PANEL_W = 208;
    private static final int INVENTORY_PANEL_H = 116;

    private static final int SIDE_PANEL_X = 220;
    private static final int SIDE_PANEL_Y = 202;
    private static final int SIDE_PANEL_W = 132;
    private static final int SIDE_PANEL_H = 116;

    private static final int GEAR_X = 334;
    private static final int GEAR_Y = 9;
    private static final int GEAR_W = 16;
    private static final int GEAR_H = 15;

    private static final int FRAME_DARK = 0xFF090C0E;
    private static final int FRAME = 0xFF4D565B;
    private static final int PANEL = 0xFF2A3035;
    private static final int PANEL_INSET = 0xFF191E22;
    private static final int PANEL_MID = 0xFF242A2F;
    private static final int SLOT_PANEL = 0xFF161B1E;
    private static final int SLOT_INNER = 0xFF0E1215;

    private static final int TEXT = 0xFFE4E8EA;
    private static final int TEXT_DIM = 0xFFAFB9BD;
    private static final int TEXT_DARK = 0xFF879196;

    private static final int CYAN = 0xFF5BAEC1;
    private static final int CYAN_BRIGHT = 0xFF8FD5E2;
    private static final int GREEN = 0xFF73A04C;
    private static final int GREEN_BRIGHT = 0xFFA2CB72;
    private static final int AMBER = 0xFFA88142;
    private static final int AMBER_BRIGHT = 0xFFD1AC66;
    private static final int OXYGEN = 0xFF8E9EA5;
    private static final int OXYGEN_BRIGHT = 0xFFD0D9DD;
    private static final int INPUT_BLUE = 0xFF3F87B8;
    private static final int OUTPUT_ORANGE = 0xFFC27A34;
    private static final int DISABLED = 0xFF3C4448;
    private static final int ERROR = 0xFFC76659;
    private static final int WARN = 0xFFE0B153;

    private boolean sidePanelOpen;

    public OxygenComplexScreen(OxygenComplexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_W, GEAR_H)) {
                sidePanelOpen = !sidePanelOpen;
                return true;
            }

            if (sidePanelOpen) {
                RelativeSide side = hoveredSide(mouseX, mouseY);
                if (side != null && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            OxygenComplexMenu.sideButtonId(side)
                    );
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);

        if (isFilterSlot(mouseX, mouseY)) {
            Slot filterSlot = menu.slots.get(OxygenComplexMenu.FILTER_SLOT_INDEX);
            if (!filterSlot.hasItem()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("Воздушный картридж"));
                tooltip.add(Component.literal("Установите картридж для запуска фильтрации воздуха."));
                gg.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
            return;
        }

        renderProcessTooltips(gg, mouseX, mouseY);
        renderSideTooltips(gg, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawOuterPanel(gg, x, y, PANEL_W, PANEL_H);
        drawHeader(gg, x + HEADER_X, y + HEADER_Y, HEADER_W, HEADER_H);

        for (int i = 0; i < Stage.values().length; i++) {
            drawStageCard(gg, x + CARD_X[i], y + CARD_Y, Stage.values()[i]);
        }

        drawProcessConnector(gg, x + 88, y + 78, CYAN);
        drawProcessConnector(gg, x + 175, y + 78, GREEN);
        drawProcessConnector(gg, x + 262, y + 78, AMBER);

        drawBarBox(gg, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H);
        fillBar(
                gg,
                x + ENERGY_X,
                y + ENERGY_Y,
                ENERGY_W,
                ENERGY_H,
                menu.getEnergy(),
                menu.getEnergyCapacity(),
                AMBER,
                AMBER_BRIGHT
        );

        drawBarBox(gg, x + OXYGEN_X, y + OXYGEN_Y, OXYGEN_W, OXYGEN_H);
        fillBar(
                gg,
                x + OXYGEN_X,
                y + OXYGEN_Y,
                OXYGEN_W,
                OXYGEN_H,
                menu.getOxygen(),
                menu.getOxygenCapacity(),
                OXYGEN,
                OXYGEN_BRIGHT
        );

        drawFrameBox(gg, x + DIAG_X, y + DIAG_Y, DIAG_W, DIAG_H, PANEL_INSET);
        drawDiagLeds(gg, x + DIAG_X + 7, y + DIAG_Y + 8);

        drawFrameBox(
                gg,
                x + INVENTORY_PANEL_X,
                y + INVENTORY_PANEL_Y,
                INVENTORY_PANEL_W,
                INVENTORY_PANEL_H,
                SLOT_PANEL
        );

        drawFrameBox(
                gg,
                x + SIDE_PANEL_X,
                y + SIDE_PANEL_Y,
                SIDE_PANEL_W,
                SIDE_PANEL_H,
                PANEL_INSET
        );

        drawActualSlotFrames(gg, x, y);
        drawGearButton(gg, x + GEAR_X, y + GEAR_Y, sidePanelOpen);

        if (sidePanelOpen) {
            drawSidePanel(gg, x, y, mouseX, mouseY);
        } else {
            drawSidePanelClosed(gg, x, y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        drawClamped(
                gg,
                Component.literal("Кислородный комплекс"),
                HEADER_X + 8,
                HEADER_Y + 9,
                220,
                TEXT
        );

        drawCentered(gg, Component.literal("ЗАБОР ВОЗДУХА"), CARD_X[0] + 3, CARD_Y + 6, CARD_W - 6, CYAN_BRIGHT);
        drawCentered(gg, Component.literal("ФИЛЬТРАЦИЯ"), CARD_X[1] + 3, CARD_Y + 6, CARD_W - 6, GREEN_BRIGHT);
        drawCentered(gg, Component.literal("КОМПРЕССИЯ"), CARD_X[2] + 3, CARD_Y + 6, CARD_W - 6, AMBER_BRIGHT);
        drawCentered(gg, Component.literal("ВЫХОД O₂"), CARD_X[3] + 3, CARD_Y + 6, CARD_W - 6, OXYGEN_BRIGHT);

        drawCentered(
                gg,
                Component.literal(compact(menu.getCollectedAir()) + " / " + compact(menu.getCollectedAirCapacity())),
                CARD_X[0] + 4,
                CARD_Y + 91,
                CARD_W - 8,
                TEXT_DIM
        );
        drawCentered(
                gg,
                Component.literal(compact(menu.getCompressedFeed()) + " / " + compact(menu.getCompressedFeedCapacity())),
                CARD_X[2] + 4,
                CARD_Y + 91,
                CARD_W - 8,
                TEXT_DIM
        );
        drawCentered(
                gg,
                Component.literal(compact(menu.getOxygen()) + " / " + compact(menu.getOxygenCapacity())),
                CARD_X[3] + 4,
                CARD_Y + 91,
                CARD_W - 8,
                TEXT_DIM
        );

        drawCentered(
                gg,
                Component.literal(menu.getEnergy() + " / " + menu.getEnergyCapacity() + " FE"),
                ENERGY_X + 4,
                ENERGY_Y + 5,
                ENERGY_W - 8,
                TEXT
        );

        gg.drawString(font, Component.literal("O₂"), OXYGEN_X + 6, OXYGEN_Y + 5, OXYGEN_BRIGHT, false);
        drawRight(
                gg,
                Component.literal(menu.getOxygen() + " / " + menu.getOxygenCapacity() + " mB"),
                OXYGEN_X + OXYGEN_W - 5,
                OXYGEN_Y + 5,
                TEXT
        );

        drawCentered(
                gg,
                getStatusText(),
                DIAG_X + 42,
                DIAG_Y + 8,
                DIAG_W - 50,
                getStatusColor()
        );

        gg.drawString(
                font,
                Component.literal("ИНВЕНТАРЬ"),
                INVENTORY_PANEL_X + 8,
                INVENTORY_PANEL_Y + 10,
                TEXT_DIM,
                false
        );

        drawCentered(
                gg,
                Component.literal("КОННЕКТОРЫ"),
                SIDE_PANEL_X + 5,
                SIDE_PANEL_Y + 6,
                SIDE_PANEL_W - 10,
                TEXT
        );

        if (!sidePanelOpen) {
            drawCentered(
                    gg,
                    Component.literal("Нажмите шестерёнку"),
                    SIDE_PANEL_X + 5,
                    SIDE_PANEL_Y + 31,
                    SIDE_PANEL_W - 10,
                    TEXT_DIM
            );
            drawCentered(
                    gg,
                    Component.literal("для настройки сторон"),
                    SIDE_PANEL_X + 5,
                    SIDE_PANEL_Y + 43,
                    SIDE_PANEL_W - 10,
                    TEXT_DARK
            );
            drawConnectorLegendLabels(gg);
        }
    }

    private void drawHeader(GuiGraphics gg, int x, int y, int w, int h) {
        drawFrameBox(gg, x, y, w, h, PANEL_MID);
        int ledY = y + 8;
        drawLed(gg, x + w - 70, ledY, menu.isFormed() ? GREEN_BRIGHT : ERROR);
        drawLed(gg, x + w - 58, ledY, anyActive() ? CYAN_BRIGHT : DISABLED);
        drawLed(gg, x + w - 46, ledY, menu.getEnergy() > 0 ? AMBER_BRIGHT : ERROR);
        drawLed(gg, x + w - 34, ledY, menu.getOxygen() > 0 ? OXYGEN_BRIGHT : DISABLED);
    }

    private void drawStageCard(GuiGraphics gg, int x, int y, Stage stage) {
        drawFrameBox(gg, x, y, CARD_W, CARD_H, PANEL_INSET);
        gg.fill(x + 4, y + 19, x + CARD_W - 4, y + 60, 0xFF111619);
        drawBevel(gg, x + 4, y + 19, CARD_W - 8, 41);

        int accent = accent(stage, isStageActive(stage));
        switch (stage) {
            case INTAKE -> drawIntakeModule(gg, x, y, accent);
            case FILTRATION -> drawFilterModule(gg, x, y, accent);
            case COMPRESSION -> drawCompressionModule(gg, x, y, accent);
            case OUTPUT -> drawOutputModule(gg, x, y, accent);
        }

        if (stage == Stage.FILTRATION) {
            drawFilterWearProgress(gg, x + 8, y + 89);
        } else {
            drawStageProgress(gg, x + 8, y + 78, stage, accent);
        }
    }

    private void drawIntakeModule(GuiGraphics gg, int x, int y, int accent) {
        int lx = x + 13;
        int ty = y + 27;
        gg.fill(lx, ty, lx + 50, ty + 27, 0xFF22292E);
        gg.fill(lx + 2, ty + 2, lx + 48, ty + 25, 0xFF0D1114);

        for (int yy = ty + 5; yy <= ty + 19; yy += 4) {
            gg.fill(lx + 7, yy, lx + 38, yy + 1, 0xFF30383D);
        }

        int pulse = (animationTick() / 4) % 12;
        gg.fill(lx + 42, ty + 5, lx + 44, ty + 22, accent);
        gg.fill(lx + 6 + pulse, ty + 23, lx + 10 + pulse, ty + 24, accent);
        gg.fill(lx + 3, ty + 10, lx + 5, ty + 18, 0xFF5C686D);
    }

    private void drawFilterModule(GuiGraphics gg, int x, int y, int accent) {
        int ty = y + 25;
        for (int i = 0; i < 3; i++) {
            int cx = x + 12 + i * 18;
            gg.fill(cx, ty, cx + 11, ty + 27, 0xFF191F22);
            gg.fill(cx + 2, ty + 4, cx + 9, ty + 23, menu.hasAirFilter() ? accent : DISABLED);
            int bubble = (animationTick() + i * 7) % 14;
            if (menu.isFilterActive()) {
                gg.fill(cx + 5, ty + 19 - bubble / 2, cx + 6, ty + 20 - bubble / 2, GREEN_BRIGHT);
            }
            gg.fill(cx, ty, cx + 11, ty + 4, 0xFF727A7F);
            gg.fill(cx, ty + 23, cx + 11, ty + 27, 0xFF4C5459);
        }
    }

    private void drawCompressionModule(GuiGraphics gg, int x, int y, int accent) {
        int px = x + 12;
        int py = y + 29;
        for (int i = 0; i < 3; i++) {
            int bx = px + i * 9;
            gg.fill(bx, py, bx + 4, py + 23, 0xFF70787D);
            gg.fill(bx + 1, py + 2, bx + 2, py + 21, 0xFFC9CED1);
        }

        int gx = x + 44;
        int gy = y + 31;
        gg.fill(gx, gy, gx + 18, gy + 18, 0xFF101518);
        gg.fill(gx + 2, gy + 2, gx + 16, gy + 16, 0xFFC7C3B7);

        int needle = (animationTick() / 2) % 6;
        gg.fill(gx + 8, gy + 8, gx + 14, gy + 9, ERROR);
        gg.fill(
                gx + 12 - needle,
                gy + 5 + needle / 2,
                gx + 13 - needle,
                gy + 6 + needle / 2,
                ERROR
        );
        // No decorative orange strip here: it was intentionally removed.
    }

    private void drawOutputModule(GuiGraphics gg, int x, int y, int accent) {
        int tx = x + 13;
        int ty = y + 25;

        gg.fill(tx, ty, tx + 15, ty + 30, 0xFF181D20);
        gg.fill(tx + 4, ty + 3, tx + 11, ty + 26, accent);
        gg.fill(tx + 2, ty, tx + 13, ty + 4, 0xFF757C81);
        gg.fill(tx + 2, ty + 26, tx + 13, ty + 30, 0xFF4C5358);

        int sx = x + 36;
        int sy = y + 31;
        gg.fill(sx, sy, sx + 26, sy + 15, 0xFF0E1417);
        gg.fill(sx + 2, sy + 2, sx + 24, sy + 13, 0xFF10272D);
        gg.fill(sx + 4, sy + 4, sx + 9, sy + 10, accent);
        gg.fill(sx + 12, sy + 4, sx + 18, sy + 6, accent);
        gg.fill(sx + 12, sy + 9, sx + 18, sy + 11, accent);

        if (menu.isOutputActive()) {
            int pulse = (animationTick() / 3) % 8;
            gg.fill(sx + 19 + pulse / 2, sy + 6, sx + 20 + pulse / 2, sy + 8, OXYGEN_BRIGHT);
        }
    }

    private void drawStageProgress(GuiGraphics gg, int x, int y, Stage stage, int accent) {
        gg.fill(x, y, x + 4, y + 4, isStageActive(stage) ? accent : DISABLED);
        gg.fill(x + 8, y + 1, x + 54, y + 3, 0xFF22292D);

        int fill = switch (stage) {
            case INTAKE -> scaled(menu.getCollectedAir(), menu.getCollectedAirCapacity(), 46);
            case FILTRATION -> scaled(menu.getFilteredAir(), menu.getFilteredAirCapacity(), 46);
            case COMPRESSION -> scaled(menu.getCompressedFeed(), menu.getCompressedFeedCapacity(), 46);
            case OUTPUT -> scaled(menu.getOxygen(), menu.getOxygenCapacity(), 46);
        };

        if (fill > 0) {
            gg.fill(x + 8, y + 1, x + 8 + fill, y + 3, accent);
        }
    }

    private void drawFilterWearProgress(GuiGraphics gg, int x, int y) {
        int max = menu.getAirFilterMaxDamage();
        int remaining = menu.getAirFilterRemaining();

        gg.fill(x, y, x + 4, y + 4, menu.hasAirFilter() ? GREEN_BRIGHT : DISABLED);
        gg.fill(x + 8, y + 1, x + 54, y + 3, 0xFF22292D);

        if (max <= 0 || remaining <= 0) {
            return;
        }

        int fill = scaled(remaining, max, 46);
        int color = remaining * 4 > max ? GREEN_BRIGHT
                : remaining * 2 > max ? AMBER_BRIGHT
                : ERROR;
        gg.fill(x + 8, y + 1, x + 8 + fill, y + 3, color);
    }

    private void drawProcessConnector(GuiGraphics gg, int x, int y, int accent) {
        gg.fill(x, y, x + 8, y + 3, 0xFF171C20);
        gg.fill(x + 1, y + 1, x + 7, y + 2, accent);
    }

    private void drawActualSlotFrames(GuiGraphics gg, int x, int y) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;

            int outer = i == OxygenComplexMenu.FILTER_SLOT_INDEX
                    ? (menu.hasAirFilter() ? GREEN : WARN)
                    : FRAME;

            gg.fill(sx, sy, sx + 18, sy + 18, FRAME_DARK);
            gg.fill(sx + 1, sy + 1, sx + 17, sy + 17, outer);
            gg.fill(sx + 2, sy + 2, sx + 16, sy + 16, SLOT_INNER);
        }
    }

    private void drawGearButton(GuiGraphics gg, int x, int y, boolean active) {
        drawFrameBox(gg, x, y, GEAR_W, GEAR_H, active ? 0xFF485158 : 0xFF2B3236);

        int cx = x + 8;
        int cy = y + 7;
        gg.fill(cx - 3, cy - 1, cx + 4, cy + 2, TEXT_DIM);
        gg.fill(cx - 1, cy - 3, cx + 2, cy + 4, TEXT_DIM);
        gg.fill(cx - 1, cy - 1, cx + 2, cy + 2, FRAME_DARK);
    }

    private void drawSidePanelClosed(GuiGraphics gg, int x, int y) {
        drawLed(gg, x + SIDE_PANEL_X + 23, y + SIDE_PANEL_Y + 66, INPUT_BLUE);
        drawLed(gg, x + SIDE_PANEL_X + 64, y + SIDE_PANEL_Y + 66, OUTPUT_ORANGE);
        drawLed(gg, x + SIDE_PANEL_X + 111, y + SIDE_PANEL_Y + 66, DISABLED);
    }

    private void drawConnectorLegendLabels(GuiGraphics gg) {
        gg.drawString(font, Component.literal("ВХОД"), SIDE_PANEL_X + 8, SIDE_PANEL_Y + 76, INPUT_BLUE, false);
        gg.drawString(font, Component.literal("ВЫХОД"), SIDE_PANEL_X + 48, SIDE_PANEL_Y + 76, OUTPUT_ORANGE, false);
        gg.drawString(font, Component.literal("ВЫКЛ"), SIDE_PANEL_X + 98, SIDE_PANEL_Y + 76, TEXT_DARK, false);
    }

    private void drawSidePanel(GuiGraphics gg, int x, int y, int mouseX, int mouseY) {
        int[][] positions = {
                {SIDE_PANEL_X + 7, SIDE_PANEL_Y + 34},
                {SIDE_PANEL_X + 49, SIDE_PANEL_Y + 34},
                {SIDE_PANEL_X + 91, SIDE_PANEL_Y + 34}
        };

        RelativeSide[] sides = {
                RelativeSide.TOP,
                RelativeSide.BOTTOM,
                RelativeSide.BACK
        };

        for (int i = 0; i < sides.length; i++) {
            drawSideButton(
                    gg,
                    x + positions[i][0],
                    y + positions[i][1],
                    37,
                    24,
                    sides[i],
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawSideButton(
            GuiGraphics gg,
            int x,
            int y,
            int w,
            int h,
            RelativeSide side,
            int mouseX,
            int mouseY
    ) {
        boolean hovered = insideAbsolute(mouseX, mouseY, x, y, w, h);
        SideMode mode = menu.getSideMode(side);
        int color = sideColor(mode);

        gg.fill(x, y, x + w, y + h, FRAME_DARK);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFF687378 : FRAME);
        gg.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF151A1D);

        String name = shortSideName(side);
        gg.drawString(
                font,
                name,
                x + Math.max(2, (w - font.width(name)) / 2),
                y + 4,
                TEXT_DIM,
                false
        );

        gg.fill(x + 5, y + 16, x + w - 5, y + 20, FRAME_DARK);
        gg.fill(x + 7, y + 17, x + w - 7, y + 19, color);
    }

    private RelativeSide hoveredSide(double mouseX, double mouseY) {
        int[][] positions = {
                {SIDE_PANEL_X + 7, SIDE_PANEL_Y + 34},
                {SIDE_PANEL_X + 49, SIDE_PANEL_Y + 34},
                {SIDE_PANEL_X + 91, SIDE_PANEL_Y + 34}
        };

        RelativeSide[] sides = {
                RelativeSide.TOP,
                RelativeSide.BOTTOM,
                RelativeSide.BACK
        };

        for (int i = 0; i < sides.length; i++) {
            if (inside(mouseX, mouseY, positions[i][0], positions[i][1], 37, 24)) {
                return sides[i];
            }
        }
        return null;
    }

    private void renderProcessTooltips(GuiGraphics gg, int mouseX, int mouseY) {
        for (int i = 0; i < Stage.values().length; i++) {
            if (inside(mouseX, mouseY, CARD_X[i], CARD_Y, CARD_W, CARD_H)) {
                gg.renderComponentTooltip(font, stageTooltip(Stage.values()[i]), mouseX, mouseY);
                return;
            }
        }

        if (inside(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Энергия комплекса"));
            tooltip.add(Component.literal(menu.getEnergy() + " / " + menu.getEnergyCapacity() + " FE"));
            tooltip.add(Component.literal("Потребление: " + menu.getCurrentEnergyUse() + " FE/т"));
            gg.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }

        if (inside(mouseX, mouseY, OXYGEN_X, OXYGEN_Y, OXYGEN_W, OXYGEN_H)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Хранилище кислорода"));
            tooltip.add(Component.literal(menu.getOxygen() + " / " + menu.getOxygenCapacity() + " mB"));
            tooltip.add(Component.literal(menu.isOutputActive() ? "Выход активен" : "Выход ожидает"));
            gg.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderSideTooltips(GuiGraphics gg, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_W, GEAR_H)) {
            gg.renderTooltip(font, Component.literal("Настройка коннекторов"), mouseX, mouseY);
            return;
        }

        if (!sidePanelOpen) {
            return;
        }

        RelativeSide side = hoveredSide(mouseX, mouseY);
        if (side == null) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(sideName(side)));
        tooltip.add(Component.literal("Режим: " + modeName(menu.getSideMode(side))));
        tooltip.add(Component.literal("ЛКМ — изменить режим"));
        tooltip.add(Component.literal("Вход принимает FE, выход отдаёт O₂."));
        gg.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private List<Component> stageTooltip(Stage stage) {
        List<Component> tooltip = new ArrayList<>();

        switch (stage) {
            case INTAKE -> {
                tooltip.add(Component.literal("Забор воздуха"));
                tooltip.add(Component.literal("Буфер: " + menu.getCollectedAir() + " / " + menu.getCollectedAirCapacity()));
                tooltip.add(Component.literal(menu.hasAtmosphere() ? "Атмосфера доступна" : "Нет доступной атмосферы"));
                tooltip.add(Component.literal(menu.isIntakeActive() ? "Состояние: работает" : "Состояние: ожидание"));
            }
            case FILTRATION -> {
                tooltip.add(Component.literal("Фильтрация"));
                tooltip.add(Component.literal("Буфер: " + menu.getFilteredAir() + " / " + menu.getFilteredAirCapacity()));
                if (menu.hasAirFilter()) {
                    tooltip.add(Component.literal("Ресурс фильтра: "
                            + menu.getAirFilterRemaining() + " / " + menu.getAirFilterMaxDamage()));
                    tooltip.add(Component.literal("Отработано: "
                            + menu.getAirFilterDamage() + " / " + menu.getAirFilterMaxDamage()));
                } else {
                    tooltip.add(Component.literal("Требуется воздушный фильтр"));
                }
                tooltip.add(Component.literal(menu.isFilterActive() ? "Состояние: фильтрация" : "Состояние: ожидание"));
            }
            case COMPRESSION -> {
                tooltip.add(Component.literal("Компрессия"));
                tooltip.add(Component.literal("Буфер: " + menu.getCompressedFeed() + " / " + menu.getCompressedFeedCapacity()));
                tooltip.add(Component.literal("Потребление комплекса: " + menu.getCurrentEnergyUse() + " FE/т"));
                tooltip.add(Component.literal(menu.isCompressionActive() ? "Состояние: сжатие" : "Состояние: ожидание"));
            }
            case OUTPUT -> {
                tooltip.add(Component.literal("Выход кислорода"));
                tooltip.add(Component.literal("O₂: " + menu.getOxygen() + " / " + menu.getOxygenCapacity() + " mB"));
                tooltip.add(Component.literal(menu.isOutputActive() ? "Состояние: выход активен" : "Состояние: ожидание"));
            }
        }

        return tooltip;
    }

    private Component getStatusText() {
        if (!menu.isFormed()) {
            return Component.literal("СТРУКТУРА НЕ СОБРАНА");
        }
        if (!menu.hasAirFilter()) {
            return Component.literal("ТРЕБУЕТСЯ ВОЗДУШНЫЙ ФИЛЬТР");
        }
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() <= 0) {
            return Component.literal("НЕТ ЭНЕРГИИ");
        }
        if (!menu.hasAtmosphere() && menu.getCollectedAir() < 16) {
            return Component.literal("НЕТ ДОСТУПНОЙ АТМОСФЕРЫ");
        }
        if (menu.isOutputActive()) {
            return Component.literal("ПРОИЗВОДСТВО КИСЛОРОДА");
        }
        if (menu.isCompressionActive()) {
            return Component.literal("ИДЁТ КОМПРЕССИЯ");
        }
        if (menu.isFilterActive()) {
            return Component.literal("ИДЁТ ФИЛЬТРАЦИЯ");
        }
        if (menu.isIntakeActive()) {
            return Component.literal("ИДЁТ ЗАБОР ВОЗДУХА");
        }
        if (menu.getOxygenCapacity() > 0 && menu.getOxygen() >= menu.getOxygenCapacity()) {
            return Component.literal("ХРАНИЛИЩЕ КИСЛОРОДА ЗАПОЛНЕНО");
        }
        return Component.literal("ГОТОВ / ОЖИДАНИЕ");
    }

    private int getStatusColor() {
        if (!menu.isFormed() || !menu.hasAirFilter()) {
            return ERROR;
        }
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() <= 0) {
            return ERROR;
        }
        if (!menu.hasAtmosphere() && menu.getCollectedAir() < 16) {
            return WARN;
        }
        if (anyActive()) {
            return GREEN_BRIGHT;
        }
        if (menu.getOxygenCapacity() > 0 && menu.getOxygen() >= menu.getOxygenCapacity()) {
            return WARN;
        }
        return TEXT_DIM;
    }

    private boolean anyActive() {
        return menu.isIntakeActive()
                || menu.isFilterActive()
                || menu.isCompressionActive()
                || menu.isOutputActive();
    }

    private boolean isStageActive(Stage stage) {
        return switch (stage) {
            case INTAKE -> menu.isIntakeActive();
            case FILTRATION -> menu.isFilterActive();
            case COMPRESSION -> menu.isCompressionActive();
            case OUTPUT -> menu.isOutputActive();
        };
    }

    private int accent(Stage stage, boolean active) {
        return switch (stage) {
            case INTAKE -> active ? CYAN_BRIGHT : CYAN;
            case FILTRATION -> active ? GREEN_BRIGHT : GREEN;
            case COMPRESSION -> active ? AMBER_BRIGHT : AMBER;
            case OUTPUT -> active ? OXYGEN_BRIGHT : OXYGEN;
        };
    }

    private static int sideColor(SideMode mode) {
        return switch (mode) {
            case INPUT -> INPUT_BLUE;
            case OUTPUT, BOTH -> OUTPUT_ORANGE;
            case DISABLED -> DISABLED;
        };
    }

    private static String modeName(SideMode mode) {
        return switch (mode) {
            case INPUT -> "Вход";
            case OUTPUT, BOTH -> "Выход";
            case DISABLED -> "Отключено";
        };
    }

    private static String shortSideName(RelativeSide side) {
        return switch (side) {
            case TOP -> "ВЕРХ";
            case BOTTOM -> "НИЗ";
            case FRONT -> "";
            case BACK -> "ЗАД";
            case LEFT -> "ЛЕВО";
            case RIGHT -> "ПРАВО";
        };
    }

    private static String sideName(RelativeSide side) {
        return switch (side) {
            case TOP -> "Верхняя сторона";
            case BOTTOM -> "Нижняя сторона";
            case FRONT -> "Передняя сторона отключена";
            case BACK -> "Задняя сторона";
            case LEFT -> "Левая сторона";
            case RIGHT -> "Правая сторона";
        };
    }

    private boolean isFilterSlot(double mouseX, double mouseY) {
        return inside(
                mouseX,
                mouseY,
                OxygenComplexMenu.FILTER_SLOT_X - 1,
                OxygenComplexMenu.FILTER_SLOT_Y - 1,
                18,
                18
        );
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }

    private static boolean insideAbsolute(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private int animationTick() {
        return minecraft != null && minecraft.player != null ? minecraft.player.tickCount : 0;
    }

    private static int scaled(int value, int capacity, int width) {
        if (value <= 0 || capacity <= 0 || width <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(width, (int) ((long) value * width / capacity)));
    }

    private static void drawOuterPanel(GuiGraphics gg, int x, int y, int w, int h) {
        gg.fill(x, y, x + w, y + h, FRAME_DARK);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, FRAME);
        gg.fill(x + 2, y + 2, x + w - 2, y + h - 2, PANEL);
        gg.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFF646C70);
        gg.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, 0xFF13171A);
    }

    private static void drawFrameBox(GuiGraphics gg, int x, int y, int w, int h, int fill) {
        gg.fill(x, y, x + w, y + h, FRAME_DARK);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, FRAME);
        gg.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
    }

    private static void drawBevel(GuiGraphics gg, int x, int y, int w, int h) {
        gg.fill(x, y, x + w, y + 1, 0xFF343C40);
        gg.fill(x, y, x + 1, y + h, 0xFF343C40);
        gg.fill(x + w - 1, y, x + w, y + h, 0xFF080B0D);
        gg.fill(x, y + h - 1, x + w, y + h, 0xFF080B0D);
    }

    private static void drawBarBox(GuiGraphics gg, int x, int y, int w, int h) {
        drawFrameBox(gg, x, y, w, h, 0xFF14191C);
    }

    private static void fillBar(
            GuiGraphics gg,
            int x,
            int y,
            int w,
            int h,
            int value,
            int capacity,
            int color,
            int bright
    ) {
        int fill = scaled(value, capacity, w - 6);
        if (fill <= 0) {
            return;
        }

        gg.fill(x + 3, y + 3, x + 3 + fill, y + h - 3, color);
        gg.fill(x + 3, y + 3, x + 3 + fill, y + 5, bright);
    }

    private static void drawLed(GuiGraphics gg, int x, int y, int color) {
        gg.fill(x, y, x + 5, y + 5, FRAME_DARK);
        gg.fill(x + 1, y + 1, x + 4, y + 4, color);
    }

    private void drawDiagLeds(GuiGraphics gg, int x, int y) {
        drawLed(gg, x, y, menu.isFormed() ? GREEN_BRIGHT : ERROR);
        drawLed(gg, x + 10, y, menu.hasAirFilter() ? GREEN_BRIGHT : ERROR);
        drawLed(gg, x + 20, y, menu.getEnergy() > 0 ? AMBER_BRIGHT : ERROR);
        drawLed(gg, x + 30, y, menu.hasAtmosphere() ? CYAN_BRIGHT : WARN);
    }

    private void drawClamped(
            GuiGraphics gg,
            Component component,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        String value = component.getString();
        if (font.width(value) > maxWidth) {
            String dots = "...";
            value = font.plainSubstrByWidth(
                    value,
                    Math.max(0, maxWidth - font.width(dots))
            ) + dots;
        }
        gg.drawString(font, value, x, y, color, false);
    }

    private void drawCentered(
            GuiGraphics gg,
            Component component,
            int x,
            int y,
            int width,
            int color
    ) {
        String value = component.getString();
        if (font.width(value) > width) {
            String dots = "...";
            value = font.plainSubstrByWidth(
                    value,
                    Math.max(0, width - font.width(dots))
            ) + dots;
        }

        gg.drawString(
                font,
                value,
                x + Math.max(0, (width - font.width(value)) / 2),
                y,
                color,
                false
        );
    }

    private void drawRight(
            GuiGraphics gg,
            Component component,
            int rightX,
            int y,
            int color
    ) {
        String value = component.getString();
        gg.drawString(font, value, rightX - font.width(value), y, color, false);
    }

    private static String compact(int value) {
        if (value < 1_000) {
            return Integer.toString(value);
        }
        if (value % 1_000 == 0) {
            return (value / 1_000) + "k";
        }
        return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
    }

    private enum Stage {
        INTAKE,
        FILTRATION,
        COMPRESSION,
        OUTPUT
    }
}
