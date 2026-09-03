package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.machine.shaft.ShaftFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class ShaftFurnaceScreen extends AbstractContainerScreen<ShaftFurnaceMenu> {
    public ShaftFurnaceScreen(ShaftFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MetallurgyGui.PANEL_WIDTH;
        imageHeight = MetallurgyGui.PANEL_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MetallurgyGui.drawBase(graphics, leftPos, topPos);
        MetallurgyGui.drawShaftSlots(graphics, leftPos, topPos);
        MetallurgyGui.drawProcess(graphics, leftPos, topPos,
                menu.getProgress(), menu.getProgressMax(), menu.getBurnTime(), menu.getBurnTimeMax(),
                menu.getBurnTime() > 0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 9, 0xFFE1E5E7, false);
        graphics.drawString(font, Component.translatable("gui.domesurvival.metallurgy.fuel"),
                MetallurgyGui.FUEL_X, MetallurgyGui.STATUS_LABEL_Y, 0xFFAEB7BB, false);
        drawCentered(graphics, Component.translatable("gui.domesurvival.metallurgy.progress"),
                MetallurgyGui.PROGRESS_X, 43, MetallurgyGui.PROGRESS_W, 0xFFC7CED1);
        drawCentered(graphics, getTimerText(), MetallurgyGui.CHAMBER_X, MetallurgyGui.STATUS_LABEL_Y,
                MetallurgyGui.CHAMBER_W, 0xFFF0D2A4);
        graphics.drawString(font, playerInventoryTitle, 14, 140, 0xFFC7CED1, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(MetallurgyGui.PROGRESS_X, MetallurgyGui.PROGRESS_Y,
                MetallurgyGui.PROGRESS_W, MetallurgyGui.PROGRESS_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.domesurvival.metallurgy.progress_tooltip",
                    getTimerValue()), mouseX, mouseY);
        } else if (isHovering(MetallurgyGui.FUEL_X, MetallurgyGui.FUEL_Y,
                MetallurgyGui.FUEL_W, MetallurgyGui.FUEL_H, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.domesurvival.metallurgy.heat_tooltip",
                    formatTicks(menu.getBurnTime())), mouseX, mouseY);
        }
    }

    private Component getTimerText() {
        String key = menu.getProgress() > 0
                ? "gui.domesurvival.metallurgy.remaining"
                : "gui.domesurvival.metallurgy.cycle_time";
        return Component.translatable(key, getTimerValue());
    }

    private String getTimerValue() {
        int ticks = menu.getProgress() > 0
                ? Math.max(0, menu.getProgressMax() - menu.getProgress())
                : Math.max(0, menu.getProgressMax());
        return formatTicks(ticks);
    }

    private static String formatTicks(int ticks) {
        int seconds = Math.max(0, (ticks + 19) / 20);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void drawCentered(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        String value = font.plainSubstrByWidth(text.getString(), width);
        graphics.drawString(font, value, x + Math.max(0, (width - font.width(value)) / 2), y, color, false);
    }
}
