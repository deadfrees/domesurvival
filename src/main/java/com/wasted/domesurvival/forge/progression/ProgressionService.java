package com.wasted.domesurvival.forge.progression;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Small native API used by DomeSurvival gameplay code.
 *
 * <p>Do not call FTB Quests directly from machines. Quest systems should unlock a
 * branch here; gameplay systems should only query this service.</p>
 */
public final class ProgressionService {
    private ProgressionService() {
    }

    public static boolean isUnlocked(
            MinecraftServer server,
            String branch
    ) {
        String normalized = ProgressionKeys.normalize(branch);
        return normalized != null
                && ProgressionSavedData.get(server).isUnlocked(normalized);
    }

    public static boolean isUnlocked(
            ServerLevel level,
            String branch
    ) {
        return isUnlocked(level.getServer(), branch);
    }

    public static ChangeResult unlock(
            MinecraftServer server,
            String branch
    ) {
        String normalized = ProgressionKeys.normalize(branch);
        if (normalized == null) {
            return ChangeResult.INVALID_ID;
        }

        return ProgressionSavedData.get(server).unlock(normalized)
                ? ChangeResult.CHANGED
                : ChangeResult.ALREADY_SET;
    }

    public static ChangeResult lock(
            MinecraftServer server,
            String branch
    ) {
        String normalized = ProgressionKeys.normalize(branch);
        if (normalized == null) {
            return ChangeResult.INVALID_ID;
        }
        if (ProgressionKeys.SURVIVAL.equals(normalized)) {
            return ChangeResult.PROTECTED;
        }

        return ProgressionSavedData.get(server).lock(normalized)
                ? ChangeResult.CHANGED
                : ChangeResult.ALREADY_SET;
    }

    public static boolean reset(MinecraftServer server) {
        return ProgressionSavedData.get(server).reset();
    }

    public static Set<String> unlocked(MinecraftServer server) {
        return ProgressionSavedData.get(server).unlockedView();
    }

    public enum ChangeResult {
        CHANGED,
        ALREADY_SET,
        INVALID_ID,
        PROTECTED
    }
}
