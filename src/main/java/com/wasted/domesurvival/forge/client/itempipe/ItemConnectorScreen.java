package com.wasted.domesurvival.forge.client.itempipe;

import com.wasted.domesurvival.forge.itempipe.ItemConnectorMenu;
import com.wasted.domesurvival.forge.itempipe.ItemConnectorMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.EnumMap;
import java.util.Map;

public final class ItemConnectorScreen extends AbstractContainerScreen<ItemConnectorMenu> {
    private final Map<ItemConnectorMode, Button> buttons = new EnumMap<>(ItemConnectorMode.class);

    public ItemConnectorScreen(ItemConnectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 126;
        titleLabelX = 12;
        titleLabelY = 9;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        int y = topPos + 75;
        addModeButton(ItemConnectorMode.INPUT, leftPos + 10, y, 62);
        addModeButton(ItemConnectorMode.OUTPUT, leftPos + 87, y, 62);
        addModeButton(ItemConnectorMode.DISABLED, leftPos + 164, y, 62);
    }

    private void addModeButton(ItemConnectorMode mode, int x, int y, int width) {
        Button button = Button.builder(
                Component.translatable("gui.domesurvival.item_pipe.mode." + mode.id()),
                ignored -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, mode.ordinal());
                    }
                }
        ).bounds(x, y, width, 20).build();
        buttons.put(mode, addRenderableWidget(button));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF111619);
        graphics.fill(left + 2, top + 2, left + imageWidth - 2, top + imageHeight - 2, 0xFF232B2F);
        graphics.fill(left + 9, top + 28, left + imageWidth - 9, top + 66, 0xFF0F1417);

        int modeColor = switch (menu.mode()) {
            case INPUT -> 0xFF2F78C7;
            case OUTPUT -> 0xFFD47A2F;
            case DISABLED -> 0xFF41484C;
        };
        graphics.fill(left + 14, top + 34, left + 22, top + 59, modeColor);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE3E7E8, false);
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.item_pipe.side", menu.side().getName().toUpperCase()),
                29, 34, 0xFFCCD2D4, false);
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.item_pipe.speed", menu.itemsPerCycle(), menu.cooldownTicks()),
                29, 46, 0xFF929CA0, false);
        graphics.drawString(font,
                Component.translatable("gui.domesurvival.item_pipe.connector_help"),
                12, 103, 0xFF788388, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Map.Entry<ItemConnectorMode, Button> entry : buttons.entrySet()) {
            entry.getValue().active = entry.getKey() != menu.mode();
        }
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
