package com.wasted.domesurvival.forge.client.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** Shared JEI renderer that mirrors DomeSurvival's industrial machine GUI language. */
final class DomeJeiStyle {
    static final int PANEL_FILL = 0xFF30363A;
    static final int PANEL_ALT = 0xFF252B2F;
    static final int INSET_FILL = 0xFF151A1D;
    static final int SLOT_FILL = 0xFF1B2125;
    static final int TEXT = 0xFFE0E4E6;
    static final int TEXT_MUTED = 0xFFB8C6CB;
    static final int TEXT_DIM = 0xFF84959C;
    static final int METAL = 0xFF65737A;
    static final int METAL_LIGHT = 0xFF9AB7C2;
    static final int PROCESS = 0xFF744128;
    static final int PROCESS_LIGHT = 0xFF925034;
    static final int ENERGY = 0xFF8D792A;
    static final int ACTIVE = 0xFF394247;

    private DomeJeiStyle() {
    }

    static void drawIndustrialPanel(GuiGraphics graphics, int x, int y, int width, int height, int fillColor) {
        graphics.fill(x, y, x + width, y + height, 0xFF0C0F11);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF464E53);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF50585D);
        graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFF14181B);
    }

    static void drawThinFrame(GuiGraphics graphics, int x, int y, int width, int height, int fillColor) {
        graphics.fill(x, y, x + width, y + height, 0xFF0B0E10);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4C555A);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
    }

    /** x/y are JEI's 16x16 ingredient coordinates. */
    static void drawSlot(GuiGraphics graphics, int x, int y, boolean output, boolean fluid) {
        int frameX = x - 4;
        int frameY = y - 4;
        int rim = output ? 0xFF536168 : 0xFF3E464B;
        graphics.fill(frameX, frameY, frameX + 24, frameY + 24, 0xFF0D1012);
        graphics.fill(frameX + 1, frameY + 1, frameX + 23, frameY + 23, rim);
        graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
        if (fluid) {
            graphics.fill(frameX + 3, frameY + 20, frameX + 21, frameY + 21, 0xFF4B7888);
        } else if (output) {
            graphics.fill(frameX + 3, frameY + 20, frameX + 21, frameY + 21, METAL_LIGHT);
        }
    }

    static void drawArrow(GuiGraphics graphics, int startX, int endX, int centerY) {
        if (endX <= startX + 6) return;
        graphics.fill(startX, centerY - 1, endX - 5, centerY + 2, METAL);
        graphics.fill(endX - 7, centerY - 4, endX - 2, centerY + 5, METAL);
        graphics.fill(endX - 4, centerY - 2, endX, centerY + 3, METAL_LIGHT);
    }

    static void drawProgress(GuiGraphics graphics, int x, int y, int width, int height,
                             float fraction, int fillColor, int highlightColor) {
        drawThinFrame(graphics, x, y, width, height, INSET_FILL);
        int innerWidth = Math.max(0, width - 6);
        int progress = Math.min(innerWidth, Math.max(0, Math.round(innerWidth * fraction)));
        if (progress <= 0) return;
        graphics.fill(x + 3, y + 3, x + 3 + progress, y + height - 3, fillColor);
        if (height >= 8) {
            graphics.fill(x + 3, y + 4, x + 3 + progress, Math.min(y + height - 3, y + 6), highlightColor);
        }
    }

    static float animationFraction(int processTicks) {
        int ticks = Math.max(20, processTicks);
        long cycleMs = Math.max(1_000L, ticks * 50L);
        return (Util.getMillis() % cycleMs) / (float) cycleMs;
    }

    static void drawCenteredClamped(GuiGraphics graphics, Component text, int centerX, int y,
                                    int maxWidth, int color) {
        Font font = Minecraft.getInstance().font;
        String value = clamp(font, text, maxWidth);
        graphics.drawString(font, value, centerX - font.width(value) / 2, y, color, false);
    }

    static void drawLeftClamped(GuiGraphics graphics, Component text, int x, int y,
                                int maxWidth, int color) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, clamp(font, text, maxWidth), x, y, color, false);
    }

    private static String clamp(Font font, Component text, int maxWidth) {
        String value = text.getString();
        if (maxWidth <= 0 || value.isBlank()) return "";
        if (font.width(value) <= maxWidth) return value;
        String dots = "...";
        int usable = Math.max(0, maxWidth - font.width(dots));
        return font.plainSubstrByWidth(value, usable) + dots;
    }
}
