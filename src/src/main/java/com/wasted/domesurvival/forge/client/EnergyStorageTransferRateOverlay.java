package com.wasted.domesurvival.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.energy.EnergyTransferRateMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, value = Dist.CLIENT)
public final class EnergyStorageTransferRateOverlay {
    private static final int CONTENT_LEFT = 10;
    private static final int CONTENT_RIGHT = 166;
    private static final int TOP_LINE_Y = 34;
    private static final int ENERGY_LINE_Y = 68;

    // Widened cleanup mask to fully cover any leftover old text.
    private static final int ENERGY_MASK_LEFT = 3;
    private static final int ENERGY_MASK_RIGHT = 173;
    private static final int ENERGY_MASK_TOP = 60;
    private static final int ENERGY_MASK_BOTTOM = 82;

    private static final float MIN_SCALE = 0.72F;
    private static final int PANEL_BG = 0xFF30363A;
    private static final int TEXT_COLOR = 0xFFE6E6E6;

    private EnergyStorageTransferRateOverlay() { }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!(screen.getMenu() instanceof EnergyTransferRateMenu menu)) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        GuiGraphics graphics = event.getGuiGraphics();
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();
        int centerX = guiLeft + 88;

        Component topLine = Component.literal(
                "Приём: " + formatRate(menu.getInputPerTick()) + " / " + formatLimit(menu.getMaxInputPerTick()) + " FE/t   " +
                "Выдача: " + formatRate(menu.getOutputPerTick()) + " / " + formatLimit(menu.getMaxOutputPerTick()) + " FE/t"
        );
        drawCenteredLine(graphics, font, topLine, centerX, guiTop + TOP_LINE_Y);

        Component energyLine = buildEnergyLine(screen.getMenu());
        if (energyLine != null) {
            // Clear a larger band first so no residual symbols from the old text remain visible.
            graphics.fill(guiLeft + ENERGY_MASK_LEFT, guiTop + ENERGY_MASK_TOP,
                    guiLeft + ENERGY_MASK_RIGHT, guiTop + ENERGY_MASK_BOTTOM, PANEL_BG);
            drawCenteredLine(graphics, font, energyLine, centerX, guiTop + ENERGY_LINE_Y);
        }
    }

    private static void drawCenteredLine(GuiGraphics graphics, Font font, Component line, int centerX, int y) {
        int width = font.width(line);
        int available = CONTENT_RIGHT - CONTENT_LEFT;
        float scale = width > available
                ? Math.max(MIN_SCALE, (float) available / (float) width)
                : 1.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0F);

        float scaledCenterX = centerX / scale;
        float scaledY = y / scale;
        float scaledX = scaledCenterX - (width / 2.0F);

        graphics.drawString(font, line, (int) scaledX, (int) scaledY, TEXT_COLOR, false);
        pose.popPose();
    }

    private static Component buildEnergyLine(Object menu) {
        Integer energy = callInt(menu, "getEnergyStored");
        Integer capacity = callInt(menu, "getEnergyCapacity");
        if (energy == null || capacity == null) return null;

        if (capacity == Integer.MAX_VALUE || energy == Integer.MAX_VALUE) {
            return Component.literal("Энергия: ∞ / ∞ FE");
        }
        return Component.literal("Энергия: " + grouped(energy) + " / " + grouped(capacity) + " FE");
    }

    private static Integer callInt(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Number number ? number.intValue() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String formatRate(int value) {
        return grouped(Math.max(0, value));
    }

    private static String formatLimit(int value) {
        return value == Integer.MAX_VALUE ? "∞" : grouped(Math.max(0, value));
    }

    private static String grouped(int value) {
        return String.format(Locale.ROOT, "%d", Math.max(0, value));
    }
}
