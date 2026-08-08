package com.wasted.domesurvival.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.dome.DomeGenerationService;
import com.wasted.domesurvival.forge.dome.DomePreview;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class DomeCommands {
    private DomeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dome")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("preview").executes(ctx -> preview(ctx.getSource())))
                .then(Commands.literal("generate").executes(ctx -> generate(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static int preview(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int particles = DomePreview.show(level);
        DomeSpec spec = DomeSpec.wastedV1();
        source.sendSuccess(() -> Component.literal(
                "Dome preview: center=" + spec.centerX() + "," + spec.baseY() + "," + spec.centerZ()
                        + " R=" + spec.surfaceRadius() + ", particles=" + particles), false);
        return 1;
    }

    private static int generate(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        DomeGenerationService.StartResult result = DomeGenerationService.start(level);
        switch (result) {
            case STARTED -> source.sendSuccess(() -> Component.literal(
                    "Dome generation started: " + DomeGenerationService.total() + " shell blocks queued."), true);
            case ALREADY_RUNNING -> source.sendFailure(Component.literal("Dome generation is already running."));
            case ALREADY_GENERATED -> source.sendFailure(Component.literal("Dome is already marked as generated in this world."));
        }
        return result == DomeGenerationService.StartResult.STARTED ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        boolean generated = DomeSavedData.get(level).isGenerated();
        source.sendSuccess(() -> Component.literal(
                "Dome status: generated=" + generated
                        + ", running=" + DomeGenerationService.isRunning()
                        + ", progress=" + DomeGenerationService.placed() + "/" + DomeGenerationService.total()), false);
        return 1;
    }
}
