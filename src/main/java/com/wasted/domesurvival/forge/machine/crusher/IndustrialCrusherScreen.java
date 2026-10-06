package com.wasted.domesurvival.forge.machine.crusher;

import com.wasted.domesurvival.forge.client.gui.DomeIndustrialGuiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class IndustrialCrusherScreen extends AbstractContainerScreen<IndustrialCrusherMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 266;

    private static final int TAB_X = 224;
    private static final int TAB_Y = 8;
    private static final int TAB_SIZE = 20;
    private static final int MODULE_PANEL_X = 248;
    private static final int MODULE_PANEL_WIDTH = 96;
    private static final int MODULE_PANEL_HEIGHT = 112;

    private static final int GAS_X = 42;
    private static final int GAS_Y = 122;
    private static final int GAS_W = 163;
    private static final int GAS_H = 10;

    public IndustrialCrusherScreen(IndustrialCrusherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 11;
        inventoryLabelY = 149;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, TAB_X, TAB_Y, TAB_SIZE, TAB_SIZE)) {
            setModulePanelOpen(!menu.isModulePanelOpen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setModulePanelOpen(boolean open) {
        if (menu.isModulePanelOpen() == open) return;
        menu.setModulePanelOpen(open);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    open
                            ? IndustrialCrusherMenu.MODULE_PANEL_OPEN_BUTTON_ID
                            : IndustrialCrusherMenu.MODULE_PANEL_CLOSE_BUTTON_ID
            );
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (inside(mouseX, mouseY, TAB_X, TAB_Y, TAB_SIZE, TAB_SIZE)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.domesurvival.upgrade_modules.tooltip"),
                    mouseX,
                    mouseY
            );
        } else if (inside(mouseX, mouseY, GAS_X, GAS_Y - 12, GAS_W, GAS_H + 14)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.industrial_crusher.process_tooltip",
                            processName(), menu.processStored(), menu.processCapacity()
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        DomeIndustrialGuiStyle.drawPanel(graphics, x, y, imageWidth, imageHeight);
        DomeIndustrialGuiStyle.drawFrame(graphics, x + 8, y + 27, 204, 118, DomeIndustrialGuiStyle.PANEL_ALT);

        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + 15, y + 42, 16, 72,
                menu.energyStored(), menu.energyCapacity(),
                DomeIndustrialGuiStyle.ENERGY, DomeIndustrialGuiStyle.ENERGY_LIGHT
        );

        DomeIndustrialGuiStyle.drawSlot(graphics, x + 55, y + 63, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 166, y + 53, true);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 166, y + 83, true);

        DomeIndustrialGuiStyle.drawProgress(
                graphics, x + 86, y + 65, 65, 14,
                menu.progress(), menu.progressMax(),
                DomeIndustrialGuiStyle.PROCESS, DomeIndustrialGuiStyle.PROCESS_LIGHT
        );

        // Logical process tank: it contains either Mineral Gas or Neoflux, never both.
        DomeIndustrialGuiStyle.drawFrame(graphics, x + GAS_X, y + GAS_Y, GAS_W, GAS_H, 0xFF11171A);
        int processInner = GAS_W - 4;
        int processFill = menu.processCapacity() <= 0 ? 0
                : (int) Math.min(processInner, (long) menu.processStored() * processInner / menu.processCapacity());
        if (processFill > 0) {
            graphics.fill(x + GAS_X + 2, y + GAS_Y + 2,
                    x + GAS_X + 2 + processFill, y + GAS_Y + GAS_H - 2, processFillColor());
        }

        graphics.fill(x + 76, y + 69, x + 84, y + 72, 0xFF65737A);
        graphics.fill(x + 151, y + 69, x + 160, y + 72, 0xFF65737A);
        graphics.fill(x + 157, y + 66, x + 162, y + 75, 0xFF65737A);
        graphics.fill(x + 160, y + 68, x + 164, y + 73, 0xFF9AB7C2);

        graphics.fill(x + 8, y + 151, x + 212, y + 152, 0xFF14181B);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                DomeIndustrialGuiStyle.drawInventoryCell(graphics, x + 14 + col * 22, y + 161 + row * 22);
            }
        }
        for (int col = 0; col < 9; col++) {
            DomeIndustrialGuiStyle.drawInventoryCell(graphics, x + 14 + col * 22, y + 229);
        }

        drawModuleTab(graphics, x + TAB_X, y + TAB_Y, menu.isModulePanelOpen());

        if (menu.isModulePanelOpen()) {
            DomeIndustrialGuiStyle.drawPanel(
                    graphics, x + MODULE_PANEL_X, y,
                    MODULE_PANEL_WIDTH, MODULE_PANEL_HEIGHT
            );
            DomeIndustrialGuiStyle.drawFrame(
                    graphics, x + MODULE_PANEL_X + 8, y + 27,
                    MODULE_PANEL_WIDTH - 16, MODULE_PANEL_HEIGHT - 35,
                    DomeIndustrialGuiStyle.PANEL_ALT
            );
            DomeIndustrialGuiStyle.drawSlot(
                    graphics,
                    x + IndustrialCrusherMenu.MODULE_SLOT_X,
                    y + IndustrialCrusherMenu.MODULE_SLOT_0_Y,
                    false
            );
            DomeIndustrialGuiStyle.drawSlot(
                    graphics,
                    x + IndustrialCrusherMenu.MODULE_SLOT_X,
                    y + IndustrialCrusherMenu.MODULE_SLOT_1_Y,
                    false
            );
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
        graphics.drawString(font, processName(),
                GAS_X, GAS_Y - 11, DomeIndustrialGuiStyle.TEXT_MUTED, false);
        Component processAmount = Component.translatable(
                "gui.domesurvival.industrial_crusher.process_amount",
                menu.processStored(), menu.processCapacity());
        graphics.drawString(font, processAmount, GAS_X + GAS_W - font.width(processAmount),
                GAS_Y - 11, DomeIndustrialGuiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("container.inventory"),
                inventoryLabelX, inventoryLabelY, DomeIndustrialGuiStyle.TEXT_MUTED, false);

        if (menu.isModulePanelOpen()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.domesurvival.upgrade_modules"),
                    MODULE_PANEL_X + MODULE_PANEL_WIDTH / 2,
                    10,
                    DomeIndustrialGuiStyle.TEXT
            );
        }
    }

    private void drawModuleTab(GuiGraphics graphics, int x, int y, boolean active) {
        int fill = active ? 0xFF334038 : DomeIndustrialGuiStyle.PANEL_ALT;
        DomeIndustrialGuiStyle.drawFrame(graphics, x, y, TAB_SIZE, TAB_SIZE, fill);

        int metal = active ? DomeIndustrialGuiStyle.BIO_LIGHT : 0xFF687278;
        graphics.fill(x + 6, y + 5, x + 14, y + 15, metal);
        graphics.fill(x + 8, y + 3, x + 12, y + 17, metal);
        graphics.fill(x + 4, y + 8, x + 16, y + 12, metal);
        graphics.fill(x + 8, y + 7, x + 12, y + 13, 0xFF151A1D);
    }

    private Component processName() {
        return switch (menu.processType()) {
            case IndustrialCrusherBlockEntity.PROCESS_MINERAL_GAS ->
                    Component.translatable("gas.domesurvival.mineral_gas");
            case IndustrialCrusherBlockEntity.PROCESS_NEOFLUX ->
                    Component.translatable("fluid.domesurvival.neoflux");
            default -> Component.translatable("gui.domesurvival.industrial_crusher.process_tank");
        };
    }

    private int processFillColor() {
        return switch (menu.processType()) {
            case IndustrialCrusherBlockEntity.PROCESS_NEOFLUX -> 0xFFC47A43;
            case IndustrialCrusherBlockEntity.PROCESS_MINERAL_GAS -> 0xFF5E9CA8;
            default -> 0xFF5E9CA8;
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case IndustrialCrusherBlockEntity.CRUSHING -> DomeIndustrialGuiStyle.PROCESS_LIGHT;
            case IndustrialCrusherBlockEntity.NO_ENERGY,
                    IndustrialCrusherBlockEntity.NOT_ENOUGH_INPUT -> DomeIndustrialGuiStyle.WARNING;
            case IndustrialCrusherBlockEntity.OUTPUT_FULL,
                    IndustrialCrusherBlockEntity.PROCESS_TANK_BLOCKED -> DomeIndustrialGuiStyle.ERROR;
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
            case IndustrialCrusherBlockEntity.PROCESS_TANK_BLOCKED ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.process_tank_blocked");
            default -> Component.translatable("gui.domesurvival.industrial_crusher.status.ready");
        };
    }
}
