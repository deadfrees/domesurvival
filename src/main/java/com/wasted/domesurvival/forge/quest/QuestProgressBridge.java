package com.wasted.domesurvival.forge.quest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * API-free static bridge for CustomNPCs scripts and other optional integrations.
 *
 * Example from JavaScript:
 * Java.type("com.wasted.domesurvival.forge.quest.QuestProgressBridge")
 */
public final class QuestProgressBridge {
    private QuestProgressBridge() {
    }

    public static boolean isKnownFlag(String flag) {
        return QuestProgressFlags.isKnown(flag);
    }

    public static boolean hasFlag(String flag) {
        ServerLevel level = overworld();
        return level != null && QuestProgressService.has(level, flag);
    }

    public static boolean setFlag(String flag) {
        return setFlag(flag, "script");
    }

    public static boolean setFlag(String flag, String source) {
        ServerLevel level = overworld();
        if (level == null) {
            return false;
        }

        QuestProgressService.MutationResult result =
                QuestProgressService.set(level, flag, "bridge:" + source);
        return result != QuestProgressService.MutationResult.UNKNOWN_FLAG;
    }

    public static boolean clearFlag(String flag) {
        return clearFlag(flag, "script");
    }

    public static boolean clearFlag(String flag, String source) {
        ServerLevel level = overworld();
        if (level == null) {
            return false;
        }

        QuestProgressService.MutationResult result =
                QuestProgressService.clear(level, flag, "bridge:" + source);
        return result != QuestProgressService.MutationResult.UNKNOWN_FLAG;
    }

    public static String inspect() {
        ServerLevel level = overworld();
        if (level == null) {
            return "Quest progression server is not available.";
        }

        QuestProgressSavedData data = QuestProgressSavedData.get(level);
        if (data.flagCount() == 0) {
            return "DomeQuest: no story flags are set.";
        }

        return "DomeQuest (" + data.flagCount() + "): "
                + String.join(", ", data.sortedFlags());
    }

    private static ServerLevel overworld() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.overworld();
    }
}
