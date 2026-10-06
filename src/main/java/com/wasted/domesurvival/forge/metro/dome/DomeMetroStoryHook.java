package com.wasted.domesurvival.forge.metro.dome;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stable Stage-4 bridge for Joseph / CustomNPCs quest-dialogue integration.
 * CustomNPCs triggers the unlock; DomeSurvival SavedData remains the source of truth.
 */
public final class DomeMetroStoryHook {
    private DomeMetroStoryHook() {
    }

    public static boolean unlockFromJoseph(ServerPlayer player) {
        return DomeMetroService.unlock(player.getServer(), player.getUUID());
    }

    public static boolean unlockFromJoseph(MinecraftServer server) {
        return DomeMetroService.unlock(server, null);
    }

    public static boolean unlockFromJoseph(MinecraftServer server, ServerLevel level, BlockPos domeAnchor, ServerPlayer player) {
        return DomeMetroService.unlock(server, level, domeAnchor, player == null ? null : player.getUUID());
    }
}
