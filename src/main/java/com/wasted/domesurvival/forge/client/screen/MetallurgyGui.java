package com.wasted.domesurvival.forge.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Shared, texture-independent industrial drawing primitives for the metallurgy machines. */
final class MetallurgyGui {
    static final int PANEL_WIDTH = 220;
    static final int PANEL_HEIGHT = 266;
    static final int MACHINE_X = 10;
    static final int MACHINE_Y = 28;
    static final int MACHINE_W = 200;
    static final int MACHINE_H = 105;
    static final int PROGRESS_X = 91;
    static final int PROGRESS_Y = 55;
    static final int PROGRESS_W = 70;
    static final int PROGRESS_H = 14;
    static final int CHAMBER_X = 96;
    static final int CHAMBER_Y = 92;
    static final int CHAMBER_W = 60;
    static final int CHAMBER_H = 34;
    static final int FUEL_X = 22;
    static final int FUEL_Y = 92;
    static final int FUEL_W = 60;
    static final int FUEL_H = 9;
    static final int STATUS_LABEL_Y = 80;
    static final int INVENTORY_X = 11;
    static final int INVENTORY_Y = 158;
    static final int SLOT_SIZE = 22;
    static final int SLOT_STEP = 22;
    static final int HOTBAR_Y = 226;

    private MetallurgyGui() { }

    static void drawBase(GuiGraphics graphics, int x, int y) {
        drawIndustrialPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, 0xFF2D3438);
        drawThinFrame(graphics, x + MACHINE_X, y + MACHINE_Y, MACHINE_W, MACHINE_H, 0xFF171D20);

        // Recessed machine bay with restrained rivets and steel separators.
        graphics.fill(x + 13, y + 31, x + 207, y + 34, 0xFF252D31);
        graphics.fill(x + 13, y + 126, x + 207, y + 129, 0xFF0C1012);
        for (int rivetX : new int[]{15, 202}) {
            graphics.fill(x + rivetX, y + 35, x + rivetX + 2, y + 37, 0xFF727B80);
            graphics.fill(x + rivetX, y + 123, x + rivetX + 2, y + 125, 0xFF596267);
        }

