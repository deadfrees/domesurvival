package com.wasted.domesurvival.forge.machine.filter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filter Regeneration Station UI.
 *
 * <p>Service-cassette prototype: the two existing slots and every menu coordinate
 * are preserved. The filter is repaired in slot I; slot II supplies the media.</p>
 */
public final class FilterRegenerationScreen extends AbstractContainerScreen<FilterRegenerationMenu> {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 266;

    private static final int ENERGY_METER_X = 14;
    private static final int ENERGY_METER_Y = 37;
    private static final int ENERGY_METER_W = 18;
    private static final int ENERGY_METER_H = 53;

    private static final int ENERGY_VALUE_X = 42;
    private static final int ENERGY_VALUE_Y = 38;
    private static final int ENERGY_VALUE_W = 166;
    private static final int ENERGY_VALUE_H = 15;

    private static final int PROCESS_BAR_X = 14;
    private static final int PROCESS_BAR_Y = 108;
    private static final int PROCESS_BAR_W = 125;
    private static final int PROCESS_BAR_H = 14;

    private static final int FILTER_SLOT_BG_X = 146;
    private static final int FILTER_SLOT_BG_Y = 102;
    private static final int MEDIA_SLOT_BG_X = 178;
    private static final int MEDIA_SLOT_BG_Y = 102;
    private static final int MACHINE_SLOT_BG_SIZE = 24;

    private static final int INVENTORY_X = 11;
    private static final int INVENTORY_Y = 158;
    private static final int INVENTORY_SLOT_SIZE = 22;
    private static final int INVENTORY_SLOT_STEP = 22;
    private static final int HOTBAR_Y = 226;

