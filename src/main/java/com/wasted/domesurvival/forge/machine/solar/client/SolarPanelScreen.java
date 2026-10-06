package com.wasted.domesurvival.forge.machine.solar.client;

import com.wasted.domesurvival.forge.machine.solar.SolarPanelMenu;
import com.wasted.domesurvival.forge.machine.solar.SolarPanelState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 142;

    private static final int ENERGY_BAR_X = 20;
    private static final int ENERGY_BAR_Y = 91;
    private static final int ENERGY_BAR_W = 180;
    private static final int ENERGY_BAR_H = 14;

    private static final int PANEL_BG = 0xFF30363A;
    private static final int FRAME_DARK = 0xFF121719;
    private static final int FRAME_LIGHT = 0xFF515B61;
    private static final int INNER_BG = 0xFF171C20;
    private static final int TEXT_MAIN = 0xFFE1E6E9;
    private static final int TEXT_DIM = 0xFFAEB8BD;
    private static final int ENERGY_FILL = 0xFF8D792A;
    private static final int ENERGY_HIGHLIGHT = 0xFFAA9438;

    public SolarPanelScreen(
            SolarPanelMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isHovering(
                ENERGY_BAR_X,
                ENERGY_BAR_Y,
                ENERGY_BAR_W,
                ENERGY_BAR_H,
                mouseX,
                mouseY
        )) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.solar_panel.energy_tooltip",
                            menu.getEnergyStored(),
                            menu.getEnergyCapacity()
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int x = leftPos;
        int y = topPos;

        drawIndustrialPanel(guiGraphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);

        drawThinFrame(guiGraphics, x + 16, y + 29, 32, 32, INNER_BG);
        if (menu.getSolarState() == SolarPanelState.NIGHT) {
            drawMoonIcon(guiGraphics, x + 23, y + 36);
        } else {
            drawSunIcon(guiGraphics, x + 23, y + 36);
        }

        drawThinFrame(
                guiGraphics,
                x + ENERGY_BAR_X,
                y + ENERGY_BAR_Y,
                ENERGY_BAR_W,
                ENERGY_BAR_H,
                INNER_BG
        );

        int capacity = Math.max(1, menu.getEnergyCapacity());
        int innerWidth = ENERGY_BAR_W - 6;
        int fillWidth = Math.min(
                innerWidth,
                (int) ((long) Math.max(0, menu.getEnergyStored()) * innerWidth / capacity)
        );

        if (fillWidth > 0) {
            int fillLeft = x + ENERGY_BAR_X + 3;
            int fillTop = y + ENERGY_BAR_Y + 3;
            int fillBottom = y + ENERGY_BAR_Y + ENERGY_BAR_H - 3;

            guiGraphics.fill(
                    fillLeft,
                    fillTop,
                    fillLeft + fillWidth,
                    fillBottom,
                    ENERGY_FILL
            );
            guiGraphics.fill(
                    fillLeft,
                    fillTop,
                    fillLeft + fillWidth,
                    fillTop + 2,
                    ENERGY_HIGHLIGHT
            );
        }

        guiGraphics.fill(x + 16, y + 75, x + PANEL_WIDTH - 16, y + 76, FRAME_DARK);
        guiGraphics.fill(x + 16, y + 76, x + PANEL_WIDTH - 16, y + 77, FRAME_LIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(font, title, imageWidth / 2, 10, TEXT_MAIN);

        SolarPanelState state = menu.getSolarState();
        Component stateText = Component.translatable(state.translationKey());

        guiGraphics.drawString(
                font,
                Component.translatable("gui.domesurvival.solar_panel.status", stateText),
                57,
                34,
                statusColor(state),
                false
        );

        int displayedGeneration = state == SolarPanelState.GENERATING
                ? menu.getGenerationPerTick()
                : 0;

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.domesurvival.solar_panel.generation",
                        displayedGeneration
                ),
                57,
                47,
                TEXT_DIM,
                false
        );

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.domesurvival.solar_panel.max_output",
                        menu.getMaxOutputPerTick()
                ),
                57,
                60,
                TEXT_DIM,
                false
        );

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.domesurvival.solar_panel.energy",
                        menu.getEnergyStored(),
                        menu.getEnergyCapacity()
                ),
                ENERGY_BAR_X,
                111,
                TEXT_MAIN,
                false
        );

    }

    private static int statusColor(SolarPanelState state) {
        return switch (state) {
            case GENERATING -> 0xFF9CCB7A;
            case BUFFER_FULL -> 0xFFE1C768;
            case NO_SKY -> 0xFFD3A068;
            case NIGHT -> 0xFF9EACBC;
        };
    }

    private static void drawIndustrialPanel(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height
    ) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0E1214);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, FRAME_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_BG);
        guiGraphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFF252B2F);
    }

    private static void drawThinFrame(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fill
    ) {
        guiGraphics.fill(x, y, x + width, y + height, FRAME_DARK);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, FRAME_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
    }

    private static void drawSunIcon(GuiGraphics guiGraphics, int x, int y) {
        int glow = 0xFFF1C44E;
        int core = 0xFFFFD768;

        guiGraphics.fill(x + 7, y, x + 11, y + 3, glow);
        guiGraphics.fill(x + 7, y + 15, x + 11, y + 18, glow);
        guiGraphics.fill(x, y + 7, x + 3, y + 11, glow);
        guiGraphics.fill(x + 15, y + 7, x + 18, y + 11, glow);

        guiGraphics.fill(x + 3, y + 3, x + 5, y + 5, glow);
        guiGraphics.fill(x + 13, y + 3, x + 15, y + 5, glow);
        guiGraphics.fill(x + 3, y + 13, x + 5, y + 15, glow);
        guiGraphics.fill(x + 13, y + 13, x + 15, y + 15, glow);

        guiGraphics.fill(x + 5, y + 5, x + 13, y + 13, glow);
        guiGraphics.fill(x + 6, y + 6, x + 12, y + 12, core);
    }

    private static void drawMoonIcon(GuiGraphics guiGraphics, int x, int y) {
        int moon = 0xFFC5CED7;

        guiGraphics.fill(x + 5, y + 2, x + 11, y + 4, moon);
        guiGraphics.fill(x + 3, y + 4, x + 11, y + 7, moon);
        guiGraphics.fill(x + 2, y + 7, x + 10, y + 13, moon);
        guiGraphics.fill(x + 3, y + 13, x + 11, y + 16, moon);
        guiGraphics.fill(x + 5, y + 16, x + 11, y + 18, moon);

        guiGraphics.fill(x + 8, y + 3, x + 14, y + 6, PANEL_BG);
        guiGraphics.fill(x + 9, y + 6, x + 15, y + 13, PANEL_BG);
        guiGraphics.fill(x + 8, y + 13, x + 14, y + 16, PANEL_BG);
    }
}
