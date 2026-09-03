package com.wasted.domesurvival.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * V32 clean-background fallback.
 * TitleScreen and all loading screens are deliberately excluded.
 * Widget geometry is never modified here.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DomeSurvivalScreenTuner {
    private static final int TEX_W = 1920;
    private static final int TEX_H = 1080;

    private static final ResourceLocation WORLD = texture("world.png");
    private static final ResourceLocation NETWORK = texture("network.png");
    private static final ResourceLocation SYSTEM = texture("system.png");
    private static final ResourceLocation CORRIDOR = texture("corridor.png");

    private DomeSurvivalScreenTuner() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBackgroundRendered(ScreenEvent.BackgroundRendered event) {
        Screen screen = event.getScreen();
        ResourceLocation texture = backgroundFor(screen);
        if (texture != null) {
            drawFullscreen(event.getGuiGraphics(), screen, texture);
        }
    }

    private static ResourceLocation backgroundFor(Screen screen) {
        String simple = screen.getClass().getSimpleName();
        String name = screen.getClass().getName();
        String lower = name.toLowerCase(Locale.ROOT);

        if (simple.equals("TitleScreen") || isLoadingScreen(simple, screen) || screen instanceof AbstractContainerScreen<?>) {
            return null;
        }

        if (simple.equals("SelectWorldScreen")
                || simple.equals("CreateWorldScreen")
                || simple.equals("EditWorldScreen")
                || simple.equals("EditGameRulesScreen")
                || simple.equals("ExperimentsScreen")
                || simple.equals("PresetFlatWorldScreen")
                || simple.equals("CreateBuffetWorldScreen")) {
            return WORLD;
        }

        if (simple.equals("JoinMultiplayerScreen")
                || simple.equals("DirectJoinServerScreen")
                || simple.equals("EditServerScreen")
                || simple.equals("DisconnectedScreen")
                || simple.equals("ShareToLanScreen")
                || simple.equals("SocialInteractionsScreen")) {
            return NETWORK;
        }

        if (simple.equals("OptionsScreen")
                || simple.equals("PauseScreen")
                || simple.equals("ConfirmScreen")
                || simple.equals("BackupConfirmScreen")
                || simple.equals("DatapackLoadFailureScreen")
                || simple.equals("ConfirmExperimentalFeaturesScreen")
                || simple.equals("DeathScreen")
                || simple.equals("TelemetryInfoScreen")) {
            return SYSTEM;
        }

        if (simple.equals("ModListScreen")
                || simple.equals("PackSelectionScreen")
                || simple.equals("StatsScreen")
                || name.startsWith("net.minecraft.client.gui.screens.options.")
                || lower.contains("embeddium")
                || lower.contains("oculus")
                || lower.contains("sodium")) {
            return CORRIDOR;
        }

        return SYSTEM;
    }

    private static boolean isLoadingScreen(String simple, Screen screen) {
        if (screen instanceof net.minecraft.client.gui.screens.LevelLoadingScreen) return true;
        return simple.equals("GenericDirtMessageScreen")
                || simple.equals("GenericWaitingScreen")
                || simple.equals("ProgressScreen")
                || simple.equals("ReceivingLevelScreen")
                || simple.equals("DownloadingTerrainScreen")
                || simple.equals("ConnectScreen")
                || simple.equals("MessageScreen")
                || simple.equals("OptimizeWorldScreen");
    }

    private static ResourceLocation texture(String file) {
        return new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/ui/v32/" + file);
    }

    private static void drawFullscreen(GuiGraphics graphics, Screen screen, ResourceLocation texture) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(texture, 0, 0, screen.width, screen.height,
                0F, 0F, TEX_W, TEX_H, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }
}
