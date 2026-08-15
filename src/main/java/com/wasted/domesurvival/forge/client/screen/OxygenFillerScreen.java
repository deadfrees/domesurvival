package com.wasted.domesurvival.forge.client.screen;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerBlockEntity;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerMenu;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenFillerMode;
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

/** Industrial oxygen filler UI matching the existing DomeSurvival machine family. */
public final class OxygenFillerScreen extends AbstractContainerScreen<OxygenFillerMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 282;
    private static final int MACHINE_PANEL_WIDTH = 220;

    private static final int GEAR_X = 224;
    private static final int GEAR_Y = 8;
    private static final int GEAR_SIZE = 20;
    private static final int SIDE_PANEL_X = 248;
    private static final int SIDE_PANEL_WIDTH = 96;
    private static final int SIDE_PANEL_HEIGHT = 122;

    private static final int ENERGY_X = 14;
    private static final int ENERGY_Y = 37;
    private static final int ENERGY_W = 18;
    private static final int ENERGY_H = 72;

    private static final int OXYGEN_X = 170;
    private static final int OXYGEN_Y = 37;
    private static final int OXYGEN_W = 26;
    private static final int OXYGEN_H = 72;

    private static final int TANK_SLOT_BG_X = 98;
    private static final int TANK_SLOT_BG_Y = 56;
    private static final int TANK_SLOT_BG_SIZE = 24;

    private static final int TANK_BAR_X = 57;
    private static final int TANK_BAR_Y = 112;
    private static final int TANK_BAR_W = 94;
    private static final int TANK_BAR_H = 13;

    private static final int MODE_BUTTON_X = 38;
    private static final int MODE_BUTTON_Y = 139;
    private static final int MODE_BUTTON_W = 144;
    private static final int MODE_BUTTON_H = 15;

    private static final int INVENTORY_X = 11;
    private static final int INVENTORY_Y = 174;
    private static final int INVENTORY_SLOT = 22;
    private static final int INVENTORY_STEP = 22;
    private static final int HOTBAR_Y = 246;

    private static final int SIDE_BUTTON_SIZE = 14;
    private static final int SIDE_GRID_STEP = 22;
    private static final EnumMap<RelativeSide, Rect> SIDE_RECTS = createSideRects();

    private static final int PORT_SIZE = 6;
    private static final int PORT_TEX_WIDTH = 24;
    private static final int PORT_TEX_HEIGHT = 6;
    private static final int PORT_OFF_U = 0;
    private static final int PORT_INPUT_U = 12;
    private static final int PORT_OUTPUT_U = 18;

    private static final int ENERGY_MAIN = 0xFF8D792A;
    private static final int ENERGY_BRIGHT = 0xFFAA9438;
    private static final int OXYGEN_DARK = 0xFF626A70;
    private static final int OXYGEN_MAIN = 0xFF9DA5AA;
    private static final int OXYGEN_BRIGHT = 0xFFE0E4E6;

    private boolean sidePanelOpen;

    public OxygenFillerScreen(OxygenFillerMenu menu, Inventory playerInventory, Component title) {
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
            if (inside(mouseX, mouseY, MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_W, MODE_BUTTON_H)
                    && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId,
                        OxygenFillerMenu.modeButtonId()
                );
                return true;
            }
            if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) {
                sidePanelOpen = !sidePanelOpen;
                return true;
            }
            if (sidePanelOpen) {
                RelativeSide side = getHoveredSide(mouseX, mouseY);
                if (side != null && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            OxygenFillerMenu.sideButtonId(machineSideForVisualSide(side))
                    );
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTooltip(gg, mouseX, mouseY);

        if (inside(mouseX, mouseY, MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_W, MODE_BUTTON_H)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(getModeText());
            tooltip.add(getStatusText());
            if (menu.getOperatingMode() == OxygenFillerMode.VENTILATION) {
                tooltip.add(Component.translatable("gui.domesurvival.oxygen_filler.ventilation.output_top"));
                tooltip.add(getRoomText());
            }
            tooltip.add(Component.translatable("gui.domesurvival.oxygen_filler.mode.click_hint"));
            gg.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
            return;
        }

        if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) {
            gg.renderTooltip(font, Component.translatable("gui.domesurvival.side_config"), mouseX, mouseY);
            return;
        }

        if (sidePanelOpen) {
            RelativeSide hovered = getHoveredSide(mouseX, mouseY);
            if (hovered != null) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.domesurvival.side." + hovered.name().toLowerCase(Locale.ROOT)));
                tooltip.add(getSideModeTooltip(menu.getSideMode(machineSideForVisualSide(hovered))));
                gg.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
                return;
            }
        }

        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            gg.renderTooltip(font, Component.translatable(
                    "gui.domesurvival.oxygen_filler.energy_tooltip",
                    menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isHovering(OXYGEN_X, OXYGEN_Y, OXYGEN_W, OXYGEN_H, mouseX, mouseY)) {
            gg.renderTooltip(font, Component.translatable(
                    "gui.domesurvival.oxygen_filler.oxygen_tooltip",
                    menu.getOxygen(), menu.getOxygenCapacity()), mouseX, mouseY);
        } else if (isHovering(TANK_BAR_X, TANK_BAR_Y, TANK_BAR_W, TANK_BAR_H, mouseX, mouseY)) {
            if (menu.getOperatingMode() == OxygenFillerMode.VENTILATION) {
                gg.renderTooltip(font, Component.translatable(
                        "gui.domesurvival.oxygen_filler.room_oxygen_tooltip",
                        menu.getRoomOxygen(), menu.getRoomOxygenRequired()), mouseX, mouseY);
            } else {
                gg.renderTooltip(font, Component.translatable(
                        "gui.domesurvival.oxygen_filler.tank_tooltip",
                        menu.getTankOxygen(), menu.getTankCapacity()), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanel(gg, x, y, PANEL_WIDTH, PANEL_HEIGHT, 0xFF30363A);
        drawFrame(gg, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H, 0xFF14191C);
        drawFrame(gg, x + OXYGEN_X, y + OXYGEN_Y, OXYGEN_W, OXYGEN_H, 0xFF151A1D);
        drawFrame(gg, x + TANK_BAR_X, y + TANK_BAR_Y, TANK_BAR_W, TANK_BAR_H, 0xFF151A1D);
        drawSlot(gg, x + TANK_SLOT_BG_X, y + TANK_SLOT_BG_Y, TANK_SLOT_BG_SIZE);

        int energyCapacity = Math.max(1, menu.getEnergyCapacity());
        int energyFill = Math.min(ENERGY_H - 6,
                (int) ((long) menu.getEnergyStored() * (ENERGY_H - 6) / energyCapacity));
        if (energyFill > 0) {
            int bottom = y + ENERGY_Y + ENERGY_H - 3;
            int top = bottom - energyFill;
            gg.fill(x + ENERGY_X + 3, top, x + ENERGY_X + ENERGY_W - 3, bottom, ENERGY_MAIN);
            gg.fill(x + ENERGY_X + 4, top, x + ENERGY_X + 6, bottom, ENERGY_BRIGHT);
        }

        drawOxygenReservoir(gg, x + OXYGEN_X, y + OXYGEN_Y, OXYGEN_W, OXYGEN_H,
                menu.getOxygen(), menu.getOxygenCapacity());

        int centerOxygen = menu.getOperatingMode() == OxygenFillerMode.VENTILATION
                ? menu.getRoomOxygen()
                : menu.getTankOxygen();
        int centerOxygenCapacity = menu.getOperatingMode() == OxygenFillerMode.VENTILATION
                ? menu.getRoomOxygenRequired()
                : menu.getTankCapacity();
        fillHorizontal(gg, x + TANK_BAR_X, y + TANK_BAR_Y, TANK_BAR_W, TANK_BAR_H,
                centerOxygen, centerOxygenCapacity, OXYGEN_MAIN, OXYGEN_BRIGHT);
        drawModeButton(gg, x + MODE_BUTTON_X, y + MODE_BUTTON_Y, MODE_BUTTON_W, MODE_BUTTON_H);

        // Dedicated inventory section: keep a clear gap from machine status and tank readouts.
        gg.fill(x + 10, y + 156, x + PANEL_WIDTH - 10, y + 157, 0xFF171B1F);
        gg.fill(x + 10, y + 157, x + PANEL_WIDTH - 10, y + 158, 0xFF4B5359);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(gg, x + INVENTORY_X + column * INVENTORY_STEP,
                        y + INVENTORY_Y + row * INVENTORY_STEP, INVENTORY_SLOT);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(gg, x + INVENTORY_X + column * INVENTORY_STEP, y + HOTBAR_Y, INVENTORY_SLOT);
        }

        drawGear(gg, x + GEAR_X, y + GEAR_Y, sidePanelOpen);
        if (sidePanelOpen) {
            drawPanel(gg, x + SIDE_PANEL_X, y, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT, 0xFF252B2F);
            drawSideModel(gg, mouseX, mouseY);
        }
    }

    private void drawOxygenReservoir(GuiGraphics gg, int x, int y, int w, int h, int value, int capacity) {
        if (value <= 0 || capacity <= 0) return;
        int innerHeight = h - 6;
        int fill = Math.min(innerHeight, (int) ((long) value * innerHeight / capacity));
        int bottom = y + h - 3;
        int top = bottom - fill;
        gg.fill(x + 3, top, x + w - 3, bottom, OXYGEN_DARK);
        gg.fill(x + 4, top, x + w - 4, bottom, OXYGEN_MAIN);
        gg.fill(x + 5, top, x + 7, bottom, 0xFFCCD1D4);

        for (int yy = bottom - 5; yy > top + 2; yy -= 7) {
            gg.fill(x + 8, yy, x + w - 5, yy + 1, 0xFFC9CED1);
            if (yy - 2 > top) gg.fill(x + 11, yy - 2, x + w - 7, yy - 1, 0xFF8F979C);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        drawClamped(gg, title, 10, 8, PANEL_WIDTH - 20, 0xFFE0E4E6);
        drawClamped(gg, Component.translatable("gui.domesurvival.oxygen_filler.energy_section"),
                14, 24, 80, 0xFFC5CBCD);
        drawCentered(gg, Component.translatable("gui.domesurvival.oxygen_filler.oxygen_section"),
                OXYGEN_X - 10, 24, OXYGEN_W + 20, 0xFFC5CBCD);

        drawCentered(gg, Component.translatable(
                        menu.getOperatingMode() == OxygenFillerMode.VENTILATION
                                ? "gui.domesurvival.oxygen_filler.room_oxygen_section"
                                : "gui.domesurvival.oxygen_filler.tank_slot"),
                62, 39, 92, 0xFFC5CBCD);

        int centerOxygen = menu.getOperatingMode() == OxygenFillerMode.VENTILATION
                ? menu.getRoomOxygen()
                : menu.getTankOxygen();
        int centerOxygenCapacity = menu.getOperatingMode() == OxygenFillerMode.VENTILATION
                ? menu.getRoomOxygenRequired()
                : menu.getTankCapacity();
        drawCentered(gg, Component.translatable("gui.domesurvival.oxygen_filler.tank_amount",
                        centerOxygen, centerOxygenCapacity),
                TANK_BAR_X, 129, TANK_BAR_W, OXYGEN_BRIGHT);

        drawClamped(gg, Component.translatable("gui.domesurvival.oxygen_filler.energy_compact",
                        compact(menu.getEnergyStored()), compact(menu.getEnergyCapacity())),
                39, 92, 112, ENERGY_BRIGHT);
        drawCentered(gg, Component.translatable("gui.domesurvival.oxygen_filler.oxygen_compact",
                        compact(menu.getOxygen()), compact(menu.getOxygenCapacity())),
                OXYGEN_X - 14, 113, OXYGEN_W + 28, OXYGEN_BRIGHT);
        drawCentered(gg, getModeText(), MODE_BUTTON_X + 3, MODE_BUTTON_Y + 3,
                MODE_BUTTON_W - 6, getStatusColor());
        drawClamped(gg, playerInventoryTitle, 11, 162, 126, 0xFFC5CBCD);

        if (sidePanelOpen) {
            drawCentered(gg, Component.translatable("gui.domesurvival.side_config"),
                    SIDE_PANEL_X + 4, 8, SIDE_PANEL_WIDTH - 8, 0xFFE0E4E6);
        }
    }

    private Component getModeText() {
        String key = menu.getOperatingMode() == OxygenFillerMode.VENTILATION
                ? "gui.domesurvival.oxygen_filler.mode.ventilation"
                : "gui.domesurvival.oxygen_filler.mode.tank_filling";
        return Component.translatable(
                "gui.domesurvival.oxygen_filler.mode.label",
                Component.translatable(key)
        );
    }

    private Component getStatusText() {
        return switch (menu.getStatus()) {
            case OxygenFillerBlockEntity.STATUS_FILLING -> Component.translatable("gui.domesurvival.oxygen_filler.status.filling");
            case OxygenFillerBlockEntity.STATUS_NO_TANK -> Component.translatable("gui.domesurvival.oxygen_filler.status.no_tank");
            case OxygenFillerBlockEntity.STATUS_TANK_FULL -> Component.translatable("gui.domesurvival.oxygen_filler.status.tank_full");
            case OxygenFillerBlockEntity.STATUS_NO_OXYGEN -> Component.translatable("gui.domesurvival.oxygen_filler.status.no_oxygen");
            case OxygenFillerBlockEntity.STATUS_NO_ENERGY -> Component.translatable("gui.domesurvival.oxygen_filler.status.no_energy");
            case OxygenFillerBlockEntity.STATUS_VENTILATING -> Component.translatable("gui.domesurvival.oxygen_filler.status.ventilating");
            case OxygenFillerBlockEntity.STATUS_VENT_OUTLET_BLOCKED -> Component.translatable("gui.domesurvival.oxygen_filler.status.vent_outlet_blocked");
            case OxygenFillerBlockEntity.STATUS_VENT_ROOM_OPEN -> Component.translatable("gui.domesurvival.oxygen_filler.status.vent_room_open");
            case OxygenFillerBlockEntity.STATUS_VENT_ROOM_TOO_LARGE -> Component.translatable("gui.domesurvival.oxygen_filler.status.vent_room_too_large");
            case OxygenFillerBlockEntity.STATUS_VENT_ROOM_UNLOADED -> Component.translatable("gui.domesurvival.oxygen_filler.status.vent_room_unloaded");
            case OxygenFillerBlockEntity.STATUS_VENT_ROOM_FULL -> Component.translatable("gui.domesurvival.oxygen_filler.status.vent_room_full");
            case OxygenFillerBlockEntity.STATUS_VENT_ROOM_LEAKING -> Component.translatable(
                    "gui.domesurvival.oxygen_filler.status.vent_room_leaking",
                    menu.getRoomPressurePercent()
            );
            case OxygenFillerBlockEntity.STATUS_VENT_ROOM_DEPRESSURIZED -> Component.translatable(
                    "gui.domesurvival.oxygen_filler.status.vent_room_depressurized"
            );
            default -> Component.translatable("gui.domesurvival.oxygen_filler.status.idle");
        };
    }

    private Component getRoomText() {
        return switch (menu.getRoomState()) {
            case SEALED -> Component.translatable(
                    "gui.domesurvival.oxygen_filler.room.sealed",
                    menu.getRoomVolume(),
                    menu.getRoomOxygen(),
                    menu.getRoomOxygenRequired()
            );
            case OPEN -> Component.translatable("gui.domesurvival.oxygen_filler.room.open");
            case TOO_LARGE -> Component.translatable(
                    "gui.domesurvival.oxygen_filler.room.too_large",
                    menu.getRoomVolume()
            );
            case UNLOADED -> Component.translatable("gui.domesurvival.oxygen_filler.room.unloaded");
            case LEAKING -> Component.translatable(
                    "gui.domesurvival.oxygen_filler.room.leaking",
                    menu.getRoomOxygen(),
                    menu.getRoomOxygenRequired(),
                    menu.getRoomPressurePercent()
            );
            case DEPRESSURIZED -> Component.translatable(
                    "gui.domesurvival.oxygen_filler.room.depressurized",
                    menu.getRoomOxygenRequired()
            );
            case UNKNOWN -> Component.translatable("gui.domesurvival.oxygen_filler.room.unknown");
        };
    }

    private int getStatusColor() {
        if (menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENTILATING) return 0xFFB8E9F2;
        if (menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENT_ROOM_FULL) return 0xFFA8D8B1;
        if (menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENT_ROOM_LEAKING) return 0xFFE6B36A;
        if (menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENT_ROOM_DEPRESSURIZED) return 0xFFD47A6B;
        if (menu.getStatus() == OxygenFillerBlockEntity.STATUS_FILLING) return OXYGEN_BRIGHT;
        if (menu.getStatus() == OxygenFillerBlockEntity.STATUS_NO_OXYGEN
                || menu.getStatus() == OxygenFillerBlockEntity.STATUS_NO_ENERGY
                || menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENT_OUTLET_BLOCKED
                || menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENT_ROOM_OPEN
                || menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENT_ROOM_TOO_LARGE
                || menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENT_ROOM_UNLOADED) {
            return 0xFFD4A36B;
        }
        return 0xFFA9B1B5;
    }

    private void drawModeButton(GuiGraphics gg, int x, int y, int w, int h) {
        boolean ventilation = menu.getOperatingMode() == OxygenFillerMode.VENTILATION;
        int fill = ventilation ? 0xFF24363A : 0xFF262C30;
        int rim = ventilation ? 0xFF6D949A : 0xFF4C555A;
        gg.fill(x, y, x + w, y + h, 0xFF0B0E10);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, rim);
        gg.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
        if (ventilation && menu.getStatus() == OxygenFillerBlockEntity.STATUS_VENTILATING) {
            gg.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFF91C7CF);
        }
    }

    private void drawSideModel(GuiGraphics gg, int mouseX, int mouseY) {
        for (RelativeSide side : RelativeSide.values()) {
            Rect rect = SIDE_RECTS.get(side);
            boolean hovered = side != RelativeSide.FRONT && rect.contains(mouseX, mouseY, leftPos, topPos);
            drawFace(gg, rect, side, menu.getSideMode(machineSideForVisualSide(side)), hovered);
        }
    }

    private void drawFace(GuiGraphics gg, Rect rect, RelativeSide side, SideMode mode, boolean hovered) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;
        int outer = hovered ? ENERGY_MAIN : 0xFF0E1214;
        int rim = hovered ? 0xFF5A5140 : 0xFF3D454A;
        int face = side == RelativeSide.FRONT ? 0xFF20262A : 0xFF252B2F;
        gg.fill(x, y, x + rect.w, y + rect.h, outer);
        gg.fill(x + 1, y + 1, x + rect.w - 1, y + rect.h - 1, rim);
        gg.fill(x + 2, y + 2, x + rect.w - 2, y + rect.h - 2, face);

        if (side == RelativeSide.FRONT) {
            gg.fill(x + 4, y + 4, x + rect.w - 4, y + rect.h - 4, 0xFF111619);
            gg.fill(x + 6, y + 6, x + rect.w - 6, y + rect.h - 5, OXYGEN_MAIN);
            gg.fill(x + 7, y + 6, x + rect.w - 7, y + 7, OXYGEN_BRIGHT);
            return;
        }

        int u = switch (mode) {
            case INPUT -> PORT_INPUT_U;
            case OUTPUT, BOTH -> PORT_OUTPUT_U;
            case DISABLED -> PORT_OFF_U;
        };
        gg.blit(PORT_TEXTURE,
                x + Math.max(1, (rect.w - PORT_SIZE) / 2),
                y + Math.max(1, (rect.h - PORT_SIZE) / 2),
                u, 0, PORT_SIZE, PORT_SIZE, PORT_TEX_WIDTH, PORT_TEX_HEIGHT);
    }

    private void drawGear(GuiGraphics gg, int x, int y, boolean active) {
        drawFrame(gg, x, y, GEAR_SIZE, GEAR_SIZE, active ? 0xFF3A3427 : 0xFF252B2F);
        int cx = x + 10;
        int cy = y + 10;
        int metal = active ? ENERGY_BRIGHT : 0xFF687278;
        gg.fill(cx - 5, cy - 2, cx + 5, cy + 2, metal);
        gg.fill(cx - 2, cy - 5, cx + 2, cy + 5, metal);
        gg.fill(cx - 4, cy - 4, cx + 4, cy + 4, metal);
        gg.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF151A1D);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }

    private RelativeSide getHoveredSide(double mouseX, double mouseY) {
        for (RelativeSide side : RelativeSide.values()) {
            if (side == RelativeSide.FRONT) continue;
            Rect rect = SIDE_RECTS.get(side);
            if (rect.contains(mouseX, mouseY, leftPos, topPos)) return side;
        }
        return null;
    }

    private static RelativeSide machineSideForVisualSide(RelativeSide visualSide) {
        return switch (visualSide) {
            case LEFT -> RelativeSide.RIGHT;
            case RIGHT -> RelativeSide.LEFT;
            default -> visualSide;
        };
    }

    private static Component getSideModeTooltip(SideMode mode) {
        return switch (mode) {
            case INPUT -> Component.translatable("gui.domesurvival.side_state.input");
            case OUTPUT, BOTH -> Component.translatable("gui.domesurvival.side_state.output");
            case DISABLED -> Component.translatable("gui.domesurvival.side_state.disabled");
        };
    }

    private static void drawPanel(GuiGraphics gg, int x, int y, int w, int h, int fill) {
        gg.fill(x, y, x + w, y + h, 0xFF0C0F11);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF464E53);
        gg.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
        gg.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFF50585D);
        gg.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, 0xFF50585D);
    }

    private static void drawFrame(GuiGraphics gg, int x, int y, int w, int h, int fill) {
        gg.fill(x, y, x + w, y + h, 0xFF0B0E10);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF4C555A);
        gg.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
    }

    private static void drawSlot(GuiGraphics gg, int x, int y, int size) {
        int inset = Math.max(2, (size - 16) / 2);
        gg.fill(x, y, x + size, y + size, 0xFF0D1012);
        gg.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF3E464B);
        gg.fill(x + inset, y + inset, x + inset + 16, y + inset + 16, 0xFF1B2125);
    }

    private static void fillHorizontal(GuiGraphics gg, int x, int y, int w, int h,
                                       int value, int capacity, int main, int bright) {
        if (value <= 0 || capacity <= 0) return;
        int fill = Math.min(w - 6, (int) ((long) value * (w - 6) / capacity));
        if (fill <= 0) return;
        gg.fill(x + 3, y + 3, x + 3 + fill, y + h - 3, main);
        gg.fill(x + 3, y + 4, x + 3 + fill, y + 6, bright);
    }

    private void drawClamped(GuiGraphics gg, Component component, int x, int y, int w, int color) {
        String value = component.getString();
        if (font.width(value) > w) {
            String dots = "...";
            value = font.plainSubstrByWidth(value, Math.max(0, w - font.width(dots))) + dots;
        }
        gg.drawString(font, value, x, y, color, false);
    }

    private void drawCentered(GuiGraphics gg, Component component, int x, int y, int w, int color) {
        String value = component.getString();
        if (font.width(value) > w) {
            String dots = "...";
            value = font.plainSubstrByWidth(value, Math.max(0, w - font.width(dots))) + dots;
        }
        gg.drawString(font, value, x + Math.max(0, (w - font.width(value)) / 2), y, color, false);
    }

    private static String compact(int value) {
        if (value < 1000) return Integer.toString(value);
        if (value % 1000 == 0) return (value / 1000) + "k";
        return String.format(Locale.ROOT, "%.1fk", value / 1000.0D);
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mouseX, double mouseY, int leftPos, int topPos) {
            double lx = mouseX - leftPos;
            double ly = mouseY - topPos;
            return lx >= x && lx < x + w && ly >= y && ly < y + h;
        }
    }
}
