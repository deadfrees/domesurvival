package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.storage.tank.UniversalTankContentKind;
import com.wasted.domesurvival.forge.storage.tank.UniversalTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * V63.2 reservoir UI.
 *
 * Connector configuration intentionally follows the established DomeSurvival
 * machine-family UI: same gear button, same cube-net projection, same port sprite
 * sheet, same click protocol and the same LEFT/RIGHT visual projection mapping.
 */
public final class UniversalTankScreen extends AbstractContainerScreen<UniversalTankMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 156;

    private static final int GEAR_X = 224;
    private static final int GEAR_Y = 8;
    private static final int GEAR_SIZE = 20;

    private static final int SIDE_PANEL_X = 248;
    private static final int SIDE_PANEL_WIDTH = 96;
    private static final int SIDE_PANEL_HEIGHT = 122;
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

    private static final int GAUGE_X = 16;
    private static final int GAUGE_Y = 36;
    private static final int GAUGE_W = 42;
    private static final int GAUGE_H = 100;

    private static final int INFO_X = 72;
    private static final int INFO_W = 134;

    private boolean sidePanelOpen;

    public UniversalTankScreen(UniversalTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();

        // Exactly like the established machine-family screens: the main machine
        // panel is centered and the gear/side panel extends to the right.
        leftPos = (width - PANEL_WIDTH) / 2;
        topPos = (height - imageHeight) / 2;
    }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> regions = new EnumMap<>(RelativeSide.class);

        int centerX = SIDE_PANEL_X + (SIDE_PANEL_WIDTH - SIDE_BUTTON_SIZE) / 2;
        int middleY = 66;

        regions.put(RelativeSide.TOP,
                new Rect(centerX, middleY - SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.LEFT,
                new Rect(centerX - SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.FRONT,
                new Rect(centerX, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.RIGHT,
                new Rect(centerX + SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BOTTOM,
                new Rect(centerX, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BACK,
                new Rect(centerX + SIDE_GRID_STEP, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));

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
                RelativeSide visualSide = getHoveredSide(mouseX, mouseY);
                if (visualSide != null && minecraft != null && minecraft.gameMode != null) {
                    RelativeSide machineSide = machineSideForVisualSide(visualSide);
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            UniversalTankMenu.sideButtonId(machineSide)
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

    /**
     * Same front-view projection used by the other DomeSurvival machine screens.
     * Visual LEFT is machine RIGHT and visual RIGHT is machine LEFT.
     */
    private static RelativeSide machineSideForVisualSide(RelativeSide visualSide) {
        return switch (visualSide) {
            case LEFT -> RelativeSide.RIGHT;
            case RIGHT -> RelativeSide.LEFT;
            default -> visualSide;
        };
    }

    private boolean isConfigurableVisualSide(RelativeSide visualSide) {
        if (!menu.usesUnifiedModel()) {
            return true;
        }

        // A formed reservoir intentionally exposes only its four centered
        // horizontal valves. UP/DOWN remain visible in the cube-net as OFF,
        // but cannot be configured.
        return visualSide == RelativeSide.FRONT
                || visualSide == RelativeSide.BACK
                || visualSide == RelativeSide.LEFT
                || visualSide == RelativeSide.RIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("gui.domesurvival.side_config"),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (sidePanelOpen) {
            RelativeSide hovered = getHoveredSide(mouseX, mouseY);
            if (hovered != null) {
                RelativeSide machineSide = machineSideForVisualSide(hovered);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(sideTranslationKey(hovered)));
                tooltip.add(getSideModeTooltip(menu.getSideMode(machineSide)));
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
                return;
            }
        }

        if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.universal_tank.amount",
                            menu.getStoredAmount(),
                            menu.getCapacity()
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

        drawGauge(guiGraphics, x, y);

        drawThinFrame(guiGraphics, x + 66, y + 28, 146, 112, 0xFF20272B);

        drawGearButton(guiGraphics, x + GEAR_X, y + GEAR_Y, sidePanelOpen);

        if (sidePanelOpen) {
            drawIndustrialPanel(
                    guiGraphics,
                    x + SIDE_PANEL_X,
                    y,
                    SIDE_PANEL_WIDTH,
                    SIDE_PANEL_HEIGHT,
                    0xFF252B2F
            );

            drawThinFrame(
                    guiGraphics,
                    leftPos + SIDE_MODEL_FRAME.x,
                    topPos + SIDE_MODEL_FRAME.y,
                    SIDE_MODEL_FRAME.width,
                    SIDE_MODEL_FRAME.height,
                    0xFF171C20
            );

            drawSideModel(guiGraphics, mouseX, mouseY);
        }
    }

    private void drawGauge(GuiGraphics guiGraphics, int x, int y) {
        int gx = x + GAUGE_X;
        int gy = y + GAUGE_Y;

        drawThinFrame(guiGraphics, gx, gy, GAUGE_W, GAUGE_H, 0xFF111619);

        int innerHeight = GAUGE_H - 6;
        int fillHeight = menu.getCapacity() <= 0
                ? 0
                : (int) ((long) menu.getStoredAmount() * innerHeight / menu.getCapacity());

        fillHeight = Math.max(0, Math.min(innerHeight, fillHeight));

        if (fillHeight <= 0) {
            return;
        }

        int color = contentColor();
        int bottom = gy + GAUGE_H - 3;
        int top = bottom - fillHeight;

        guiGraphics.fill(gx + 3, top, gx + GAUGE_W - 3, bottom, color);
        guiGraphics.fill(gx + 4, top, gx + GAUGE_W - 4, Math.min(bottom, top + 2), 0x90FFFFFF);
    }

    private int contentColor() {
        if (menu.getContentKind() == UniversalTankContentKind.OXYGEN) {
            return 0xFF9DA5AA;
        }

        if (menu.getContentKind() == UniversalTankContentKind.FLUID) {
            FluidStack stack = menu.getClientFluidStack();
            if (!stack.isEmpty()) {
                int tint = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
                return 0xD0000000 | (tint & 0x00FFFFFF);
            }
            return 0xD04B90D9;
        }

        return 0x00000000;
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
            boolean configurable = isConfigurableVisualSide(visualSide);
            boolean hovered = configurable
                    && rect.contains(mouseX, mouseY, leftPos, topPos);

            RelativeSide machineSide = machineSideForVisualSide(visualSide);
            SideMode mode = configurable
                    ? menu.getSideMode(machineSide)
                    : SideMode.DISABLED;

            drawMachineFace(guiGraphics, rect, mode, hovered, configurable);
        }
    }

    private void drawMachineFace(
            GuiGraphics guiGraphics,
            Rect rect,
            SideMode mode,
            boolean hovered,
            boolean configurable
    ) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;

        int outer = hovered ? 0xFF697278 : 0xFF0E1214;
        int rim = hovered ? 0xFF50585E : 0xFF3D454A;
        int face = configurable ? 0xFF252B2F : 0xFF1B2023;

        guiGraphics.fill(x, y, x + rect.width, y + rect.height, outer);
        guiGraphics.fill(x + 1, y + 1, x + rect.width - 1, y + rect.height - 1, rim);
        guiGraphics.fill(x + 2, y + 2, x + rect.width - 2, y + rect.height - 2, face);

        int portU = switch (mode) {
            case INPUT -> PORT_INPUT_U;
            case OUTPUT, BOTH -> PORT_OUTPUT_U;
            case DISABLED -> PORT_OFF_U;
        };

        guiGraphics.blit(
                PORT_TEXTURE,
                x + Math.max(1, (rect.width - PORT_SIZE) / 2),
                y + Math.max(1, (rect.height - PORT_SIZE) / 2),
                portU,
                0,
                PORT_SIZE,
                PORT_SIZE,
                PORT_TEX_WIDTH,
                PORT_TEX_HEIGHT
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawFitted(guiGraphics, title, 10, 10, 198, 0xFFE7ECEF);

        drawCenteredFitted(
                guiGraphics,
                Component.translatable("gui.domesurvival.universal_tank.level", menu.getFillPercent()),
                GAUGE_X - 4,
                22,
                GAUGE_W + 8,
                0xFFD8E1E5
        );

        drawFitted(
                guiGraphics,
                Component.translatable("gui.domesurvival.universal_tank.content", contentName()),
                INFO_X,
                36,
                INFO_W,
                0xFFE2E8EB
        );

        drawFitted(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.universal_tank.amount",
                        menu.getStoredAmount(),
                        menu.getCapacity()
                ),
                INFO_X,
                54,
                INFO_W,
                0xFFC9D2D6
        );

        drawFitted(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.universal_tank.structure",
                        menu.getSizeX(),
                        menu.getSizeY(),
                        menu.getSizeZ()
                ),
                INFO_X,
                76,
                INFO_W,
                0xFFC9D2D6
        );

        drawFitted(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.universal_tank.blocks",
                        menu.getBlockCount(),
                        4000
                ),
                INFO_X,
                94,
                INFO_W,
                0xFFC9D2D6
        );

        drawFitted(
                guiGraphics,
                Component.translatable(
                        menu.usesUnifiedModel()
                                ? "gui.domesurvival.universal_tank.model.unified"
                                : "gui.domesurvival.universal_tank.model.modular"
                ),
                INFO_X,
                118,
                INFO_W,
                menu.usesUnifiedModel() ? 0xFF76D49B : 0xFFBFC8CC
        );

        if (sidePanelOpen) {
            drawCenteredFitted(
                    guiGraphics,
                    Component.translatable("gui.domesurvival.side_config"),
                    SIDE_PANEL_X + 4,
                    8,
                    SIDE_PANEL_WIDTH - 8,
                    0xFFE0E4E6
            );
        }
    }

    private RelativeSide getHoveredSide(double mouseX, double mouseY) {
        for (RelativeSide visualSide : RelativeSide.values()) {
            if (!isConfigurableVisualSide(visualSide)) {
                continue;
            }

            Rect rect = SIDE_RECTS.get(visualSide);
            if (rect.contains(mouseX, mouseY, leftPos, topPos)) {
                return visualSide;
            }
        }

        return null;
    }

    private Component contentName() {
        return switch (menu.getContentKind()) {
            case EMPTY -> Component.translatable("gui.domesurvival.universal_tank.content.empty");
            case OXYGEN -> Component.translatable("gui.domesurvival.universal_tank.content.oxygen");
            case FLUID -> {
                FluidStack stack = menu.getClientFluidStack();
                yield stack.isEmpty()
                        ? Component.translatable("gui.domesurvival.universal_tank.content.fluid")
                        : stack.getDisplayName();
            }
        };
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

    private void drawFitted(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        int width = font.width(text);

        if (width <= maxWidth || width <= 0) {
            guiGraphics.drawString(font, text, x, y, color, false);
            return;
        }

        float scale = maxWidth / (float) width;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawCenteredFitted(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        int width = font.width(text);

        if (width <= maxWidth || width <= 0) {
            guiGraphics.drawString(font, text, x + (maxWidth - width) / 2, y, color, false);
            return;
        }

        float scale = maxWidth / (float) width;
        float scaledHeight = font.lineHeight * scale;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y + (font.lineHeight - scaledHeight) * 0.5F, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
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

    private record Rect(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY, int leftPos, int topPos) {
            double localX = mouseX - leftPos;
            double localY = mouseY - topPos;

            return localX >= x
                    && localX < x + width
                    && localY >= y
                    && localY < y + height;
        }
    }
}
