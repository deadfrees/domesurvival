package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.core.oxygen.OxygenRules;
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

        // Mimic vanilla air HUD behavior: disappear once reserve is full in breathable air.
        if (ClientOxygenState.breathable() && oxygen >= max) {
            return;
        }

        int rightEdge = screenWidth / 2 + 91;
        int y = screenHeight - 59;

        // If vanilla underwater bubbles are also visible, keep our environmental O2 row separate.
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

        /*
         * V3.1: no "O2" text label.
         * In Minecraft's pixel font it was visually ambiguous and looked like the number "02".
         * The cyan bubble row itself is the oxygen indicator.
         */
    }
}
