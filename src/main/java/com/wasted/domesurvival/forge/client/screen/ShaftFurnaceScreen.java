package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ShaftFurnaceScreen extends AbstractContainerScreen<ShaftFurnaceMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/furnace.png");

    public ShaftFurnaceScreen(ShaftFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // The vanilla furnace has one output slot. Draw a second matching slot for slag.
        graphics.blit(TEXTURE, x + 115, y + 52, 7, 83, 18, 18);
        graphics.blit(TEXTURE, x + 115, y + 16, 7, 83, 18, 18);

        int burn = menu.getBurnPixels();
        if (burn > 0) {
            graphics.blit(TEXTURE, x + 56, y + 36 + 12 - burn, 176, 12 - burn, 14, burn + 1);
        }
        int progress = menu.getProgressPixels();
        if (progress > 0) {
            graphics.blit(TEXTURE, x + 79, y + 34, 176, 14, progress + 1, 16);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
