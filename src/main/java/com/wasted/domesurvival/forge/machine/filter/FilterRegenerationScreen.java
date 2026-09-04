package com.wasted.domesurvival.forge.machine.filter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FilterRegenerationScreen extends AbstractContainerScreen<FilterRegenerationMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 222;
    private static final int ENERGY_X = 18;
    private static final int ENERGY_Y = 42;
    private static final int ENERGY_W = 18;
    private static final int ENERGY_H = 58;
    private static final int PROGRESS_X = 61;
    private static final int PROGRESS_Y = 91;
    private static final int PROGRESS_W = 78;
    private static final int PROGRESS_H = 8;

    public FilterRegenerationScreen(FilterRegenerationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 12;
        inventoryLabelY = 119;
        titleLabelX = 0;
        titleLabelY = 0;
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
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        panel(graphics, x, y, WIDTH, HEIGHT, 0xFF252D31);
        panel(graphics, x + 8, y + 29, WIDTH - 16, 82, 0xFF182025);
        slot(graphics, x + 60, y + 58);
        slot(graphics, x + 96, y + 58);

        panel(graphics, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H, 0xFF0B1013);
        int capacity = Math.max(1, menu.energyCapacity());
        int fillHeight = Math.min(ENERGY_H - 6,
                (int) ((long) menu.energyStored() * (ENERGY_H - 6) / capacity));
        if (fillHeight > 0) {
            int bottom = y + ENERGY_Y + ENERGY_H - 3;
            graphics.fill(x + ENERGY_X + 3, bottom - fillHeight,
                    x + ENERGY_X + ENERGY_W - 3, bottom, 0xFF4A9A77);
        }

        panel(graphics, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H, 0xFF0B1013);
        int progressMax = Math.max(1, menu.progressMax());
        int progressWidth = (int) ((long) (PROGRESS_W - 6) * menu.progress() / progressMax);
        if (progressWidth > 0) {
            graphics.fill(x + PROGRESS_X + 3, y + PROGRESS_Y + 3,
                    x + PROGRESS_X + 3 + progressWidth, y + PROGRESS_Y + PROGRESS_H - 3, 0xFF63C39A);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(graphics, x + 8 + column * 22, y + 128 + row * 22);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(graphics, x + 8 + column * 22, y + 194);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, WIDTH / 2, 10, 0xFFD6E0E4);
        graphics.drawCenteredString(font, Component.literal("Фильтр"), 71, 43, 0xFFBFC8CC);
        graphics.drawCenteredString(font, Component.literal("Уголь"), 107, 43, 0xFFBFC8CC);
        graphics.drawCenteredString(font, statusText(), 110, 103, statusColor());
        graphics.drawString(font,
                Component.literal("Регенерации: " + menu.regenerationCycles() + " / " + menu.maxRegenerationCycles()),
                145, 62, 0xFFBFC8CC, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFBFC8CC, false);
    }

    private Component statusText() {
        return switch (menu.status()) {
            case FilterRegenerationBlockEntity.STATUS_REGENERATING -> Component.literal("Регенерация");
            case FilterRegenerationBlockEntity.STATUS_NO_ENERGY -> Component.literal("Недостаточно энергии");
            case FilterRegenerationBlockEntity.STATUS_NO_FILTER -> Component.literal("Установите фильтр");
            case FilterRegenerationBlockEntity.STATUS_NO_MEDIA -> Component.literal("Нужен древесный уголь");
            case FilterRegenerationBlockEntity.STATUS_FILTER_HEALTHY -> Component.literal("Фильтр не повреждён");
            case FilterRegenerationBlockEntity.STATUS_EXHAUSTED -> Component.literal("Ресурс регенерации исчерпан");
            default -> Component.literal("Готово к работе");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case FilterRegenerationBlockEntity.STATUS_READY -> 0xFF7ED69A;
            case FilterRegenerationBlockEntity.STATUS_REGENERATING -> 0xFF63C39A;
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
