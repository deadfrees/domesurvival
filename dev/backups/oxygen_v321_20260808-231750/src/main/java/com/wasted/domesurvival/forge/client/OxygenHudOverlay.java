package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.core.oxygen.OxygenRules;
import com.wasted.domesurvival.core.oxygen.OxygenSource;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class OxygenHudOverlay {
    private static final ResourceLocation FULL =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/oxygen_full.png");
    private static final ResourceLocation HALF =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/oxygen_half.png");
    private static final ResourceLocation EMPTY =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/oxygen_empty.png");
    private static final ResourceLocation TANK =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/oxygen_tank_source.png");

    private static final int ICON_SIZE = 9;
    private static final int ICON_STEP = 8;

    private OxygenHudOverlay() {
    }

    public static final IGuiOverlay HUD = OxygenHudOverlay::render;

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui,
                               GuiGraphics graphics,
                               float partialTick,
                               int screenWidth,
                               int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ClientOxygenState.initialized()) {
            return;
        }

        if (minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }

        int oxygen = ClientOxygenState.oxygen();
        int max = ClientOxygenState.maxOxygen();

        // Full personal reserve in ambient breathable air needs no HUD.
        if (ClientOxygenState.breathable()
                && ClientOxygenState.source() == OxygenSource.ENVIRONMENT
                && oxygen >= max) {
            return;
        }

        int rightEdge = screenWidth / 2 + 91;
        int y = screenHeight - 59;

        if (minecraft.player.isUnderWater()) {
            y -= 10;
        }

        double filledBubbles = oxygen * (double) OxygenRules.HUD_BUBBLES / max;

        for (int i = 0; i < OxygenRules.HUD_BUBBLES; i++) {
            double amount = filledBubbles - i;
            ResourceLocation texture = amount >= 1.0 ? FULL : amount >= 0.5 ? HALF : EMPTY;

            int x = rightEdge - ICON_SIZE - i * ICON_STEP;
            graphics.blit(texture, x, y, 0.0F, 0.0F,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }

        // A tiny cylinder glyph distinguishes tank oxygen from the emergency reserve.
        // No text is used, preserving the V3.1 feedback decision.
        if (ClientOxygenState.source() == OxygenSource.TANK) {
            int tankX = rightEdge - ICON_SIZE - OxygenRules.HUD_BUBBLES * ICON_STEP - 11;
            graphics.blit(TANK, tankX, y, 0.0F, 0.0F,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }
}
