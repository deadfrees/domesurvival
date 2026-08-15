package com.wasted.domesurvival.forge.integration.customnpcs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wasted.domesurvival.forge.progression.WorkshopUpgradeApplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "domesurvival", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JosephScriptCommand {
    private static final String SCRIPT_FILE = "joseph_cooper_gui.js";
    private static final String RESET_LOCK_KEY = "domesurvival.release.logical_reset.v70";
    private static final String PREPARED_KEY = "domesurvival.release.prepared.v70";
    private static final String[] RESET_PREFIXES = {
        "domesurvival.stage01.", "domesurvival.stage1.",
        "domesurvival.stage02.", "domesurvival.stage2.",
        "domesurvival.stage03.", "domesurvival.stage3.",
        "domesurvival.stage04.", "domesurvival.stage4.",
        "domesurvival.stage05.", "domesurvival.stage5.",
        "domesurvival.workshop.",
        "domesurvival.warehouse.",
        "domesurvival.release."
    };

    private JosephScriptCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("josephscript")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                    .executes(context -> inspect(context.getSource())))
                .then(Commands.literal("apply")
                    .executes(context -> apply(context.getSource())))
                .then(Commands.literal("console")
                    .executes(context -> console(context.getSource())))
                .then(Commands.literal("clearconsole")
                    .executes(context -> clearConsole(context.getSource())))
                .then(Commands.literal("resetprogress")
                    .executes(context -> resetProgress(context.getSource())))
                .then(Commands.literal("resetworkshop")
                    .executes(context -> resetWorkshop(context.getSource())))
                .then(Commands.literal("resettest")
                    .executes(context -> resetTest(context.getSource())))
                .then(Commands.literal("prepareworld")
                    .executes(context -> prepareWorld(context.getSource())))
        );
    }

    private static int inspect(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EntityNPCInterface npc = findJoseph(player);

        if (npc == null) {
            source.sendFailure(Component.literal(
                "Joseph Cooper was not found within 48 blocks."
            ));
            return 0;
        }

        DataScript data = npc.script;
        List<ScriptContainer> containers = data.getScripts();

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] NPC=" + npc.getName().getString()
                + ", enabled=" + data.getEnabled()
                + ", language=" + data.getLanguage()
                + ", containers=" + containers.size()
        ), false);

        boolean controllerReady = ScriptController.Instance != null;
        boolean fileLoaded = controllerReady
            && ScriptController.Instance.scripts.containsKey(SCRIPT_FILE);

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] ScriptController=" + controllerReady
                + ", externalFileLoaded=" + fileLoaded
                + " (" + SCRIPT_FILE + ")"
        ), false);

        for (int i = 0; i < containers.size(); i++) {
            ScriptContainer container = containers.get(i);

            String inline = container.script == null ? "" : container.script;
            String preview = inline
                .replace("\r", " ")
                .replace("\n", " ");

            if (preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }

            final int index = i;
            final String finalPreview = preview;

            source.sendSuccess(() -> Component.literal(
                "[JosephScript] #" + index
                    + " inlineLength=" + inline.length()
                    + ", containsHelloDev=" + inline.contains("Hello Dev")
                    + ", linked=" + container.scripts
                    + ", errored=" + container.isErrored()
                    + ", valid=" + container.isValid()
                    + ", preview=" + finalPreview
            ), false);
        }

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] consoleLines=" + data.getConsoleText().size()
                + " (use /josephscript console)"
        ), false);

        return 1;
    }

    private static int console(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EntityNPCInterface npc = findJoseph(player);

        if (npc == null) {
            source.sendFailure(Component.literal(
                "Joseph Cooper was not found within 48 blocks."
            ));
            return 0;
        }

        DataScript data = npc.script;

        ArrayList<Map.Entry<Long, String>> entries =
            new ArrayList<>(data.getConsoleText().entrySet());

        entries.sort(Map.Entry.comparingByKey());

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Script console, total lines=" + entries.size()
        ), false);

        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                "[JosephScript] Console is empty."
            ), false);
            return 1;
        }

        int start = Math.max(0, entries.size() - 20);

        for (int i = start; i < entries.size(); i++) {
            Map.Entry<Long, String> entry = entries.get(i);

            String text = entry.getValue() == null
                ? "<null>"
                : entry.getValue()
                    .replace("\r", " ")
                    .replace("\n", " ");

            if (text.length() > 300) {
                text = text.substring(0, 300) + "...";
            }

            final long time = entry.getKey();
            final String line = text;

            source.sendSuccess(() -> Component.literal(
                "[JosephScript][" + time + "] " + line
            ), false);
        }

        return 1;
    }

    private static int clearConsole(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EntityNPCInterface npc = findJoseph(player);

        if (npc == null) {
            source.sendFailure(Component.literal(
                "Joseph Cooper was not found within 48 blocks."
            ));
            return 0;
        }

        npc.script.clearConsole();

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Script console cleared."
        ), false);

        return 1;
    }

    private static int resetProgress(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResetResult result = clearQuestStoredData(player);

        if (result.data == null) {
            source.sendFailure(Component.literal(
                "[JosephScript] CustomNPCs world StoredData is unavailable."
            ));
            return 0;
        }

        boolean oldWorkshopComplete = isJavaWorkshopAlreadyComplete();
        if (oldWorkshopComplete) {
            result.data.put(RESET_LOCK_KEY, 1);
        } else {
            result.data.remove(RESET_LOCK_KEY);
        }
        result.data.remove(PREPARED_KEY);

        final int removed = result.removed;
        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Quest progress reset. Removed StoredData keys: " + removed
        ), true);

        if (oldWorkshopComplete) {
            source.sendSuccess(() -> Component.literal(
                "[JosephScript] This test world already has completed Java workshop state. "
                    + "Logical reset mode is enabled, so Joseph will still start from Stage 01."
            ), false);
        }

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Right-click Joseph: Stage 01 should be active, Stages 02-05 locked."
        ), false);
        return 1;
    }

    private static int resetWorkshop(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        WorkshopUpgradeApplier.RemovalResult removal =
            WorkshopUpgradeApplier.removeForTesting(player.serverLevel());

        if (removal == WorkshopUpgradeApplier.RemovalResult.STORAGE_NOT_EMPTY) {
            source.sendFailure(Component.literal(
                "[JosephScript] Сброс мастерской отменён: сначала забери предметы из контейнеров внутри мастерской."
            ));
            return 0;
        }
        if (removal == WorkshopUpgradeApplier.RemovalResult.TEMPLATE_MISSING) {
            source.sendFailure(Component.literal(
                "[JosephScript] Сброс мастерской не выполнен: не найден cleanup-шаблон постройки."
            ));
            return 0;
        }
        if (removal == WorkshopUpgradeApplier.RemovalResult.PLACEMENT_FAILED) {
            source.sendFailure(Component.literal(
                "[JosephScript] Не удалось применить cleanup-шаблон мастерской."
            ));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Мастерская удалена, а Java-состояние её строительства сброшено."
        ), true);
        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Для полного тестового сброса всех этапов используй /josephscript resettest."
        ), false);
        return 1;
    }

    private static int resetTest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        WorkshopUpgradeApplier.RemovalResult removal =
            WorkshopUpgradeApplier.removeForTesting(player.serverLevel());

        if (removal == WorkshopUpgradeApplier.RemovalResult.STORAGE_NOT_EMPTY) {
            source.sendFailure(Component.literal(
                "[JosephScript] Полный тестовый сброс отменён: сначала забери предметы из контейнеров мастерской."
            ));
            return 0;
        }
        if (removal == WorkshopUpgradeApplier.RemovalResult.TEMPLATE_MISSING) {
            source.sendFailure(Component.literal(
                "[JosephScript] Полный тестовый сброс не выполнен: не найден cleanup-шаблон мастерской."
            ));
            return 0;
        }
        if (removal == WorkshopUpgradeApplier.RemovalResult.PLACEMENT_FAILED) {
            source.sendFailure(Component.literal(
                "[JosephScript] Полный тестовый сброс остановлен: мастерскую удалить не удалось."
            ));
            return 0;
        }

        ResetResult result = clearQuestStoredData(player);
        if (result.data == null) {
            source.sendFailure(Component.literal(
                "[JosephScript] Мастерская удалена, но CustomNPCs StoredData сейчас недоступен."
            ));
            return 0;
        }

        result.data.remove(RESET_LOCK_KEY);
        result.data.remove(PREPARED_KEY);

        final int removed = result.removed;
        source.sendSuccess(() -> Component.literal(
            "[JosephScript] ПОЛНЫЙ ТЕСТОВЫЙ СБРОС: мастерская удалена, Java-прогресс сброшен, удалено StoredData-ключей: "
                + removed
        ), true);
        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Теперь Stage 01 активен, Stage 02-05 закрыты. "
                + "После повторного выполнения Stage 02 мастерская построится заново."
        ), false);
        return 1;
    }

    private static int prepareWorld(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResetResult result = clearQuestStoredData(player);

        if (result.data == null) {
            source.sendFailure(Component.literal(
                "[JosephScript] CustomNPCs world StoredData is unavailable."
            ));
            return 0;
        }

        boolean oldWorkshopComplete = isJavaWorkshopAlreadyComplete();
        if (oldWorkshopComplete) {
            result.data.put(RESET_LOCK_KEY, 1);
        } else {
            result.data.remove(RESET_LOCK_KEY);
        }
        result.data.put(PREPARED_KEY, 1);

        final int removed = result.removed;
        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Base-world progression prepared. Removed StoredData keys: " + removed
        ), true);
        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Start state: Stage 01 active; Stages 02-05 locked."
        ), false);
        source.sendSuccess(() -> Component.literal(
            "[JosephScript] World blocks/buildings are NOT rolled back by this command. "
                + "For WASTED_BASE use a clean world/copy containing only intended starting structures."
        ), false);

        if (oldWorkshopComplete) {
            source.sendSuccess(() -> Component.literal(
                "[JosephScript] Existing Java workshop completion detected. "
                    + "Logical reset mode was enabled for testing. Final release base should be made from a fresh world."
            ), false);
        }
        return 1;
    }

    private static ResetResult clearQuestStoredData(ServerPlayer player) {
        try {
            NpcAPI api = NpcAPI.Instance();
            if (api == null) {
                return new ResetResult(null, 0);
            }

            IWorld world = api.getIWorld(player.serverLevel());
            if (world == null) {
                return new ResetResult(null, 0);
            }

            IData data = world.getStoreddata();
            if (data == null) {
                return new ResetResult(null, 0);
            }

            int removed = 0;
            String[] keys = data.getKeys();
            if (keys != null) {
                for (String key : Arrays.copyOf(keys, keys.length)) {
                    if (shouldResetKey(key)) {
                        data.remove(key);
                        removed++;
                    }
                }
            }
            return new ResetResult(data, removed);
        } catch (Throwable error) {
            error.printStackTrace();
            return new ResetResult(null, 0);
        }
    }

    private static boolean shouldResetKey(String key) {
        if (key == null) {
            return false;
        }
        for (String prefix : RESET_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJavaWorkshopAlreadyComplete() {
        try {
            Class<?> bridge = Class.forName(
                "com.wasted.domesurvival.forge.progression.JosephCooperBridge"
            );
            Object value = bridge.getMethod("workshopComplete").invoke(null);
            return Boolean.TRUE.equals(value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ResetResult {
        private final IData data;
        private final int removed;

        private ResetResult(IData data, int removed) {
            this.data = data;
            this.removed = removed;
        }
    }

    private static int apply(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EntityNPCInterface npc = findJoseph(player);

        if (npc == null) {
            source.sendFailure(Component.literal(
                "Joseph Cooper was not found within 48 blocks."
            ));
            return 0;
        }

        if (ScriptController.Instance == null) {
            source.sendFailure(Component.literal(
                "CustomNPCs ScriptController is not ready."
            ));
            return 0;
        }

        if (!ScriptController.Instance.scripts.containsKey(SCRIPT_FILE)) {
            source.sendFailure(Component.literal(
                SCRIPT_FILE + " is not loaded by CustomNPCs. "
                    + "Keep it in customnpcs/scripts/ecmascript and restart the world/client."
            ));
            return 0;
        }

        DataScript data = npc.script;

        String language = data.getLanguage();
        if (language == null || language.isBlank()) {
            language = ScriptController.Instance.languages.keySet().stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).contains("ecma")
                    || name.toLowerCase(Locale.ROOT).contains("javascript"))
                .findFirst()
                .orElse("ECMAScript");
            data.setLanguage(language);
        }

        data.setEnabled(true);

        List<ScriptContainer> containers = data.getScripts();
        containers.clear();

        ScriptContainer container = new ScriptContainer(data);
        container.script = "";
        container.fullscript = "";
        container.scripts.clear();
        container.scripts.add(SCRIPT_FILE);

        containers.add(container);

        npc.updateClient();

        final String finalLanguage = language;

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Old inline scripts removed. "
                + SCRIPT_FILE + " linked. language=" + finalLanguage
        ), true);

        source.sendSuccess(() -> Component.literal(
            "[JosephScript] Run /josephscript inspect, then right-click Joseph."
        ), false);

        return 1;
    }

    private static EntityNPCInterface findJoseph(ServerPlayer player) {
        AABB search = player.getBoundingBox().inflate(48.0D);

        List<EntityNPCInterface> npcs = player.serverLevel()
            .getEntitiesOfClass(EntityNPCInterface.class, search);

        return npcs.stream()
            .filter(npc -> {
                String name = npc.getName().getString().toLowerCase(Locale.ROOT);
                return name.contains("джозеф")
                    || name.contains("joseph")
                    || name.contains("куппер")
                    || name.contains("cooper");
            })
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    }
}
