package com.wasted.domesurvival.forge.machine.organic;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.gui.DomeIndustrialGuiStyle;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public final class OrganicProcessorScreen extends AbstractContainerScreen<OrganicProcessorMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    private static final int WIDTH = 220;
    private static final int HEIGHT = 266;

    private static final int WATER_X = 15;
    private static final int WATER_Y = 44;
    private static final int WATER_W = 14;
    private static final int WATER_H = 68;
    private static final int ENERGY_X = 33;
    private static final int ENERGY_Y = 44;
    private static final int ENERGY_W = 14;
    private static final int ENERGY_H = 68;

    private static final int TAB_X = 224;
    private static final int SIDE_TAB_Y = 8;
    private static final int MODULE_TAB_Y = 32;
    private static final int TAB_SIZE = 20;

    private static final int SIDE_PANEL_X = 248;
    private static final int SIDE_PANEL_WIDTH = 96;
    private static final int SIDE_PANEL_HEIGHT = 122;
    private static final int SIDE_BUTTON_SIZE = 14;
    private static final int SIDE_GRID_STEP = 22;
    private static final EnumMap<RelativeSide, Rect> SIDE_RECTS = createSideRects();

    private static final int PORT_SIZE = 6;
    private static final int PORT_TEX_WIDTH = 24;
    private static final int PORT_TEX_HEIGHT = 6;
    private static final int PORT_OFF_U = 0;
    private static final int PORT_INPUT_U = 12;
    private static final int PORT_OUTPUT_U = 18;

    private boolean sidePanelOpen;

    public OrganicProcessorScreen(OrganicProcessorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 11;
        inventoryLabelY = 149;
    }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> regions = new EnumMap<>(RelativeSide.class);
        int centerX = SIDE_PANEL_X + (SIDE_PANEL_WIDTH - SIDE_BUTTON_SIZE) / 2;
        int middleY = 66;
        regions.put(RelativeSide.TOP, new Rect(centerX, middleY - SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.LEFT, new Rect(centerX - SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.FRONT, new Rect(centerX, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.RIGHT, new Rect(centerX + SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BOTTOM, new Rect(centerX, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BACK, new Rect(centerX + SIDE_GRID_STEP, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        return regions;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, TAB_X, SIDE_TAB_Y, TAB_SIZE, TAB_SIZE)) {
                if (menu.isModulePanelOpen()) setModulePanelOpen(false);
                sidePanelOpen = !sidePanelOpen;
                return true;
            }

            if (inside(mouseX, mouseY, TAB_X, MODULE_TAB_Y, TAB_SIZE, TAB_SIZE)) {
                sidePanelOpen = false;
                setModulePanelOpen(!menu.isModulePanelOpen());
                return true;
            }

            if (sidePanelOpen) {
                RelativeSide side = getHoveredSide(mouseX, mouseY);
                if (side != null && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            OrganicProcessorMenu.sideButtonId(machineSideForVisualSide(side))
                    );
                    return true;
                }
            }
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
                            ? OrganicProcessorMenu.MODULE_PANEL_OPEN_BUTTON_ID
                            : OrganicProcessorMenu.MODULE_PANEL_CLOSE_BUTTON_ID
            );
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }

    private static RelativeSide machineSideForVisualSide(RelativeSide visualSide) {
        return switch (visualSide) {
            case LEFT -> RelativeSide.RIGHT;
            case RIGHT -> RelativeSide.LEFT;
            default -> visualSide;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(WATER_X, WATER_Y, WATER_W, WATER_H, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.organic_processor.water_tooltip",
                            menu.waterStored(), menu.waterCapacity()
                    ),
                    mouseX, mouseY
            );
            return;
        }

        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.organic_processor.energy_tooltip",
                            menu.energyStored(), menu.energyCapacity()
                    ),
                    mouseX, mouseY
            );
            return;
        }

        if (inside(mouseX, mouseY, TAB_X, SIDE_TAB_Y, TAB_SIZE, TAB_SIZE)) {
            graphics.renderTooltip(font, Component.translatable("gui.domesurvival.side_config"), mouseX, mouseY);
            return;
        }

        if (inside(mouseX, mouseY, TAB_X, MODULE_TAB_Y, TAB_SIZE, TAB_SIZE)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.domesurvival.upgrade_modules.tooltip"),
                    mouseX, mouseY
            );
            return;
        }

        if (sidePanelOpen) {
            RelativeSide hoveredSide = getHoveredSide(mouseX, mouseY);
            if (hoveredSide != null) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(sideTranslationKey(hoveredSide)));
                tooltip.add(getSideModeTooltip(menu.getSideMode(machineSideForVisualSide(hoveredSide))));
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        DomeIndustrialGuiStyle.drawPanel(graphics, x, y, imageWidth, imageHeight);
        DomeIndustrialGuiStyle.drawFrame(graphics, x + 8, y + 27, 204, 118, DomeIndustrialGuiStyle.PANEL_ALT);

        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + WATER_X, y + WATER_Y, WATER_W, WATER_H,
                menu.waterStored(), menu.waterCapacity(),
                DomeIndustrialGuiStyle.FLUID, DomeIndustrialGuiStyle.FLUID_LIGHT
        );
        DomeIndustrialGuiStyle.drawVerticalMeter(
                graphics, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H,
                menu.energyStored(), menu.energyCapacity(),
                DomeIndustrialGuiStyle.ENERGY, DomeIndustrialGuiStyle.ENERGY_LIGHT
        );

        DomeIndustrialGuiStyle.drawSlot(graphics, x + 55, y + 58, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 55, y + 88, false);
        DomeIndustrialGuiStyle.drawSlot(graphics, x + 166, y + 73, true);

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

        drawGearTab(graphics, x + TAB_X, y + SIDE_TAB_Y, sidePanelOpen);
        drawModuleTab(graphics, x + TAB_X, y + MODULE_TAB_Y, menu.isModulePanelOpen());

        if (sidePanelOpen) {
            drawSidePanel(graphics, mouseX, mouseY);
        }

        if (menu.isModulePanelOpen()) {
            drawModulePanel(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 10, DomeIndustrialGuiStyle.TEXT, false);

        // Resource names and numeric values intentionally live only in meter tooltips.
        graphics.drawString(font, statusText(), 86, 96, statusColor(), false);
        graphics.drawString(font, Component.translatable("container.inventory"),
                inventoryLabelX, inventoryLabelY, DomeIndustrialGuiStyle.TEXT_MUTED, false);

        if (sidePanelOpen) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.domesurvival.side_config"),
                    SIDE_PANEL_X + SIDE_PANEL_WIDTH / 2,
                    10,
                    DomeIndustrialGuiStyle.TEXT
            );
        } else if (menu.isModulePanelOpen()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.domesurvival.upgrade_modules"),
                    SIDE_PANEL_X + SIDE_PANEL_WIDTH / 2,
                    10,
                    DomeIndustrialGuiStyle.TEXT
            );
        }
    }

    private void drawSidePanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        DomeIndustrialGuiStyle.drawPanel(
                graphics, x + SIDE_PANEL_X, y,
                SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT
        );
        DomeIndustrialGuiStyle.drawFrame(
                graphics,
                x + SIDE_PANEL_X + 8, y + 27,
                SIDE_PANEL_WIDTH - 16, SIDE_PANEL_HEIGHT - 35,
                DomeIndustrialGuiStyle.PANEL_ALT
        );

        for (RelativeSide visualSide : RelativeSide.values()) {
            Rect rect = SIDE_RECTS.get(visualSide);
            boolean hovered = visualSide != RelativeSide.FRONT
                    && rect.contains(mouseX, mouseY, leftPos, topPos);
            drawMachineFace(
                    graphics,
                    rect,
                    visualSide,
                    menu.getSideMode(machineSideForVisualSide(visualSide)),
                    hovered
            );
        }
    }

    private void drawModulePanel(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;

        DomeIndustrialGuiStyle.drawPanel(
                graphics, x + SIDE_PANEL_X, y,
                SIDE_PANEL_WIDTH, 112
        );
        DomeIndustrialGuiStyle.drawFrame(
                graphics,
                x + SIDE_PANEL_X + 8, y + 27,
                SIDE_PANEL_WIDTH - 16, 77,
                DomeIndustrialGuiStyle.PANEL_ALT
        );
        DomeIndustrialGuiStyle.drawSlot(
                graphics,
                x + OrganicProcessorMenu.MODULE_SLOT_X,
                y + OrganicProcessorMenu.MODULE_SLOT_0_Y,
                false
        );
        DomeIndustrialGuiStyle.drawSlot(
                graphics,
                x + OrganicProcessorMenu.MODULE_SLOT_X,
                y + OrganicProcessorMenu.MODULE_SLOT_1_Y,
                false
        );
    }

    private void drawGearTab(GuiGraphics graphics, int x, int y, boolean active) {
        int fill = active ? 0xFF3A3427 : DomeIndustrialGuiStyle.PANEL_ALT;
        DomeIndustrialGuiStyle.drawFrame(graphics, x, y, TAB_SIZE, TAB_SIZE, fill);
        int cx = x + TAB_SIZE / 2;
        int cy = y + TAB_SIZE / 2;
        int metal = active ? DomeIndustrialGuiStyle.ENERGY_LIGHT : 0xFF687278;
        graphics.fill(cx - 5, cy - 2, cx + 5, cy + 2, metal);
        graphics.fill(cx - 2, cy - 5, cx + 2, cy + 5, metal);
        graphics.fill(cx - 4, cy - 4, cx + 4, cy + 4, metal);
        graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF151A1D);
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

    private void drawMachineFace(GuiGraphics graphics, Rect rect, RelativeSide side, SideMode mode, boolean hovered) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;
        int outer = hovered ? DomeIndustrialGuiStyle.BIO : 0xFF0E1214;
        int rim = hovered ? DomeIndustrialGuiStyle.BIO_LIGHT : 0xFF3D454A;
        int face = side == RelativeSide.FRONT ? 0xFF20262A : DomeIndustrialGuiStyle.PANEL_ALT;

        graphics.fill(x, y, x + rect.width, y + rect.height, outer);
        graphics.fill(x + 1, y + 1, x + rect.width - 1, y + rect.height - 1, rim);
        graphics.fill(x + 2, y + 2, x + rect.width - 2, y + rect.height - 2, face);

        if (side == RelativeSide.FRONT) {
            graphics.fill(x + 4, y + 5, x + rect.width - 4, y + rect.height - 4, 0xFF121719);
            graphics.fill(x + 6, y + 6, x + rect.width - 6, y + rect.height - 5, DomeIndustrialGuiStyle.BIO);
            return;
        }

        int portU = switch (mode) {
            case INPUT -> PORT_INPUT_U;
            case OUTPUT, BOTH -> PORT_OUTPUT_U;
            case DISABLED -> PORT_OFF_U;
        };

        graphics.blit(
                PORT_TEXTURE,
                x + Math.max(1, (rect.width - PORT_SIZE) / 2),
                y + Math.max(1, (rect.height - PORT_SIZE) / 2),
                portU, 0,
                PORT_SIZE, PORT_SIZE,
                PORT_TEX_WIDTH, PORT_TEX_HEIGHT
        );
    }

    private RelativeSide getHoveredSide(double mouseX, double mouseY) {
        for (RelativeSide side : RelativeSide.values()) {
            if (side == RelativeSide.FRONT) continue;
            Rect rect = SIDE_RECTS.get(side);
            if (rect.contains(mouseX, mouseY, leftPos, topPos)) return side;
        }
        return null;
    }

    private static String sideTranslationKey(RelativeSide side) {
        return "gui.domesurvival.side." + side.name().toLowerCase(Locale.ROOT);
    }

    private static Component getSideModeTooltip(SideMode mode) {
        return switch (mode) {
            case INPUT -> Component.translatable("gui.domesurvival.side_state.input");
            case OUTPUT, BOTH -> Component.translatable("gui.domesurvival.side_state.output");
            case DISABLED -> Component.translatable("gui.domesurvival.side_state.disabled");
        };
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

    private record Rect(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY, int leftPos, int topPos) {
            double localX = mouseX - leftPos;
            double localY = mouseY - topPos;
            return localX >= x && localX < x + width && localY >= y && localY < y + height;
        }
    }
}
