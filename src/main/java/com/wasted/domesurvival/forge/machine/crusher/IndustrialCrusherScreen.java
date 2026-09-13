package com.wasted.domesurvival.forge.machine.crusher;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class IndustrialCrusherScreen extends AbstractContainerScreen<IndustrialCrusherMenu> {
    public IndustrialCrusherScreen(IndustrialCrusherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1F2328);
        graphics.fill(x + 5, y + 5, x + imageWidth - 5, y + 132, 0xFF30363D);
        graphics.fill(x + 5, y + 135, x + imageWidth - 5, y + imageHeight - 5, 0xFF262B31);

        slot(graphics, x + 42, y + 59);
        slot(graphics, x + 132, y + 50);
        slot(graphics, x + 132, y + 76);
        slot(graphics, x + 81, y + 106);
        slot(graphics, x + 103, y + 106);

        int progressMax = Math.max(1, menu.progressMax());
        int progressWidth = Math.min(56, (int) ((long) menu.progress() * 56L / progressMax));
        graphics.fill(x + 70, y + 64, x + 128, y + 74, 0xFF15181C);
        if (progressWidth > 0) {
            graphics.fill(x + 71, y + 65, x + 71 + progressWidth, y + 73, 0xFFB88746);
        }

        int capacity = Math.max(1, menu.energyCapacity());
        int energyHeight = Math.min(54, (int) ((long) menu.energyStored() * 54L / capacity));
        graphics.fill(x + 14, y + 38, x + 24, y + 94, 0xFF111418);
        if (energyHeight > 0) {
            graphics.fill(x + 15, y + 93 - energyHeight, x + 23, y + 93, 0xFF5DADE2);
        }
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 20, y + 20, 0xFF111418);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF4B535C);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 9, 0xE6EDF3, false);
        graphics.drawString(font,
                Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"),
                30, 39, 0xD7DEE7, false);
        graphics.drawString(font, statusText(), 70, 82, 0xD7DEE7, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, 129, 0xD7DEE7, false);
    }

    private Component statusText() {
        return switch (menu.status()) {
            case IndustrialCrusherBlockEntity.CRUSHING ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.crushing");
            case IndustrialCrusherBlockEntity.NO_ENERGY ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.no_energy");
            case IndustrialCrusherBlockEntity.NO_RECIPE ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.no_recipe");
            case IndustrialCrusherBlockEntity.OUTPUT_FULL ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.output_full");
            case IndustrialCrusherBlockEntity.NOT_ENOUGH_INPUT ->
                    Component.translatable("gui.domesurvival.industrial_crusher.status.not_enough_input");
            default -> Component.translatable("gui.domesurvival.industrial_crusher.status.ready");
        };
    }
}
