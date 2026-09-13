package com.wasted.domesurvival.forge.machine.organic;

import com.wasted.domesurvival.forge.client.gui.DomeIndustrialGuiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class OrganicProcessorScreen extends AbstractContainerScreen<OrganicProcessorMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 266;

    public OrganicProcessorScreen(OrganicProcessorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 11;
        inventoryLabelY = 149;
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

        DomeIndustrialGuiStyle.drawPanel(graphics, x, y, imageWidth, imageHeight);
        DomeIndustrialGuiStyle.drawFrame(graphics, x + 8, y + 27, 204, 118, DomeIndustrialGuiStyle.PANEL_ALT);

        // Purified water and FE are deliberately separate resources: blue fluid, amber energy.
        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 15, y + 44, 14, 68,
                menu.waterStored(), menu.waterCapacity(),
                DomeIndustrialGuiStyle.FLUID, DomeIndustrialGuiStyle.FLUID_LIGHT
        );
        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 33, y + 44, 14, 68,
                menu.energyStored(), menu.energyCapacity(),
                DomeIndustrialGuiStyle.ENERGY, DomeIndustrialGuiStyle.ENERGY_LIGHT
        );

        DomeIndustrialGuiStyle.drawSlot(graphics, x + 55, y + 58, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 55, y + 88, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 166, y + 73, true);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 86, y + 118, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 116, y + 118, false);

        DomeIndustrialGuiStyle.drawProgress(
                graphics, x + 86, y + 72, 65, 14,
                menu.progress(), menu.progressMax(),
                DomeIndustrialGuiStyle.BIO, DomeIndustrialGuiStyle.BIO_LIGHT
        );

        graphics.fill(x + 76, y + 77, x + 84, y + 80, 0xFF65737A);
        graphics.fill(x + 151, y + 77, x + 160, y + 80, 0xFF65737A);
        graphics.fill(x + 157, y + 74, x + 162, y + 83, 0xFF65737A);
        graphics.fill(x + 160, y + 76, x + 164, y + 81, 0xFF9AB7C2);

        graphics.fill(x + 8, y + 151, x + 212, y + 152, 0xFF14181B);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                DomeIndustrialGuiStyle.drawInventoryCell(graphics, x + 14 + col * 22, y + 161 + row * 22);
            }
        }
        for (int col = 0; col < 9; col++) {
            DomeIndustrialGuiStyle.drawInventoryCell(graphics, x + 14 + col * 22, y + 229);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 10, DomeIndustrialGuiStyle.TEXT, false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.domesurvival.organic_processor.water",
                        menu.waterStored(), menu.waterCapacity()
                ),
                53, 33, DomeIndustrialGuiStyle.FLUID_LIGHT, false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.domesurvival.organic_processor.energy",
                        menu.energyStored(), menu.energyCapacity()
                ),
                53, 44, DomeIndustrialGuiStyle.ENERGY_LIGHT, false
        );
        graphics.drawString(font, statusText(), 86, 96, statusColor(), false);
        graphics.drawString(font, Component.translatable("container.inventory"),
                inventoryLabelX, inventoryLabelY, DomeIndustrialGuiStyle.TEXT_MUTED, false);
    }

    private int statusColor() {
        return switch (menu.status()) {
            case OrganicProcessorBlockEntity.PROCESSING -> DomeIndustrialGuiStyle.BIO_LIGHT;
            case OrganicProcessorBlockEntity.NO_ENERGY,
                    OrganicProcessorBlockEntity.NO_WATER,
                    OrganicProcessorBlockEntity.NOT_ENOUGH_INPUT -> DomeIndustrialGuiStyle.WARNING;
            case OrganicProcessorBlockEntity.OUTPUT_FULL -> DomeIndustrialGuiStyle.ERROR;
            case OrganicProcessorBlockEntity.NO_RECIPE -> DomeIndustrialGuiStyle.TEXT_DIM;
            default -> DomeIndustrialGuiStyle.READY;
        };
    }

    private Component statusText() {
        return switch (menu.status()) {
            case OrganicProcessorBlockEntity.PROCESSING ->
                    Component.translatable("gui.domesurvival.organic_processor.status.processing");
            case OrganicProcessorBlockEntity.NO_ENERGY ->
                    Component.translatable("gui.domesurvival.organic_processor.status.no_energy");
            case OrganicProcessorBlockEntity.NO_RECIPE ->
                    Component.translatable("gui.domesurvival.organic_processor.status.no_recipe");
            case OrganicProcessorBlockEntity.NOT_ENOUGH_INPUT ->
                    Component.translatable("gui.domesurvival.organic_processor.status.not_enough_input");
            case OrganicProcessorBlockEntity.NO_WATER ->
                    Component.translatable("gui.domesurvival.organic_processor.status.no_water");
            case OrganicProcessorBlockEntity.OUTPUT_FULL ->
                    Component.translatable("gui.domesurvival.organic_processor.status.output_full");
            default -> Component.translatable("gui.domesurvival.organic_processor.status.ready");
        };
    }
}
