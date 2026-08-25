package com.wasted.domesurvival.forge.quest;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Global main-story synchronization with a single global reward winner.
 *
 * Completion is server-wide. Material/XP reward is not duplicated: only the
 * player whose action first completed the quest receives its visible rewards.
 */
public final class QuestGlobalSyncService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> CATCHUP_SUPPRESSED = new HashSet<>();
    private static final Set<UUID> BROADCASTING = new HashSet<>();

    private QuestGlobalSyncService() {
    }

    public static int completeFromReward(
            ServerPlayer source,
            String rawQuestId,
            QuestFeedback.Tier feedback,
            String rawStoryFlag
    ) {
        String questId = normalize(rawQuestId);
        if (!QuestGlobalRegistry.isKnownQuest(questId)) {
            return 0;
        }

        String storyFlag = QuestProgressFlags.normalize(rawStoryFlag);
        if (!storyFlag.isEmpty()
                && !"-".equals(storyFlag)
                && QuestProgressFlags.isKnown(storyFlag)) {
            QuestProgressService.set(
                    source.serverLevel(),
                    storyFlag,
                    "quest:" + questId
            );
        }

        GlobalQuestProgressSavedData data =
                GlobalQuestProgressSavedData.get(source.serverLevel());

        GlobalQuestProgressSavedData.CompletionResult completion =
                data.completeOnce(
                        questId,
                        source.getUUID(),
                        source.getGameProfile().getName()
                );

        if (!completion.newlyCompleted()) {
            // This player/team is receiving a mirrored/catch-up completion.
            // Mark the visible rewards as claimed WITHOUT Reward.claim(), so
            // FTB cannot give a second global copy.
            if (!FtbQuestRewardCompat.suppressVisibleRewards(source, questId)) {
                LOGGER.error(
                        "[DomeQuest] Could not suppress mirrored rewards for {} / {}",
                        source.getGameProfile().getName(),
                        questId
                );
                return 0;
            }

            data.markPlayerRevision(
                    source.getUUID(),
                    Math.max(
                            data.playerRevision(source.getUUID()),
                            completion.revision()
                    )
            );
            return 1;
        }

        // The actual actor is the one global reward winner.
        // Do NOT pre-claim visible rewards here: FTB will process the visible
        // auto-enabled reward immediately after this first internal reward.
        if (!isFeedbackSuppressed(source)) {
            QuestFeedback.play(source, feedback);
        }

        data.markPlayerRevision(
                source.getUUID(),
                Math.max(
                        data.playerRevision(source.getUUID()),
                        completion.revision()
                )
        );

        LOGGER.info(
                "[DomeQuest] GLOBAL COMPLETE quest={} winner={} revision={}",
                questId,
                source.getGameProfile().getName(),
                completion.revision()
        );

        MinecraftServer server = source.server;
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            if (target.getUUID().equals(source.getUUID())) {
                continue;
            }

            BROADCASTING.add(target.getUUID());
            try {
                int result = forceComplete(target, questId);
                if (result > 0) {
                    data.markPlayerRevision(
                            target.getUUID(),
                            Math.max(
                                    data.playerRevision(target.getUUID()),
                                    completion.revision()
                            )
                    );
                }
            } finally {
                BROADCASTING.remove(target.getUUID());
            }
        }

        return 1;
    }

    public static CatchupResult catchUp(ServerPlayer player) {
        if (!ModList.get().isLoaded("ftbquests")) {
            return new CatchupResult(0, 0, false);
        }

        if (!FtbQuestRewardCompat.isAvailable()) {
            LOGGER.error(
                    "[DomeQuest] Catch-up stopped: FTB reward compatibility unavailable."
            );
            return new CatchupResult(0, playerRevision(player), false);
        }

        GlobalQuestProgressSavedData data =
                GlobalQuestProgressSavedData.get(player.serverLevel());

        int before = data.playerRevision(player.getUUID());
        var missing = data.completionsAfter(before);
        if (missing.isEmpty()) {
            return new CatchupResult(0, before, true);
        }

        int completed = 0;
        int lastGoodRevision = before;
        boolean allSuccessful = true;

        CATCHUP_SUPPRESSED.add(player.getUUID());
        try {
            for (GlobalQuestProgressSavedData.Completion completion : missing) {
                int result = forceComplete(player, completion.questId());
                if (result > 0) {
                    completed++;
                    lastGoodRevision = completion.revision();
                } else {
                    allSuccessful = false;
                    break;
                }
            }
        } finally {
            CATCHUP_SUPPRESSED.remove(player.getUUID());
        }

        data.markPlayerRevision(player.getUUID(), lastGoodRevision);

        if (completed > 0) {
            player.sendSystemMessage(
                    Component.literal(
                            "Dome Survival: синхронизировано пропущенных квестов: "
                                    + completed
                    ).withStyle(ChatFormatting.GRAY)
            );
        }

        return new CatchupResult(completed, lastGoodRevision, allSuccessful);
    }

    public static void resetCatchupRevision(ServerPlayer player) {
        GlobalQuestProgressSavedData.get(player.serverLevel())
                .resetPlayerRevision(player.getUUID());
    }

    public static boolean isGlobalCompleted(ServerPlayer player, String questId) {
        return GlobalQuestProgressSavedData.get(player.serverLevel())
                .isCompleted(questId);
    }

    public static String winnerName(ServerPlayer player, String questId) {
        String name = GlobalQuestProgressSavedData.get(player.serverLevel())
                .winnerName(questId);
        return name.isBlank() ? "<legacy/no-winner>" : name;
    }

    public static int currentRevision(ServerPlayer player) {
        return GlobalQuestProgressSavedData.get(player.serverLevel())
                .currentRevision();
    }

    public static int playerRevision(ServerPlayer player) {
        return GlobalQuestProgressSavedData.get(player.serverLevel())
                .playerRevision(player.getUUID());
    }

    public static int globalCompletedCount(ServerPlayer player) {
        return GlobalQuestProgressSavedData.get(player.serverLevel())
                .allCompletedIds()
                .size();
    }

    private static int forceComplete(ServerPlayer player, String questId) {
        if (!ModList.get().isLoaded("ftbquests")) {
            return 0;
        }

        String command = "ftbquests change_progress "
                + player.getScoreboardName()
                + " complete "
                + questId;

        return player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack()
                        .withSuppressedOutput(),
                command
        );
    }

    private static boolean isFeedbackSuppressed(ServerPlayer player) {
        return CATCHUP_SUPPRESSED.contains(player.getUUID());
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toUpperCase();
    }

    public record CatchupResult(
            int completedCount,
            int revision,
            boolean allSuccessful
    ) {
    }
}
