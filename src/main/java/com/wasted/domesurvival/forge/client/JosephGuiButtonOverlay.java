package com.wasted.domesurvival.forge.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.ICustomGuiComponent;

import java.lang.reflect.Field;

/**
 * Draws visible captions over Joseph's native CustomNPCs buttons.
 *
 * GBPort compatibility note:
 * do NOT call ICustomGui#getName(), ICustomGuiComponent#getVisible()
 * or ICustomGuiComponent#getEnabled(); those methods are absent in the
 * unofficial 1.20.1 port used by this project.
 */
@Mod.EventBusSubscriber(
        modid = "domesurvival",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class JosephGuiButtonOverlay {

    private static final int BTN_PROJECT = 6501;
    private static final int BTN_BASE = 6502;
    private static final int BTN_PLAN = 6503;
    private static final int BTN_CLOSE = 6504;
    private static final int BTN_CONTRIBUTE = 6505;
    private static final int BTN_BACK = 6506;

    private JosephGuiButtonOverlay() {
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        ICustomGui gui = findCustomGui(screen);

        if (gui == null || !looksLikeJosephGui(gui)) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int guiLeft = (screen.width - gui.getWidth()) / 2;
        int guiTop = (screen.height - gui.getHeight()) / 2;

        drawCaption(graphics, font, gui, BTN_PROJECT,
                "\u041f\u0440\u043e\u0435\u043a\u0442", guiLeft, guiTop);

        drawCaption(graphics, font, gui, BTN_BASE,
                "\u0421\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435 \u0431\u0430\u0437\u044b", guiLeft, guiTop);

        drawCaption(graphics, font, gui, BTN_PLAN,
                "\u041f\u043b\u0430\u043d \u0440\u0430\u0437\u0432\u0438\u0442\u0438\u044f", guiLeft, guiTop);

        drawCaption(graphics, font, gui, BTN_CLOSE,
                "\u0417\u0430\u043a\u0440\u044b\u0442\u044c", guiLeft, guiTop);

        drawCaption(graphics, font, gui, BTN_CONTRIBUTE,
                "\u041f\u0435\u0440\u0435\u0434\u0430\u0442\u044c \u0440\u0435\u0441\u0443\u0440\u0441\u044b", guiLeft, guiTop);

        drawCaption(graphics, font, gui, BTN_BACK,
                "\u041d\u0430\u0437\u0430\u0434", guiLeft, guiTop);
    }

    /**
     * Joseph's main GUI always has all four main-menu button IDs.
     * This avoids relying on getName(), which the GBPort API does not expose.
     */
    private static boolean looksLikeJosephGui(ICustomGui gui) {
        try {
            boolean mainPage = gui.getComponent(BTN_PROJECT) != null
                    && gui.getComponent(BTN_BASE) != null
                    && gui.getComponent(BTN_PLAN) != null
                    && gui.getComponent(BTN_CLOSE) != null;

            if (mainPage) {
                return true;
            }

            /*
             * Sub-pages do not have the four main-menu buttons. Detect Joseph
             * there by the unique BACK/CONTRIBUTE IDs.
             */
            return gui.getComponent(BTN_BACK) != null
                    || gui.getComponent(BTN_CONTRIBUTE) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void drawCaption(
            GuiGraphics graphics,
            Font font,
            ICustomGui gui,
            int componentId,
            String text,
            int guiLeft,
            int guiTop
    ) {
        ICustomGuiComponent component;

        try {
            component = gui.getComponent(componentId);
        } catch (Throwable ignored) {
            return;
        }

        if (component == null) {
            return;
        }

        int x = guiLeft + component.getPosX();
        int y = guiTop + component.getPosY();
        int width = component.getWidth();
        int height = component.getHeight();

        int centerX = x + width / 2;
        int textY = y + Math.max(1, (height - font.lineHeight) / 2) + 1;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        graphics.drawCenteredString(font, text, centerX, textY, 0xFFFFFF);
        graphics.pose().popPose();
    }

    /**
     * Locate the CustomNPCs ICustomGui stored inside the active client screen
     * without depending on the port's concrete screen implementation class.
     */
    private static ICustomGui findCustomGui(Screen screen) {
        Class<?> type = screen.getClass();

        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(screen);
                    if (value instanceof ICustomGui) {
                        return (ICustomGui) value;
                    }
                } catch (Throwable ignored) {
                }
            }

            type = type.getSuperclass();
        }

        return null;
    }
}
