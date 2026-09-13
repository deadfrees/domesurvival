package com.wasted.domesurvival.forge.technology;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.StringJoiner;

/** Developer/admin diagnostics for technology state. */
public final class TechnologyCommands {
    private TechnologyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dome")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("technology")
                        .then(Commands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> unlock(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")
                                        ))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> lock(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")
                                        ))))));
    }

    private static int list(CommandSourceStack source) {
        TechnologySavedData data = TechnologySavedData.get(source.getServer());
        StringJoiner unlocked = new StringJoiner(", ");
        for (String id : data.unlockedView()) {
            unlocked.add(id);
        }
        String text = unlocked.length() == 0 ? "<none>" : unlocked.toString();
        source.sendSuccess(() -> Component.literal(
                "Technologies v" + data.dataVersion() + ": " + text
        ).withStyle(ChatFormatting.AQUA), false);
        return data.unlockedView().size();
    }

    private static int unlock(CommandSourceStack source, String rawId) {
        String id = TechnologyKeys.normalize(rawId);
        if (id == null) {
            source.sendFailure(Component.literal("Invalid technology id."));
            return 0;
        }

        TechnologyService.ChangeResult result = TechnologyService.unlock(source.getServer(), id);
        if (result == TechnologyService.ChangeResult.CHANGED) {
            source.sendSuccess(() -> Component.literal("Unlocked technology: " + id)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        if (result == TechnologyService.ChangeResult.ALREADY_SET) {
            source.sendSuccess(() -> Component.literal("Technology already unlocked: " + id)
                    .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        source.sendFailure(Component.literal("Could not unlock technology: " + id));
        return 0;
    }

    private static int lock(CommandSourceStack source, String rawId) {
        String id = TechnologyKeys.normalize(rawId);
        if (id == null) {
            source.sendFailure(Component.literal("Invalid technology id."));
            return 0;
        }

        TechnologyService.ChangeResult result = TechnologyService.lock(source.getServer(), id);
        if (result == TechnologyService.ChangeResult.CHANGED) {
            source.sendSuccess(() -> Component.literal("Locked technology: " + id)
                    .withStyle(ChatFormatting.YELLOW), true);
            return 1;
        }
        if (result == TechnologyService.ChangeResult.ALREADY_SET) {
            source.sendSuccess(() -> Component.literal("Technology already locked: " + id), false);
            return 1;
        }
        source.sendFailure(Component.literal("Could not lock technology: " + id));
        return 0;
    }
}