        graphics.fill(x + 10, y + 153, x + PANEL_WIDTH - 10, y + 154, 0xFF151A1D);
        graphics.fill(x + 10, y + 154, x + PANEL_WIDTH - 10, y + 155, 0xFF4A5358);
        drawInventory(graphics, x, y);
    }

    static void drawCokeSlots(GuiGraphics graphics, int x, int y) {
        drawSlot(graphics, x + 22, y + 50, 24);
        drawSlot(graphics, x + 58, y + 50, 24);
        drawSlot(graphics, x + 174, y + 50, 24);
        drawFlowLine(graphics, x + 82, y + 61, x + PROGRESS_X, true);
        drawFlowLine(graphics, x + PROGRESS_X + PROGRESS_W, y + 61, x + 174, false);
    }

    static void drawShaftSlots(GuiGraphics graphics, int x, int y) {
        drawSlot(graphics, x + 22, y + 50, 24);
        drawSlot(graphics, x + 58, y + 50, 24);
        drawSlot(graphics, x + 174, y + 38, 24);
        drawSlot(graphics, x + 174, y + 68, 24);
        drawFlowLine(graphics, x + 82, y + 61, x + PROGRESS_X, true);
        graphics.fill(x + PROGRESS_X + PROGRESS_W, y + 60, x + 174, y + 63, 0xFF354248);
        graphics.fill(x + 169, y + 49, x + 174, y + 52, 0xFF59656A);
        graphics.fill(x + 169, y + 79, x + 174, y + 82, 0xFF59656A);
    }

    static void drawProcess(GuiGraphics graphics, int x, int y, int progress, int progressMax,
                            int burnTime, int burnTimeMax, boolean working) {
        drawThinFrame(graphics, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H, 0xFF0D1214);
        int innerWidth = PROGRESS_W - 6;
        int filled = progressMax <= 0 ? 0 : Mth.clamp(progress * innerWidth / progressMax, 0, innerWidth);
        if (filled > 0) {
            graphics.fill(x + PROGRESS_X + 3, y + PROGRESS_Y + 3,
                    x + PROGRESS_X + 3 + filled, y + PROGRESS_Y + PROGRESS_H - 3, 0xFF88502B);
            graphics.fill(x + PROGRESS_X + 3, y + PROGRESS_Y + 3,
                    x + PROGRESS_X + 3 + filled, y + PROGRESS_Y + 5, 0xFFC07437);
        }

        drawThinFrame(graphics, x + FUEL_X, y + FUEL_Y, FUEL_W, FUEL_H, 0xFF0D1214);
        int fuelInner = FUEL_W - 6;
        int fuel = burnTimeMax <= 0 ? 0 : Mth.clamp(burnTime * fuelInner / burnTimeMax, 0, fuelInner);
        if (fuel > 0) {
            graphics.fill(x + FUEL_X + 3, y + FUEL_Y + 3,
                    x + FUEL_X + 3 + fuel, y + FUEL_Y + FUEL_H - 2, 0xFF9A572D);
        }

        drawHeatChamber(graphics, x + CHAMBER_X, y + CHAMBER_Y, working);
    }

    private static void drawHeatChamber(GuiGraphics graphics, int x, int y, boolean working) {
        drawThinFrame(graphics, x, y, CHAMBER_W, CHAMBER_H, 0xFF101517);
        graphics.fill(x + 6, y + 6, x + CHAMBER_W - 6, y + CHAMBER_H - 6, 0xFF080B0D);
        for (int vent = 0; vent < 4; vent++) {
            int vx = x + 10 + vent * 11;
            graphics.fill(vx, y + 8, vx + 4, y + CHAMBER_H - 8, 0xFF252C30);
        }
        if (!working) return;

        long phase = System.currentTimeMillis() / 130L;
        for (int flame = 0; flame < 4; flame++) {
            int height = 8 + (int) ((phase + flame * 2L) % 4L) * 3;
            int fx = x + 11 + flame * 11;
            int bottom = y + CHAMBER_H - 8;
            graphics.fill(fx, bottom - height, fx + 4, bottom, 0xFF9D3C19);
            graphics.fill(fx + 1, bottom - height + 3, fx + 3, bottom - 2, 0xFFE07425);
            if (((phase + flame) & 1L) == 0L) {
                graphics.fill(fx + 1, bottom - height - 2, fx + 3, bottom - height + 1, 0xFFC85B1F);
            }
        }
    }

    private static void drawFlowLine(GuiGraphics graphics, int fromX, int y, int toX, boolean rightArrow) {
        graphics.fill(fromX, y, toX, y + 3, 0xFF354248);
        int arrowX = rightArrow ? toX - 4 : fromX;
        graphics.fill(arrowX, y - 2, arrowX + 4, y + 5, 0xFF59656A);
    }

    private static void drawInventory(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x + INVENTORY_X + column * SLOT_STEP,
                        y + INVENTORY_Y + row * SLOT_STEP, SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x + INVENTORY_X + column * SLOT_STEP, y + HOTBAR_Y, SLOT_SIZE);
        }
    }

    static void drawIndustrialPanel(GuiGraphics graphics, int x, int y, int width, int height, int fillColor) {
        graphics.fill(x, y, x + width, y + height, 0xFF090C0E);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF465056);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF596269);
        graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFF111619);
    }

    static void drawThinFrame(GuiGraphics graphics, int x, int y, int width, int height, int fillColor) {
        graphics.fill(x, y, x + width, y + height, 0xFF080B0D);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF465056);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
    }

    static void drawSlot(GuiGraphics graphics, int x, int y, int size) {
        int inset = Math.max(2, (size - 16) / 2);
        graphics.fill(x, y, x + size, y + size, 0xFF090C0E);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF424B50);
        graphics.fill(x + inset, y + inset, x + inset + 16, y + inset + 16, 0xFF1A2023);
    }
}
