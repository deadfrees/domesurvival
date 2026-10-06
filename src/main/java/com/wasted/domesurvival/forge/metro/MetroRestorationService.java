package com.wasted.domesurvival.forge.metro;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.metro.network.MetroNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative Stage-3 restoration and persistent partial deposits. */
public final class MetroRestorationService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_USE_DISTANCE_SQR = 8.0D * 8.0D;

    private MetroRestorationService() {
    }

    /**
     * Transfers every currently useful material from the player's inventory into the
     * station's persistent contribution progress, up to each requirement's remaining cap.
     */
    public static Result tryDeposit(ServerPlayer player, MetroRestorationConsoleBlockEntity console) {
        if (!(player.level() instanceof ServerLevel level) || console.getLevel() != level) return Result.INVALID_STATION;
        if (!isNear(player, console)) return Result.TOO_FAR;

        console.ensureRegistered(level);
        UUID stationId = console.getStationId();
        if (stationId == null) return Result.INVALID_STATION;

        MetroStationSavedData data = MetroStationSavedData.get(level.getServer());
        synchronized (data) {
            MetroStationRecord record = data.get(stationId);
            if (!matchesConsole(level, console, record)) return Result.INVALID_STATION;
            if (record.state() == StationState.RESTORED) return Result.ALREADY_RESTORED;
            if (record.state() == StationState.RESTORING) return Result.BUSY;

            Inventory inventory = player.getInventory();
            MetroRestorationRequirements.DepositPlan plan = MetroRestorationRequirements.planDeposit(inventory, record);
            if (plan == null) return MetroRestorationRequirements.isComplete(record)
                    ? Result.DEPOSITS_COMPLETE
                    : Result.NOTHING_TO_DEPOSIT;
            if (!plan.stillValid(inventory)) return Result.NOTHING_TO_DEPOSIT;

            List<ItemStack> inventorySnapshot = snapshot(inventory);
            Map<String, Integer> depositSnapshot = record.restorationDeposits();
            try {
                plan.consume(inventory);
                for (Map.Entry<String, Integer> contribution : plan.contributions().entrySet()) {
                    MetroRestorationRequirements.Requirement requirement = MetroRestorationRequirements.byId(contribution.getKey());
                    if (requirement == null) {
                        throw new IllegalStateException("Unknown metro restoration requirement: " + contribution.getKey());
                    }
                    int accepted = data.addRestorationDeposit(
                            stationId,
                            requirement.id(),
                            contribution.getValue(),
                            requirement.required()
                    );
                    if (accepted != contribution.getValue()) {
                        throw new IllegalStateException("Deposit cap changed during transaction for " + requirement.id());
                    }
                }
            }
            catch (RuntimeException exception) {
                restoreSnapshot(inventory, inventorySnapshot);
                data.replaceRestorationDeposits(stationId, depositSnapshot);
                console.refreshFromSavedData(level);
                LOGGER.error("Metro material deposit rolled back for station {}", stationId, exception);
                return Result.ERROR;
            }

            console.refreshFromSavedData(level);
            player.containerMenu.broadcastChanges();
            MetroStationRecord updated = data.get(stationId);
            boolean complete = updated != null && MetroRestorationRequirements.isComplete(updated);
            LOGGER.info("Metro materials deposited: station={} player={} complete={} contributions={}",
                    stationId, player.getGameProfile().getName(), complete, plan.contributions());
            return complete ? Result.DEPOSITS_COMPLETE : Result.DEPOSITED;
        }
    }

    /** Final activation consumes no inventory: all materials must already be deposited. */
    public static Result tryRestore(ServerPlayer player, MetroRestorationConsoleBlockEntity console) {
        if (!(player.level() instanceof ServerLevel level) || console.getLevel() != level) return Result.INVALID_STATION;
        if (!isNear(player, console)) return Result.TOO_FAR;

        console.ensureRegistered(level);
        UUID stationId = console.getStationId();
        if (stationId == null) return Result.INVALID_STATION;

        MetroStationSavedData data = MetroStationSavedData.get(level.getServer());
        synchronized (data) {
            MetroStationRecord record = data.get(stationId);
            if (!matchesConsole(level, console, record)) return Result.INVALID_STATION;
            if (record.state() == StationState.RESTORED) {
                applyRestoredVisuals(level, record.anchor());
                MetroStationInfrastructure.ensureRestoredEntrance(level, record.anchor());
                RestoredMetroAtmosphere.rebuild(level.getServer());
                console.refreshFromSavedData(level);
                return Result.ALREADY_RESTORED;
            }
            if (record.state() == StationState.RESTORING) return Result.BUSY;
            if (!MetroRestorationRequirements.isComplete(record)) return Result.MISSING_RESOURCES;

            if (!data.setState(stationId, StationState.ABANDONED, StationState.RESTORING)) return Result.BUSY;
            try {
                if (!data.markRestored(stationId, player.getUUID(), level.getGameTime())) {
                    throw new IllegalStateException("Could not commit RESTORED state for " + stationId);
                }
            }
            catch (RuntimeException exception) {
                data.setState(stationId, StationState.RESTORING, StationState.ABANDONED);
                console.refreshFromSavedData(level);
                LOGGER.error("Metro restoration activation failed for station {}", stationId, exception);
                return Result.ERROR;
            }

            console.refreshFromSavedData(level);
            player.containerMenu.broadcastChanges();
            try {
                applyRestoredVisuals(level, record.anchor());
                MetroStationInfrastructure.ensureRestoredEntrance(level, record.anchor());
            }
            catch (RuntimeException exception) {
                LOGGER.error("Metro station {} restored, but an infrastructure update failed", stationId, exception);
            }

            RestoredMetroAtmosphere.rebuild(level.getServer());
            MetroNetworkService.onStationRestored(level.getServer(), record);
            LOGGER.info("Metro station restored: {} {} by {}",
                    record.stationName(), stationId, player.getGameProfile().getName());
            return Result.SUCCESS;
        }
    }

    /** A persisted RESTORING state is completed after restart without charging again. */
    public static void recoverInterrupted(ServerLevel level, MetroRestorationConsoleBlockEntity console) {
        UUID stationId = console.getStationId();
        if (stationId == null) return;

        MetroStationSavedData data = MetroStationSavedData.get(level.getServer());
        synchronized (data) {
            MetroStationRecord record = data.get(stationId);
            if (!matchesConsole(level, console, record) || record.state() != StationState.RESTORING) return;

            if (data.markRestored(stationId, null, level.getGameTime())) {
                LOGGER.warn("Recovered interrupted metro restoration as RESTORED: {} {}",
                        record.stationName(), stationId);
            }
            console.refreshFromSavedData(level);
            applyRestoredVisuals(level, record.anchor());
            MetroStationInfrastructure.ensureRestoredEntrance(level, record.anchor());
            RestoredMetroAtmosphere.rebuild(level.getServer());
            MetroNetworkService.onStationRestored(level.getServer(), record);
        }
    }

    public static void ensureRestoredVisuals(ServerLevel level, MetroRestorationConsoleBlockEntity console) {
        UUID stationId = console.getStationId();
        if (stationId == null) return;
        MetroStationRecord record = MetroStationSavedData.get(level.getServer()).get(stationId);
        if (matchesConsole(level, console, record) && record.state() == StationState.RESTORED) {
            applyRestoredVisuals(level, record.anchor());
            MetroStationInfrastructure.ensureRestoredEntrance(level, record.anchor());
        }
    }

    public static boolean forceRestore(ServerLevel level, UUID stationId, @Nullable UUID actorId) {
        MetroStationSavedData data = MetroStationSavedData.get(level.getServer());
        synchronized (data) {
            MetroStationRecord record = data.get(stationId);
            if (record == null || !record.dimension().equals(level.dimension().location().toString())) return false;

            if (record.state() == StationState.ABANDONED) {
                // Dev restore fills progress so the GUI stays internally consistent.
                for (MetroRestorationRequirements.Requirement requirement : MetroRestorationRequirements.all()) {
                    int missing = Math.max(0, requirement.required() - record.restorationDeposit(requirement.id()));
                    if (missing > 0) data.addRestorationDeposit(stationId, requirement.id(), missing, requirement.required());
                }
                if (!data.setState(stationId, StationState.ABANDONED, StationState.RESTORING)) return false;
            }
            if (record.state() != StationState.RESTORED) {
                if (!data.markRestored(stationId, actorId, level.getGameTime())) return false;
            }
            applyRestoredVisuals(level, record.anchor());
            MetroStationInfrastructure.ensureRestoredEntrance(level, record.anchor());
            RestoredMetroAtmosphere.rebuild(level.getServer());
            MetroNetworkService.onStationRestored(level.getServer(), record);
            LOGGER.info("Metro station force-restored by dev command: {} {}", record.stationName(), stationId);
            return true;
        }
    }

    public static boolean reset(ServerLevel level, UUID stationId) {
        MetroStationSavedData data = MetroStationSavedData.get(level.getServer());
        synchronized (data) {
            MetroStationRecord record = data.get(stationId);
            if (record == null || !record.dimension().equals(level.dimension().location().toString())) return false;
            if (!data.resetRestoration(stationId)) return false;
            resetVisuals(level, record.anchor());
            MetroStationInfrastructure.removeRestoredEntrance(level, record.anchor());
            RestoredMetroAtmosphere.rebuild(level.getServer());
            MetroNetworkService.onStationReset(level.getServer(), stationId);
            LOGGER.info("Metro station restoration + deposit progress reset by dev command: {} {}", record.stationName(), stationId);
            return true;
        }
    }

    public static void applyRestoredVisuals(ServerLevel level, BlockPos anchor) {
        for (BlockPos offset : MetroStationLayout.BROKEN_LIGHTS) {
            BlockPos fixture = MetroStationLayout.at(anchor, offset);
            if (!level.hasChunkAt(fixture)) continue;

            level.setBlockAndUpdate(fixture, Blocks.POLISHED_ANDESITE.defaultBlockState());
            BlockPos light = fixture.below();
            if (level.hasChunkAt(light)) {
                if (level.getBlockState(light).isAir() || level.getBlockState(light).is(Blocks.LIGHT)) {
                    level.setBlockAndUpdate(light, Blocks.LIGHT.defaultBlockState());
                }
            }
        }

        for (BlockPos offset : MetroStationLayout.DAMAGED_DISPLAYS) {
            BlockPos display = MetroStationLayout.at(anchor, offset);
            if (level.hasChunkAt(display)) {
                level.setBlockAndUpdate(display, Blocks.CYAN_STAINED_GLASS.defaultBlockState());
            }
        }
    }

    private static void resetVisuals(ServerLevel level, BlockPos anchor) {
        for (BlockPos offset : MetroStationLayout.BROKEN_LIGHTS) {
            BlockPos fixture = MetroStationLayout.at(anchor, offset);
            if (!level.hasChunkAt(fixture)) continue;
            level.setBlockAndUpdate(fixture, Blocks.GRAY_CONCRETE.defaultBlockState());

            BlockPos light = fixture.below();
            if (level.hasChunkAt(light) && level.getBlockState(light).is(Blocks.LIGHT)) {
                level.setBlockAndUpdate(light, Blocks.AIR.defaultBlockState());
            }
        }

        for (BlockPos offset : MetroStationLayout.DAMAGED_DISPLAYS) {
            BlockPos display = MetroStationLayout.at(anchor, offset);
            if (level.hasChunkAt(display)) {
                level.setBlockAndUpdate(display, Blocks.BLACK_CONCRETE.defaultBlockState());
            }
        }
    }

    private static boolean isNear(ServerPlayer player, MetroRestorationConsoleBlockEntity console) {
        return player.distanceToSqr(
                console.getBlockPos().getX() + 0.5D,
                console.getBlockPos().getY() + 0.5D,
                console.getBlockPos().getZ() + 0.5D
        ) <= MAX_USE_DISTANCE_SQR;
    }

    private static boolean matchesConsole(ServerLevel level,
                                          MetroRestorationConsoleBlockEntity console,
                                          @Nullable MetroStationRecord record) {
        return record != null
                && record.dimension().equals(level.dimension().location().toString())
                && record.anchor().equals(console.getBlockPos());
    }

    private static List<ItemStack> snapshot(Inventory inventory) {
        ArrayList<ItemStack> result = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) result.add(inventory.getItem(slot).copy());
        return result;
    }

    private static void restoreSnapshot(Inventory inventory, List<ItemStack> snapshot) {
        int size = Math.min(inventory.getContainerSize(), snapshot.size());
        for (int slot = 0; slot < size; slot++) inventory.setItem(slot, snapshot.get(slot).copy());
        inventory.setChanged();
    }

    public enum Result {
        SUCCESS("gui.domesurvival.metro.restoration.result.success"),
        DEPOSITED("gui.domesurvival.metro.restoration.result.deposited"),
        DEPOSITS_COMPLETE("gui.domesurvival.metro.restoration.result.deposits_complete"),
        NOTHING_TO_DEPOSIT("gui.domesurvival.metro.restoration.result.nothing_to_deposit"),
        ALREADY_RESTORED("gui.domesurvival.metro.restoration.result.already_restored"),
        BUSY("gui.domesurvival.metro.restoration.result.busy"),
        MISSING_RESOURCES("gui.domesurvival.metro.restoration.result.missing_resources"),
        TOO_FAR("gui.domesurvival.metro.restoration.result.too_far"),
        INVALID_STATION("gui.domesurvival.metro.restoration.result.invalid"),
        ERROR("gui.domesurvival.metro.restoration.result.error");

        private final String translationKey;

        Result(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
