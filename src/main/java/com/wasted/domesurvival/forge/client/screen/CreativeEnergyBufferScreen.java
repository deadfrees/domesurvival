package com.wasted.domesurvival.forge.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.energy.CreativeEnergyBufferMenu;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.EnumMap;
import java.util.Locale;

/** Creative Nexus instruments share the series layout and keep a permanent red charge display. */
public final class CreativeEnergyBufferScreen extends AbstractContainerScreen<CreativeEnergyBufferMenu> {
    private static final String KEY = "gui.domesurvival.steel_buffer_v2.";
    private static final ResourceLocation PANEL = tex("panel");
    private static final ResourceLocation CONFIG = tex("configuration");
    private static final ResourceLocation WIDGETS =
            new ResourceLocation("domesurvival", "textures/gui/coal_generator_v2/widgets.png");
    private static final int TEXT = 0xFFCAD2D4;
    private static final int BLUE = 0xFF83B8D2;
    private static final int RED = 0xFFF07B72;
    private static final EnumMap<RelativeSide, Rect> SIDES = new EnumMap<>(RelativeSide.class);

    static {
        SIDES.put(RelativeSide.TOP, new Rect(46, 51, 20, 20));
        SIDES.put(RelativeSide.LEFT, new Rect(22, 75, 20, 20));
        SIDES.put(RelativeSide.FRONT, new Rect(46, 75, 20, 20));
        SIDES.put(RelativeSide.RIGHT, new Rect(70, 75, 20, 20));
        SIDES.put(RelativeSide.BOTTOM, new Rect(46, 99, 20, 20));
        SIDES.put(RelativeSide.BACK, new Rect(70, 99, 20, 20));
    }

    public CreativeEnergyBufferScreen(CreativeEnergyBufferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 266;
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("domesurvival", "textures/gui/adamantium_buffer_v2/" + name + ".png");
    }

    private static RelativeSide actual(RelativeSide side) {
        return side == RelativeSide.LEFT ? RelativeSide.RIGHT
                : side == RelativeSide.RIGHT ? RelativeSide.LEFT : side;
    }

    private boolean inside(double x, double y, Rect rect) {
        return rect.contains(x - leftPos, y - topPos);
    }

