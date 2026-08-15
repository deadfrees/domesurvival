package com.wasted.domesurvival.forge.progression;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "domesurvival")
public final class DomeProjectCommands {
    private DomeProjectCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dome")
                        .then(Commands.literal("project")
                                .then(Commands.literal("status")
                                        .executes(ctx -> status(ctx.getSource())))
                                .then(Commands.literal("contribute")
                                        .then(Commands.literal(WorkshopProject.ID)
                                                .executes(ctx -> contributeWorkshop(ctx.getSource()))))
                                .then(Commands.literal("apply")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal(WorkshopProject.ID)
                                                .executes(ctx -> applyWorkshop(ctx.getSource()))))
                                .then(Commands.literal("rebuild")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal(WorkshopProject.ID)
                                                .executes(ctx -> rebuildWorkshop(ctx.getSource()))))
                                .then(Commands.literal("reset")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal(WorkshopProject.ID)
                                                .executes(ctx -> resetWorkshop(ctx.getSource())))))
        );
    }

    private static int status(CommandSourceStack source) {
        DomeProgressSavedData data = DomeProgressSavedData.get(source.getLevel());

        source.sendSuccess(
                () -> Component.literal("Проект Джозефа Куппера: Восстановление мастерской")
                        .withStyle(ChatFormatting.GOLD),
                false
        );

        sendRequirement(source, "Железо", data.workshopIron(), WorkshopProject.IRON_REQUIRED);
        sendRequirement(source, "Медь", data.workshopCopper(), WorkshopProject.COPPER_REQUIRED);
        sendRequirement(source, "Редстоун", data.workshopRedstone(), WorkshopProject.REDSTONE_REQUIRED);

        if (data.workshopComplete()) {
            source.sendSuccess(
                    () -> Component.literal(
                            data.workshopUpgradeApplied()
                                    ? "Проект завершён. Мастерская физически восстановлена."
                                    : "Проект завершён. Ожидается безопасное применение модернизации."
                    ).withStyle(data.workshopUpgradeApplied() ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal(
                            "Ресурсы может сдавать любой игрок. Прогресс общий для всего мира."
                    ).withStyle(ChatFormatting.GRAY),
                    false
            );
        }

        return 1;
    }

    private static int contributeWorkshop(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DomeProjectService.ContributionResult result = DomeProjectService.contributeWorkshop(player);

        if (result.alreadyComplete()) {
            source.sendFailure(Component.literal("Проект Джозефа Куппера уже завершён."));
            return 0;
        }

        if (result.total() <= 0) {
            source.sendFailure(Component.literal(
                    "В инвентаре нет ресурсов, которые сейчас нужны для мастерской."
            ));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Передано: железо " + result.iron()
                                + ", медь " + result.copper()
                                + ", редстоун " + result.redstone()
                ).withStyle(ChatFormatting.YELLOW),
                false
        );

        if (result.completedNow()) {
            WorkshopUpgradeApplier.ApplyResult upgrade =
                    WorkshopUpgradeApplier.applyIfNeeded(player.serverLevel());

            source.sendSuccess(
                    () -> Component.literal(
                            "Проект Джозефа Куппера «Восстановление мастерской» завершён! База перешла на этап 1."
                    ).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    true
            );

            sendUpgradeResult(source, upgrade);
        }

        status(source);
        return result.total();
    }

    private static int applyWorkshop(CommandSourceStack source) {
        WorkshopUpgradeApplier.ApplyResult result =
                WorkshopUpgradeApplier.adminApply(source.getLevel());

        sendUpgradeResult(source, result);
        return (result == WorkshopUpgradeApplier.ApplyResult.APPLIED
                || result == WorkshopUpgradeApplier.ApplyResult.REBUILT) ? 1 : 0;
    }

    private static int rebuildWorkshop(CommandSourceStack source) {
        WorkshopUpgradeApplier.ApplyResult result =
                WorkshopUpgradeApplier.rebuild(source.getLevel());

        sendUpgradeResult(source, result);
        return result == WorkshopUpgradeApplier.ApplyResult.REBUILT ? 1 : 0;
    }

    private static int resetWorkshop(CommandSourceStack source) {
        DomeProgressSavedData data = DomeProgressSavedData.get(source.getLevel());
        data.resetWorkshopProgress();

        source.sendSuccess(
                () -> Component.literal(
                        "Тестовый прогресс проекта Джозефа Куппера сброшен. Физические постройки не удаляются."
                ).withStyle(ChatFormatting.RED),
                true
        );
        return 1;
    }

    private static void sendUpgradeResult(CommandSourceStack source, WorkshopUpgradeApplier.ApplyResult result) {
        switch (result) {
            case APPLIED -> source.sendSuccess(
                    () -> Component.literal(
                            "Мастерская Джозефа Куппера безопасно построена внутри купола."
                    ).withStyle(ChatFormatting.AQUA),
                    true
            );
            case REBUILT -> source.sendSuccess(
                    () -> Component.literal(
                            "Мастерская Джозефа Куппера перестроена: восстановлена в исходном положении; под зданием добавлен сплошной пол."
                    ).withStyle(ChatFormatting.AQUA),
                    true
            );
            case ALREADY_APPLIED -> source.sendSuccess(
                    () -> Component.literal("Мастерская уже была построена. Для перестройки используй: /dome project rebuild workshop")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
            case PROJECT_NOT_COMPLETE -> source.sendFailure(
                    Component.literal("Сначала нужно завершить проект Джозефа Куппера.")
            );
            case STORAGE_NOT_EMPTY -> source.sendFailure(
                    Component.literal("Перестройка отменена: в сундуке, бочке или печи старой мастерской остались предметы.")
            );
            case TEMPLATE_MISSING -> source.sendFailure(
                    Component.literal("Не найден один из шаблонов мастерской Stage 3 V9.")
            );
            case PLACEMENT_FAILED -> source.sendFailure(
                    Component.literal("Не удалось разместить или восстановить мастерскую. Изменения остановлены.")
            );
        }
    }

    private static void sendRequirement(CommandSourceStack source, String name, int current, int target) {
        ChatFormatting color = current >= target ? ChatFormatting.GREEN : ChatFormatting.WHITE;
        source.sendSuccess(
                () -> Component.literal(" • " + name + ": " + current + " / " + target)
                        .withStyle(color),
                false
        );
    }
}
