package com.wasted.domesurvival.forge.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared drawing primitives for DomeSurvival's compact industrial machine GUIs.
 * The palette and bevel language intentionally match CoalGeneratorScreen.
 */
public final class DomeIndustrialGuiStyle {
    public static final int PANEL_FILL = 0xFF30363A;
    public static final int PANEL_ALT = 0xFF252B2F;
    public static final int INSET_FILL = 0xFF151A1D;
    public static final int SLOT_FILL = 0xFF1B2125;
    public static final int TEXT = 0xFFE0E4E6;
    public static final int TEXT_MUTED = 0xFFB8C6CB;
    public static final int TEXT_DIM = 0xFF84959C;
    public static final int ENERGY = 0xFF8D792A;
    public static final int ENERGY_LIGHT = 0xFFAA9438;
    public static final int PROCESS = 0xFF744128;
    public static final int PROCESS_LIGHT = 0xFF925034;
    public static final int FLUID = 0xFF4B7888;
    public static final int FLUID_LIGHT = 0xFF68A0B5;
    public static final int BIO = 0xFF617C68;
    public static final int BIO_LIGHT = 0xFF85A58C;
    public static final int READY = 0xFF8EA66C;
    public static final int WARNING = 0xFFD2AC58;
    public static final int ERROR = 0xFFBC6653;

    private DomeIndustrialGuiStyle() {
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF0C0F11);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF464E53);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_FILL);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF50585D);
        graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFF14181B);
    }

    public static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, 0xFF0B0E10);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4C555A);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
    }

    /** Draws the 22x22 visual frame around a vanilla 16x16 menu slot at slotX/slotY. */
    public static void drawSlot(GuiGraphics graphics, int slotX, int slotY, boolean output) {
        int x = slotX - 3;
        int y = slotY - 3;
        graphics.fill(x, y, x + 22, y + 22, 0xFF0D1012);
        graphics.fill(x + 1, y + 1, x + 21, y + 21, output ? 0xFF536168 : 0xFF3E464B);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT_FILL);
        if (output) {
            graphics.fill(x + 3, y + 18, x + 19, y + 19, 0xFF9AB7C2);
        }
    }

    public static void drawProgress(GuiGraphics graphics, int x, int y, int width, int height,
                                    int value, int maximum, int fill, int highlight) {
        drawFrame(graphics, x, y, width, height, INSET_FILL);
        int innerWidth = Math.max(0, width - 6);
        int safeMaximum = Math.max(1, maximum);
        int progress = (int) Math.min(innerWidth, (long) Math.max(0, value) * innerWidth / safeMaximum);
        if (progress <= 0) return;
        graphics.fill(x + 3, y + 3, x + 3 + progress, y + height - 3, fill);
        if (height >= 8) {
            graphics.fill(x + 3, y + 4, x + 3 + progress, Math.min(y + height - 3, y + 6), highlight);
        }
    }

    public static void drawVerticalMeter(GuiGraphics graphics, int x, int y, int width, int height,
                                         int value, int maximum, int fill, int highlight) {
        drawFrame(graphics, x, y, width, height, INSET_FILL);
        int innerWidth = Math.max(0, width - 6);
        int innerHeight = Math.max(0, height - 6);
        int safeMaximum = Math.max(1, maximum);
        int amount = (int) Math.min(innerHeight, (long) Math.max(0, value) * innerHeight / safeMaximum);
        if (amount <= 0 || innerWidth <= 0) return;
        int bottom = y + height - 3;
        int top = bottom - amount;
        graphics.fill(x + 3, top, x + 3 + innerWidth, bottom, fill);
        if (innerWidth >= 4) {
            graphics.fill(x + 4, top, Math.min(x + 6, x + width - 3), bottom, highlight);
        }
    }

    public static void drawInventoryCell(GuiGraphics graphics, int slotX, int slotY) {
        graphics.fill(slotX - 3, slotY - 3, slotX + 19, slotY + 19, 0xFF111518);
        graphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0xFF3D464B);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF20262A);
    }
}
