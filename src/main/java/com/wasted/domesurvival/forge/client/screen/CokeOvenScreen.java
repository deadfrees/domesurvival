package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.machine.shaft.CokeOvenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CokeOvenScreen extends AbstractContainerScreen<CokeOvenMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");

    public CokeOvenScreen(CokeOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int burn = menu.getBurnPixels();
        if (burn > 0) graphics.blit(TEXTURE, leftPos + 56, topPos + 36 + 12 - burn, 176, 12 - burn, 14, burn + 1);
        int progress = menu.getProgressPixels();
        if (progress > 0) graphics.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 14, progress + 1, 16);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
