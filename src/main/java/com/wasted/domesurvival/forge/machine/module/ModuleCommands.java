package com.wasted.domesurvival.forge.machine.module;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.stream.Collectors;

/** Developer/admin diagnostics for the shared machine module API. */
public final class ModuleCommands {
    private ModuleCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dome")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("modules")
                        .then(Commands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))));
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "DomeSurvival machine modules: " + MachineModuleCatalog.all().size()
        ).withStyle(ChatFormatting.AQUA), false);

        for (MachineModule module : MachineModuleCatalog.all()) {
            MachineModuleModifiers effect = module.modifiers();
            String conflicts = module.incompatibleWith().isEmpty()
                    ? "none"
                    : module.incompatibleWith().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(","));

            source.sendSuccess(() -> Component.literal(
                    module.id() + " type=" + module.type()
                            + " speed=" + effect.speedPercent() + "%"
                            + " energy=" + effect.energyPercent() + "%"
                            + " buffer=" + effect.bufferPercent() + "%"
                            + " automation=" + effect.automation()
                            + " preserve=" + effect.preserveProgress()
                            + " communication=" + effect.communication()
                            + " conflicts=" + conflicts
            ), false);
        }
        return MachineModuleCatalog.all().size();
    }
}
