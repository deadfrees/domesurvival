package com.wasted.domesurvival.forge.machine.crusher;

import com.wasted.domesurvival.forge.client.gui.DomeIndustrialGuiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class IndustrialCrusherScreen extends AbstractContainerScreen<IndustrialCrusherMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 266;

    public IndustrialCrusherScreen(IndustrialCrusherMenu menu, Inventory inventory, Component title) {
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

        // FE buffer: same amber language as the coal generator, but the crusher only consumes energy.
        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 15, y + 42, 16, 72,
                menu.energyStored(), menu.energyCapacity(),
                DomeIndustrialGuiStyle.ENERGY, DomeIndustrialGuiStyle.ENERGY_LIGHT
        );

        DomeIndustrialGuiStyle.drawSlot(graphics, x + 55, y + 63, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 166, y + 53, true);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 166, y + 83, true);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 86, y + 118, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 116, y + 118, false);

        DomeIndustrialGuiStyle.drawProgress(
                graphics, x + 86, y + 65, 65, 14,
                menu.progress(), menu.progressMax(),
                DomeIndustrialGuiStyle.PROCESS, DomeIndustrialGuiStyle.PROCESS_LIGHT
        );

        // Thin machine-flow guides make input/output direction readable without adding fake controls.
        graphics.fill(x + 76, y + 69, x + 84, y + 72, 0xFF65737A);
        graphics.fill(x + 151, y + 69, x + 160, y + 72, 0xFF65737A);
        graphics.fill(x + 157, y + 66, x + 162, y + 75, 0xFF65737A);
        graphics.fill(x + 160, y + 68, x + 164, y + 73, 0xFF9AB7C2);

        // Player inventory uses the exact 22 px rhythm of CoalGeneratorMenu.
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
                        "gui.domesurvival.industrial_crusher.energy",
                        menu.energyStored(), menu.energyCapacity()
                ),
                40, 42, DomeIndustrialGuiStyle.TEXT_MUTED, false
        );
        graphics.drawString(font, statusText(), 86, 88, statusColor(), false);
        graphics.drawString(font, Component.translatable("container.inventory"),
                inventoryLabelX, inventoryLabelY, DomeIndustrialGuiStyle.TEXT_MUTED, false);
    }

    private int statusColor() {
        return switch (menu.status()) {
            case IndustrialCrusherBlockEntity.CRUSHING -> DomeIndustrialGuiStyle.PROCESS_LIGHT;
            case IndustrialCrusherBlockEntity.NO_ENERGY,
                    IndustrialCrusherBlockEntity.NOT_ENOUGH_INPUT -> DomeIndustrialGuiStyle.WARNING;
            case IndustrialCrusherBlockEntity.OUTPUT_FULL -> DomeIndustrialGuiStyle.ERROR;
            case IndustrialCrusherBlockEntity.NO_RECIPE -> DomeIndustrialGuiStyle.TEXT_DIM;
            default -> DomeIndustrialGuiStyle.READY;
        };
    }

    private Component statusText() {
        return switch (menu.status()) {
            case IndustrialCrusherBlockEntity.CRUSHING ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.crushing");
            case IndustrialCrusherBlockEntity.NO_ENERGY ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.no_energy");
            case IndustrialCrusherBlockEntity.NO_RECIPE ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.no_recipe");
            case IndustrialCrusherBlockEntity.OUTPUT_FULL ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.output_full");
            case IndustrialCrusherBlockEntity.NOT_ENOUGH_INPUT ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.not_enough_input");
            default -> Component.translatable("gui.domesurvival.industrial_crusher.status.ready");
        };
    }
}
