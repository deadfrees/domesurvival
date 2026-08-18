package com.wasted.domesurvival.forge.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

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


    /**
     * Stage 01 environmental upgrade.
     *
     * Converts ONLY the connected exposed dirt/coarse-dirt strip nearest Joseph
     * into vanilla dirt path blocks. The search is deliberately bounded so it
     * cannot spread through the whole dome terrain.
     *
     * @return changed block count; 0 when a finished path is already found;
     *         negative values mean the path could not be resolved safely.
     */

    /**
     * Stage 01 environmental upgrade.
     *
     * Expands the already-visible trail near Joseph into the COMPLETE connected
     * dirt/coarse-dirt/rooted-dirt road. Existing DIRT_PATH blocks are used as
     * connectors so a previous partial conversion can be safely completed.
     *
     * The traversal is bounded and never changes sand, stone, machines,
     * containers, grass, structures or arbitrary terrain.
     */
    public static int upgradeStage1Path(String playerName, double npcX, double npcY, double npcZ) {
        ServerPlayer player = findPlayer(playerName);
        if (player == null) {
            return -2;
        }

        ServerLevel level = player.serverLevel();
        BlockPos npc = BlockPos.containing(npcX, npcY, npcZ);

        BlockPos start = null;
        double bestDistance = Double.MAX_VALUE;

        // Prefer an already existing dirt path. That lets V7.3.3 continue from
        // the partially converted V7.3.2 road instead of searching only for
        // unconverted dirt beside Joseph.
        for (int dy = -2; dy <= 0; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos pos = npc.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (!state.is(Blocks.DIRT_PATH) && !isStage1TrailSource(state)) {
                        continue;
                    }
                    if (!isExposedTrailSurface(level, pos)) {
                        continue;
                    }

                    double distance = dx * dx + dz * dz + dy * dy * 2.0;

                    // Dirt path gets priority over source dirt at similar range.
                    if (state.is(Blocks.DIRT_PATH)) {
                        distance -= 500.0;
                    }

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        start = pos.immutable();
                    }
                }
            }
        }

        if (start == null) {
            return -1;
        }

        final int maxRadius = 64;
        final int maxBlocks = 4096;
        final int minY = start.getY() - 1;
        final int maxY = start.getY() + 1;

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(start);
        visited.add(start.asLong());

        int changed = 0;

        while (!queue.isEmpty() && visited.size() <= maxBlocks) {
            BlockPos pos = queue.removeFirst();

            int dxFromStart = pos.getX() - start.getX();
            int dzFromStart = pos.getZ() - start.getZ();
            if (dxFromStart * dxFromStart + dzFromStart * dzFromStart > maxRadius * maxRadius) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            boolean isExistingPath = state.is(Blocks.DIRT_PATH);
            boolean isSource = isStage1TrailSource(state);

            if ((!isExistingPath && !isSource) || !isExposedTrailSurface(level, pos)) {
                continue;
            }

            if (isSource) {
                level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 3);
                changed++;
            }

            // Traverse both existing path and not-yet-converted dirt. This is
            // the key difference from V7.3.2 and completes the whole road.
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos next = new BlockPos(
                        pos.getX() + dir[0],
                        y,
                        pos.getZ() + dir[1]
                    );

                    if (!visited.add(next.asLong())) {
                        continue;
                    }

                    int ndx = next.getX() - start.getX();
                    int ndz = next.getZ() - start.getZ();
                    if (ndx * ndx + ndz * ndz > maxRadius * maxRadius) {
                        continue;
                    }

                    BlockState nextState = level.getBlockState(next);
                    if ((nextState.is(Blocks.DIRT_PATH) || isStage1TrailSource(nextState))
                            && isExposedTrailSurface(level, next)) {
                        queue.addLast(next);
                    }
                }
            }
        }

        return changed;
    }

    private static boolean isStage1TrailSource(BlockState state) {
        return state.is(Blocks.DIRT)
            || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.ROOTED_DIRT);
    }

    private static boolean isExposedTrailSurface(ServerLevel level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.isAir() || above.canBeReplaced();
    }


    public static String progressText() {
        DomeProgressSavedData data = data();
        if (data == null) {
            return "Данные проекта временно недоступны.";
        }

        return "Железный слиток: " + data.workshopIron() + " / " + WorkshopProject.IRON_REQUIRED
                + "\nМедный слиток: " + data.workshopCopper() + " / " + WorkshopProject.COPPER_REQUIRED
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
