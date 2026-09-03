package com.wasted.domesurvival.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.mixin.LevelLoadingScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * V27 single-owner loading compositor.
 *
 * Every handled loading screen is repainted before vanilla can draw its status
 * caption/chunk map.  The lower bar is ALWAYS animated:
 * - LevelLoadingScreen: real StoringChunkProgressListener value.
 * - Stages without a numeric progress API: left-to-right indeterminate sweep,
 *   no percentage text and no claim that the sweep is a real percentage.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EarlyWorldLoadingBackground {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/loading/loading_dome_city.png");
    private static final ResourceLocation BAR_FILL =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/loading/loading_bar_fill_v27.png");

    private static final int TEX_W = 1672;
    private static final int TEX_H = 941;

    // Inner cavity measured from the approved loading artwork.
    private static final int BAR_X = 227;
    private static final int BAR_Y = 807;
    private static final int BAR_W = 1223;
    private static final int BAR_H = 30;

    private static final int FILL_W = 1223;
    private static final int FILL_H = 30;

    // Indeterminate stages visibly fill from LEFT -> RIGHT, then restart.
    // No number is displayed, so this is not presented as a fake percentage.
    private static final long INDETERMINATE_PERIOD_NS = 2_200_000_000L;

    private static Screen progressScreen;
    private static float displayedProgress;
    private static Screen indeterminateScreen;
    private static long indeterminateStartedNs;
    private static boolean warned;

    private EarlyWorldLoadingBackground() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        if (!isLoadingStage(screen)) {
            resetIfScreenChanged(screen);
            return;
        }

        if (!has(BACKGROUND) || !has(BAR_FILL)) {
            warnOnce();
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        drawFullscreen(graphics, screen);

        if (screen instanceof LevelLoadingScreen levelLoadingScreen) {
            drawRealProgress(graphics, levelLoadingScreen, event.getPartialTick());
            indeterminateScreen = null;
            indeterminateStartedNs = 0L;
        } else {
            progressScreen = null;
            displayedProgress = 0.0F;
            drawIndeterminateSweep(graphics, screen);
        }

        // Keep only real action buttons such as Cancel, when a loading screen has one.
        // Vanilla loading captions / percentages / chunk map are intentionally not rendered.
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof Button button && button.visible) {
                button.render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }
        }

        event.setCanceled(true);
    }

    private static boolean isLoadingStage(Screen screen) {
        if (screen instanceof LevelLoadingScreen) return true;
        String simple = screen.getClass().getSimpleName();
        return simple.equals("GenericDirtMessageScreen")
                || simple.equals("GenericWaitingScreen")
                || simple.equals("ProgressScreen")
                || simple.equals("ReceivingLevelScreen")
                || simple.equals("DownloadingTerrainScreen")
                || simple.equals("ConnectScreen")
                || simple.equals("MessageScreen")
                || simple.equals("OptimizeWorldScreen");
    }

    private static void drawRealProgress(GuiGraphics graphics, LevelLoadingScreen screen, float partialTick) {
        StoringChunkProgressListener listener =
                ((LevelLoadingScreenAccessor) (Object) screen).domesurvival$getProgressListener();

        float target = Mth.clamp(listener.getProgress() / 100.0F, 0.0F, 1.0F);

        if (progressScreen != screen || target + 0.001F < displayedProgress) {
            progressScreen = screen;
            displayedProgress = 0.0F;
        }

        // Smooth only toward the real value and never move beyond it.
        float delta = target - displayedProgress;
        if (delta > 0.0F) {
            float partial = Mth.clamp(partialTick, 0.0F, 1.0F);
            float step = Math.max(0.0030F, delta * (0.22F + 0.05F * partial));
            displayedProgress = Math.min(target, displayedProgress + step);
        }

        drawProgressFill(graphics, barRect(screen), displayedProgress);
    }

    private static void drawIndeterminateSweep(GuiGraphics graphics, Screen screen) {
        long now = System.nanoTime();
        if (indeterminateScreen != screen || indeterminateStartedNs == 0L) {
            indeterminateScreen = screen;
            indeterminateStartedNs = now;
        }

        long elapsed = Math.max(0L, now - indeterminateStartedNs);
        float phase = (elapsed % INDETERMINATE_PERIOD_NS) / (float) INDETERMINATE_PERIOD_NS;

        // Fill from the left edge like the approved artwork.  At the end the
        // sweep restarts because this stage has no real numeric progress value.
        float visualFill = Mth.clamp(phase, 0.0F, 1.0F);
        drawProgressFill(graphics, barRect(screen), visualFill);
    }

    private static BarRect barRect(Screen screen) {
        int x = Math.round(screen.width * (BAR_X / (float) TEX_W));
        int y = Math.round(screen.height * (BAR_Y / (float) TEX_H));
        int width = Math.max(1, Math.round(screen.width * (BAR_W / (float) TEX_W)));
        int height = Math.max(1, Math.round(screen.height * (BAR_H / (float) TEX_H)));
        return new BarRect(x, y, width, height);
    }

    private static void drawProgressFill(GuiGraphics graphics, BarRect bar, float progress) {
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        if (progress <= 0.0F) return;

        int destinationWidth = Math.max(1, Math.round(bar.width * progress));
        int sourceWidth = Math.max(1, Math.min(FILL_W, Math.round(FILL_W * progress)));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(
                BAR_FILL,
                bar.x,
                bar.y,
                destinationWidth,
                bar.height,
                0F,
                0F,
                sourceWidth,
                FILL_H,
                FILL_W,
                FILL_H
        );
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private static void drawFullscreen(GuiGraphics graphics, Screen screen) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(
                BACKGROUND,
                0,
                0,
                screen.width,
                screen.height,
                0F,
                0F,
                TEX_W,
                TEX_H,
                TEX_W,
                TEX_H
        );
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private static void resetIfScreenChanged(Screen screen) {
        if (progressScreen != null && progressScreen != screen) {
            progressScreen = null;
            displayedProgress = 0.0F;
        }
        if (indeterminateScreen != null && indeterminateScreen != screen) {
            indeterminateScreen = null;
            indeterminateStartedNs = 0L;
        }
    }

    private static boolean has(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    private static void warnOnce() {
        if (!warned) {
            warned = true;
            LOGGER.warn("[DomeSurvival UI V27] Loading artwork/fill not available yet; renderer will retry.");
        }
    }

    private record BarRect(int x, int y, int width, int height) {}
}
