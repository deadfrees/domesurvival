package com.wasted.domesurvival.forge.machine.sieve;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.wasted.domesurvival.forge.block.ModBlocks;

public final class SandSieveScreen extends AbstractContainerScreen<SandSieveMenu> {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 227;
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
            "domesurvival", "textures/gui/sand_sieve_v2/panel.png");
    private final ItemStack sieveIcon = new ItemStack(ModBlocks.SAND_SIEVE.get());

    public SandSieveScreen(SandSieveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 51;
        inventoryLabelY = 124;
        titleLabelX = 0;
        titleLabelY = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(20, 52, 22, 54, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(menu.water() + " / "
                    + menu.waterCapacity() + " mB"), mouseX, mouseY);
        } else if (isHovering(58, 91, 210, 8, mouseX, mouseY)) {
            graphics.renderTooltip(font, statusText(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(PANEL, x, y, WIDTH, HEIGHT, 0, 0, 1200, 908, 1200, 908);
        com.wasted.domesurvival.forge.client.gui.MachineGaugeRenderer.fluid(
                graphics, x + 24, y + 56, 14, 46, menu.water(), menu.waterCapacity(),
                net.minecraft.world.level.material.Fluids.WATER);

        int progress = menu.progressMax() <= 0 ? 0 : 204 * menu.progress() / menu.progressMax();
        if (progress > 0) {
            graphics.fill(x + 61, y + 94, x + 61 + progress, y + 96, 0xFFB77A3E);
            graphics.fill(x + 61, y + 94, x + 61 + progress, y + 95, 0xFFE3AE61);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(x + 148, y + 56, 0);
        graphics.pose().scale(1.6F, 1.6F, 1.6F);
        graphics.renderItem(sieveIcon, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String name = font.plainSubstrByWidth(title.getString(), 198);
        graphics.drawString(font, name, (WIDTH - font.width(name)) / 2, 11, 0xFF2C241B, false);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.water"),
                31, 39, 0xFF63C5D5);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.sand"),
                73, 43, 0xFFD3B77A);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.mesh"),
                111, 43, 0xFFBFC8CC);
        graphics.drawCenteredString(font, Component.translatable("gui.domesurvival.sand_sieve.output"),
                237, 43, 0xFFBFC8CC);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFBFC8CC, false);
    }

    private Component statusText() {
        String key = switch (menu.status()) {
            case SandSieveBlockEntity.STATUS_READY_DRY -> "dry";
            case SandSieveBlockEntity.STATUS_READY_WET -> "wet";
            case SandSieveBlockEntity.STATUS_RUNNING_DRY -> "running_dry";
            case SandSieveBlockEntity.STATUS_RUNNING_WET -> "running_wet";
            case SandSieveBlockEntity.STATUS_NO_SAND -> "no_sand";
            case SandSieveBlockEntity.STATUS_NO_MESH -> "no_mesh";
            case SandSieveBlockEntity.STATUS_OUTPUT_BLOCKED -> "output_blocked";
            default -> "idle";
        };
        return Component.translatable((key.equals("dry") || key.equals("wet")
                ? "gui.domesurvival.sand_sieve.mode." : "gui.domesurvival.sand_sieve.status.") + key);
    }

}
