package com.wasted.domesurvival.forge.machine.transformer;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Transformer GUI using the same visual system as CoalGeneratorScreen.
 *
 * <p>The transformer has fixed physical ports, so unlike configurable machines there is
 * intentionally no side-config gear or side panel.</p>
 */
public final class TransformerScreen extends AbstractContainerScreen<TransformerMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(
                    DomeSurvival.MOD_ID,
                    "textures/gui/coal_generator_ports.png"
            );

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 266;
    private static final int MACHINE_PANEL_WIDTH = 220;

    private static final int ENERGY_METER_X = 14;
    private static final int ENERGY_METER_Y = 37;
    private static final int ENERGY_METER_W = 18;
    private static final int ENERGY_METER_H = 53;

    private static final int ENERGY_VALUE_X = 42;
    private static final int ENERGY_VALUE_Y = 38;
    private static final int ENERGY_VALUE_W = 166;
    private static final int ENERGY_VALUE_H = 15;

    private static final int MODE_X = 42;
    private static final int MODE_Y = 64;
    private static final int MODE_W = 166;
    private static final int MODE_H = 22;

    private static final int INPUT_BOX_X = 42;
    private static final int OUTPUT_BOX_X = 178;
    private static final int PORT_BOX_Y = 96;
    private static final int PORT_BOX_SIZE = 24;

    private static final int INVENTORY_X = 11;
    private static final int INVENTORY_Y = 158;
    private static final int INVENTORY_SLOT_SIZE = 22;
    private static final int INVENTORY_SLOT_STEP = 22;
    private static final int HOTBAR_Y = 226;

    private static final int PORT_SIZE = 6;
    private static final int PORT_TEX_WIDTH = 24;
    private static final int PORT_TEX_HEIGHT = 6;
    private static final int PORT_INPUT_U = 12;
    private static final int PORT_OUTPUT_U = 18;

    public TransformerScreen(
            TransformerMenu menu,
            Inventory playerInventory,
            Component title
    ) {
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

    private boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;

        return localX >= x
                && localX < x + width
                && localY >= y
                && localY < y + height;
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (inside(
                mouseX,
                mouseY,
                MODE_X,
                MODE_Y,
                MODE_W,
                MODE_H
        )) {
            guiGraphics.renderComponentTooltip(
                    font,
                    menu.validTopology()
                            ? List.of(
                                    Component.translatable(
                                            "gui.domesurvival.transformer.auto_mode"
                                    ),
                                    Component.translatable(
                                            "gui.domesurvival.transformer.mode_rates",
                                            menu.inputRate(),
                                            menu.outputRate()
                                    )
                            )
                            : List.of(
                                    Component.translatable(
                                            "gui.domesurvival.transformer.invalid_topology"
                                    ),
                                    Component.translatable(
                                            "gui.domesurvival.transformer.adjacent_tiers_only"
                                    )
                            ),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY
            );
            return;
        }

        if (isHovering(
                ENERGY_METER_X,
                ENERGY_METER_Y,
                ENERGY_METER_W,
                ENERGY_METER_H,
                mouseX,
                mouseY
        ) || isHovering(
                ENERGY_VALUE_X,
                ENERGY_VALUE_Y,
                ENERGY_VALUE_W,
                ENERGY_VALUE_H,
                mouseX,
                mouseY
        )) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.domesurvival.transformer.energy_tooltip",
                            menu.energyStored(),
                            menu.energyCapacity()
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int x = leftPos;
        int y = topPos;

        drawIndustrialPanel(
                guiGraphics,
                x,
                y,
                MACHINE_PANEL_WIDTH,
                PANEL_HEIGHT,
                0xFF30363A
        );

        drawThinFrame(
                guiGraphics,
                x + ENERGY_METER_X,
                y + ENERGY_METER_Y,
                ENERGY_METER_W,
                ENERGY_METER_H,
                0xFF14191C
        );
        drawThinFrame(
                guiGraphics,
                x + ENERGY_VALUE_X,
                y + ENERGY_VALUE_Y,
                ENERGY_VALUE_W,
                ENERGY_VALUE_H,
                0xFF151A1D
        );

        int capacity = Math.max(1, menu.energyCapacity());
        int innerHeight = ENERGY_METER_H - 6;
        int energyHeight = Math.min(
                innerHeight,
                (int) ((long) menu.energyStored() * innerHeight / capacity)
        );

        if (energyHeight > 0) {
            int fillBottom =
                    y + ENERGY_METER_Y + ENERGY_METER_H - 3;
            int fillTop = fillBottom - energyHeight;

            guiGraphics.fill(
                    x + ENERGY_METER_X + 3,
                    fillTop,
                    x + ENERGY_METER_X + ENERGY_METER_W - 3,
                    fillBottom,
                    0xFF8D792A
            );
            guiGraphics.fill(
                    x + ENERGY_METER_X + 4,
                    fillTop,
                    x + ENERGY_METER_X + 6,
                    fillBottom,
                    0xFFAA9438
            );
        }

        int modeBackground = menu.validTopology() ? 0xFF252B2F : 0xFF2E2424;

        drawThinFrame(
                guiGraphics,
                x + MODE_X,
                y + MODE_Y,
                MODE_W,
                MODE_H,
                modeBackground
        );

        // Fixed physical rear input / front output indicators.
        drawThinFrame(
                guiGraphics,
                x + INPUT_BOX_X,
                y + PORT_BOX_Y,
                PORT_BOX_SIZE,
                PORT_BOX_SIZE,
                0xFF171C20
        );
        drawThinFrame(
                guiGraphics,
                x + OUTPUT_BOX_X,
                y + PORT_BOX_Y,
                PORT_BOX_SIZE,
                PORT_BOX_SIZE,
                0xFF171C20
        );

        blitPortSprite(
                guiGraphics,
                x + INPUT_BOX_X + 9,
                y + PORT_BOX_Y + 9,
                PORT_INPUT_U
        );
        blitPortSprite(
                guiGraphics,
                x + OUTPUT_BOX_X + 9,
                y + PORT_BOX_Y + 9,
                PORT_OUTPUT_U
        );

        drawTransferArrow(guiGraphics, x + 76, y + 103, menu.active());

        guiGraphics.fill(
                x + 10,
                y + 153,
                x + MACHINE_PANEL_WIDTH - 10,
                y + 154,
                0xFF171B1F
        );
        guiGraphics.fill(
                x + 10,
                y + 154,
                x + MACHINE_PANEL_WIDTH - 10,
                y + 155,
                0xFF4B5359
        );

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

    }

    private static void drawTransferArrow(
            GuiGraphics guiGraphics,
            int x,
            int y,
            boolean active
    ) {
        int color = active ? 0xFFB9A246 : 0xFF687278;

        guiGraphics.fill(x, y + 5, x + 84, y + 8, color);
        guiGraphics.fill(x + 78, y + 1, x + 84, y + 12, color);
        guiGraphics.fill(x + 84, y + 3, x + 88, y + 10, color);
    }

    private static void blitPortSprite(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int u
    ) {
        guiGraphics.blit(
                PORT_TEXTURE,
                x,
                y,
                u,
                0,
                PORT_SIZE,
                PORT_SIZE,
                PORT_TEX_WIDTH,
                PORT_TEX_HEIGHT
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        drawClampedText(
                guiGraphics,
                title,
                10,
                8,
                MACHINE_PANEL_WIDTH - 20,
                0xFFE0E4E6
        );

        drawClampedText(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.transformer.energy"
                ),
                14,
                24,
                MACHINE_PANEL_WIDTH - 28,
                0xFFC5CBCD
        );

        drawCenteredClampedText(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.transformer.energy_compact",
                        compactFe(menu.energyStored()),
                        compactFe(menu.energyCapacity())
                ),
                ENERGY_VALUE_X + 3,
                ENERGY_VALUE_Y + 4,
                ENERGY_VALUE_W - 6,
                0xFFB9A246
        );

        drawCenteredClampedText(
                guiGraphics,
                Component.literal(modeLabel()),
                MODE_X + 3,
                MODE_Y + 7,
                MODE_W - 6,
                menu.validTopology() ? 0xFF83B58A : 0xFFC47D6A
        );

        drawCenteredClampedText(
                guiGraphics,
                menu.validTopology()
                        ? Component.translatable(
                                "gui.domesurvival.transformer.input_rate",
                                menu.inputRate()
                        )
                        : Component.translatable(
                                "gui.domesurvival.transformer.input_port"
                        ),
                INPUT_BOX_X - 12,
                124,
                72,
                0xFF6EB6E5
        );

        drawCenteredClampedText(
                guiGraphics,
                menu.validTopology()
                        ? Component.translatable(
                                "gui.domesurvival.transformer.output_rate",
                                menu.outputRate()
                        )
                        : Component.translatable(
                                "gui.domesurvival.transformer.output_port"
                        ),
                OUTPUT_BOX_X - 36,
                124,
                72,
                0xFFE3A36B
        );

        drawCenteredClampedText(
                guiGraphics,
                Component.translatable(
                        menu.active()
                                ? "gui.domesurvival.transformer.active"
                                : "gui.domesurvival.transformer.idle",
                        menu.inputThisTick(),
                        menu.outputThisTick()
                ),
                14,
                139,
                194,
                menu.active() ? 0xFF83B58A : 0xFF9DA5A9
        );

        drawClampedText(
                guiGraphics,
                playerInventoryTitle,
                14,
                146,
                194,
                0xFFC5CBCD
        );
    }

    private String modeLabel() {
        TransformerMode mode = menu.mode();

        if (mode == null) {
            return "AUTO: НЕТ СВЯЗКИ";
        }

        return switch (mode) {
            case LV_TO_MV -> "AUTO: LV  →  MV";
            case MV_TO_LV -> "AUTO: MV  →  LV";
            case MV_TO_HV -> "AUTO: MV  →  HV";
            case HV_TO_MV -> "AUTO: HV  →  MV";
        };
    }

    private void drawClampedText(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        String value = text.getString();

        guiGraphics.enableScissor(
                leftPos + x,
                topPos + y,
                leftPos + x + maxWidth,
                topPos + y + font.lineHeight + 1
        );

        if (font.width(value) <= maxWidth) {
            guiGraphics.drawString(
                    font,
                    value,
                    x,
                    y,
                    color,
                    false
            );
        } else {
            String dots = "...";
            int usableWidth =
                    Math.max(0, maxWidth - font.width(dots));
            String clipped =
                    font.plainSubstrByWidth(value, usableWidth);
            guiGraphics.drawString(
                    font,
                    clipped + dots,
                    x,
                    y,
                    color,
                    false
            );
        }

        guiGraphics.disableScissor();
    }

    private void drawCenteredClampedText(
            GuiGraphics guiGraphics,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        String value = text.getString();

        if (font.width(value) > maxWidth) {
            String dots = "...";
            int usableWidth =
                    Math.max(0, maxWidth - font.width(dots));
            value =
                    font.plainSubstrByWidth(value, usableWidth) + dots;
        }

        int drawX =
                x + Math.max(0, (maxWidth - font.width(value)) / 2);

        guiGraphics.enableScissor(
                leftPos + x,
                topPos + y,
                leftPos + x + maxWidth,
                topPos + y + font.lineHeight + 1
        );
        guiGraphics.drawString(
                font,
                value,
                drawX,
                y,
                color,
                false
        );
        guiGraphics.disableScissor();
    }

    private static String compactFe(int value) {
        if (value < 1_000) {
            return Integer.toString(value);
        }

        if (value % 1_000 == 0) {
            return (value / 1_000) + "k";
        }

        return String.format(
                Locale.ROOT,
                "%.1fk",
                value / 1_000.0D
        );
    }

    private static void drawIndustrialPanel(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        guiGraphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xFF0C0F11
        );
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + width - 1,
                y + height - 1,
                0xFF464E53
        );
        guiGraphics.fill(
                x + 2,
                y + 2,
                x + width - 2,
                y + height - 2,
                fillColor
        );
        guiGraphics.fill(
                x + 3,
                y + 3,
                x + width - 3,
                y + 4,
                0xFF50585D
        );
        guiGraphics.fill(
                x + 3,
                y + height - 4,
                x + width - 3,
                y + height - 3,
                0xFF14181B
        );
    }

    private static void drawThinFrame(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        guiGraphics.fill(
                x,
                y,
                x + width,
                y + height,
                0xFF0B0E10
        );
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + width - 1,
                y + height - 1,
                0xFF4C555A
        );
        guiGraphics.fill(
                x + 2,
                y + 2,
                x + width - 2,
                y + height - 2,
                fillColor
        );
    }

    private static void drawSlot(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int size
    ) {
        int contentSize = 16;
        int inset = Math.max(2, (size - contentSize) / 2);

        guiGraphics.fill(
                x,
                y,
                x + size,
                y + size,
                0xFF0D1012
        );
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + size - 1,
                y + size - 1,
                0xFF3E464B
        );
        guiGraphics.fill(
                x + inset,
                y + inset,
                x + inset + contentSize,
                y + inset + contentSize,
                0xFF1B2125
        );
    }
}
