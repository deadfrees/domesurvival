package com.wasted.domesurvival.forge.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "domesurvival")
public final class DomeQuestCommands {
    private DomeQuestCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("domequest")
                        .then(Commands.literal("inspect")
                                .executes(ctx -> inspectAll(ctx.getSource()))
                                .then(flagArgument()
                                        .executes(ctx -> inspectFlag(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "flag")
                                        ))))
                        .then(Commands.literal("check")
                                .then(flagArgument()
                                        .executes(ctx -> inspectFlag(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "flag")
                                        ))))
                        .then(Commands.literal("registry")
                                .executes(ctx -> registry(ctx.getSource())))
                        .then(Commands.literal("feedback")
                                .then(Commands.literal("normal")
                                        .executes(ctx -> feedback(
                                                ctx.getSource(),
                                                QuestFeedback.Tier.NORMAL
                                        )))
                                .then(Commands.literal("milestone")
                                        .executes(ctx -> feedback(
                                                ctx.getSource(),
                                                QuestFeedback.Tier.MILESTONE
                                        )))
                                .then(Commands.literal("chapter")
                                        .executes(ctx -> feedback(
                                                ctx.getSource(),
                                                QuestFeedback.Tier.CHAPTER
                                        ))))
                        .then(Commands.literal("complete")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("quest_id", StringArgumentType.word())
                                        .then(Commands.literal("normal")
                                                .then(Commands.argument("flag", StringArgumentType.word())
                                                        .executes(ctx -> completeQuest(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "quest_id"),
                                                                QuestFeedback.Tier.NORMAL,
                                                                StringArgumentType.getString(ctx, "flag")
                                                        ))))
                                        .then(Commands.literal("milestone")
                                                .then(Commands.argument("flag", StringArgumentType.word())
                                                        .executes(ctx -> completeQuest(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "quest_id"),
                                                                QuestFeedback.Tier.MILESTONE,
                                                                StringArgumentType.getString(ctx, "flag")
                                                        ))))
                                        .then(Commands.literal("chapter")
                                                .then(Commands.argument("flag", StringArgumentType.word())
                                                        .executes(ctx -> completeQuest(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "quest_id"),
                                                                QuestFeedback.Tier.CHAPTER,
                                                                StringArgumentType.getString(ctx, "flag")
                                                        ))))))
                        .then(Commands.literal("sync")
                                .then(Commands.literal("inspect")
                                        .executes(ctx -> syncInspect(ctx.getSource())))
                                .then(Commands.literal("catchup")
                                        .executes(ctx -> syncCatchup(ctx.getSource())))
                                .then(Commands.literal("resync")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> syncResync(ctx.getSource())))
                                .then(Commands.literal("reward_compat")
                                        .executes(ctx -> rewardCompat(ctx.getSource())))
                                .then(Commands.literal("winner")
                                        .then(Commands.argument("quest_id", StringArgumentType.word())
                                                .executes(ctx -> winner(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "quest_id")
                                                )))))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(flagArgument()
                                        .executes(ctx -> mutate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "flag"),
                                                true
                                        ))))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(2))
                                .then(flagArgument()
                                        .executes(ctx -> mutate(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "flag"),
                                                false
                                        ))))
                        .then(Commands.literal("gate")
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2))
                                        .then(flagArgument()
                                                .executes(ctx -> mutate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "flag"),
                                                        true
                                                ))))
                                .then(Commands.literal("clear")
                                        .requires(source -> source.hasPermission(2))
                                        .then(flagArgument()
                                                .executes(ctx -> mutate(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "flag"),
                                                        false
                                                )))))
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> flagArgument() {
        return Commands.argument("flag", StringArgumentType.word())
                .suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(QuestProgressFlags.all(), builder));
    }

    private static int completeQuest(
            CommandSourceStack source,
            String questId,
            QuestFeedback.Tier tier,
            String storyFlag
    ) {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("DomeQuest completion требует контекст игрока."));
            return 0;
        }

        return QuestGlobalSyncService.completeFromReward(player, questId, tier, storyFlag);
    }

    private static int syncInspect(CommandSourceStack source) {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Команда доступна только игроку."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "DomeQuest global: completed="
                                + QuestGlobalSyncService.globalCompletedCount(player)
                                + ", revision="
                                + QuestGlobalSyncService.currentRevision(player)
                                + ", yourRevision="
                                + QuestGlobalSyncService.playerRevision(player)
                ).withStyle(ChatFormatting.AQUA),
                false
        );
        return 1;
    }

    private static int syncCatchup(CommandSourceStack source) {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Команда доступна только игроку."));
            return 0;
        }

        var result = QuestGlobalSyncService.catchUp(player);
        source.sendSuccess(
                () -> Component.literal(
                        "Catch-up: " + result.completedCount()
                                + ", revision=" + result.revision()
                                + ", success=" + result.allSuccessful()
                ).withStyle(ChatFormatting.GRAY),
                false
        );
        return result.allSuccessful() ? 1 : 0;
    }

    private static int syncResync(CommandSourceStack source) {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Команда доступна только игроку."));
            return 0;
        }

        QuestGlobalSyncService.resetCatchupRevision(player);
        return syncCatchup(source);
    }

    private static int rewardCompat(CommandSourceStack source) {
        boolean ok = FtbQuestRewardCompat.isAvailable();
        source.sendSuccess(
                () -> Component.literal(
                        "FTB reward compat: " + FtbQuestRewardCompat.status()
                ).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED),
                false
        );
        return ok ? 1 : 0;
    }

    private static int winner(CommandSourceStack source, String questId) {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Команда доступна только игроку."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Global reward winner "
                                + questId.toUpperCase()
                                + ": "
                                + QuestGlobalSyncService.winnerName(player, questId)
                ).withStyle(ChatFormatting.GOLD),
                false
        );
        return 1;
    }

    private static int feedback(CommandSourceStack source, QuestFeedback.Tier tier) {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Quest feedback доступен только игроку."));
            return 0;
        }

        return QuestFeedback.play(player, tier);
    }

    private static int inspectAll(CommandSourceStack source) {
        QuestProgressSavedData data = QuestProgressSavedData.get(source.getLevel());

        source.sendSuccess(
                () -> Component.literal(
                        "DomeQuest: установлено " + data.flagCount()
                                + " / " + QuestProgressFlags.all().size() + " флагов."
                ).withStyle(ChatFormatting.GOLD),
                false
        );

        if (data.flagCount() == 0) {
            source.sendSuccess(
                    () -> Component.literal(" • сюжетные флаги пока не установлены.")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
            return 1;
        }

        for (String flag : data.sortedFlags()) {
            source.sendSuccess(
                    () -> Component.literal(" ✓ " + flag).withStyle(ChatFormatting.GREEN),
                    false
            );
        }

        return data.flagCount();
    }

    private static int inspectFlag(CommandSourceStack source, String rawFlag) {
        String flag = QuestProgressFlags.normalize(rawFlag);
        if (!QuestProgressFlags.isKnown(flag)) {
            source.sendFailure(Component.literal("Неизвестный DomeQuest flag: " + flag));
            return 0;
        }

        boolean enabled = QuestProgressService.has(source.getLevel(), flag);
        source.sendSuccess(
                () -> Component.literal((enabled ? "✓ " : "○ ") + flag + " = " + enabled)
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                false
        );
        return enabled ? 1 : 0;
    }

    private static int mutate(CommandSourceStack source, String rawFlag, boolean set) {
        String flag = QuestProgressFlags.normalize(rawFlag);
        String auditSource = "command:" + source.getTextName();

        QuestProgressService.MutationResult result = set
                ? QuestProgressService.set(source.getLevel(), flag, auditSource)
                : QuestProgressService.clear(source.getLevel(), flag, auditSource);

        if (result == QuestProgressService.MutationResult.UNKNOWN_FLAG) {
            source.sendFailure(Component.literal("Неизвестный DomeQuest flag: " + flag));
            return 0;
        }

        if (result == QuestProgressService.MutationResult.UNCHANGED) {
            source.sendSuccess(
                    () -> Component.literal(
                            flag + (set ? " уже установлен." : " уже сброшен.")
                    ).withStyle(ChatFormatting.GRAY),
                    false
            );
            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(
                        (set ? "Установлен flag: " : "Сброшен flag: ") + flag
                ).withStyle(set ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                true
        );
        return 1;
    }

    private static int registry(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal(
                        "Dome Survival campaign registry: "
                                + QuestCampaignRegistry.CHAPTERS.size()
                                + " глав; nominal design slots "
                                + QuestCampaignRegistry.nominalDesignSlotCount()
                                + "; финальная цель 290-310"
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        for (QuestCampaignRegistry.ChapterSpec chapter : QuestCampaignRegistry.CHAPTERS) {
            source.sendSuccess(
                    () -> Component.literal(
                            String.format(
                                    " %02d | %s | ~%d | %s",
                                    chapter.index(),
                                    chapter.id(),
                                    chapter.targetQuestCount(),
                                    chapter.title()
                            )
                    ).withStyle(ChatFormatting.GRAY),
                    false
            );
        }

        return QuestCampaignRegistry.CHAPTERS.size();
    }
}