    private Component t(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && inside(x, y, new Rect(192, 6, 20, 20))) {
            menu.setSidePanelOpen(!menu.isSidePanelOpen());
            send(menu.isSidePanelOpen() ? 202 : 201);
            return true;
        }
        if (button == 0 && menu.isSidePanelOpen()) {
            for (var entry : SIDES.entrySet()) {
                if (inside(x, y, entry.getValue())) {
                    if (entry.getKey() != RelativeSide.FRONT) {
                        send(CreativeEnergyBufferMenu.sideButtonId(actual(entry.getKey())));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private void widget(GuiGraphics graphics, int x, int y, int u, int v) {
        graphics.blit(WIDGETS, leftPos + x, topPos + y, 20, 20,
                u * 4F, v * 4F, 80, 80, 512, 256);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(PANEL, leftPos, topPos, 220, 266, 0, 0, 880, 1064, 880, 1064);
        widget(graphics, 192, 6, 0, 0);
        if (menu.isSidePanelOpen()) {
            graphics.blit(CONFIG, leftPos + 8, topPos + 29, 204, 111,
                    0, 0, 816, 444, 816, 444);
            for (var entry : SIDES.entrySet()) {
                SideMode mode = menu.getSideMode(actual(entry.getKey()));
                Rect rect = entry.getValue();
                widget(graphics, rect.x, rect.y,
                        mode == SideMode.INPUT ? 20 : mode.allowsOutput() ? 40 : 0, 24);
            }
        } else {
            // Four stationary divisions show a full, inexhaustible creative reservoir.
            graphics.fill(leftPos + 19, topPos + 47, leftPos + 141, topPos + 55, 0xFF751C25);
            graphics.fill(leftPos + 19, topPos + 47, leftPos + 141, topPos + 49, 0xFFF0645E);
            graphics.fill(leftPos + 19, topPos + 53, leftPos + 141, topPos + 55, 0xFFC3383C);
            for (int i = 1; i < 4; i++) {
                int divider = leftPos + 19 + i * 122 / 4;
                graphics.fill(divider, topPos + 47, divider + 1, topPos + 55, 0xFF42242A);
            }
            drawPreview(graphics, leftPos + 176, topPos + 104, 30);
        }
        if (menu.isSidePanelOpen() || inside(mouseX, mouseY, new Rect(192, 6, 20, 20))) {
            graphics.renderOutline(leftPos + 192, topPos + 6, 20, 20, RED);
        }
    }

    private static void drawPreview(GuiGraphics graphics, int x, int y, int scale) {
        graphics.flush();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 150);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(20));
        pose.mulPose(Axis.YP.rotationDegrees(150));
        pose.translate(-.5, -.5, -.5);
        Lighting.setupFor3DItems();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModBlocks.ENERGY_BUFFER_CREATIVE.get().defaultBlockState(),
                pose, graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
        graphics.flush();
        pose.popPose();
        Lighting.setupFor3DItems();
    }

    private void label(GuiGraphics graphics, Component value, int x, int y, int width, int color) {
        String text = value.getString();
        if (font.width(text) > width) {
            text = font.plainSubstrByWidth(text, width - font.width("...")) + "...";
        }
        graphics.drawString(font, text, x, y, color, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        label(graphics, title, 13, 10, 166, RED);
        label(graphics, playerInventoryTitle, 14, 146, 62, TEXT);
        if (menu.isSidePanelOpen()) {
            label(graphics, Component.translatable("gui.domesurvival.side_config"), 15, 33, 190, TEXT);
            label(graphics, t("input"), 111, 52, 90, BLUE);
            label(graphics, t("input_role"), 111, 66, 90, TEXT);
            label(graphics, t("output"), 111, 87, 90, RED);
            label(graphics, t("output_role"), 111, 101, 90, TEXT);
            for (var entry : SIDES.entrySet()) {
                Rect rect = entry.getValue();
                graphics.drawCenteredString(font,
                        Component.translatable("gui.domesurvival.coal_generator.side_letter."
                                + entry.getKey().name().toLowerCase(Locale.ROOT)),
                        rect.x + 10, rect.y + 5,
                        entry.getKey() == RelativeSide.FRONT ? 0xFF78858B : TEXT);
            }
        } else {
            label(graphics, Component.translatable("gui.domesurvival.energy_buffer.creative_tooltip"),
                    18, 35, 132, RED);
            label(graphics, Component.translatable("gui.domesurvival.energy_buffer.creative_energy"),
                    18, 68, 144, TEXT);
            label(graphics, t("flow_in", menu.getInputPerTick()), 18, 90, 129, BLUE);
            label(graphics, t("flow_out", menu.getOutputPerTick()), 18, 106, 129, RED);
        }
    }

    private void help(GuiGraphics graphics, Component text, int x, int y) {
        graphics.renderTooltip(font, font.split(text, 220), x, y);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, x, y, partialTick);
        renderTooltip(graphics, x, y);
        if (hoveredSlot != null && hoveredSlot.hasItem()) return;
        if (inside(x, y, new Rect(192, 6, 20, 20))) {
            help(graphics, Component.translatable("gui.domesurvival.side_config"), x, y);
        } else if (menu.isSidePanelOpen()) {
            for (var entry : SIDES.entrySet()) {
                if (inside(x, y, entry.getValue())) {
                    help(graphics, t(entry.getKey() == RelativeSide.FRONT ? "front" : switch (menu.getSideMode(actual(entry.getKey()))) {
                        case INPUT -> "input_help";
                        case OUTPUT, BOTH -> "output_help";
                        default -> "off";
                    }), x, y);
                }
            }
        } else if (inside(x, y, new Rect(18, 43, 123, 12))) {
            help(graphics, Component.translatable("gui.domesurvival.energy_buffer.creative_tooltip"), x, y);
        }
    }

    private record Rect(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
