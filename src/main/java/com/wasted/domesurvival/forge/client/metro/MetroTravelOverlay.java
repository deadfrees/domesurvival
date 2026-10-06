package com.wasted.domesurvival.forge.client.metro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class MetroTravelOverlay {
    private MetroTravelOverlay() {
    }

    public static final IGuiOverlay HUD = MetroTravelOverlay::render;

    private static void render(ForgeGui gui,
                               GuiGraphics graphics,
                               float partialTick,
                               int screenWidth,
                               int screenHeight) {
        MetroTravelClientState.Snapshot state = MetroTravelClientState.snapshot();
        if (state.mode() == MetroTravelClientState.Mode.NONE) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Font font = minecraft.font;

        if (state.mode() == MetroTravelClientState.Mode.TRAVEL) {
            double progress = Math.max(0.0D, Math.min(1.0D, state.progress()));

            int fade = 110;
            if (progress < 0.12D) {
                fade = 110 + (int) ((0.12D - progress) / 0.12D * 90.0D);
            } else if (progress > 0.82D) {
                fade = 110 + (int) ((progress - 0.82D) / 0.18D * 125.0D);
            }
            fade = Math.max(0, Math.min(235, fade));

            graphics.fill(0, 0, screenWidth, screenHeight, (fade << 24) | 0x05090C);

            int boxWidth = Math.min(420, screenWidth - 36);
            int boxX = (screenWidth - boxWidth) / 2;
            int boxY = screenHeight / 2 - 38;

            graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 76, 0xD5121A1F);
            graphics.fill(boxX + 1, boxY + 1, boxX + boxWidth - 1, boxY + 2, 0xFF4A9BAD);

            drawCentered(graphics, font,
                    "\u041f\u041e\u0415\u0417\u0414 \u0412 \u041f\u0423\u0422\u0418",
                    screenWidth / 2, boxY + 11, 0xFFBFEAF2);

            drawCentered(graphics, font,
                    "\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435: " + state.destination(),
                    screenWidth / 2, boxY + 28, 0xFFE8EDF0);

            long seconds = Math.max(0L, (state.remainingMillis() + 999L) / 1000L);
            drawCentered(graphics, font,
                    "\u0414\u043e \u043f\u0440\u0438\u0431\u044b\u0442\u0438\u044f: " + seconds + " \u0441",
                    screenWidth / 2, boxY + 43, 0xFF9BAAB2);

            int barX = boxX + 18;
            int barY = boxY + 61;
            int barW = boxWidth - 36;
            graphics.fill(barX, barY, barX + barW, barY + 6, 0xFF263139);
            graphics.fill(barX, barY, barX + (int) Math.round(barW * progress), barY + 6, 0xFF55C3D9);
            return;
        }

        String title = state.mode() == MetroTravelClientState.Mode.ARRIVAL
                ? "\u041f\u0420\u0418\u0411\u042b\u0422\u0418\u0415"
                : "\u041f\u041e\u0415\u0417\u0414\u041a\u0410 \u041e\u0422\u041c\u0415\u041d\u0415\u041d\u0410";

        String line = state.mode() == MetroTravelClientState.Mode.ARRIVAL
                ? state.destination()
                : state.detail();

        int boxWidth = Math.min(380, screenWidth - 40);
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = screenHeight / 2 - 28;
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 56, 0xD5121A1F);
        graphics.fill(
                boxX + 1, boxY + 1, boxX + boxWidth - 1, boxY + 2,
                state.mode() == MetroTravelClientState.Mode.ARRIVAL ? 0xFF57B968 : 0xFFC95E5E
        );

        drawCentered(graphics, font, title, screenWidth / 2, boxY + 12, 0xFFE9EEF0);
        drawCentered(graphics, font, abbreviate(line, 54), screenWidth / 2, boxY + 31, 0xFFB7C0C5);
    }

    private static void drawCentered(GuiGraphics graphics,
                                     Font font,
                                     String text,
                                     int centerX,
                                     int y,
                                     int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private static String abbreviate(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(1, max - 1)) + "\u2026";
    }
}
