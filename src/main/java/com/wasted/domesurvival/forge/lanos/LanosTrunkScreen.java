package com.wasted.domesurvival.forge.lanos;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class LanosTrunkScreen extends AbstractContainerScreen<LanosTrunkMenu> {
    public LanosTrunkScreen(LanosTrunkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 67;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF111519);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF343B40);
        graphics.fill(x + 5, y + 5, x + imageWidth - 5, y + imageHeight - 5, 0xFF171C20);

        graphics.fill(x + 9, y + 19, x + imageWidth - 9, y + 61, 0xFF090C0E);
        graphics.fill(x + 12, y + 22, x + imageWidth - 12, y + 58, 0xFF242A2E);
        graphics.fill(x + 12, y + 22, x + 16, y + 58, 0xFFC27A2C);
        graphics.fill(x + imageWidth - 16, y + 22, x + imageWidth - 12, y + 58, 0xFFC27A2C);

        for (int column = 0; column < 6; column++) drawSlot(graphics, x + 34 + column * 18, y + 34);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlot(graphics, x + 7 + column * 18, y + 78 + row * 18);
        }
        for (int column = 0; column < 9; column++) drawSlot(graphics, x + 7 + column * 18, y + 136);

        for (int[] rivet : new int[][]{{8,8},{166,8},{8,156},{166,156}}) {
            graphics.fill(x + rivet[0], y + rivet[1], x + rivet[0] + 2, y + rivet[1] + 2, 0xFF8B9499);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF050708);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF586166);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF151A1D);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 8, 0xFFE0E5E8);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB7C0C5, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
