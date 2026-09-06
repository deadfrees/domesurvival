package com.wasted.domesurvival.forge.progression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.StringJoiner;

/**
 * Neutral bridge for FTB Quests, CustomNPCs and server administration.
 *
 * <p>Example FTB Quests command reward:
 * {@code /domeprogress unlock industrial}</p>
 */
public final class ProgressionCommands {
    private ProgressionCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("domeprogress")
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
                        .then(Commands.literal("has")
                                .then(Commands.argument(
                                                "branch",
                                                StringArgumentType.word()
                                        )
                                        .executes(ctx -> has(
                                                ctx.getSource(),
                                                StringArgumentType.getString(
                                                        ctx,
                                                        "branch"
                                                )
                                        ))))
                        .then(Commands.literal("unlock")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument(
                                                "branch",
                                                StringArgumentType.word()
                                        )
                                        .executes(ctx -> unlock(
                                                ctx.getSource(),
                                                StringArgumentType.getString(
                                                        ctx,
                                                        "branch"
                                                )
                                        ))))
                        .then(Commands.literal("lock")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument(
                                                "branch",
                                                StringArgumentType.word()
                                        )
                                        .executes(ctx -> lock(
                                                ctx.getSource(),
                                                StringArgumentType.getString(
                                                        ctx,
                                                        "branch"
                                                )
                                        ))))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> reset(ctx.getSource())))
        );
    }

    private static int status(CommandSourceStack source) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String branch : ProgressionService.unlocked(source.getServer())) {
            joiner.add(branch);
        }

        String result = joiner.length() == 0
                ? "<none>"
                : joiner.toString();

        source.sendSuccess(
                () -> Component.literal(
                        "DomeSurvival progression: " + result
                ).withStyle(ChatFormatting.AQUA),
                false
        );
        return ProgressionService.unlocked(source.getServer()).size();
    }

    private static int has(
            CommandSourceStack source,
            String rawBranch
    ) {
        String branch = ProgressionKeys.normalize(rawBranch);
        if (branch == null) {
            source.sendFailure(
                    Component.literal("Invalid progression branch id.")
            );
            return 0;
        }

        boolean unlocked = ProgressionService.isUnlocked(
                source.getServer(),
                branch
        );

        Component message = Component.literal(
                branch + "=" + unlocked
        ).withStyle(
                unlocked ? ChatFormatting.GREEN : ChatFormatting.RED
        );

        source.sendSuccess(() -> message, false);
        return unlocked ? 1 : 0;
    }

    private static int unlock(
            CommandSourceStack source,
            String rawBranch
    ) {
        String branch = ProgressionKeys.normalize(rawBranch);
        if (branch == null) {
            source.sendFailure(
                    Component.literal("Invalid progression branch id.")
            );
            return 0;
        }

        ProgressionService.ChangeResult result =
                ProgressionService.unlock(source.getServer(), branch);

        return switch (result) {
            case CHANGED -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "Unlocked progression branch: " + branch
                        ).withStyle(ChatFormatting.GREEN),
                        true
                );
                yield 1;
            }
            case ALREADY_SET -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "Progression branch already unlocked: " + branch
                        ).withStyle(ChatFormatting.YELLOW),
                        false
                );
                yield 1;
            }
            case INVALID_ID, PROTECTED -> {
                source.sendFailure(
                        Component.literal(
                                "Could not unlock progression branch: " + branch
                        )
                );
                yield 0;
            }
        };
    }

    private static int lock(
            CommandSourceStack source,
            String rawBranch
    ) {
        String branch = ProgressionKeys.normalize(rawBranch);
        if (branch == null) {
            source.sendFailure(
                    Component.literal("Invalid progression branch id.")
            );
            return 0;
        }

        ProgressionService.ChangeResult result =
                ProgressionService.lock(source.getServer(), branch);

        return switch (result) {
            case CHANGED -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "Locked progression branch: " + branch
                        ).withStyle(ChatFormatting.YELLOW),
                        true
                );
                yield 1;
            }
            case ALREADY_SET -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "Progression branch already locked: " + branch
                        ),
                        false
                );
                yield 1;
            }
            case PROTECTED -> {
                source.sendFailure(
                        Component.literal(
                                "The survival base branch cannot be locked."
                        )
                );
                yield 0;
            }
            case INVALID_ID -> {
                source.sendFailure(
                        Component.literal(
                                "Invalid progression branch id."
                        )
                );
                yield 0;
            }
        };
    }

    private static int reset(CommandSourceStack source) {
        boolean changed = ProgressionService.reset(source.getServer());

        source.sendSuccess(
                () -> Component.literal(
                        changed
                                ? "Progression reset to survival."
                                : "Progression is already at survival."
                ).withStyle(ChatFormatting.YELLOW),
                true
        );
        return 1;
    }
}