    public FilterRegenerationScreen(FilterRegenerationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - PANEL_WIDTH) / 2;
        topPos = (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(
                ENERGY_METER_X, ENERGY_METER_Y,
                ENERGY_METER_W, ENERGY_METER_H,
                mouseX, mouseY
        ) || isHovering(
                ENERGY_VALUE_X, ENERGY_VALUE_Y,
                ENERGY_VALUE_W, ENERGY_VALUE_H,
                mouseX, mouseY
        )) {
            guiGraphics.renderTooltip(
                    font,
                    Component.literal("Энергия: " + menu.energyStored() + " / " + menu.energyCapacity() + " FE"),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (isHovering(PROCESS_BAR_X, PROCESS_BAR_Y, PROCESS_BAR_W, PROCESS_BAR_H, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Регенерация фильтра"));
            tooltip.add(Component.literal("Прогресс: " + menu.progress() + " / " + menu.progressMax() + " тиков"));
            tooltip.add(Component.literal("Расход: " + FilterRegenerationBlockEntity.ENERGY_PER_REGENERATION + " FE"));
            tooltip.add(Component.literal("Восстановление: 25% ресурса"));
            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
            return;
        }

        // Keep vanilla item/durability tooltips when a slot contains an item.
        if (!menu.getSlot(0).hasItem() && isHovering(FILTER_SLOT_BG_X, FILTER_SLOT_BG_Y,
                MACHINE_SLOT_BG_SIZE, MACHINE_SLOT_BG_SIZE, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("I: фильтр восстанавливается в этом же слоте"), mouseX, mouseY);
            return;
        }

        if (!menu.getSlot(1).hasItem() && isHovering(MEDIA_SLOT_BG_X, MEDIA_SLOT_BG_Y,
                MACHINE_SLOT_BG_SIZE, MACHINE_SLOT_BG_SIZE, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("II: регенерационный сорбент — расходуется"), mouseX, mouseY);
            return;
        }
        if (isHovering(14, 126, 125, font.lineHeight, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, statusText(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawIndustrialPanel(guiGraphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, 0xFF353E3D);
        // Inset header, service dividers and restrained resource identification.
        guiGraphics.fill(x + 5, y + 5, x + PANEL_WIDTH - 5, y + 20, 0xFF1B2324);
        guiGraphics.fill(x + 10, y + 20, x + PANEL_WIDTH - 10, y + 21, 0xFF65716B);

        // Energy section.
        drawThinFrame(
                guiGraphics,
                x + ENERGY_METER_X,
                y + ENERGY_METER_Y,
                ENERGY_METER_W,
                ENERGY_METER_H,
                0xFF14191C
        );
        drawThinFrame(
                guiGraphics,
                x + ENERGY_VALUE_X,
                y + ENERGY_VALUE_Y,
                ENERGY_VALUE_W,
                ENERGY_VALUE_H,
                0xFF151A1D
        );

        int capacity = Math.max(1, menu.energyCapacity());
        int energyInnerHeight = ENERGY_METER_H - 6;
        int energyHeight = Math.max(0, Math.min(
                energyInnerHeight,
                (int) ((long) menu.energyStored() * energyInnerHeight / capacity)
        ));
        if (energyHeight > 0) {
            int fillBottom = y + ENERGY_METER_Y + ENERGY_METER_H - 3;
            int fillTop = fillBottom - energyHeight;

            // Same amber energy color as the coal generator.
            guiGraphics.fill(
                    x + ENERGY_METER_X + 3,
                    fillTop,
                    x + ENERGY_METER_X + ENERGY_METER_W - 3,
                    fillBottom,
                    0xFF8D792A
            );
            guiGraphics.fill(
                    x + ENERGY_METER_X + 4,
                    fillTop,
                    x + ENERGY_METER_X + 6,
                    fillBottom,
                    0xFFAA9438
            );
        }

        // Regeneration work section: same construction as the generator fuel row.
        drawThinFrame(
                guiGraphics,
                x + PROCESS_BAR_X,
                y + PROCESS_BAR_Y,
                PROCESS_BAR_W,
                PROCESS_BAR_H,
                0xFF151A1D
        );
        drawSlot(guiGraphics, x + FILTER_SLOT_BG_X, y + FILTER_SLOT_BG_Y, MACHINE_SLOT_BG_SIZE);
        drawSlot(guiGraphics, x + MEDIA_SLOT_BG_X, y + MEDIA_SLOT_BG_Y, MACHINE_SLOT_BG_SIZE);

        int progressMax = Math.max(1, menu.progressMax());
        int processWidth = Math.max(0, Math.min(
                PROCESS_BAR_W - 6,
                (int) ((long) menu.progress() * (PROCESS_BAR_W - 6) / progressMax)
        ));
        if (processWidth > 0) {
            // Green process accent distinguishes regeneration from combustion,
            // while the frame geometry stays exactly in the generator family.
            guiGraphics.fill(
                    x + PROCESS_BAR_X + 3,
                    y + PROCESS_BAR_Y + 3,
                    x + PROCESS_BAR_X + 3 + processWidth,
                    y + PROCESS_BAR_Y + PROCESS_BAR_H - 3,
                    0xFF2E7655
            );
            guiGraphics.fill(
                    x + PROCESS_BAR_X + 3,
                    y + PROCESS_BAR_Y + 4,
                    x + PROCESS_BAR_X + 3 + processWidth,
                    y + PROCESS_BAR_Y + 6,
                    0xFF48A975
            );
        }

        // Same divider and player inventory geometry as CoalGeneratorScreen.
        guiGraphics.fill(x + 10, y + 153, x + PANEL_WIDTH - 10, y + 154, 0xFF171B1F);
        guiGraphics.fill(x + 10, y + 154, x + PANEL_WIDTH - 10, y + 155, 0xFF4B5359);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(
                        guiGraphics,
                        x + INVENTORY_X + column * INVENTORY_SLOT_STEP,
                        y + INVENTORY_Y + row * INVENTORY_SLOT_STEP,
                        INVENTORY_SLOT_SIZE
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(
                    guiGraphics,
                    x + INVENTORY_X + column * INVENTORY_SLOT_STEP,
                    y + HOTBAR_Y,
                    INVENTORY_SLOT_SIZE
            );
        }
        // Slot I is both input and restored result; this is not a third output slot.
        guiGraphics.fill(x + 10, y + 250, x + PANEL_WIDTH - 10, y + 251, 0xFF65716B);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawClampedText(guiGraphics, title, 10, 8, PANEL_WIDTH - 20, 0xFFE0E4E6);
        drawCenteredClampedText(guiGraphics, Component.literal("I"), 146, 94, 24, 0xFF9DC2A4);
        drawCenteredClampedText(guiGraphics, Component.literal("II"), 178, 94, 24, 0xFFC5A252);

        drawClampedText(
                guiGraphics,
                Component.literal("Энергия"),
                14,
                24,
                PANEL_WIDTH - 28,
                0xFFC5CBCD
        );
        drawClampedText(guiGraphics, Component.literal("I — фильтр · II — сорбент"),
                10, 254, PANEL_WIDTH - 20, 0xFFACB7AF);

        drawCenteredClampedText(
                guiGraphics,
                Component.literal(compactFe(menu.energyStored()) + " / " + compactFe(menu.energyCapacity()) + " FE"),
                ENERGY_VALUE_X + 3,
                ENERGY_VALUE_Y + 4,
                ENERGY_VALUE_W - 6,
                0xFFB9A246
        );

        drawClampedText(
                guiGraphics,
                Component.literal("Потребление: " + FilterRegenerationBlockEntity.ENERGY_PER_TICK + " FE/t"),
                42,
                58,
                166,
                0xFFC1C7CA
        );
        drawClampedText(
                guiGraphics,
                Component.literal("Цикл: " + FilterRegenerationBlockEntity.ENERGY_PER_REGENERATION + " FE / 10 сек."),
                42,
                70,
                166,
                0xFFC1C7CA
        );

        drawClampedText(
                guiGraphics,
                Component.literal("Фильтр + сорбент"),
                14,
                94,
                125,
                0xFFC5CBCD
        );

        drawClampedText(
                guiGraphics,
                statusText(),
                14,
                126,
                125,
                statusColor()
        );

        drawCenteredClampedText(
                guiGraphics,
                Component.literal(menu.regenerationCycles() + " / " + menu.maxRegenerationCycles()),
                146,
                130,
                56,
                0xFFA9B1B5
        );

        drawClampedText(
                guiGraphics,
                playerInventoryTitle,
                14,
                140,
                194,
                0xFFC5CBCD
        );
    }

    private Component statusText() {
        return switch (menu.status()) {
            case FilterRegenerationBlockEntity.STATUS_REGENERATING -> Component.literal("Регенерация...");
            case FilterRegenerationBlockEntity.STATUS_NO_ENERGY -> Component.literal("Недостаточно энергии");
            case FilterRegenerationBlockEntity.STATUS_NO_FILTER -> Component.literal("Нет фильтра");
            case FilterRegenerationBlockEntity.STATUS_NO_MEDIA -> Component.literal("Нет сорбента");
            case FilterRegenerationBlockEntity.STATUS_FILTER_HEALTHY -> Component.literal("Фильтр исправен");
            case FilterRegenerationBlockEntity.STATUS_EXHAUSTED -> Component.literal("Лимит исчерпан");
            default -> Component.literal("Готово к работе");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case FilterRegenerationBlockEntity.STATUS_READY -> 0xFF9AB99F;
            case FilterRegenerationBlockEntity.STATUS_REGENERATING -> 0xFF79B98F;
            case FilterRegenerationBlockEntity.STATUS_FILTER_HEALTHY -> 0xFFA9B1B5;
            default -> 0xFFC98268;
        };
    }

    private static void drawIndustrialPanel(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0C0F11);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF464E53);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF50585D);
        guiGraphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFF14181B);
    }

    private static void drawThinFrame(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0B0E10);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4C555A);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y, int size) {
        int contentSize = 16;
        int inset = Math.max(2, (size - contentSize) / 2);

        guiGraphics.fill(x, y, x + size, y + size, 0xFF0D1012);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF65716B);
        guiGraphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF303A3B);
        guiGraphics.fill(
                x + inset,
                y + inset,
                x + inset + contentSize,
                y + inset + contentSize,
                0xFF1B2125
        );
    }

    private void drawClampedText(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        String value = text.getString();
        guiGraphics.enableScissor(
                leftPos + x,
                topPos + y,
                leftPos + x + maxWidth,
                topPos + y + font.lineHeight + 1
        );

        if (font.width(value) <= maxWidth) {
            guiGraphics.drawString(font, value, x, y, color, false);
        } else {
            String dots = "...";
            int usableWidth = Math.max(0, maxWidth - font.width(dots));
            String clipped = font.plainSubstrByWidth(value, usableWidth);
            guiGraphics.drawString(font, clipped + dots, x, y, color, false);
        }

        guiGraphics.disableScissor();
    }

    private void drawCenteredClampedText(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        String value = text.getString();

        if (font.width(value) > maxWidth) {
            String dots = "...";
            int usableWidth = Math.max(0, maxWidth - font.width(dots));
            value = font.plainSubstrByWidth(value, usableWidth) + dots;
        }

        int drawX = x + Math.max(0, (maxWidth - font.width(value)) / 2);

        guiGraphics.enableScissor(
                leftPos + x,
                topPos + y,
                leftPos + x + maxWidth,
                topPos + y + font.lineHeight + 1
        );
        guiGraphics.drawString(font, value, drawX, y, color, false);
        guiGraphics.disableScissor();
    }

    private static String compactFe(int value) {
        if (value < 1_000) {
            return Integer.toString(value);
        }
        if (value % 1_000 == 0) {
            return (value / 1_000) + "k";
        }
        return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
    }
}
