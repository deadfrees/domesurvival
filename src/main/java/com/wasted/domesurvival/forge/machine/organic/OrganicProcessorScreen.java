package com.wasted.domesurvival.forge.machine.organic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class OrganicProcessorScreen extends AbstractContainerScreen<OrganicProcessorMenu> {
    public OrganicProcessorScreen(OrganicProcessorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1F2328);
        graphics.fill(x + 5, y + 5, x + imageWidth - 5, y + 132, 0xFF30363D);
        graphics.fill(x + 5, y + 135, x + imageWidth - 5, y + imageHeight - 5, 0xFF262B31);

        slot(graphics, x + 42, y + 52);
        slot(graphics, x + 42, y + 80);
        slot(graphics, x + 130, y + 66);
        slot(graphics, x + 81, y + 106);
        slot(graphics, x + 103, y + 106);

        int progressMax = Math.max(1, menu.progressMax());
        int progressWidth = Math.min(56, (int) ((long) menu.progress() * 56L / progressMax));
        graphics.fill(x + 69, y + 66, x + 127, y + 76, 0xFF15181C);
        if (progressWidth > 0) {
            graphics.fill(x + 70, y + 67, x + 70 + progressWidth, y + 75, 0xFF6FA66F);
        }

        int waterCapacity = Math.max(1, menu.waterCapacity());
        int waterHeight = Math.min(54, (int) ((long) menu.waterStored() * 54L / waterCapacity));
        graphics.fill(x + 12, y + 38, x + 21, y + 94, 0xFF111418);
        if (waterHeight > 0) {
            graphics.fill(x + 13, y + 93 - waterHeight, x + 20, y + 93, 0xFF4A90D9);
        }

        int energyCapacity = Math.max(1, menu.energyCapacity());
        int energyHeight = Math.min(54, (int) ((long) menu.energyStored() * 54L / energyCapacity));
        graphics.fill(x + 25, y + 38, x + 34, y + 94, 0xFF111418);
        if (energyHeight > 0) {
            graphics.fill(x + 26, y + 93 - energyHeight, x + 33, y + 93, 0xFFD19A4A);
        }
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 20, y + 20, 0xFF111418);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF4B535C);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Органический процессор"), 8, 9, 0xE6EDF3, false);
        graphics.drawString(font, statusText(), 69, 86, 0xD7DEE7, false);
        graphics.drawString(font,
                Component.literal(menu.waterStored() + "/" + menu.waterCapacity() + " mB"),
                8, 105, 0x9EC7E8, false);
        graphics.drawString(font,
                Component.literal(menu.energyStored() + "/" + menu.energyCapacity() + " FE"),
                8, 116, 0xE1BD7A, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, 129, 0xD7DEE7, false);
    }

    private Component statusText() {
        return switch (menu.status()) {
            case OrganicProcessorBlockEntity.PROCESSING -> Component.literal("Обработка");
            case OrganicProcessorBlockEntity.NO_ENERGY -> Component.literal("Нет энергии");
            case OrganicProcessorBlockEntity.NO_RECIPE -> Component.literal("Нет рецепта");
            case OrganicProcessorBlockEntity.NOT_ENOUGH_INPUT -> Component.literal("Недостаточно ингредиентов");
            case OrganicProcessorBlockEntity.NO_WATER -> Component.literal("Нет очищенной воды");
            case OrganicProcessorBlockEntity.OUTPUT_FULL -> Component.literal("Выход заполнен");
            default -> Component.literal("Готов");
        };
    }
}
