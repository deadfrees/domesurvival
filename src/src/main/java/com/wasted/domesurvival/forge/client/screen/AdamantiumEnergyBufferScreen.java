package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferMenu;
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

/**
 * Compact Energy Buffer UI. The side selector is intentionally copied from
 * the established CoalGenerator machine-family UI: same gear, cube-net,
 * sprites, click protocol and tooltips.
 */
public final class AdamantiumEnergyBufferScreen extends AbstractContainerScreen<AdamantiumEnergyBufferMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 104;
    private static final int GEAR_X = 180;
    private static final int GEAR_Y = 8;
    private static final int GEAR_SIZE = 20;
    private static final int SIDE_PANEL_X = 204;
    private static final int SIDE_PANEL_WIDTH = 96;
    private static final int SIDE_PANEL_HEIGHT = 122;

    private static final int BAR_X = 24;
    private static final int BAR_Y = 43;
    private static final int BAR_W = 128;
    private static final int BAR_H = 18;

    private static final Rect SIDE_MODEL_FRAME = new Rect(SIDE_PANEL_X + 8, 28, 80, 90);
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

    public AdamantiumEnergyBufferScreen(AdamantiumEnergyBufferMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - PANEL_WIDTH) / 2;
        topPos = (height - imageHeight) / 2;
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
            if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) {
                sidePanelOpen = !sidePanelOpen;
                return true;
            }

            if (sidePanelOpen) {
                RelativeSide side = getHoveredSide(mouseX, mouseY);
                if (side != null && minecraft != null && minecraft.gameMode != null) {
                    RelativeSide machineSide = machineSideForVisualSide(side);
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            AdamantiumEnergyBufferMenu.sideButtonId(machineSide)
                    );
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.domesurvival.side_config"), mouseX, mouseY);
            return;
        }

        if (sidePanelOpen) {
            RelativeSide hoveredSide = getHoveredSide(mouseX, mouseY);
            if (hoveredSide != null) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(sideTranslationKey(hoveredSide)));
                tooltip.add(getSideModeTooltip(menu.getSideMode(machineSideForVisualSide(hoveredSide))));
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
                return;
            }
        }

        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.energy_buffer.tooltip",
                            menu.getEnergyStored(),
                            menu.getEnergyCapacity()
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawIndustrialPanel(guiGraphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, 0xFF30363A);

        drawThinFrame(guiGraphics, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, 0xFF151A1D);
        int capacity = Math.max(1, menu.getEnergyCapacity());
        int fill = Math.min(BAR_W - 6,
                (int) ((long) menu.getEnergyStored() * (BAR_W - 6) / capacity));
        if (fill > 0) {
            guiGraphics.fill(x + BAR_X + 3, y + BAR_Y + 3,
                    x + BAR_X + 3 + fill, y + BAR_Y + BAR_H - 3, 0xFF8D792A);
            guiGraphics.fill(x + BAR_X + 3, y + BAR_Y + 4,
                    x + BAR_X + 3 + fill, y + BAR_Y + 7, 0xFFC6AA42);
            guiGraphics.fill(x + BAR_X + 3, y + BAR_Y + BAR_H - 7,
                    x + BAR_X + 3 + fill, y + BAR_Y + BAR_H - 4, 0xFFE0C35C);
        }

        drawGearButton(guiGraphics, x + GEAR_X, y + GEAR_Y, sidePanelOpen);

        if (sidePanelOpen) {
            drawIndustrialPanel(guiGraphics, x + SIDE_PANEL_X, y,
                    SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT, 0xFF252B2F);
            drawThinFrame(guiGraphics,
                    leftPos + SIDE_MODEL_FRAME.x,
                    topPos + SIDE_MODEL_FRAME.y,
                    SIDE_MODEL_FRAME.width,
                    SIDE_MODEL_FRAME.height,
                    0xFF171C20);
            drawSideModel(guiGraphics, mouseX, mouseY);
        }
    }

    private void drawGearButton(GuiGraphics guiGraphics, int x, int y, boolean active) {
        int bg = active ? 0xFF394247 : 0xFF252B2F;
        drawThinFrame(guiGraphics, x, y, GEAR_SIZE, GEAR_SIZE, bg);
        int cx = x + GEAR_SIZE / 2;
        int cy = y + GEAR_SIZE / 2;
        int metal = active ? 0xFF869197 : 0xFF687278;
        guiGraphics.fill(cx - 5, cy - 2, cx + 5, cy + 2, metal);
        guiGraphics.fill(cx - 2, cy - 5, cx + 2, cy + 5, metal);
        guiGraphics.fill(cx - 4, cy - 4, cx + 4, cy + 4, metal);
        guiGraphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF151A1D);
    }

    private void drawSideModel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (RelativeSide visualSide : RelativeSide.values()) {
            Rect rect = SIDE_RECTS.get(visualSide);
            boolean hovered = visualSide != RelativeSide.FRONT
                    && rect.contains(mouseX, mouseY, leftPos, topPos);
            RelativeSide machineSide = machineSideForVisualSide(visualSide);
            drawMachineFace(guiGraphics, rect, visualSide, menu.getSideMode(machineSide), hovered);
        }
    }

    private void drawMachineFace(GuiGraphics guiGraphics, Rect rect, RelativeSide side,
                                 SideMode mode, boolean hovered) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;

        int outer = hovered ? 0xFF697278 : 0xFF0E1214;
        int rim = hovered ? 0xFF50585E : 0xFF3D454A;
        int face = side == RelativeSide.FRONT ? 0xFF20262A : 0xFF252B2F;

        guiGraphics.fill(x, y, x + rect.width, y + rect.height, outer);
        guiGraphics.fill(x + 1, y + 1, x + rect.width - 1, y + rect.height - 1, rim);
        guiGraphics.fill(x + 2, y + 2, x + rect.width - 2, y + rect.height - 2, face);

        if (side == RelativeSide.FRONT) {
            guiGraphics.fill(x + 4, y + 5, x + rect.width - 4, y + rect.height - 4, 0xFF121719);
            for (int i = 0; i < 3; i++) {
                int ventX = x + 5 + i * 2;
                guiGraphics.fill(ventX, y + 7, ventX + 1, y + rect.height - 6, 0xFF424A4F);
            }
            return;
        }

        int portU = switch (mode) {
            case INPUT -> PORT_INPUT_U;
            case OUTPUT, BOTH -> PORT_OUTPUT_U;
            case DISABLED -> PORT_OFF_U;
        };

        guiGraphics.blit(
                PORT_TEXTURE,
                x + Math.max(1, (rect.width - PORT_SIZE) / 2),
                y + Math.max(1, (rect.height - PORT_SIZE) / 2),
                portU, 0,
                PORT_SIZE, PORT_SIZE,
                PORT_TEX_WIDTH, PORT_TEX_HEIGHT
        );
    }

    private static void drawIndustrialPanel(GuiGraphics guiGraphics, int x, int y,
                                            int width, int height, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0C0F11);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF464E53);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF50585D);
        guiGraphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFF14181B);
    }

    private static void drawThinFrame(GuiGraphics guiGraphics, int x, int y,
                                      int width, int height, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0B0E10);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4C555A);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 10, 10, 0xFFE0E4E6, false);
        guiGraphics.drawString(font,
                Component.translatable(
                        "gui.domesurvival.energy_buffer.energy",
                        menu.getEnergyStored(),
                        menu.getEnergyCapacity()
                ),
                24, 70, 0xFFD0D6DA, false);

        if (sidePanelOpen) {
            guiGraphics.drawCenteredString(
                    font,
                    Component.translatable("gui.domesurvival.side_config"),
                    SIDE_PANEL_X + SIDE_PANEL_WIDTH / 2,
                    10,
                    0xFFE0E4E6
            );
        }
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

    private record Rect(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY, int leftPos, int topPos) {
            double localX = mouseX - leftPos;
            double localY = mouseY - topPos;
            return localX >= x && localX < x + width
                    && localY >= y && localY < y + height;
        }
    }
}
