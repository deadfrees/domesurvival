package com.wasted.domesurvival.forge.client.music;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Development-only client commands for testing music signals without changing
 * server state or requiring operator permissions.
 */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DomeMusicClientCommands {
    private DomeMusicClientCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("domemusic")
                        .then(Commands.literal("story").executes(context -> {
                            DomeMusicSignals.requestStoryOverride();
                            message("DomeMusic: story cue requested (03).");
                            return 1;
                        }))
                        .then(Commands.literal("discovery").executes(context -> {
                            DomeMusicSignals.requestDiscovery();
                            message("DomeMusic: discovery cue requested (08).");
                            return 1;
                        }))
                        .then(Commands.literal("status").executes(context -> {
                            message("DomeMusic: " + DomeMusicClientEvents.debugStatus());
                            return 1;
                        }))
        );
    }

    private static void message(String text) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(text), false);
        }
    }
}
