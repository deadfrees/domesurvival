package com.wasted.domesurvival.forge.machine.transformer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TransformerScreen extends AbstractContainerScreen<TransformerMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 128;
    private static final int ENERGY_X = 18;
    private static final int ENERGY_Y = 35;
    private static final int ENERGY_W = 18;
    private static final int ENERGY_H = 70;
    private static final int MODE_X = 54;
    private static final int MODE_Y = 76;
    private static final int MODE_W = 140;
    private static final int MODE_H = 18;

    public TransformerScreen(TransformerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        titleLabelX = 0;
        titleLabelY = 0;
        inventoryLabelX = 0;
        inventoryLabelY = 10_000;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            graphics.renderTooltip(font,
                    Component.literal("Энергия: " + menu.energyStored() + " / " + menu.energyCapacity() + " FE"),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null
                && isHovering(MODE_X, MODE_Y, MODE_W, MODE_H, mouseX, mouseY)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TransformerMenu.modeButtonId());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        panel(graphics, x, y, WIDTH, HEIGHT, 0xFF252D31);
        panel(graphics, x + 8, y + 28, WIDTH - 16, 88, 0xFF182025);

        panel(graphics, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H, 0xFF0B1013);
        int capacity = Math.max(1, menu.energyCapacity());
        int fillHeight = Math.min(ENERGY_H - 6,
                (int) ((long) menu.energyStored() * (ENERGY_H - 6) / capacity));
        if (fillHeight > 0) {
            int bottom = y + ENERGY_Y + ENERGY_H - 3;
            graphics.fill(x + ENERGY_X + 3, bottom - fillHeight,
                    x + ENERGY_X + ENERGY_W - 3, bottom, 0xFF8858C8);
        }

        panel(graphics, x + MODE_X, y + MODE_Y, MODE_W, MODE_H, 0xFF11171B);
        if (isHovering(MODE_X, MODE_Y, MODE_W, MODE_H, mouseX, mouseY)) {
            graphics.fill(x + MODE_X + 3, y + MODE_Y + 3,
                    x + MODE_X + MODE_W - 3, y + MODE_Y + MODE_H - 3, 0xFF39434A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, WIDTH / 2, 10, 0xFFD6E0E4);
        graphics.drawString(font, Component.literal("Вход (тыл): " + menu.inputRate() + " FE/t"),
                54, 39, 0xFF6EB6E5, false);
        graphics.drawString(font, Component.literal("Выход (фронт): " + menu.outputRate() + " FE/t"),
                54, 53, 0xFFE3A36B, false);
        graphics.drawString(font,
                Component.literal("Сейчас: +" + menu.inputThisTick() + " / -" + menu.outputThisTick() + " FE"),
                54, 65, 0xFFBFC8CC, false);
        graphics.drawCenteredString(font, Component.literal(modeLabel()),
                MODE_X + MODE_W / 2, MODE_Y + 5, 0xFFD9E1E4);
        graphics.drawCenteredString(font, Component.literal("Нажмите для смены режима"),
                124, 102, 0xFF8F9A9F);
    }

    private String modeLabel() {
        return switch (menu.mode()) {
            case LV_TO_MV -> "LV → MV";
            case MV_TO_LV -> "MV → LV";
            case MV_TO_HV -> "MV → HV";
            case HV_TO_MV -> "HV → MV";
        };
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF0A0E10);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF667278);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, color);
    }
}
