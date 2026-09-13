package com.wasted.domesurvival.forge.technology;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

/**
 * Small server-side API for technology checks. Gameplay code should use this class
 * instead of reading SavedData directly.
 */
public final class TechnologyService {
    private TechnologyService() {
    }

    public static boolean isUnlocked(MinecraftServer server, String rawId) {
        String id = TechnologyKeys.normalize(rawId);
        return id != null && TechnologySavedData.get(server).isUnlocked(id);
    }

    public static boolean isUnlocked(ServerLevel level, String rawId) {
        return isUnlocked(level.getServer(), rawId);
    }

    public static ChangeResult unlock(MinecraftServer server, String rawId) {
        String id = TechnologyKeys.normalize(rawId);
        if (id == null) {
            return ChangeResult.INVALID_ID;
        }
        return TechnologySavedData.get(server).unlock(id)
                ? ChangeResult.CHANGED
                : ChangeResult.ALREADY_SET;
    }

    public static ChangeResult lock(MinecraftServer server, String rawId) {
        String id = TechnologyKeys.normalize(rawId);
        if (id == null) {
            return ChangeResult.INVALID_ID;
        }
        return TechnologySavedData.get(server).lock(id)
                ? ChangeResult.CHANGED
                : ChangeResult.ALREADY_SET;
    }

    public static Set<String> unlocked(MinecraftServer server) {
        return TechnologySavedData.get(server).unlockedView();
    }

    public enum ChangeResult {
        CHANGED,
        ALREADY_SET,
        INVALID_ID
    }
}
