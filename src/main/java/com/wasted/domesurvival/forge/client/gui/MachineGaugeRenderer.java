package com.wasted.domesurvival.forge.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Fixed-size gauge artwork: changes in level reveal pixels, never rescale them. */
public final class MachineGaugeRenderer {
    private static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(
            "domesurvival", "textures/gui/coal_generator_v2/widgets.png");

    private MachineGaugeRenderer() { }

    private static int filledHeight(int height, int amount, int capacity) {
        if (height <= 0 || amount <= 0 || capacity <= 0) return 0;
        return (int) Math.min(height, (long) height * amount / capacity);
    }

    public static void segmented(GuiGraphics graphics, int x, int y, int width, int height,
                                 int amount, int capacity) {
        int filled = filledHeight(height, amount, capacity);
        if (filled == 0 || width <= 0) return;
        graphics.enableScissor(x, y + height - filled, x + width, y + height);
        graphics.blit(WIDGETS, x, y, width, height, 256, 0, 48, 188, 512, 256);
        graphics.disableScissor();
    }

    public static void fluid(GuiGraphics graphics, int x, int y, int width, int height,
                             int amount, int capacity, Fluid fluid) {
        int filled = filledHeight(height, amount, capacity);
        if (filled == 0 || width <= 0) return;
        var extension = IClientFluidTypeExtensions.of(fluid);
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extension.getStillTexture());
        int tint = extension.getTintColor();
        graphics.setColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F,
                (tint & 255) / 255F, ((tint >>> 24) & 255) / 255F);
        graphics.enableScissor(x, y + height - filled, x + width, y + height);
        // Tile the animated vanilla sprite at its natural scale, cropping edge tiles.
        for (int dy = 0; dy < height; dy += 16) {
            for (int dx = 0; dx < width; dx += 16) {
                graphics.blit(x + dx, y + dy, 0, 16, 16, sprite);
            }
        }
        graphics.disableScissor();
        graphics.setColor(1, 1, 1, 1);
    }
}
