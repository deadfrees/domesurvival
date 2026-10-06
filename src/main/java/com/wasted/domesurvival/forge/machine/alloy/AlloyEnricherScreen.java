package com.wasted.domesurvival.forge.machine.alloy;

import com.wasted.domesurvival.forge.client.gui.DomeIndustrialGuiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AlloyEnricherScreen extends AbstractContainerScreen<AlloyEnricherMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 266;
    private static final int NEOFLUX = 0xFFC47A43;
    private static final int NEOFLUX_LIGHT = 0xFFE3A06E;

    public AlloyEnricherScreen(AlloyEnricherMenu menu, Inventory inventory, Component title) {
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

        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        if (localX >= 16 && localX < 32 && localY >= 47 && localY < 119) {
            graphics.renderTooltip(font,
                    Component.translatable("gui.domesurvival.alloy_enricher.neoflux_tooltip",
                            menu.neofluxStored(), menu.neofluxCapacity()), mouseX, mouseY);
        } else if (localX >= 38 && localX < 54 && localY >= 47 && localY < 119) {
            graphics.renderTooltip(font,
                    Component.translatable("gui.domesurvival.alloy_enricher.energy_tooltip",
                            menu.energyStored(), menu.energyCapacity()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        DomeIndustrialGuiStyle.drawPanel(graphics, x, y, imageWidth, imageHeight);
        DomeIndustrialGuiStyle.drawFrame(graphics, x + 8, y + 27, 204, 118, DomeIndustrialGuiStyle.PANEL_ALT);

        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 16, y + 47, 16, 72,
                menu.neofluxStored(), menu.neofluxCapacity(), NEOFLUX, NEOFLUX_LIGHT
        );
        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 38, y + 47, 16, 72,
                menu.energyStored(), menu.energyCapacity(),
                DomeIndustrialGuiStyle.ENERGY, DomeIndustrialGuiStyle.ENERGY_LIGHT
        );

        DomeIndustrialGuiStyle.drawSlot(graphics, x + 63, y + 72, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 157, y + 72, true);
        DomeIndustrialGuiStyle.drawProgress(
                graphics, x + 91, y + 74, 50, 14,
                menu.progress(), menu.progressMax(),
                DomeIndustrialGuiStyle.PROCESS, DomeIndustrialGuiStyle.PROCESS_LIGHT
        );

        graphics.fill(x + 83, y + 78, x + 89, y + 81, 0xFF65737A);
        graphics.fill(x + 143, y + 78, x + 151, y + 81, 0xFF65737A);
        graphics.fill(x + 148, y + 75, x + 153, y + 84, 0xFF65737A);
        graphics.fill(x + 151, y + 77, x + 155, y + 82, 0xFF9AB7C2);

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
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.alloy_enricher.neoflux",
                        menu.neofluxStored(), menu.neofluxCapacity()),
                61, 38, NEOFLUX_LIGHT, false);
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.alloy_enricher.energy",
                        menu.energyStored(), menu.energyCapacity()),
                61, 49, DomeIndustrialGuiStyle.ENERGY_LIGHT, false);
        graphics.drawString(font, statusText(), 91, 98, statusColor(), false);
        graphics.drawString(font, Component.translatable("container.inventory"),
                inventoryLabelX, inventoryLabelY, DomeIndustrialGuiStyle.TEXT_MUTED, false);
    }

    private Component statusText() {
        return switch (menu.status()) {
            case AlloyEnricherBlockEntity.ENRICHING ->
                    Component.translatable("gui.domesurvival.alloy_enricher.status.enriching");
            case AlloyEnricherBlockEntity.NO_ENERGY ->
                    Component.translatable("gui.domesurvival.alloy_enricher.status.no_energy");
            case AlloyEnricherBlockEntity.NO_RECIPE ->
                    Component.translatable("gui.domesurvival.alloy_enricher.status.no_recipe");
            case AlloyEnricherBlockEntity.NOT_ENOUGH_INPUT ->
                    Component.translatable("gui.domesurvival.alloy_enricher.status.not_enough_input");
            case AlloyEnricherBlockEntity.NO_NEOFLUX ->
                    Component.translatable("gui.domesurvival.alloy_enricher.status.no_neoflux");
            case AlloyEnricherBlockEntity.OUTPUT_FULL ->
                    Component.translatable("gui.domesurvival.alloy_enricher.status.output_full");
            default -> Component.translatable("gui.domesurvival.alloy_enricher.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case AlloyEnricherBlockEntity.ENRICHING -> DomeIndustrialGuiStyle.PROCESS_LIGHT;
            case AlloyEnricherBlockEntity.NO_ENERGY,
                    AlloyEnricherBlockEntity.NO_NEOFLUX,
                    AlloyEnricherBlockEntity.NOT_ENOUGH_INPUT -> DomeIndustrialGuiStyle.WARNING;
            case AlloyEnricherBlockEntity.OUTPUT_FULL -> DomeIndustrialGuiStyle.ERROR;
            case AlloyEnricherBlockEntity.NO_RECIPE -> DomeIndustrialGuiStyle.TEXT_DIM;
            default -> DomeIndustrialGuiStyle.READY;
        };
    }
}
