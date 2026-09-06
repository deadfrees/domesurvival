package com.wasted.domesurvival.forge.machine.forming;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * Forming Press UI built directly on the current CoalGeneratorScreen visual system.
 *
 * <p>This intentionally reuses the Coal Generator's exact main panel size, industrial
 * frame helpers, 22 px inventory cells, gear button, 96x122 side-routing panel,
 * cube-net connector geometry, RF port atlas and text clipping rules.</p>
 */
public final class FormingPressScreen extends AbstractContainerScreen<FormingPressMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    // Exact CoalGeneratorScreen composite geometry.
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

    // Forming workflow fitted into the Coal Generator's machine section.
    private static final int INPUT_SLOT_BG_X = 42;
    private static final int OUTPUT_SLOT_BG_X = 178;
    private static final int PROCESS_SLOT_BG_Y = 62;
    private static final int PROCESS_SLOT_BG_SIZE = 24;

    private static final int PROGRESS_X = 78;
    private static final int PROGRESS_Y = 67;
    private static final int PROGRESS_W = 90;
    private static final int PROGRESS_H = 14;

    private static final int OPERATION_X = 24;
    private static final int OPERATION_Y = 108;
    private static final int OPERATION_W = 30;
    private static final int OPERATION_H = 26;
    private static final int OPERATION_STEP = 36;
    private static final EnumMap<FormingOperation, Rect> OPERATION_RECTS = createOperationRects();

    // Exact Coal Generator inventory geometry.
    private static final int INVENTORY_X = 11;
    private static final int INVENTORY_Y = 158;
    private static final int INVENTORY_SLOT_SIZE = 22;
    private static final int INVENTORY_SLOT_STEP = 22;
    private static final int HOTBAR_Y = 226;

    // Exact Coal Generator side-routing panel geometry.
    private static final Rect SIDE_MODEL_FRAME = new Rect(SIDE_PANEL_X + 8, 28, 80, 90);
    private static final int SIDE_BUTTON_SIZE = 14;
    private static final int SIDE_GRID_STEP = 22;
    private static final EnumMap<RelativeSide, Rect> SIDE_RECTS = createSideRects();

    // Exact Coal Generator 6x6 port atlas.
    private static final int PORT_SIZE = 6;
    private static final int PORT_TEX_WIDTH = 24;
    private static final int PORT_TEX_HEIGHT = 6;
    private static final int PORT_OFF_U = 0;
    private static final int PORT_INPUT_U = 12;
    private static final int PORT_OUTPUT_U = 18;

    private final EnumMap<FormingOperation, ItemStack> operationIcons =
            new EnumMap<>(FormingOperation.class);

    private boolean sidePanelOpen;

    public FormingPressScreen(FormingPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;

        operationIcons.put(FormingOperation.PRESS, stackPrefer("steel_plate", "copper_plate"));
        operationIcons.put(FormingOperation.GEAR, stackPrefer("steel_gear", "copper_gear"));
        operationIcons.put(FormingOperation.ROD, stackPrefer("steel_rod", "copper_rod"));
        operationIcons.put(FormingOperation.WIRE, stackPrefer("steel_wire", "copper_wire"));
        operationIcons.put(FormingOperation.TUBE, stackPrefer("steel_tube", "copper_tube"));
    }

    @Override
    protected void init() {
        super.init();

        // Same centering rule as CoalGeneratorScreen: center only the 220 px machine
        // panel; the 96 px connector panel opens to its right.
        leftPos = (width - MACHINE_PANEL_WIDTH) / 2;
        topPos = (height - imageHeight) / 2;
    }

    private static EnumMap<FormingOperation, Rect> createOperationRects() {
        EnumMap<FormingOperation, Rect> result = new EnumMap<>(FormingOperation.class);
        for (FormingOperation operation : FormingOperation.values()) {
            result.put(
                    operation,
                    new Rect(
                            OPERATION_X + operation.ordinal() * OPERATION_STEP,
                            OPERATION_Y,
                            OPERATION_W,
                            OPERATION_H
                    )
            );
        }
        return result;
    }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> regions = new EnumMap<>(RelativeSide.class);

        int centerX = SIDE_PANEL_X + (SIDE_PANEL_WIDTH - SIDE_BUTTON_SIZE) / 2;
        int middleY = 66;

        regions.put(
                RelativeSide.TOP,
                new Rect(centerX, middleY - SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE)
        );
        regions.put(
                RelativeSide.LEFT,
                new Rect(centerX - SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE)
        );
        regions.put(
                RelativeSide.FRONT,
                new Rect(centerX, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE)
        );
        regions.put(
                RelativeSide.RIGHT,
                new Rect(centerX + SIDE_GRID_STEP, middleY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE)
        );
        regions.put(
                RelativeSide.BOTTOM,
                new Rect(centerX, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE)
        );
        regions.put(
                RelativeSide.BACK,
                new Rect(centerX + SIDE_GRID_STEP, middleY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE)
        );

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
                            FormingPressMenu.sideButtonId(machineSide)
                    );
                    return true;
                }
            }

            FormingOperation operation = getHoveredOperation(mouseX, mouseY);
            if (operation != null && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId,
                        FormingPressMenu.operationButtonId(operation)
                );
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;
        return localX >= x && localX < x + width
                && localY >= y && localY < y + height;
    }

    /**
     * Matches CoalGeneratorScreen's player-facing projection.
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
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("gui.domesurvival.side_config"),
                    mouseX,
                    mouseY
            );
            return;
        }

        FormingOperation hoveredOperation = getHoveredOperation(mouseX, mouseY);
        if (hoveredOperation != null) {
            guiGraphics.renderTooltip(
                    font,
                    operationName(hoveredOperation),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (sidePanelOpen) {
            RelativeSide hoveredSide = getHoveredSide(mouseX, mouseY);
            if (hoveredSide != null) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(sideTranslationKey(hoveredSide)));
                tooltip.add(getSideModeTooltip(
                        menu.getSideMode(machineSideForVisualSide(hoveredSide))
                ));
                guiGraphics.renderComponentTooltip(
                        font,
                        tooltip,
                        mouseX,
                        mouseY,
                        ItemStack.EMPTY
                );
                return;
            }
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
                            "gui.domesurvival.forming_press.energy_tooltip",
                            menu.energyStored(),
                            menu.energyCapacity()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)
                && menu.progressMax() > 0) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(processChain(menu.operation()));
            tooltip.add(Component.translatable(
                    "gui.domesurvival.forming_press.recipe_tooltip",
                    menu.requiredInputCount(),
                    menu.recipeEnergy(),
                    String.format(Locale.ROOT, "%.1f", menu.progressMax() / 20.0D)
            ));
            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
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

        // Exact CoalGeneratorScreen panel helper and colors.
        drawIndustrialPanel(guiGraphics, x, y, MACHINE_PANEL_WIDTH, PANEL_HEIGHT, 0xFF30363A);

        // Exact Coal Generator energy section geometry and colors.
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
        int energyInnerHeight = ENERGY_METER_H - 6;
        int energyHeight = Math.min(
                energyInnerHeight,
                (int) ((long) menu.energyStored() * energyInnerHeight / capacity)
        );

        if (energyHeight > 0) {
            int fillBottom = y + ENERGY_METER_Y + ENERGY_METER_H - 3;
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

        // Machine input/output slots use exactly the Coal Generator's 24 px slot frame.
        drawSlot(
                guiGraphics,
                x + INPUT_SLOT_BG_X,
                y + PROCESS_SLOT_BG_Y,
                PROCESS_SLOT_BG_SIZE
        );
        drawSlot(
                guiGraphics,
                x + OUTPUT_SLOT_BG_X,
                y + PROCESS_SLOT_BG_Y,
                PROCESS_SLOT_BG_SIZE
        );

        // Processing bar uses the Coal Generator thin-frame / brown fill language.
        drawThinFrame(
                guiGraphics,
                x + PROGRESS_X,
                y + PROGRESS_Y,
                PROGRESS_W,
                PROGRESS_H,
                0xFF151A1D
        );

        int maxProgress = menu.progressMax();
        int progressWidth = maxProgress <= 0
                ? 0
                : Math.min(
                        PROGRESS_W - 6,
                        menu.progress() * (PROGRESS_W - 6) / Math.max(1, maxProgress)
                );

        if (progressWidth > 0) {
            guiGraphics.fill(
                    x + PROGRESS_X + 3,
                    y + PROGRESS_Y + 3,
                    x + PROGRESS_X + 3 + progressWidth,
                    y + PROGRESS_Y + PROGRESS_H - 3,
                    0xFF744128
            );
            guiGraphics.fill(
                    x + PROGRESS_X + 3,
                    y + PROGRESS_Y + 4,
                    x + PROGRESS_X + 3 + progressWidth,
                    y + PROGRESS_Y + 6,
                    0xFF925034
            );
        }

        // Five forming operations use the same compact framed-button vocabulary as
        // the Coal Generator gear button; real vanilla item icons stay crisp at 16x16.
        for (FormingOperation operation : FormingOperation.values()) {
            drawOperationButton(
                    guiGraphics,
                    OPERATION_RECTS.get(operation),
                    operation,
                    operation == menu.operation()
            );
        }

        // Exact Coal Generator inventory divider and 22 px inventory grid.
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

        // Exact Coal Generator gear button + expandable routing panel.
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

    private void drawOperationButton(
            GuiGraphics guiGraphics,
            Rect rect,
            FormingOperation operation,
            boolean active
    ) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;
        int background = active ? 0xFF394247 : 0xFF252B2F;

        drawThinFrame(
                guiGraphics,
                x,
                y,
                rect.width,
                rect.height,
                background
        );

        ItemStack icon = operationIcons.get(operation);
        if (icon != null && !icon.isEmpty()) {
            int iconX = x + (rect.width - 16) / 2;
            int iconY = y + (rect.height - 16) / 2;
            guiGraphics.renderItem(icon, iconX, iconY);
        }
    }

    private void drawGearButton(
            GuiGraphics guiGraphics,
            int x,
            int y,
            boolean active
    ) {
        int background = active ? 0xFF394247 : 0xFF252B2F;
        drawThinFrame(guiGraphics, x, y, GEAR_SIZE, GEAR_SIZE, background);

        int centerX = x + GEAR_SIZE / 2;
        int centerY = y + GEAR_SIZE / 2;
        int metal = active ? 0xFF869197 : 0xFF687278;

        guiGraphics.fill(centerX - 5, centerY - 2, centerX + 5, centerY + 2, metal);
        guiGraphics.fill(centerX - 2, centerY - 5, centerX + 2, centerY + 5, metal);
        guiGraphics.fill(centerX - 4, centerY - 4, centerX + 4, centerY + 4, metal);
        guiGraphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFF151A1D);
    }

    private void drawSideModel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (RelativeSide visualSide : RelativeSide.values()) {
            Rect rect = SIDE_RECTS.get(visualSide);
            boolean hovered = visualSide != RelativeSide.FRONT
                    && rect.contains(mouseX, mouseY, leftPos, topPos);

            RelativeSide machineSide = machineSideForVisualSide(visualSide);

            drawMachineFace(
                    guiGraphics,
                    rect,
                    visualSide,
                    menu.getSideMode(machineSide),
                    hovered
            );
        }
    }

    private void drawMachineFace(
            GuiGraphics guiGraphics,
            Rect rect,
            RelativeSide side,
            SideMode mode,
            boolean hovered
    ) {
        int x = leftPos + rect.x;
        int y = topPos + rect.y;

        int outer = hovered ? 0xFF697278 : 0xFF0E1214;
        int rim = hovered ? 0xFF50585E : 0xFF3D454A;
        int face = side == RelativeSide.FRONT ? 0xFF20262A : 0xFF252B2F;

        guiGraphics.fill(x, y, x + rect.width, y + rect.height, outer);
        guiGraphics.fill(x + 1, y + 1, x + rect.width - 1, y + rect.height - 1, rim);
        guiGraphics.fill(x + 2, y + 2, x + rect.width - 2, y + rect.height - 2, face);

        if (side == RelativeSide.FRONT) {
            guiGraphics.fill(
                    x + 4,
                    y + 5,
                    x + rect.width - 4,
                    y + rect.height - 4,
                    0xFF121719
            );
            for (int i = 0; i < 3; i++) {
                int ventX = x + 5 + i * 2;
                guiGraphics.fill(
                        ventX,
                        y + 7,
                        ventX + 1,
                        y + rect.height - 6,
                        0xFF424A4F
                );
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

    /**
     * Exact CoalGeneratorScreen panel renderer.
     */
    private static void drawIndustrialPanel(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0C0F11);
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

    /**
     * Exact CoalGeneratorScreen thin-frame renderer.
     */
    private static void drawThinFrame(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0B0E10);
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

    /**
     * Exact CoalGeneratorScreen slot renderer.
     */
    private static void drawSlot(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int size
    ) {
        int contentSize = 16;
        int inset = Math.max(2, (size - contentSize) / 2);

        guiGraphics.fill(x, y, x + size, y + size, 0xFF0D1012);
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

    @Override
    protected void renderLabels(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        // Same title position/clipping as CoalGeneratorScreen.
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
                Component.translatable("gui.domesurvival.forming_press.energy_section"),
                14,
                24,
                MACHINE_PANEL_WIDTH - 28,
                0xFFC5CBCD
        );

        drawCenteredClampedText(
                guiGraphics,
                Component.translatable(
                        "gui.domesurvival.forming_press.energy_compact",
                        compactRf(menu.energyStored()),
                        compactRf(menu.energyCapacity())
                ),
                ENERGY_VALUE_X + 3,
                ENERGY_VALUE_Y + 4,
                ENERGY_VALUE_W - 6,
                0xFFB9A246
        );

        // Single centered status row above the mode buttons.
        drawCenteredClampedText(
                guiGraphics,
                statusText(),
                14,
                96,
                194,
                statusColor()
        );

        drawClampedText(
                guiGraphics,
                playerInventoryTitle,
                14,
                140,
                194,
                0xFFC5CBCD
        );

        if (sidePanelOpen) {
            drawCenteredClampedText(
                    guiGraphics,
                    Component.translatable("gui.domesurvival.side_config"),
                    SIDE_PANEL_X + 4,
                    8,
                    SIDE_PANEL_WIDTH - 8,
                    0xFFE0E4E6
            );
        }
    }

    private Component statusText() {
        Component operation = operationName(menu.operation());

        if (menu.status() == FormingPressBlockEntity.STATUS_NOT_ENOUGH_INPUT) {
            return Component.translatable(
                    "gui.domesurvival.forming_press.status.not_enough_input_short",
                    menu.inputCount(),
                    menu.requiredInputCount()
            );
        }

        if (menu.status() == FormingPressBlockEntity.STATUS_NO_RECIPE) {
            @Nullable FormingOperation suggested = menu.suggestedOperation();
            if (suggested != null && suggested != menu.operation()) {
                return Component.translatable(
                        "gui.domesurvival.forming_press.status.try_mode",
                        operationName(suggested)
                );
            }
        }

        String statusKey = switch (menu.status()) {
            case FormingPressBlockEntity.STATUS_FORMING -> "forming";
            case FormingPressBlockEntity.STATUS_NO_ENERGY -> "no_energy";
            case FormingPressBlockEntity.STATUS_NO_RECIPE -> "no_recipe";
            case FormingPressBlockEntity.STATUS_OUTPUT_FULL -> "output_full";
            default -> "ready";
        };

        return Component.literal(
                operation.getString()
                        + " · "
                        + Component.translatable(
                                "gui.domesurvival.forming_press.status." + statusKey
                        ).getString()
        );
    }

    private int statusColor() {
        return switch (menu.status()) {
            case FormingPressBlockEntity.STATUS_READY -> 0xFF83B58A;
            case FormingPressBlockEntity.STATUS_FORMING -> 0xFFB9A246;
            case FormingPressBlockEntity.STATUS_NOT_ENOUGH_INPUT -> 0xFFB8A75B;
            default -> 0xFFC47D6A;
        };
    }

    private Component processChain(FormingOperation operation) {
        return Component.translatable(
                "gui.domesurvival.forming_press.chain." + operation.getSerializedName()
        );
    }

    private Component operationName(FormingOperation operation) {
        return Component.literal(switch (operation) {
            case PRESS -> "Прокатка";
            case GEAR -> "Штамповка";
            case ROD -> "Вытяжка";
            case WIRE -> "Волочение";
            case TUBE -> "Гибка";
        });
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
            guiGraphics.drawString(font, value, x, y, color, false);
        } else {
            String dots = "...";
            int usableWidth = Math.max(0, maxWidth - font.width(dots));
            String clipped = font.plainSubstrByWidth(value, usableWidth);
            guiGraphics.drawString(font, clipped + dots, x, y, color, false);
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
            int usableWidth = Math.max(0, maxWidth - font.width(dots));
            value = font.plainSubstrByWidth(value, usableWidth) + dots;
        }

        int drawX = x + Math.max(0, (maxWidth - font.width(value)) / 2);

        guiGraphics.enableScissor(
                leftPos + x,
                topPos + y,
                leftPos + x + maxWidth,
                topPos + y + font.lineHeight + 1
        );
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

    private FormingOperation getHoveredOperation(double mouseX, double mouseY) {
        for (FormingOperation operation : FormingOperation.values()) {
            Rect rect = OPERATION_RECTS.get(operation);
            if (rect.contains(mouseX, mouseY, leftPos, topPos)) {
                return operation;
            }
        }
        return null;
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
            case OUTPUT -> Component.translatable("gui.domesurvival.side_state.output");
            case BOTH -> Component.translatable("gui.domesurvival.forming_press.side_state.both");
            case DISABLED -> Component.translatable("gui.domesurvival.side_state.disabled");
        };
    }

    private static ItemStack stackPrefer(String primary, String fallback) {
        Item item = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(DomeSurvival.MOD_ID, primary)
        );
        if (item != null) {
            return new ItemStack(item);
        }
        return stack(fallback);
    }

    private static ItemStack stack(String path) {
        Item item = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(DomeSurvival.MOD_ID, path)
        );
        return item == null
                ? new ItemStack(Items.BARRIER)
                : new ItemStack(item);
    }

    private record Rect(int x, int y, int width, int height) {
        private boolean contains(
                double mouseX,
                double mouseY,
                int leftPos,
                int topPos
        ) {
            double localX = mouseX - leftPos;
            double localY = mouseY - topPos;
            return localX >= x && localX < x + width
                    && localY >= y && localY < y + height;
        }
    }
}
