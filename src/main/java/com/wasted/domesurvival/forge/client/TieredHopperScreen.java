package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.hopper.TieredHopperMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class TieredHopperScreen extends AbstractContainerScreen<TieredHopperMenu> {
    public TieredHopperScreen(TieredHopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;
        this.imageHeight = 114 + menu.getContainerRows() * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFFC6C6C6);
        graphics.fill(left, top, left + imageWidth, top + 2, 0xFFFFFFFF);
        graphics.fill(left, top + imageHeight - 2, left + imageWidth, top + imageHeight, 0xFF555555);
        graphics.fill(left, top, left + 2, top + imageHeight, 0xFFFFFFFF);
        graphics.fill(left + imageWidth - 2, top, left + imageWidth, top + imageHeight, 0xFF555555);

        for (Slot slot : menu.slots) {
            int x = left + slot.x - 1;
            int y = top + slot.y - 1;

            graphics.fill(x, y, x + 18, y + 18, 0xFF6B6B6B);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFEEEEEE);
            graphics.fill(x + 2, y + 2, x + 17, y + 17, 0xFF8B8B8B);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                0x404040,
                false
        );
    }
}
