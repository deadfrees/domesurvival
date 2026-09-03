package com.wasted.domesurvival.forge.machine.forming;

import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.EnumMap;

public final class FormingPressScreen extends AbstractContainerScreen<FormingPressMenu> {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 222;
    private static final int ENERGY_X = 18;
    private static final int ENERGY_Y = 42;
    private static final int ENERGY_W = 18;
    private static final int ENERGY_H = 58;
    private static final int PROGRESS_X = 91;
    private static final int PROGRESS_Y = 67;
    private static final int PROGRESS_W = 34;
    private static final int PROGRESS_H = 8;

    private static final EnumMap<RelativeSide, Rect> SIDE_RECTS = createSideRects();

    public FormingPressScreen(FormingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 12;
        inventoryLabelY = 119;
        titleLabelX = 0;
        titleLabelY = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.forming_press.energy_value",
                            menu.energyStored(),
                            menu.energyCapacity()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }

        RelativeSide side = hoveredSide(mouseX, mouseY);
        if (side != null) {
            String sideKey = side.name().toLowerCase(java.util.Locale.ROOT);
            Component sideName = Component.translatable("gui.domesurvival.forming_press.side." + sideKey);
            Component mode = side == RelativeSide.FRONT
                    ? Component.translatable("gui.domesurvival.forming_press.front_reserved")
                    : Component.translatable("gui.domesurvival.forming_press.mode."
                            + menu.getSideMode(side).getSerializedName());
            graphics.renderTooltip(font, Component.literal(sideName.getString() + ": " + mode.getString()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            RelativeSide side = hoveredSide(mouseX, mouseY);
            if (side != null && FormingPressBlockEntity.isConfigurableSide(side)
                    && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FormingPressMenu.sideButtonId(side));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        panel(graphics, x, y, WIDTH, HEIGHT, 0xFF252D31);
        panel(graphics, x + 8, y + 29, WIDTH - 16, 82, 0xFF182025);

        slot(graphics, x + 60, y + 58);
        slot(graphics, x + 132, y + 58);

        panel(graphics, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H, 0xFF0B1013);
        int capacity = Math.max(1, menu.energyCapacity());
        int fillHeight = Math.min(ENERGY_H - 6,
                (int) ((long) menu.energyStored() * (ENERGY_H - 6) / capacity));
        if (fillHeight > 0) {
            int bottom = y + ENERGY_Y + ENERGY_H - 3;
            graphics.fill(x + ENERGY_X + 3, bottom - fillHeight,
                    x + ENERGY_X + ENERGY_W - 3, bottom, 0xFFB77A3E);
            graphics.fill(x + ENERGY_X + 4, bottom - fillHeight,
                    x + ENERGY_X + 7, bottom, 0xFFE3AE61);
        }

        panel(graphics, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H, 0xFF0B1013);
        int progressMax = menu.progressMax();
        int progress = progressMax <= 0 ? 0
                : (int) ((long) (PROGRESS_W - 6) * menu.progress() / progressMax);
        if (progress > 0) {
            graphics.fill(x + PROGRESS_X + 3, y + PROGRESS_Y + 3,
                    x + PROGRESS_X + 3 + progress, y + PROGRESS_Y + PROGRESS_H - 3, 0xFFD38B46);
        }

        for (var entry : SIDE_RECTS.entrySet()) {
            Rect rect = entry.getValue();
            SideMode mode = entry.getKey() == RelativeSide.FRONT
                    ? SideMode.DISABLED
                    : menu.getSideMode(entry.getKey());
            graphics.fill(x + rect.x, y + rect.y, x + rect.x + rect.w, y + rect.y + rect.h, 0xFF080B0D);
            graphics.fill(x + rect.x + 2, y + rect.y + 2,
                    x + rect.x + rect.w - 2, y + rect.y + rect.h - 2, modeColor(mode));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(graphics, x + 8 + column * 22, y + 128 + row * 22);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(graphics, x + 8 + column * 22, y + 194);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, WIDTH / 2, 10, 0xFFD6E0E4);
        graphics.drawCenteredString(font,
                Component.translatable("gui.domesurvival.forming_press.input"), 71, 43, 0xFFBFC8CC);
        graphics.drawCenteredString(font,
                Component.translatable("gui.domesurvival.forming_press.output"), 143, 43, 0xFFBFC8CC);
        graphics.drawCenteredString(font,
                Component.translatable("gui.domesurvival.forming_press.sides"), 190, 18, 0xFFBFC8CC);
        graphics.drawCenteredString(font, statusText(), 108, 94, statusColor());
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFBFC8CC, false);
    }

    private Component statusText() {
        String key = switch (menu.status()) {
            case FormingPressBlockEntity.STATUS_FORMING -> "forming";
            case FormingPressBlockEntity.STATUS_NO_ENERGY -> "no_energy";
            case FormingPressBlockEntity.STATUS_NO_RECIPE -> "no_recipe";
            case FormingPressBlockEntity.STATUS_OUTPUT_FULL -> "output_full";
            default -> "ready";
        };
        return Component.translatable("gui.domesurvival.forming_press.status." + key);
    }

    private int statusColor() {
        return switch (menu.status()) {
            case FormingPressBlockEntity.STATUS_READY -> 0xFF7ED69A;
            case FormingPressBlockEntity.STATUS_FORMING -> 0xFFE8B86B;
            default -> 0xFFE28A74;
        };
    }

    private RelativeSide hoveredSide(double mouseX, double mouseY) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        for (var entry : SIDE_RECTS.entrySet()) {
            if (entry.getValue().contains(localX, localY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> result = new EnumMap<>(RelativeSide.class);
        result.put(RelativeSide.TOP, new Rect(184, 36, 12, 12));
        result.put(RelativeSide.LEFT, new Rect(166, 54, 12, 12));
        result.put(RelativeSide.FRONT, new Rect(184, 54, 12, 12));
        result.put(RelativeSide.RIGHT, new Rect(202, 54, 12, 12));
        result.put(RelativeSide.BOTTOM, new Rect(184, 72, 12, 12));
        result.put(RelativeSide.BACK, new Rect(202, 72, 12, 12));
        return result;
    }

    private static int modeColor(SideMode mode) {
        return switch (mode) {
            case INPUT -> 0xFF3D7FA8;
            case OUTPUT, BOTH -> 0xFFC77A3D;
            case DISABLED -> 0xFF30383D;
        };
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF0A0E10);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF667278);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, color);
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 22, y + 22, 0xFF080B0D);
        graphics.fill(x + 1, y + 1, x + 21, y + 21, 0xFF657177);
        graphics.fill(x + 3, y + 3, x + 19, y + 19, 0xFF11171B);
    }

    private record Rect(int x, int y, int w, int h) {
        private boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }
}
