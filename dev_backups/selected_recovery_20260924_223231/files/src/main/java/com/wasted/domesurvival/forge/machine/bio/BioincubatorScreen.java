package com.wasted.domesurvival.forge.machine.bio;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.Locale;

/**
 * BIOINCUBATOR_GUI_V193
 *
 * Refined after in-game feedback:
 * - centered labels and bars
 * - required slots split left / right and lifted upward
 * - no side-configuration helper tooltips
 * - palette aligned with the other DomeSurvival machines
 */
public final class BioincubatorScreen extends AbstractContainerScreen<BioincubatorMenu> {
    private static final ResourceLocation PORT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/coal_generator_ports.png");

    private static final ResourceLocation CHICKEN =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/bio/chicken.png");
    private static final ResourceLocation SHEEP =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/bio/sheep.png");
    private static final ResourceLocation COW =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/bio/cow.png");
    private static final ResourceLocation PIG =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/bio/pig.png");

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 310;

    private static final int ENERGY_X = 18;
    private static final int ENERGY_Y = 42;
    private static final int ENERGY_W = 18;
    private static final int ENERGY_H = 112;

    private static final int WATER_X = 264;
    private static final int WATER_Y = 42;
    private static final int WATER_W = 18;
    private static final int WATER_H = 112;

    private static final int CHAMBER_X = 48;
    private static final int CHAMBER_Y = 28;
    private static final int CHAMBER_W = 204;
    private static final int CHAMBER_H = 132;

    private static final int SPECIES_FRAME_X = 127;
    private static final int SPECIES_FRAME_Y = 47;
    private static final int SPECIES_FRAME_W = 46;
    private static final int SPECIES_FRAME_H = 46;

    private static final int PROGRESS_X = 86;
    private static final int PROGRESS_Y = 108;
    private static final int PROGRESS_W = 128;
    private static final int PROGRESS_H = 10;

    private static final int CAPSULE_SLOT_BG_X = 104;
    private static final int FEED_SLOT_BG_X = 172;
    private static final int MACHINE_SLOT_BG_Y = 128;
    private static final int MACHINE_SLOT_SIZE = 22;
    private static final int MODE_X = 80;
    private static final int MODE_Y = 31;
    private static final int MODE_W = 140;
    private static final int MODE_H = 14;

    private static final int STATUS_X = 50;
    private static final int STATUS_Y = 168;
    private static final int STATUS_W = 202;
    private static final int STATUS_H = 24;

    private static final int GEAR_X = 261;
    private static final int GEAR_Y = 167;
    private static final int GEAR_SIZE = 24;

    private static final int INVENTORY_X = 51;
    private static final int INVENTORY_Y = 213;
    private static final int INVENTORY_STEP = 22;
    private static final int SLOT_SIZE = 22;
    private static final int HOTBAR_Y = 281;

    private static final int SIDE_PANEL_X = 310;
    private static final int SIDE_PANEL_Y = 22;
    private static final int SIDE_PANEL_W = 106;
    private static final int SIDE_PANEL_H = 126;
    private static final int SIDE_BUTTON_SIZE = 16;
    private static final int SIDE_GRID_STEP = 25;

    private static final EnumMap<RelativeSide, Rect> SIDE_RECTS = createSideRects();

    private static final int PORT_SIZE = 6;
    private static final int PORT_OFF_U = 0;
    private static final int PORT_INPUT_U = 12;
    private static final int PORT_OUTPUT_U = 18;

    private static final int PANEL_DARK = 0xFF101519;
    private static final int PANEL_MID = 0xFF20282E;
    private static final int PANEL_LIGHT = 0xFF39444B;
    private static final int PANEL_EDGE = 0xFF5A666C;
    private static final int TEXT_MAIN = 0xFFE4E9EC;
    private static final int TEXT_DIM = 0xFFBBC3C8;
    private static final int ENERGY_MAIN = 0xFFC88A2C;
    private static final int ENERGY_BRIGHT = 0xFFE6AF49;
    private static final int WATER_MAIN = 0xFF1F8DB3;
    private static final int WATER_BRIGHT = 0xFF56BCD8;
    private static final int PROGRESS_MAIN = 0xFF3AAAC8;
    private static final int PROGRESS_BRIGHT = 0xFF72CFE6;
    private static final int STATUS_WARNING = 0xFFFFC13A;
    private static final int STATUS_OK = 0xFF72E299;

    private boolean sidePanelOpen;

