package com.wasted.domesurvival.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.List;

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

    /**
     * Forge adds the creative-page arrows as ordinary 20x20 vanilla buttons.
     * Their widget plate is the dark square visible in the pack UI. Keep the
     * original paging actions, but replace their vanilla renderer with a
     * glyph-only widget. Widget alpha does not hide the nine-slice plate in
     * 1.20.1, which is why the former alpha-based fix left dark squares.
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen)) {
            return;
        }

        for (GuiEventListener listener : List.copyOf(event.getListenersList())) {
            if (!(listener instanceof Button button)) {
                continue;
            }
            String message = button.getMessage().getString();
            if ((message.equals("<") || message.equals(">"))
                    && button.getWidth() == 20 && button.getHeight() == 20) {
                event.removeListener(button);
                TransparentPageButton replacement = new TransparentPageButton(button);
                replacement.active = button.active;
                replacement.visible = button.visible;
                event.addListener(replacement);
            }
        }
    }

    private static final class TransparentPageButton extends AbstractButton {
        private final Button original;

        private TransparentPageButton(Button original) {
            super(original.getX(), original.getY(), original.getWidth(), original.getHeight(), original.getMessage());
            this.original = original;
        }

        @Override
        public void onPress() {
            original.onPress();
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            original.updateWidgetNarration(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFD5E1E7;
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
        }
    }

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

        if (simple.equals("TitleScreen")
                || isLoadingScreen(simple, screen)
                // Creative tab paging arrows must keep the vanilla transparent backing.
                || simple.equals("CreativeModeInventoryScreen")
                || screen instanceof AbstractContainerScreen<?>
                || lower.startsWith("mezz.jei.")) {
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
