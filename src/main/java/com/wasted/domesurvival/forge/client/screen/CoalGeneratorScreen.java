package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.coal.CoalGeneratorBlockEntity;
import com.wasted.domesurvival.forge.machine.coal.CoalGeneratorMenu;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * Coal Generator UI.
 *
 * <p>dev.17m keeps the compact RF routing panel, centers the main machine panel,
 * restores one authoritative vanilla item render per slot, and adds a deeper bottom
 * margin under the hotbar so the inventory block no longer feels cramped.</p>
 */
public final class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 266;
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

    private static final int FUEL_BAR_X = 14;
    private static final int FUEL_BAR_Y = 108;
    private static final int FUEL_BAR_W = 153;
    private static final int FUEL_BAR_H = 14;
    private static final int FUEL_SLOT_BG_X = 178;
    private static final int FUEL_SLOT_BG_Y = 102;
    private static final int FUEL_SLOT_BG_SIZE = 24;

    private static final int INVENTORY_X = 11;
    private static final int INVENTORY_Y = 158;
    private static final int INVENTORY_SLOT_SIZE = 22;
    private static final int INVENTORY_SLOT_STEP = 22;
    private static final int HOTBAR_Y = 226;

    private static final Rect SIDE_MODEL_FRAME = new Rect(SIDE_PANEL_X + 8, 28, 80, 90);
    private static final int SIDE_BUTTON_SIZE = 14;
    private static final int SIDE_GRID_STEP = 22;
    private static final EnumMap<RelativeSide, Rect> SIDE_RECTS = createSideRects();

    // 6x6 sprites: sealed/off, energy output, fixed fuel input, fixed item output.
    private static final int PORT_SIZE = 6;
    private static final int PORT_TEX_WIDTH = 24;
    private static final int PORT_TEX_HEIGHT = 6;
    private static final int PORT_OFF_U = 0;
    private static final int PORT_INPUT_U = 12;
    private static final int PORT_OUTPUT_U = 18;

    private boolean sidePanelOpen;

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        // AbstractContainerScreen normally centers the full 312 px composite (main + RF panel).
        // That makes the actual machine GUI look shifted left. Keep the 220 px machine panel
        // itself centered and let the compact RF panel extend to its right.
        leftPos = (width - MACHINE_PANEL_WIDTH) / 2;
        topPos = (height - imageHeight) / 2;
    }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> regions = new EnumMap<>(RelativeSide.class);

        // Exact symmetric cube-net layout. Every marker is 14x14 and every adjacent
        // center is separated by the same 22 px step. FRONT remains static, but it
        // occupies the same grid cell as every configurable side.
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
                            CoalGeneratorMenu.sideButtonId(machineSide)
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
     * The preview is drawn as the player sees the machine from the front. A machine's
     * relative LEFT therefore appears on the viewer's right, and relative RIGHT appears
     * on the viewer's left. Keep that projection detail local to the client preview so
     * the server-side routing model remains consistently machine-relative.
     */
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
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.coal_generator.energy_tooltip",
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

        drawIndustrialPanel(guiGraphics, x, y, MACHINE_PANEL_WIDTH, PANEL_HEIGHT, 0xFF30363A);

        // Energy section.
        drawThinFrame(guiGraphics, x + ENERGY_METER_X, y + ENERGY_METER_Y, ENERGY_METER_W, ENERGY_METER_H, 0xFF14191C);
        drawThinFrame(guiGraphics, x + ENERGY_VALUE_X, y + ENERGY_VALUE_Y, ENERGY_VALUE_W, ENERGY_VALUE_H, 0xFF151A1D);

        int capacity = Math.max(1, menu.getEnergyCapacity());
        int energyInnerHeight = ENERGY_METER_H - 6;
        int energyHeight = Math.min(
                energyInnerHeight,
                (int) ((long) menu.getEnergyStored() * energyInnerHeight / capacity)
        );
        if (energyHeight > 0) {
            int fillBottom = y + ENERGY_METER_Y + ENERGY_METER_H - 3;
            int fillTop = fillBottom - energyHeight;
            guiGraphics.fill(x + ENERGY_METER_X + 3, fillTop, x + ENERGY_METER_X + ENERGY_METER_W - 3, fillBottom, 0xFF8D792A);
            guiGraphics.fill(x + ENERGY_METER_X + 4, fillTop, x + ENERGY_METER_X + 6, fillBottom, 0xFFAA9438);
        }

        // Fuel starts below the energy meter; the label can no longer collide with it.
        drawThinFrame(guiGraphics, x + FUEL_BAR_X, y + FUEL_BAR_Y, FUEL_BAR_W, FUEL_BAR_H, 0xFF151A1D);
        drawSlot(guiGraphics, x + FUEL_SLOT_BG_X, y + FUEL_SLOT_BG_Y, FUEL_SLOT_BG_SIZE);

        int maxBurn = menu.getMaxBurnTime();
        int burnWidth = maxBurn <= 0 ? 0 : Math.min(FUEL_BAR_W - 6, menu.getBurnTime() * (FUEL_BAR_W - 6) / maxBurn);
        if (burnWidth > 0) {
            guiGraphics.fill(
                    x + FUEL_BAR_X + 3,
                    y + FUEL_BAR_Y + 3,
                    x + FUEL_BAR_X + 3 + burnWidth,
                    y + FUEL_BAR_Y + FUEL_BAR_H - 3,
                    0xFF744128
            );
            guiGraphics.fill(
                    x + FUEL_BAR_X + 3,
                    y + FUEL_BAR_Y + 4,
                    x + FUEL_BAR_X + 3 + burnWidth,
                    y + FUEL_BAR_Y + 6,
                    0xFF925034
            );
        }

        // Player inventory title has its own clear row; the divider sits below it.
        guiGraphics.fill(x + 10, y + 153, x + MACHINE_PANEL_WIDTH - 10, y + 154, 0xFF171B1F);
        guiGraphics.fill(x + 10, y + 154, x + MACHINE_PANEL_WIDTH - 10, y + 155, 0xFF4B5359);

        // Roomier 22 px visual cells keep the vanilla 16x16 item render centered with
        // a clear three-pixel visual margin on each side. There is no second item render.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(
                        guiGraphics,
                        x + INVENTORY_X + column * INVENTORY_SLOT_STEP,
                        y + INVENTORY_Y + row * INVENTORY_SLOT_STEP,
                        INVENTORY_SLOT_SIZE
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(
                    guiGraphics,
                    x + INVENTORY_X + column * INVENTORY_SLOT_STEP,
                    y + HOTBAR_Y,
                    INVENTORY_SLOT_SIZE
            );
        }

        drawGearButton(guiGraphics, x + GEAR_X, y + GEAR_Y, sidePanelOpen);
        if (sidePanelOpen) {
            drawIndustrialPanel(guiGraphics, x + SIDE_PANEL_X, y, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT, 0xFF252B2F);
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

    private void drawMachineFace(GuiGraphics guiGraphics, Rect rect, RelativeSide side, SideMode mode, boolean hovered) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;

        int outer = hovered ? 0xFF697278 : 0xFF0E1214;
        int rim = hovered ? 0xFF50585E : 0xFF3D454A;
        int face = side == RelativeSide.FRONT ? 0xFF20262A : 0xFF252B2F;

        guiGraphics.fill(x, y, x + rect.width, y + rect.height, outer);
        guiGraphics.fill(x + 1, y + 1, x + rect.width - 1, y + rect.height - 1, rim);
        guiGraphics.fill(x + 2, y + 2, x + rect.width - 2, y + rect.height - 2, face);

        if (side == RelativeSide.FRONT) {
            // Front is a pure machine face: no item/energy connector is ever rendered here.
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

        blitPortSprite(
                guiGraphics,
                x + Math.max(1, (rect.width - PORT_SIZE) / 2),
                y + Math.max(1, (rect.height - PORT_SIZE) / 2),
                portU
        );
    }

    /**
     * Item icons and their stack-count overlays are intentionally left to
     * AbstractContainerScreen's vanilla renderer. The real 16x16 Slot positions are
     * centered inside our 22x22 visual cells, so a single vanilla render stays crisp,
     * centered and fully contained without the double-render ghosting seen in dev.17l.
     */
    private static void blitPortSprite(GuiGraphics guiGraphics, int x, int y, int u) {
        guiGraphics.blit(PORT_TEXTURE, x, y, u, 0, PORT_SIZE, PORT_SIZE, PORT_TEX_WIDTH, PORT_TEX_HEIGHT);
    }

    private static void drawIndustrialPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0C0F11);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF464E53);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF50585D);
        guiGraphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, 0xFF14181B);
    }

    private static void drawThinFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0B0E10);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4C555A);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y, int size) {
        // Flat industrial slot style: no bright top/left gloss strips.
        // dev.18g: the usable slot field is exactly 16x16, matching Minecraft's real
        // Slot/item/hover area. This makes the vanilla hover highlight fill the entire
        // visible slot field and keeps every item geometrically centered in the frame.
        int contentSize = 16;
        int inset = Math.max(2, (size - contentSize) / 2);

        guiGraphics.fill(x, y, x + size, y + size, 0xFF0D1012);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF3E464B);
        guiGraphics.fill(x + inset, y + inset, x + inset + contentSize, y + inset + contentSize, 0xFF1B2125);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawClampedText(guiGraphics, title, 10, 8, MACHINE_PANEL_WIDTH - 20, 0xFFE0E4E6);

        drawClampedText(
                guiGraphics,
                Component.translatable("gui.domesurvival.coal_generator.energy_section"),
                14,
                24,
                MACHINE_PANEL_WIDTH - 28,
                0xFFC5CBCD
        );

        drawCenteredClampedText(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.coal_generator.energy_compact_spaced",
                        compactRf(menu.getEnergyStored()),
                        compactRf(menu.getEnergyCapacity())
                ),
                ENERGY_VALUE_X + 3,
                ENERGY_VALUE_Y + 4,
                ENERGY_VALUE_W - 6,
                0xFFB9A246
        );

        drawClampedText(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.coal_generator.generation",
                        CoalGeneratorBlockEntity.GENERATION_PER_TICK
                ),
                42,
                58,
                166,
                0xFFC1C7CA
        );
        drawClampedText(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.coal_generator.output",
                        CoalGeneratorBlockEntity.MAX_OUTPUT_PER_TICK
                ),
                42,
                70,
                166,
                0xFFC1C7CA
        );

        // Fuel label is now below the 53px energy meter (which ends at y=90).
        drawClampedText(
                guiGraphics,
                Component.translatable("gui.domesurvival.coal_generator.fuel_section"),
                14,
                94,
                MACHINE_PANEL_WIDTH - 28,
                0xFFC5CBCD
        );

        drawClampedText(
                guiGraphics,
                getFuelRemainingText(),
                14,
                126,
                194,
                0xFFA9B1B5
        );

        drawClampedText(guiGraphics, playerInventoryTitle, 14, 140, 194, 0xFFC5CBCD);

        if (sidePanelOpen) {
            drawCenteredClampedText(guiGraphics, Component.translatable("gui.domesurvival.side_config"),
                    SIDE_PANEL_X + 4, 8, SIDE_PANEL_WIDTH - 8, 0xFFE0E4E6);
        }
    }

    private Component getFuelRemainingText() {
        if (menu.getMaxBurnTime() <= 0 || menu.getBurnTime() <= 0) {
            return Component.translatable("gui.domesurvival.coal_generator.fuel_remaining_empty");
        }
        int seconds = Math.max(0, menu.getBurnTime() / 20);
        int percent = Math.min(100, menu.getBurnTime() * 100 / Math.max(1, menu.getMaxBurnTime()));
        return Component.translatable(
                "gui.domesurvival.coal_generator.fuel_remaining",
                formatDuration(seconds),
                percent
        );
    }

    private static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private void drawClampedText(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        guiGraphics.enableScissor(leftPos + x, topPos + y, leftPos + x + maxWidth, topPos + y + font.lineHeight + 1);
        if (font.width(value) <= maxWidth) {
            guiGraphics.drawString(font, value, x, y, color, false);
        } else {
            String dots = "...";
            int usableWidth = Math.max(0, maxWidth - font.width(dots));
            String clipped = font.plainSubstrByWidth(value, usableWidth);
            guiGraphics.drawString(font, clipped + dots, x, y, color, false);
        }
        guiGraphics.disableScissor();
    }

    private void drawCenteredClampedText(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        if (font.width(value) > maxWidth) {
            String dots = "...";
            int usableWidth = Math.max(0, maxWidth - font.width(dots));
            value = font.plainSubstrByWidth(value, usableWidth) + dots;
        }
        int drawX = x + Math.max(0, (maxWidth - font.width(value)) / 2);
        guiGraphics.enableScissor(leftPos + x, topPos + y, leftPos + x + maxWidth, topPos + y + font.lineHeight + 1);
        guiGraphics.drawString(font, value, drawX, y, color, false);
        guiGraphics.disableScissor();
    }

    private static String compactRf(int value) {
        if (value < 1_000) {
            return Integer.toString(value);
        }
        if (value % 1_000 == 0) {
            return (value / 1_000) + "k";
        }
        return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
    }

    private RelativeSide getHoveredSide(double mouseX, double mouseY) {
        for (RelativeSide side : RelativeSide.values()) {
            if (side == RelativeSide.FRONT) {
                continue;
            }
            Rect rect = SIDE_RECTS.get(side);
            if (rect.contains(mouseX, mouseY, leftPos, topPos)) {
                return side;
            }
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
