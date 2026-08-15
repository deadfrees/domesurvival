package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.water.WaterPurifierBlockEntity;
import com.wasted.domesurvival.forge.machine.water.WaterPurifierMenu;
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
 * Water purifier UI aligned with the coal generator style.
 * This revision reduces label collisions, tones down the orange palette,
 * and keeps the whole machine family visually consistent.
 */
public final class WaterPurifierScreen extends AbstractContainerScreen<WaterPurifierMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 284;
    private static final int MACHINE_PANEL_WIDTH = 220;
    private static final int GEAR_X = 224;
    private static final int GEAR_Y = 8;
    private static final int GEAR_SIZE = 20;
    private static final int SIDE_PANEL_X = 248;
    private static final int SIDE_PANEL_WIDTH = 96;
    private static final int SIDE_PANEL_HEIGHT = 122;

    private static final int ENERGY_METER_X = 14;
    private static final int ENERGY_METER_Y = 37;
    private static final int ENERGY_METER_W = 18;
    private static final int ENERGY_METER_H = 53;
    private static final int ENERGY_VALUE_X = 42;
    private static final int ENERGY_VALUE_Y = 38;
    private static final int ENERGY_VALUE_W = 166;
    private static final int ENERGY_VALUE_H = 15;

    private static final int RAW_BAR_X = 14;
    private static final int RAW_BAR_Y = 108;
    private static final int RAW_BAR_W = 60;
    private static final int RAW_BAR_H = 14;
    private static final int PROGRESS_BAR_X = 80;
    private static final int PROGRESS_BAR_Y = 108;
    private static final int PROGRESS_BAR_W = 60;
    private static final int PROGRESS_BAR_H = 14;
    private static final int PURE_BAR_X = 146;
    private static final int PURE_BAR_Y = 108;
    private static final int PURE_BAR_W = 60;
    private static final int PURE_BAR_H = 14;

    private static final int WATER_SLOT_BG_X = 146;
    private static final int FILTER_SLOT_BG_X = 178;
    private static final int MACHINE_SLOT_BG_Y = 136;
    private static final int MACHINE_SLOT_BG_SIZE = 24;

    private static final int INVENTORY_X = 11;
    private static final int INVENTORY_Y = 179;
    private static final int INVENTORY_SLOT_SIZE = 22;
    private static final int INVENTORY_SLOT_STEP = 22;
    private static final int HOTBAR_Y = 247;

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

    // Close to the coal generator palette, less saturated than the first purifier draft.
    private static final int ENERGY_DARK = 0xFF6F5F28;
    private static final int ENERGY_MAIN = 0xFF8D792A;
    private static final int ENERGY_BRIGHT = 0xFFAA9438;
    private static final int RAW_WATER = 0xFF2B678A;
    private static final int RAW_WATER_BRIGHT = 0xFF4B9BC4;
    private static final int PURE_WATER = 0xFF2B8E97;
    private static final int PURE_WATER_BRIGHT = 0xFF72D4DB;

    private boolean sidePanelOpen;

    public WaterPurifierScreen(WaterPurifierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - MACHINE_PANEL_WIDTH) / 2;
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
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            WaterPurifierMenu.sideButtonId(machineSideForVisualSide(side))
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

        if (isHovering(ENERGY_METER_X, ENERGY_METER_Y, ENERGY_METER_W, ENERGY_METER_H, mouseX, mouseY)
                || isHovering(ENERGY_VALUE_X, ENERGY_VALUE_Y, ENERGY_VALUE_W, ENERGY_VALUE_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable(
                    "gui.domesurvival.water_purifier.energy_tooltip",
                    menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isHovering(RAW_BAR_X, RAW_BAR_Y, RAW_BAR_W, RAW_BAR_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable(
                    "gui.domesurvival.water_purifier.raw_tooltip",
                    menu.getRawWater(), menu.getRawCapacity()), mouseX, mouseY);
        } else if (isHovering(PURE_BAR_X, PURE_BAR_Y, PURE_BAR_W, PURE_BAR_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable(
                    "gui.domesurvival.water_purifier.purified_tooltip",
                    menu.getPurifiedWater(), menu.getPurifiedCapacity()), mouseX, mouseY);
        } else if (isHovering(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_W, PROGRESS_BAR_H, mouseX, mouseY)) {
            int percent = menu.getProgressMax() <= 0 ? 0 : menu.getProgress() * 100 / menu.getProgressMax();
            guiGraphics.renderTooltip(font, Component.translatable(
                    "gui.domesurvival.water_purifier.progress_tooltip", percent), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawIndustrialPanel(guiGraphics, x, y, MACHINE_PANEL_WIDTH, PANEL_HEIGHT, 0xFF30363A);

        drawThinFrame(guiGraphics, x + ENERGY_METER_X, y + ENERGY_METER_Y, ENERGY_METER_W, ENERGY_METER_H, 0xFF14191C);
        drawThinFrame(guiGraphics, x + ENERGY_VALUE_X, y + ENERGY_VALUE_Y, ENERGY_VALUE_W, ENERGY_VALUE_H, 0xFF151A1D);
        drawThinFrame(guiGraphics, x + RAW_BAR_X, y + RAW_BAR_Y, RAW_BAR_W, RAW_BAR_H, 0xFF151A1D);
        drawThinFrame(guiGraphics, x + PROGRESS_BAR_X, y + PROGRESS_BAR_Y, PROGRESS_BAR_W, PROGRESS_BAR_H, 0xFF151A1D);
        drawThinFrame(guiGraphics, x + PURE_BAR_X, y + PURE_BAR_Y, PURE_BAR_W, PURE_BAR_H, 0xFF151A1D);
        drawSlot(guiGraphics, x + WATER_SLOT_BG_X, y + MACHINE_SLOT_BG_Y, MACHINE_SLOT_BG_SIZE);
        drawSlot(guiGraphics, x + FILTER_SLOT_BG_X, y + MACHINE_SLOT_BG_Y, MACHINE_SLOT_BG_SIZE);

        int capacity = Math.max(1, menu.getEnergyCapacity());
        int energyInnerHeight = ENERGY_METER_H - 6;
        int energyHeight = Math.min(energyInnerHeight, (int) ((long) menu.getEnergyStored() * energyInnerHeight / capacity));
        if (energyHeight > 0) {
            int fillBottom = y + ENERGY_METER_Y + ENERGY_METER_H - 3;
            int fillTop = fillBottom - energyHeight;
            guiGraphics.fill(x + ENERGY_METER_X + 3, fillTop, x + ENERGY_METER_X + ENERGY_METER_W - 3, fillBottom, ENERGY_MAIN);
            guiGraphics.fill(x + ENERGY_METER_X + 4, fillTop, x + ENERGY_METER_X + 6, fillBottom, ENERGY_BRIGHT);
        }

        fillHorizontal(guiGraphics, x + RAW_BAR_X, y + RAW_BAR_Y, RAW_BAR_W, RAW_BAR_H,
                menu.getRawWater(), menu.getRawCapacity(), RAW_WATER, RAW_WATER_BRIGHT);
        fillHorizontal(guiGraphics, x + PURE_BAR_X, y + PURE_BAR_Y, PURE_BAR_W, PURE_BAR_H,
                menu.getPurifiedWater(), menu.getPurifiedCapacity(), PURE_WATER, PURE_WATER_BRIGHT);

        int progressMax = Math.max(1, menu.getProgressMax());
        int progress = Math.min(PROGRESS_BAR_W - 6, menu.getProgress() * (PROGRESS_BAR_W - 6) / progressMax);
        if (progress > 0) {
            guiGraphics.fill(x + PROGRESS_BAR_X + 3, y + PROGRESS_BAR_Y + 3,
                    x + PROGRESS_BAR_X + 3 + progress, y + PROGRESS_BAR_Y + PROGRESS_BAR_H - 3, ENERGY_MAIN);
            guiGraphics.fill(x + PROGRESS_BAR_X + 3, y + PROGRESS_BAR_Y + 4,
                    x + PROGRESS_BAR_X + 3 + progress, y + PROGRESS_BAR_Y + 6, ENERGY_BRIGHT);
        }

        // Machine/inventory separator in neutral metal colors, without the extra orange stripe.
        guiGraphics.fill(x + 10, y + 173, x + MACHINE_PANEL_WIDTH - 10, y + 174, 0xFF171B1F);
        guiGraphics.fill(x + 10, y + 174, x + MACHINE_PANEL_WIDTH - 10, y + 175, 0xFF4B5359);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics,
                        x + INVENTORY_X + column * INVENTORY_SLOT_STEP,
                        y + INVENTORY_Y + row * INVENTORY_SLOT_STEP,
                        INVENTORY_SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(guiGraphics,
                    x + INVENTORY_X + column * INVENTORY_SLOT_STEP,
                    y + HOTBAR_Y,
                    INVENTORY_SLOT_SIZE);
        }

        drawGearButton(guiGraphics, x + GEAR_X, y + GEAR_Y, sidePanelOpen);

        if (sidePanelOpen) {
            drawIndustrialPanel(guiGraphics, x + SIDE_PANEL_X, y, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT, 0xFF252B2F);
            drawThinFrame(guiGraphics, x + SIDE_MODEL_FRAME.x, y + SIDE_MODEL_FRAME.y,
                    SIDE_MODEL_FRAME.width, SIDE_MODEL_FRAME.height, 0xFF171C20);
            drawSideModel(guiGraphics, mouseX, mouseY);
        }
    }

    private static int scaled(int value, int capacity, int maxPixels) {
        if (value <= 0 || capacity <= 0) return 0;
        return Math.min(maxPixels, (int) ((long) value * maxPixels / capacity));
    }

    private static void fillHorizontal(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                       int value, int capacity, int main, int bright) {
        int fill = scaled(value, capacity, width - 6);
        if (fill <= 0) return;
        guiGraphics.fill(x + 3, y + 3, x + 3 + fill, y + height - 3, main);
        guiGraphics.fill(x + 3, y + 4, x + 3 + fill, y + 6, bright);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawClampedText(guiGraphics, title, 10, 8, MACHINE_PANEL_WIDTH - 20, 0xFFE0E4E6);
        drawClampedText(guiGraphics, Component.translatable("gui.domesurvival.water_purifier.energy_section"),
                14, 24, MACHINE_PANEL_WIDTH - 28, 0xFFC5CBCD);
        drawCenteredClampedText(guiGraphics, Component.translatable(
                        "gui.domesurvival.water_purifier.energy_compact",
                        compactRf(menu.getEnergyStored()), compactRf(menu.getEnergyCapacity())),
                ENERGY_VALUE_X + 3, ENERGY_VALUE_Y + 4, ENERGY_VALUE_W - 6, ENERGY_BRIGHT);
        drawClampedText(guiGraphics, Component.translatable(
                        "gui.domesurvival.water_purifier.consumption", WaterPurifierBlockEntity.ENERGY_PER_TICK),
                42, 58, 166, 0xFFC1C7CA);
        drawClampedText(guiGraphics, Component.translatable(
                        "gui.domesurvival.water_purifier.cycle", WaterPurifierBlockEntity.PROCESS_TICKS / 20.0D),
                42, 70, 166, 0xFFC1C7CA);

        // One evenly spaced row: raw water / process / purified water.
        // No extra section/status/slot captions are rendered here, so nothing can overlap.
        drawCenteredClampedText(guiGraphics, Component.translatable("gui.domesurvival.water_purifier.raw_short"),
                RAW_BAR_X, 96, RAW_BAR_W, 0xFF79B8D7);
        drawCenteredClampedText(guiGraphics, Component.translatable("gui.domesurvival.water_purifier.process_short"),
                PROGRESS_BAR_X, 96, PROGRESS_BAR_W, ENERGY_BRIGHT);
        drawCenteredClampedText(guiGraphics, Component.translatable("gui.domesurvival.water_purifier.pure_short"),
                PURE_BAR_X, 96, PURE_BAR_W, 0xFF8CE0E6);

        drawClampedText(guiGraphics, playerInventoryTitle, 14, 163, 126, 0xFFC5CBCD);

        if (sidePanelOpen) {
            drawCenteredClampedText(guiGraphics, Component.translatable("gui.domesurvival.side_config"),
                    SIDE_PANEL_X + 4, 8, SIDE_PANEL_WIDTH - 8, 0xFFE0E4E6);
        }
    }

    private Component getStatusText() {
        return switch (menu.getStatus()) {
            case WaterPurifierBlockEntity.STATUS_RUNNING -> Component.translatable("gui.domesurvival.water_purifier.status.running");
            case WaterPurifierBlockEntity.STATUS_NO_WATER -> Component.translatable("gui.domesurvival.water_purifier.status.no_water");
            case WaterPurifierBlockEntity.STATUS_NO_FILTER -> Component.translatable("gui.domesurvival.water_purifier.status.no_filter");
            case WaterPurifierBlockEntity.STATUS_NO_ENERGY -> Component.translatable("gui.domesurvival.water_purifier.status.no_energy");
            case WaterPurifierBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("gui.domesurvival.water_purifier.status.output_full");
            default -> Component.translatable("gui.domesurvival.water_purifier.status.idle");
        };
    }

    private int getStatusColor() {
        return menu.getStatus() == WaterPurifierBlockEntity.STATUS_RUNNING ? ENERGY_BRIGHT : 0xFFA9B1B5;
    }

    private void drawGearButton(GuiGraphics guiGraphics, int x, int y, boolean active) {
        int bg = active ? 0xFF3A3427 : 0xFF252B2F;
        drawThinFrame(guiGraphics, x, y, GEAR_SIZE, GEAR_SIZE, bg);
        int cx = x + GEAR_SIZE / 2;
        int cy = y + GEAR_SIZE / 2;
        int metal = active ? ENERGY_BRIGHT : 0xFF687278;
        guiGraphics.fill(cx - 5, cy - 2, cx + 5, cy + 2, metal);
        guiGraphics.fill(cx - 2, cy - 5, cx + 2, cy + 5, metal);
        guiGraphics.fill(cx - 4, cy - 4, cx + 4, cy + 4, metal);
        guiGraphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF151A1D);
    }

    private void drawSideModel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (RelativeSide visualSide : RelativeSide.values()) {
            Rect rect = SIDE_RECTS.get(visualSide);
            boolean hovered = visualSide != RelativeSide.FRONT && rect.contains(mouseX, mouseY, leftPos, topPos);
            drawMachineFace(guiGraphics, rect, visualSide,
                    menu.getSideMode(machineSideForVisualSide(visualSide)), hovered);
        }
    }

    private void drawMachineFace(GuiGraphics guiGraphics, Rect rect, RelativeSide side, SideMode mode, boolean hovered) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;
        int outer = hovered ? ENERGY_MAIN : 0xFF0E1214;
        int rim = hovered ? 0xFF5A5140 : 0xFF3D454A;
        int face = side == RelativeSide.FRONT ? 0xFF20262A : 0xFF252B2F;
        guiGraphics.fill(x, y, x + rect.width, y + rect.height, outer);
        guiGraphics.fill(x + 1, y + 1, x + rect.width - 1, y + rect.height - 1, rim);
        guiGraphics.fill(x + 2, y + 2, x + rect.width - 2, y + rect.height - 2, face);
        if (side == RelativeSide.FRONT) {
            guiGraphics.fill(x + 4, y + 5, x + rect.width - 4, y + rect.height - 4, 0xFF121719);
            guiGraphics.fill(x + 6, y + 6, x + rect.width - 6, y + rect.height - 5, PURE_WATER);
            return;
        }
        int portU = switch (mode) {
            case INPUT -> PORT_INPUT_U;
            case OUTPUT, BOTH -> PORT_OUTPUT_U;
            case DISABLED -> PORT_OFF_U;
        };
        blitPortSprite(guiGraphics, x + Math.max(1, (rect.width - PORT_SIZE) / 2),
                y + Math.max(1, (rect.height - PORT_SIZE) / 2), portU);
    }

    private static void blitPortSprite(GuiGraphics guiGraphics, int x, int y, int u) {
        guiGraphics.blit(PORT_TEXTURE, x, y, u, 0, PORT_SIZE, PORT_SIZE, PORT_TEX_WIDTH, PORT_TEX_HEIGHT);
    }

    private static void drawIndustrialPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0C0F11);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF464E53);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF50585D);
        guiGraphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFF50585D);
    }

    private static void drawThinFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0B0E10);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4C555A);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y, int size) {
        int contentSize = 16;
        int inset = Math.max(2, (size - contentSize) / 2);
        guiGraphics.fill(x, y, x + size, y + size, 0xFF0D1012);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF3E464B);
        guiGraphics.fill(x + inset, y + inset, x + inset + contentSize, y + inset + contentSize, 0xFF1B2125);
    }

    private void drawClampedText(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        guiGraphics.enableScissor(leftPos + x, topPos + y, leftPos + x + maxWidth, topPos + y + font.lineHeight + 1);
        if (font.width(value) <= maxWidth) {
            guiGraphics.drawString(font, value, x, y, color, false);
        } else {
            String dots = "...";
            int usableWidth = Math.max(0, maxWidth - font.width(dots));
            guiGraphics.drawString(font, font.plainSubstrByWidth(value, usableWidth) + dots, x, y, color, false);
        }
        guiGraphics.disableScissor();
    }

    private void drawCenteredClampedText(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        if (font.width(value) > maxWidth) {
            String dots = "...";
            value = font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width(dots))) + dots;
        }
        int drawX = x + Math.max(0, (maxWidth - font.width(value)) / 2);
        guiGraphics.enableScissor(leftPos + x, topPos + y, leftPos + x + maxWidth, topPos + y + font.lineHeight + 1);
        guiGraphics.drawString(font, value, drawX, y, color, false);
        guiGraphics.disableScissor();
    }

    private static String compactRf(int value) {
        if (value < 1_000) return Integer.toString(value);
        if (value % 1_000 == 0) return (value / 1_000) + "k";
        return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
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
            return localX >= x && localX < x + width && localY >= y && localY < y + height;
        }
    }
}