    public BioincubatorScreen(BioincubatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - PANEL_WIDTH) / 2;
        topPos = (height - PANEL_HEIGHT) / 2;
    }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> regions = new EnumMap<>(RelativeSide.class);

        int centerX = SIDE_PANEL_X + SIDE_PANEL_W / 2 - SIDE_BUTTON_SIZE / 2;
        int centerY = SIDE_PANEL_Y + 57;

        regions.put(RelativeSide.TOP, new Rect(centerX, centerY - SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.LEFT, new Rect(centerX - SIDE_GRID_STEP, centerY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.FRONT, new Rect(centerX, centerY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.RIGHT, new Rect(centerX + SIDE_GRID_STEP, centerY, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BOTTOM, new Rect(centerX, centerY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));
        regions.put(RelativeSide.BACK, new Rect(centerX + SIDE_GRID_STEP, centerY + SIDE_GRID_STEP, SIDE_BUTTON_SIZE, SIDE_BUTTON_SIZE));

        return regions;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, MODE_X, MODE_Y, MODE_W, MODE_H)
                    && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BioincubatorMenu.modeButtonId());
                return true;
            }
            if (inside(mouseX, mouseY, GEAR_X, GEAR_Y, GEAR_SIZE, GEAR_SIZE)) {
                sidePanelOpen = !sidePanelOpen;
                return true;
            }

            if (sidePanelOpen) {
                RelativeSide side = getHoveredSide(mouseX, mouseY);
                if (side != null
                        && BioincubatorBlockEntity.isConfigurableSide(side)
                        && minecraft != null
                        && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            BioincubatorMenu.sideButtonId(machineSideForVisualSide(side))
                    );
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            graphics.renderTooltip(font,
                    Component.literal(menu.getEnergy() + " / " + menu.getEnergyCapacity() + " FE"),
                    mouseX, mouseY);
        } else if (isHovering(WATER_X, WATER_Y, WATER_W, WATER_H, mouseX, mouseY)) {
            graphics.renderTooltip(font,
                    Component.literal(menu.getWater() + " / " + menu.getWaterCapacity() + " mB"),
                    mouseX, mouseY);
        } else if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            int percent = menu.getProgressMax() <= 0 ? 0 : menu.getProgress() * 100 / menu.getProgressMax();
            graphics.renderTooltip(font, Component.literal(percent + "%"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawIndustrialPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_MID);

        drawInset(graphics, x + 70, y + 7, 160, 23, PANEL_DARK);
        drawIndustrialPanel(graphics, x + CHAMBER_X, y + CHAMBER_Y, CHAMBER_W, CHAMBER_H, 0xFF1A2126);
        drawModeSwitch(graphics, x, y);

        drawMeterFrame(graphics, x + ENERGY_X, y + ENERGY_Y, ENERGY_W, ENERGY_H);
        drawMeterFrame(graphics, x + WATER_X, y + WATER_Y, WATER_W, WATER_H);

        int energyFill = scaled(menu.getEnergy(), menu.getEnergyCapacity(), ENERGY_H - 8);
        if (energyFill > 0) {
            int bottom = y + ENERGY_Y + ENERGY_H - 4;
            graphics.fill(x + ENERGY_X + 4, bottom - energyFill, x + ENERGY_X + ENERGY_W - 4, bottom, ENERGY_MAIN);
            graphics.fill(x + ENERGY_X + 5, bottom - energyFill, x + ENERGY_X + 8, bottom, ENERGY_BRIGHT);
        }

        int waterFill = scaled(menu.getWater(), menu.getWaterCapacity(), WATER_H - 8);
        if (waterFill > 0) {
            int bottom = y + WATER_Y + WATER_H - 4;
            graphics.fill(x + WATER_X + 4, bottom - waterFill, x + WATER_X + WATER_W - 4, bottom, WATER_MAIN);
            graphics.fill(x + WATER_X + 5, bottom - waterFill, x + WATER_X + 8, bottom, WATER_BRIGHT);
        }

        drawInset(graphics, x + SPECIES_FRAME_X, y + SPECIES_FRAME_Y, SPECIES_FRAME_W, SPECIES_FRAME_H, 0xFF0C1114);
        drawSpeciesIcon(graphics, x, y);

        drawInset(graphics, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H, 0xFF080C0E);
        int progress = scaled(menu.getProgress(), Math.max(1, menu.getProgressMax()), PROGRESS_W - 6);
        if (progress > 0) {
            graphics.fill(x + PROGRESS_X + 3, y + PROGRESS_Y + 3, x + PROGRESS_X + 3 + progress, y + PROGRESS_Y + PROGRESS_H - 3, PROGRESS_MAIN);
            graphics.fill(x + PROGRESS_X + 3, y + PROGRESS_Y + 3, x + PROGRESS_X + 3 + progress, y + PROGRESS_Y + 5, PROGRESS_BRIGHT);
        }

        if (menu.getMode() == BioincubatorBlockEntity.MODE_REPAIR) {
            for (int slotX : new int[]{70, 104, 138, 172, 206}) {
                drawSlot(graphics, x + slotX, y + MACHINE_SLOT_BG_Y, MACHINE_SLOT_SIZE);
            }
            // The fifth slot is the restoration result, not another input.
            // A small recessed arrow makes that role clear even while it is empty.
            graphics.fill(x + 196, y + 137, x + 203, y + 140, PANEL_EDGE);
            graphics.fill(x + 201, y + 134, x + 204, y + 143, PANEL_EDGE);
            graphics.fill(x + 203, y + 136, x + 206, y + 141, PROGRESS_BRIGHT);
        } else {
            drawSlot(graphics, x + CAPSULE_SLOT_BG_X, y + MACHINE_SLOT_BG_Y, MACHINE_SLOT_SIZE);
            drawSlot(graphics, x + FEED_SLOT_BG_X, y + MACHINE_SLOT_BG_Y, MACHINE_SLOT_SIZE);
        }

        drawInset(graphics, x + STATUS_X, y + STATUS_Y, STATUS_W, STATUS_H, 0xFF0B1115);
        drawGearButton(graphics, x + GEAR_X, y + GEAR_Y, sidePanelOpen);

        graphics.fill(x + 12, y + 198, x + PANEL_WIDTH - 12, y + 199, 0xFF0B0F12);
        graphics.fill(x + 12, y + 199, x + PANEL_WIDTH - 12, y + 200, PANEL_EDGE);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x + INVENTORY_X + column * INVENTORY_STEP, y + INVENTORY_Y + row * INVENTORY_STEP, SLOT_SIZE);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x + INVENTORY_X + column * INVENTORY_STEP, y + HOTBAR_Y, SLOT_SIZE);
        }

        if (sidePanelOpen) {
            drawSidePanel(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawCenteredClampedText(graphics, title, 75, 14, 150, TEXT_MAIN);

        graphics.drawString(font, Component.literal("FE"), 18, 30, ENERGY_MAIN, false);
        graphics.drawString(font, Component.literal("Hв‚‚O"), 259, 30, WATER_BRIGHT, false);

        drawCenteredScaledText(graphics,
                Component.translatable("gui.domesurvival.incubator.incubation"),
                MODE_X + 3, MODE_Y + 4, MODE_W / 2 - 3,
                menu.getMode() == BioincubatorBlockEntity.MODE_INCUBATION ? TEXT_MAIN : TEXT_DIM, 0.68F);
        drawCenteredScaledText(graphics,
                Component.translatable("gui.domesurvival.incubator.repair"),
                MODE_X + MODE_W / 2, MODE_Y + 4, MODE_W / 2 - 3,
                menu.getMode() == BioincubatorBlockEntity.MODE_REPAIR ? TEXT_MAIN : TEXT_DIM, 0.68F);
        drawCenteredClampedText(graphics, speciesText(), CHAMBER_X + 12, 96, CHAMBER_W - 24, TEXT_MAIN);
        drawCenteredClampedText(graphics, Component.literal(remainingTime()), CHAMBER_X + 12, 120, CHAMBER_W - 24, TEXT_DIM);
        drawCenteredClampedText(graphics, statusText(), STATUS_X + 8, STATUS_Y + 8, STATUS_W - 16, statusColor());

        drawCenteredClampedText(graphics, Component.literal(compactEnergy(menu.getEnergyCapacity())), 1, 158, 52, ENERGY_MAIN);
        drawCenteredClampedText(graphics, Component.literal(menu.getWaterCapacity() + " mB"), 246, 158, 52, WATER_BRIGHT);
        graphics.drawString(font, playerInventoryTitle, 51, 203, TEXT_MAIN, false);
    }

    private Component speciesText() {
        EntityType<?> type = selectedSpecies();
        return type == null
                ? Component.translatable("gui.domesurvival.bioincubator.species.none")
                : type.getDescription();
    }

    private Component statusText() {
        return switch (menu.getStatus()) {
            case BioincubatorBlockEntity.STATUS_RUNNING ->
                    Component.translatable("gui.domesurvival.bioincubator.status.running");
            case BioincubatorBlockEntity.STATUS_NO_CAPSULE ->
                    Component.translatable("gui.domesurvival.bioincubator.status.no_capsule");
            case BioincubatorBlockEntity.STATUS_INVALID_CAPSULE ->
                    Component.translatable("gui.domesurvival.bioincubator.status.invalid_capsule");
            case BioincubatorBlockEntity.STATUS_NO_FEED ->
                    Component.translatable("gui.domesurvival.bioincubator.status.no_feed");
            case BioincubatorBlockEntity.STATUS_NO_WATER ->
                    Component.translatable("gui.domesurvival.bioincubator.status.no_water");
            case BioincubatorBlockEntity.STATUS_NO_ENERGY ->
                    Component.translatable("gui.domesurvival.bioincubator.status.no_energy");
            case BioincubatorBlockEntity.STATUS_OUTPUT_BLOCKED ->
                    Component.translatable("gui.domesurvival.bioincubator.status.blocked");
            case BioincubatorBlockEntity.STATUS_DATABASE_LOCKED ->
                    Component.translatable("gui.domesurvival.bioincubator.status.database_locked");
            case BioincubatorBlockEntity.STATUS_DAMAGED_CAPSULE ->
                    Component.translatable("gui.domesurvival.bioincubator.status.damaged_capsule");
            case BioincubatorBlockEntity.STATUS_REQUIRES_DAMAGED ->
                    Component.translatable("gui.domesurvival.bioincubator.status.requires_damaged");
            case BioincubatorBlockEntity.STATUS_NO_REPAIR_MATERIALS ->
                    Component.translatable("gui.domesurvival.bioincubator.status.no_repair_materials");
            case BioincubatorBlockEntity.STATUS_REPAIR_OUTPUT_FULL ->
                    Component.translatable("gui.domesurvival.bioincubator.status.repair_output_full");
            default ->
                    Component.translatable("gui.domesurvival.bioincubator.status.idle");
        };
    }

    private int statusColor() {
        return menu.getStatus() == BioincubatorBlockEntity.STATUS_RUNNING ? STATUS_OK : STATUS_WARNING;
    }

    private String remainingTime() {
        int max = menu.getProgressMax();
        if (max <= 0) {
            return "--:--";
        }

        int remainingTicks = Math.max(0, max - menu.getProgress());
        int totalSeconds = (remainingTicks + 19) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private void drawSpeciesIcon(GuiGraphics graphics, int x, int y) {
        EntityType<?> type = selectedSpecies();
        ResourceLocation id = type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
        ResourceLocation texture = id == null ? null : switch (id.getPath()) {
            case "chicken" -> CHICKEN;
            case "sheep" -> SHEEP;
            case "cow", "mooshroom" -> COW;
            case "pig" -> PIG;
            default -> null;
        };

        if (texture != null) {
            graphics.blit(texture, x + 132, y + 53, 0, 0, 36, 36, 36, 36);
        }
    }

    private void drawModeSwitch(GuiGraphics graphics, int x, int y) {
        drawInset(graphics, x + MODE_X, y + MODE_Y, MODE_W, MODE_H, 0xFF11181C);
        int half = MODE_W / 2;
        if (menu.getMode() == BioincubatorBlockEntity.MODE_REPAIR) {
            graphics.fill(x + MODE_X + half, y + MODE_Y + 3,
                    x + MODE_X + MODE_W - 3, y + MODE_Y + MODE_H - 3, 0xFF315B67);
        } else {
            graphics.fill(x + MODE_X + 3, y + MODE_Y + 3,
                    x + MODE_X + half, y + MODE_Y + MODE_H - 3, 0xFF315B67);
        }
    }

    private EntityType<?> selectedSpecies() {
        int registryId = menu.getSpecies() - 1;
        return registryId < 0 ? null : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.byId(registryId);
    }

    private void drawSidePanel(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;

        drawIndustrialPanel(graphics, x + SIDE_PANEL_X, y + SIDE_PANEL_Y, SIDE_PANEL_W, SIDE_PANEL_H, 0xFF222A2F);
        for (RelativeSide side : RelativeSide.values()) {
            Rect rect = SIDE_RECTS.get(side);
            SideMode mode = BioincubatorBlockEntity.isConfigurableSide(side)
                    ? menu.getSideMode(machineSideForVisualSide(side))
                    : SideMode.DISABLED;

            int fill = mode == SideMode.INPUT ? 0xFF234D68 : 0xFF161C20;
            drawInset(graphics, x + rect.x, y + rect.y, rect.width, rect.height, fill);

            int u = switch (mode) {
                case INPUT -> PORT_INPUT_U;
                case OUTPUT, BOTH -> PORT_OUTPUT_U;
                case DISABLED -> PORT_OFF_U;
            };

            graphics.blit(
                    PORT_TEXTURE,
                    x + rect.x + (rect.width - PORT_SIZE) / 2,
                    y + rect.y + (rect.height - PORT_SIZE) / 2,
                    u, 0, PORT_SIZE, PORT_SIZE, 24, 6
            );
        }
    }

    private RelativeSide getHoveredSide(double mouseX, double mouseY) {
        for (RelativeSide side : RelativeSide.values()) {
            Rect rect = SIDE_RECTS.get(side);
            if (rect.contains(mouseX, mouseY, leftPos, topPos)) {
                return side;
            }
        }
        return null;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        double localX = mouseX - leftPos;
        double localY = mouseY - topPos;

        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    private static RelativeSide machineSideForVisualSide(RelativeSide visualSide) {
        return switch (visualSide) {
            case LEFT -> RelativeSide.RIGHT;
            case RIGHT -> RelativeSide.LEFT;
            default -> visualSide;
        };
    }

    private static int scaled(int value, int max, int pixels) {
        if (value <= 0 || max <= 0) {
            return 0;
        }
        return Math.min(pixels, (int) ((long) value * pixels / max));
    }

    private static String compactEnergy(int energy) {
        if (energy >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1f MFE", energy / 1_000_000.0D);
        }
        if (energy >= 1_000) {
            return String.format(Locale.ROOT, "%d kFE", energy / 1_000);
        }
        return energy + " FE";
    }

    private static void drawIndustrialPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, 0xFF070A0C);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_EDGE);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, PANEL_DARK);
        graphics.fill(x + 6, y + 6, x + width - 6, y + height - 6, fill);

        drawBolt(graphics, x + 8, y + 8);
        drawBolt(graphics, x + width - 12, y + 8);
        drawBolt(graphics, x + 8, y + height - 12);
        drawBolt(graphics, x + width - 12, y + height - 12);
    }

    private static void drawInset(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, 0xFF080B0D);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF4A555C);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, fill);
    }

    private static void drawMeterFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        drawInset(graphics, x, y, width, height, 0xFF080C0F);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, 0xFF080B0D);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF59646A);
        graphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, 0xFF1B2226);
    }

    private static void drawGearButton(GuiGraphics graphics, int x, int y, boolean active) {
        drawInset(graphics, x, y, GEAR_SIZE, GEAR_SIZE, active ? 0xFF344A55 : 0xFF242C31);

        int color = active ? 0xFF77D9F2 : 0xFFC8D0D4;
        int cx = x + GEAR_SIZE / 2;
        int cy = y + GEAR_SIZE / 2;

        graphics.fill(cx - 5, cy - 2, cx + 6, cy + 3, color);
        graphics.fill(cx - 2, cy - 5, cx + 3, cy + 6, color);
        graphics.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xFF242C31);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
    }

    private static void drawBolt(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 4, y + 4, 0xFF11171A);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, 0xFF8D989D);
    }

    private void drawCenteredClampedText(GuiGraphics graphics, Component component, int x, int y, int maxWidth, int color) {
        Component fitted = fitText(component, maxWidth);
        int width = font.width(fitted);
        graphics.drawString(font, fitted, x + (maxWidth - width) / 2, y, color, false);
    }

    private void drawCenteredScaledText(GuiGraphics graphics, Component component,
                                        int x, int y, int maxWidth, int color, float scale) {
        int textWidth = font.width(component);
        graphics.pose().pushPose();
        graphics.pose().translate(x + maxWidth / 2.0F, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, component, -textWidth / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private Component fitText(Component component, int maxWidth) {
        if (font.width(component) <= maxWidth) {
            return component;
        }

        String text = component.getString();
        String suffix = "...";

        while (!text.isEmpty() && font.width(text + suffix) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }

        return Component.literal(text + suffix);
    }

    private record Rect(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY, int originX, int originY) {
            return mouseX >= originX + x
                    && mouseX < originX + x + width
                    && mouseY >= originY + y
                    && mouseY < originY + y + height;
        }
    }
}
