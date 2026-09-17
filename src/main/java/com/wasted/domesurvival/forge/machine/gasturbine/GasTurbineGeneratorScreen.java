package com.wasted.domesurvival.forge.machine.gasturbine;

import com.wasted.domesurvival.forge.client.gui.DomeIndustrialGuiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GasTurbineGeneratorScreen extends AbstractContainerScreen<GasTurbineGeneratorMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 154;
    private static final int GAS = 0xFF5E9CA8;
    private static final int GAS_LIGHT = 0xFF8CC5CF;

    public GasTurbineGeneratorScreen(GasTurbineGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = 1000;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        if (localX >= 20 && localX < 36 && localY >= 47 && localY < 119) {
            graphics.renderTooltip(font,
                    Component.translatable("gui.domesurvival.gas_turbine_generator.energy_tooltip",
                            menu.energyStored(), menu.energyCapacity()), mouseX, mouseY);
        } else if (localX >= 184 && localX < 200 && localY >= 47 && localY < 119) {
            graphics.renderTooltip(font,
                    Component.translatable("gui.domesurvival.gas_turbine_generator.gas_tooltip",
                            menu.gasStored(), menu.gasCapacity()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        DomeIndustrialGuiStyle.drawPanel(graphics, x, y, imageWidth, imageHeight);
        DomeIndustrialGuiStyle.drawFrame(graphics, x + 8, y + 28, 204, 114, DomeIndustrialGuiStyle.PANEL_ALT);

        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 20, y + 47, 16, 72,
                menu.energyStored(), menu.energyCapacity(),
                DomeIndustrialGuiStyle.ENERGY, DomeIndustrialGuiStyle.ENERGY_LIGHT
        );
        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 184, y + 47, 16, 72,
                menu.gasStored(), menu.gasCapacity(), GAS, GAS_LIGHT
        );

        // Turbine rotor: deliberately procedural so this screen needs no new GUI texture.
        int cx = x + 110;
        int cy = y + 79;
        graphics.fill(cx - 28, cy - 28, cx + 28, cy + 28, 0xFF151A1D);
        graphics.fill(cx - 24, cy - 24, cx + 24, cy + 24, 0xFF2B3338);
        graphics.fill(cx - 4, cy - 22, cx + 4, cy + 22, 0xFF687278);
        graphics.fill(cx - 22, cy - 4, cx + 22, cy + 4, 0xFF687278);
        graphics.fill(cx - 7, cy - 7, cx + 7, cy + 7,
                menu.status() == GasTurbineGeneratorBlockEntity.GENERATING
                        ? DomeIndustrialGuiStyle.ENERGY_LIGHT : 0xFF3C464C);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 10, DomeIndustrialGuiStyle.TEXT, false);
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.gas_turbine_generator.energy",
                        menu.energyStored(), menu.energyCapacity()),
                44, 40, DomeIndustrialGuiStyle.TEXT_MUTED, false);
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.gas_turbine_generator.gas",
                        menu.gasStored(), menu.gasCapacity()),
                44, 54, DomeIndustrialGuiStyle.TEXT_MUTED, false);
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.gas_turbine_generator.output", menu.generationPerTick()),
                44, 111, DomeIndustrialGuiStyle.TEXT_MUTED, false);
        graphics.drawCenteredString(font, statusText(), WIDTH / 2, 128, statusColor());
    }

    private Component statusText() {
        return switch (menu.status()) {
            case GasTurbineGeneratorBlockEntity.GENERATING ->
                    Component.translatable("gui.domesurvival.gas_turbine_generator.status.generating");
            case GasTurbineGeneratorBlockEntity.NO_GAS ->
                    Component.translatable("gui.domesurvival.gas_turbine_generator.status.no_gas");
            case GasTurbineGeneratorBlockEntity.ENERGY_FULL ->
                    Component.translatable("gui.domesurvival.gas_turbine_generator.status.energy_full");
            default -> Component.translatable("gui.domesurvival.gas_turbine_generator.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case GasTurbineGeneratorBlockEntity.GENERATING -> DomeIndustrialGuiStyle.ENERGY_LIGHT;
            case GasTurbineGeneratorBlockEntity.NO_GAS -> DomeIndustrialGuiStyle.WARNING;
            case GasTurbineGeneratorBlockEntity.ENERGY_FULL -> DomeIndustrialGuiStyle.TEXT_DIM;
            default -> DomeIndustrialGuiStyle.READY;
        };
    }
}
