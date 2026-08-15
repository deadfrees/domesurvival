package com.wasted.domesurvival.forge.progression;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Stable server-side bridge for CustomNPCs scripts.
 *
 * IMPORTANT:
 * This class has NO compile-time dependency on CustomNPCs.
 * CustomNPCs JavaScript calls these public static methods through Java.type(...).
 */
public final class JosephCooperBridge {
    private JosephCooperBridge() {
    }

    public static String projectTitle() {
        return "Восстановление мастерской";
    }

    public static int ironCurrent() {
        DomeProgressSavedData data = data();
        return data == null ? 0 : data.workshopIron();
    }

    public static int ironRequired() {
        return WorkshopProject.IRON_REQUIRED;
    }

    public static int copperCurrent() {
        DomeProgressSavedData data = data();
        return data == null ? 0 : data.workshopCopper();
    }

    public static int copperRequired() {
        return WorkshopProject.COPPER_REQUIRED;
    }

    public static int redstoneCurrent() {
        DomeProgressSavedData data = data();
        return data == null ? 0 : data.workshopRedstone();
    }

    public static int redstoneRequired() {
        return WorkshopProject.REDSTONE_REQUIRED;
    }

    public static boolean workshopComplete() {
        DomeProgressSavedData data = data();
        return data != null && data.workshopComplete();
    }

    public static boolean workshopBuilt() {
        DomeProgressSavedData data = data();
        return data != null && data.workshopUpgradeApplied();
    }

    /**
     * Story text shown on Joseph's main screen.
     */
    public static String narrative() {
        DomeProgressSavedData data = data();

        if (data == null) {
            return "Системы базы пока недоступны. Попробуйте обратиться ко мне через несколько секунд.";
        }

        if (data.workshopComplete() && data.workshopUpgradeApplied()) {
            return "Мастерская снова работает. Теперь у нас есть место для ремонта и восстановления оборудования. "
                    + "Следующим этапом займёмся энергетической инфраструктурой купола.";
        }

        if (data.workshopComplete()) {
            return "Материалы собраны. Осталось завершить восстановление мастерской и проверить оборудование.";
        }

        return "Купол держится, но часть инфраструктуры всё ещё разрушена. "
                + "Начнём с мастерской — без неё мы не сможем обслуживать оборудование и готовить будущие вылазки.";
    }

    /**
     * Takes all currently useful workshop materials from this player's inventory.
     * Returns a short text intended to be displayed INSIDE the CustomNPCs GUI.
     */
    public static String contributeWorkshop(String playerName) {
        ServerPlayer player = findPlayer(playerName);
        if (player == null) {
            return "Игрок не найден на сервере.";
        }

        DomeProjectService.ContributionResult result =
                DomeProjectService.contributeWorkshop(player);

        if (result.alreadyComplete()) {
            return "Проект уже завершён.";
        }

        if (result.total() <= 0) {
            return "В инвентаре нет материалов, которые сейчас нужны проекту.";
        }

        StringBuilder text = new StringBuilder("Передано: ");
        boolean hasPrevious = false;

        if (result.iron() > 0) {
            text.append("железо ").append(result.iron());
            hasPrevious = true;
        }
        if (result.copper() > 0) {
            if (hasPrevious) {
                text.append(", ");
            }
            text.append("медь ").append(result.copper());
            hasPrevious = true;
        }
        if (result.redstone() > 0) {
            if (hasPrevious) {
                text.append(", ");
            }
            text.append("редстоун ").append(result.redstone());
        }

        if (result.completedNow()) {
            text.append("\nОсновные материалы собраны. Для строительства мастерской нужна полная комплектация этапа 02.");
        }

        return text.toString();
    }

    /**
     * Called by the Joseph CustomNPCs script only after ALL Stage 02
     * requirements are complete (Java core + script-side building supplies).
     */
    public static String finalizeWorkshop(String playerName) {
        ServerPlayer player = findPlayer(playerName);
        if (player == null) {
            return "Игрок не найден на сервере.";
        }

        DomeProgressSavedData progress = DomeProgressSavedData.get(player.serverLevel());
        if (!progress.workshopComplete()) {
            return "Основные материалы мастерской ещё не собраны.";
        }

        WorkshopUpgradeApplier.ApplyResult upgrade =
                WorkshopUpgradeApplier.applyIfNeeded(player.serverLevel());

        if (upgrade == WorkshopUpgradeApplier.ApplyResult.APPLIED) {
            return "Все материалы собраны. Мастерская восстановлена.";
        }
        if (upgrade == WorkshopUpgradeApplier.ApplyResult.ALREADY_APPLIED) {
            return "Мастерская уже восстановлена.";
        }
        if (upgrade == WorkshopUpgradeApplier.ApplyResult.TEMPLATE_MISSING) {
            return "Шаблон мастерской не найден — сообщите администратору.";
        }
        if (upgrade == WorkshopUpgradeApplier.ApplyResult.PLACEMENT_FAILED) {
            return "Не удалось разместить мастерскую — сообщите администратору.";
        }
        if (upgrade == WorkshopUpgradeApplier.ApplyResult.STORAGE_NOT_EMPTY) {
            return "Строительство остановлено: внутри зоны мастерской есть предметы в контейнерах.";
        }

        return "Мастерская пока не готова к строительству.";
    }

    public static String progressText() {
        DomeProgressSavedData data = data();
        if (data == null) {
            return "Данные проекта временно недоступны.";
        }

        return "Железо: " + data.workshopIron() + " / " + WorkshopProject.IRON_REQUIRED
                + "\nМедь: " + data.workshopCopper() + " / " + WorkshopProject.COPPER_REQUIRED
                + "\nРедстоун: " + data.workshopRedstone() + " / " + WorkshopProject.REDSTONE_REQUIRED;
    }

    private static DomeProgressSavedData data() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        return DomeProgressSavedData.get(server.overworld());
    }

    private static ServerPlayer findPlayer(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        return server.getPlayerList().getPlayerByName(playerName);
    }
}
