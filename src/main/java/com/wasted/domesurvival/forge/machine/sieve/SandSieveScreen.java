package com.wasted.domesurvival.forge.machine.sieve;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SandSieveScreen extends AbstractContainerScreen<SandSieveMenu> {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 227;

    public SandSieveScreen(SandSieveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 51;
        inventoryLabelY = 124;
        titleLabelX = 0;
        titleLabelY = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(20, 52, 22, 54, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(menu.water() + " / "
                    + menu.waterCapacity() + " mB"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        panel(graphics, x, y, WIDTH, HEIGHT, 0xFF232B30);
        panel(graphics, x + 9, y + 30, WIDTH - 18, 90, 0xFF182025);

        slot(graphics, x + 62, y + 58);
        slot(graphics, x + 100, y + 58);
        for (int i = 0; i < 3; i++) slot(graphics, x + 202 + i * 24, y + 58);

        panel(graphics, x + 20, y + 52, 22, 54, 0xFF10171B);
        int fill = menu.waterCapacity() <= 0 ? 0 : 46 * menu.water() / menu.waterCapacity();
        if (fill > 0) {
            graphics.fill(x + 24, y + 102 - fill, x + 38, y + 102, 0xFF347E91);
            graphics.fill(x + 25, y + 102 - fill, x + 28, y + 102, 0xFF63C5D5);
        }

        panel(graphics, x + 58, y + 91, 210, 8, 0xFF0B1013);
        int progress = menu.progressMax() <= 0 ? 0 : 204 * menu.progress() / menu.progressMax();
        if (progress > 0) {
            graphics.fill(x + 61, y + 94, x + 61 + progress, y + 96, 0xFFB77A3E);
            graphics.fill(x + 61, y + 94, x + 61 + progress, y + 95, 0xFFE3AE61);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) slot(graphics,
                    x + 48 + column * 22, y + 134 + row * 22);
        }
        for (int column = 0; column < 9; column++) slot(graphics,
                x + 48 + column * 22, y + 200);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, WIDTH / 2, 10, 0xFFD6E0E4);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.water"),
                31, 39, 0xFF63C5D5);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.sand"),
                73, 43, 0xFFD3B77A);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.mesh"),
                111, 43, 0xFFBFC8CC);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.output"),
                237, 43, 0xFFBFC8CC);
        graphics.drawCenteredString(font, statusText(), WIDTH / 2, 106, statusColor());
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFBFC8CC, false);
    }

    private Component statusText() {
        String key = switch (menu.status()) {
            case SandSieveBlockEntity.STATUS_READY_DRY -> "ready_dry";
            case SandSieveBlockEntity.STATUS_READY_WET -> "ready_wet";
            case SandSieveBlockEntity.STATUS_RUNNING_DRY -> "running_dry";
            case SandSieveBlockEntity.STATUS_RUNNING_WET -> "running_wet";
            case SandSieveBlockEntity.STATUS_NO_SAND -> "no_sand";
            case SandSieveBlockEntity.STATUS_NO_MESH -> "no_mesh";
            case SandSieveBlockEntity.STATUS_OUTPUT_BLOCKED -> "output_blocked";
            default -> "idle";
        };
        return Component.translatable("gui.domesurvival.sand_sieve.status." + key);
    }

    private int statusColor() {
        return switch (menu.status()) {
            case SandSieveBlockEntity.STATUS_READY_DRY, SandSieveBlockEntity.STATUS_READY_WET -> 0xFF7ED69A;
            case SandSieveBlockEntity.STATUS_RUNNING_DRY, SandSieveBlockEntity.STATUS_RUNNING_WET -> 0xFFE8B86B;
            default -> 0xFFE28A74;
        };
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF0A0E10);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF667278);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, color);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 22, y + 22, 0xFF080B0D);
        graphics.fill(x + 1, y + 1, x + 21, y + 21, 0xFF657177);
        graphics.fill(x + 3, y + 3, x + 19, y + 19, 0xFF11171B);
    }
}
