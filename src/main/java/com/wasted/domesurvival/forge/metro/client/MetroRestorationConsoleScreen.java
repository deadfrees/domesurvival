package com.wasted.domesurvival.forge.metro.client;

import com.wasted.domesurvival.forge.client.gui.DomeIndustrialGuiStyle;
import com.wasted.domesurvival.forge.metro.MetroRestorationConsoleMenu;
import com.wasted.domesurvival.forge.metro.MetroRestorationRequirements;
import com.wasted.domesurvival.forge.metro.StationState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MetroRestorationConsoleScreen extends AbstractContainerScreen<MetroRestorationConsoleMenu> {
    private static final int WIDTH = 330;
    private static final int HEIGHT = 304;

    private final Inventory playerInventory;
    private Button depositButton;
    private Button restoreButton;

    public MetroRestorationConsoleScreen(MetroRestorationConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.playerInventory = inventory;
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        depositButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.domesurvival.metro.restoration.deposit_button"),
                        button -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId,
                                        MetroRestorationConsoleMenu.BUTTON_DEPOSIT
                                );
                            }
                        })
                .bounds(leftPos + 51, topPos + 245, 228, 20)
                .build());

        restoreButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.domesurvival.metro.restoration.restore_button"),
                        button -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId,
                                        MetroRestorationConsoleMenu.BUTTON_RESTORE
                                );
                            }
                        })
                .bounds(leftPos + 51, topPos + 269, 228, 20)
                .build());
        updateButtonState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtonState();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void updateButtonState() {
        if (depositButton == null || restoreButton == null) return;

        if (menu.state() == StationState.RESTORED || menu.restored()) {
            depositButton.active = false;
            depositButton.setMessage(Component.translatable("gui.domesurvival.metro.restoration.deposit_complete_button"));
            restoreButton.active = false;
            restoreButton.setMessage(Component.translatable("gui.domesurvival.metro.restoration.restored_button"));
            return;
        }
        if (menu.state() == StationState.RESTORING) {
            depositButton.active = false;
            restoreButton.active = false;
            restoreButton.setMessage(Component.translatable("gui.domesurvival.metro.restoration.restoring_button"));
            return;
        }

        boolean complete = menu.allDeposited();
        boolean canDeposit = !complete && hasUsefulMaterialInInventory();
        depositButton.active = canDeposit;
        depositButton.setMessage(Component.translatable(complete
                ? "gui.domesurvival.metro.restoration.deposit_complete_button"
                : canDeposit
                    ? "gui.domesurvival.metro.restoration.deposit_button"
                    : "gui.domesurvival.metro.restoration.deposit_empty_button"));

        restoreButton.active = complete;
        restoreButton.setMessage(Component.translatable(complete
                ? "gui.domesurvival.metro.restoration.restore_button"
                : "gui.domesurvival.metro.restoration.deposit_first_button"));
    }

    private boolean hasUsefulMaterialInInventory() {
        for (int i = 0; i < MetroRestorationRequirements.all().size(); i++) {
            MetroRestorationRequirements.Requirement requirement = MetroRestorationRequirements.all().get(i);
            if (menu.deposited(i) >= requirement.required()) continue;
            if (MetroRestorationRequirements.count(playerInventory, requirement) > 0) return true;
        }
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        DomeIndustrialGuiStyle.drawPanel(graphics, x, y, imageWidth, imageHeight);
        DomeIndustrialGuiStyle.drawFrame(graphics, x + 8, y + 28, 314, 208, DomeIndustrialGuiStyle.PANEL_ALT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 10, DomeIndustrialGuiStyle.TEXT, false);

        label(graphics, "gui.domesurvival.metro.restoration.state", 18, 39);
        graphics.drawString(font, stateText(menu.state()), 164, 39, stateColor(menu.state()), false);

        label(graphics, "gui.domesurvival.metro.restoration.level", 18, 55);
        graphics.drawString(font,
                Component.translatable(menu.state() == StationState.RESTORED
                        ? "gui.domesurvival.metro.restoration.level2"
                        : "gui.domesurvival.metro.restoration.level_unrestored"),
                164, 55,
                menu.state() == StationState.RESTORED
                        ? DomeIndustrialGuiStyle.READY
                        : DomeIndustrialGuiStyle.TEXT_DIM,
                false);

        int rowY = 78;
        for (int i = 0; i < MetroRestorationRequirements.all().size(); i++) {
            MetroRestorationRequirements.Requirement requirement = MetroRestorationRequirements.all().get(i);
            int submitted = Math.min(menu.deposited(i), requirement.required());
            boolean ready = submitted >= requirement.required();

            graphics.drawString(font,
                    requirement.displayName(),
                    40, rowY,
                    requirement.registered() ? DomeIndustrialGuiStyle.TEXT_MUTED : DomeIndustrialGuiStyle.WARNING,
                    false);
            graphics.drawString(font,
                    Component.literal(submitted + " / " + requirement.required()),
                    254, rowY,
                    ready ? DomeIndustrialGuiStyle.READY : DomeIndustrialGuiStyle.WARNING,
                    false);
            if (!requirement.displayStack().isEmpty()) {
                graphics.renderItem(requirement.displayStack(), 18, rowY - 4);
            }
            rowY += 16;
        }

        graphics.drawString(font,
                Component.translatable(menu.state() == StationState.RESTORED
                        ? "gui.domesurvival.metro.restoration.restored_note"
                        : "gui.domesurvival.metro.restoration.deposit_note"),
                18, 226,
                menu.state() == StationState.RESTORED
                        ? DomeIndustrialGuiStyle.READY
                        : DomeIndustrialGuiStyle.TEXT_DIM,
                false);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y,
                DomeIndustrialGuiStyle.TEXT_MUTED, false);
    }

    private static int stateColor(StationState state) {
        return switch (state) {
            case ABANDONED -> DomeIndustrialGuiStyle.WARNING;
            case RESTORING -> DomeIndustrialGuiStyle.FLUID_LIGHT;
            case RESTORED -> DomeIndustrialGuiStyle.READY;
        };
    }

    private static Component stateText(StationState state) {
        return switch (state) {
            case ABANDONED -> Component.translatable("gui.domesurvival.metro.state.abandoned");
            case RESTORING -> Component.translatable("gui.domesurvival.metro.state.restoring");
            case RESTORED -> Component.translatable("gui.domesurvival.metro.state.restored");
        };
    }
}
