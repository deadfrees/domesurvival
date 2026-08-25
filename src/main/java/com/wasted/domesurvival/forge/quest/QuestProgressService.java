package com.wasted.domesurvival.forge.quest;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Single mutation point for story flags.
 *
 * Keeping mutation here makes bridge calls idempotent and gives one place for
 * diagnostics before FTB/CustomNPCs synchronization is connected.
 */
public final class QuestProgressService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private QuestProgressService() {
    }

    public static boolean has(ServerLevel level, String rawFlag) {
        return QuestProgressSavedData.get(level).hasFlag(rawFlag);
    }

    public static MutationResult set(ServerLevel level, String rawFlag, String source) {
        String flag = QuestProgressFlags.normalize(rawFlag);
        if (!QuestProgressFlags.isKnown(flag)) {
            return MutationResult.UNKNOWN_FLAG;
        }

        boolean changed = QuestProgressSavedData.get(level).setFlag(flag);
        if (changed) {
            LOGGER.info("[DomeQuest] SET flag={} source={}", flag, safeSource(source));
            return MutationResult.CHANGED;
        }

        return MutationResult.UNCHANGED;
    }

    public static MutationResult clear(ServerLevel level, String rawFlag, String source) {
        String flag = QuestProgressFlags.normalize(rawFlag);
        if (!QuestProgressFlags.isKnown(flag)) {
            return MutationResult.UNKNOWN_FLAG;
        }

        boolean changed = QuestProgressSavedData.get(level).clearFlag(flag);
        if (changed) {
            LOGGER.info("[DomeQuest] CLEAR flag={} source={}", flag, safeSource(source));
            return MutationResult.CHANGED;
        }

        return MutationResult.UNCHANGED;
    }

    private static String safeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        return source.replace('\n', '_').replace('\r', '_');
    }

    public enum MutationResult {
        CHANGED,
        UNCHANGED,
        UNKNOWN_FLAG
    }
}
