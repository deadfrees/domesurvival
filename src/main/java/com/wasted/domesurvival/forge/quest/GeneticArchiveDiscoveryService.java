package com.wasted.domesurvival.forge.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative discovery event for the hidden "Последние из живых" branch.
 *
 * Performance:
 * - one global check per second;
 * - one short inventory scan per online player per second only after the branch opens;
 * - no world/chunk scan and no forced chunk generation.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneticArchiveDiscoveryService {
    public static final String SIGNAL_FLAG = "GENETIC_ARCHIVE_SIGNAL_FOUND";

    private static final String OXYGEN_READY_FLAG = "OXYGEN_INFRASTRUCTURE_READY";
    private static final long DISCOVERY_DELAY_TICKS = 12_000L; // 10 minutes at 20 TPS.
    private static final double SITE_REACH_RADIUS = 10.0D;
    private static final double SITE_REACH_RADIUS_SQR = SITE_REACH_RADIUS * SITE_REACH_RADIUS;

    private static final String BRIDGE_QUEST_ID = "70CE4EBCBA38CD21";
    private static final String COMPASS_QUEST_ID = "4AA418B9DF3B79A4";
    private static final String COMPASS_BOUND_QUEST_ID = "515C1A05E15F3F67";
    private static final String SITE_REACHED_QUEST_ID = "0F7B71D5BDCBD296";

    private static final String ARCHIVE_COMPASS_TAG = "DomeSurvivalGeneticArchiveCompass";

    private static final Map<UUID, Integer> LOGIN_SYNC_DELAY = new HashMap<>();

    private GeneticArchiveDiscoveryService() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // FTB Teams / FTB Quests need a little time to finish login synchronization.
            LOGIN_SYNC_DELAY.put(player.getUUID(), 60);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOGIN_SYNC_DELAY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        GeneticArchiveDiscoverySavedData data = GeneticArchiveDiscoverySavedData.get(overworld);

        boolean oxygenReady = QuestProgressService.has(overworld, OXYGEN_READY_FLAG);
        boolean signalFound = QuestProgressService.has(overworld, SIGNAL_FLAG);

        if (!signalFound) {
            if (!oxygenReady) {
                data.clearSchedule();
            } else {
                data.ensureTarget(overworld);

                if (!data.isScheduled()) {
                    data.scheduleAt(overworld.getGameTime() + DISCOVERY_DELAY_TICKS);
                }

                if (overworld.getGameTime() >= data.triggerAtGameTime()
                        && !server.getPlayerList().getPlayers().isEmpty()) {
                    fireDiscovery(server, overworld, data);
                    signalFound = QuestProgressService.has(overworld, SIGNAL_FLAG);
                }
            }
        } else {
            data.ensureTarget(overworld);
            data.clearSchedule();
        }

        if (!signalFound) {
            tickLoginDelays(false);
            return;
        }

        tickLoginDelays(true);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player, overworld, data);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("domearchive")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
                        .then(Commands.literal("trigger")
                                .executes(ctx -> triggerNow(ctx.getSource())))
        );
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        GeneticArchiveDiscoverySavedData data = GeneticArchiveDiscoverySavedData.get(level);

        boolean oxygenReady = QuestProgressService.has(level, OXYGEN_READY_FLAG);
        boolean signalFound = QuestProgressService.has(level, SIGNAL_FLAG);

        String timer;
        if (signalFound) {
            timer = "event=FIRED";
        } else if (data.isScheduled()) {
            long remaining = Math.max(0L, data.triggerAtGameTime() - level.getGameTime());
            timer = "remaining=" + remaining + " ticks (" + (remaining / 20L) + " sec)";
        } else {
            timer = "event=NOT_SCHEDULED";
        }

        BlockPos target = data.target(level);
        source.sendSuccess(
                () -> Component.literal(
                        "Genetic archive: oxygenReady=" + oxygenReady
                                + ", signalFound=" + signalFound
                                + ", " + timer
                                + ", debugTarget=" + target.getX() + " " + target.getY() + " " + target.getZ()
                ).withStyle(ChatFormatting.GRAY),
                false
        );
        return 1;
    }

    private static int triggerNow(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        if (QuestProgressService.has(level, SIGNAL_FLAG)) {
            source.sendSuccess(
                    () -> Component.literal("Genetic archive signal is already discovered.")
                            .withStyle(ChatFormatting.YELLOW),
                    false
            );
            return 1;
        }

        GeneticArchiveDiscoverySavedData data = GeneticArchiveDiscoverySavedData.get(level);
        data.ensureTarget(level);
        data.scheduleAt(level.getGameTime());

        if (source.getServer().getPlayerList().getPlayers().isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Discovery is due now and will fire when an online player is present."
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
            return 1;
        }

        fireDiscovery(source.getServer(), level, data);
        return 1;
    }

    private static void fireDiscovery(
            MinecraftServer server,
            ServerLevel overworld,
            GeneticArchiveDiscoverySavedData data
    ) {
        QuestProgressService.MutationResult result =
                QuestProgressService.set(overworld, SIGNAL_FLAG, "genetic_archive:10_minute_timer");

        if (result == QuestProgressService.MutationResult.UNKNOWN_FLAG) {
            return;
        }

        data.clearSchedule();

        // If this was already fired by another path, do not replay the global story moment.
        if (result != QuestProgressService.MutationResult.CHANGED) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            forceBridgeQuest(player);
            playDiscoverySound(player);
        }

        Component message = Component.empty()
                .append(Component.literal("[Джозеф Куппер] ")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                .append(Component.literal(
                        "Я нашёл кое-что интересное... В старых архивах сохранилась привязка "
                                + "к навигационному маяку. Точные координаты повреждены. "
                                + "Обычный компас должен помочь."
                ).withStyle(ChatFormatting.LIGHT_PURPLE));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void tickLoginDelays(boolean signalFound) {
        if (LOGIN_SYNC_DELAY.isEmpty()) {
            return;
        }

        for (var iterator = LOGIN_SYNC_DELAY.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int next = entry.getValue() - 20;

            if (next > 0) {
                entry.setValue(next);
                continue;
            }

            iterator.remove();

            if (!signalFound) {
                continue;
            }

            MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                forceBridgeQuest(player);
            }
        }
    }

    private static void tickPlayer(
            ServerPlayer player,
            ServerLevel overworld,
            GeneticArchiveDiscoverySavedData data
    ) {
        if (QuestGlobalSyncService.isGlobalCompleted(player, COMPASS_QUEST_ID)) {
            ensureArchiveCompass(player, data.target(overworld));
        }

        if (!QuestGlobalSyncService.isGlobalCompleted(player, COMPASS_BOUND_QUEST_ID)) {
            return;
        }

        if (QuestGlobalSyncService.isGlobalCompleted(player, SITE_REACHED_QUEST_ID)
                || !Level.OVERWORLD.equals(player.level().dimension())) {
            return;
        }

        BlockPos target = data.target(overworld);
        double dx = player.getX() - (target.getX() + 0.5D);
        double dy = player.getY() - (target.getY() + 0.5D);
        double dz = player.getZ() - (target.getZ() + 0.5D);

        if (dx * dx + dy * dy + dz * dz <= SITE_REACH_RADIUS_SQR) {
            grantAdvancement(player, "genetic_archive_site_reached");
        }
    }
    private static void ensureArchiveCompass(ServerPlayer player, BlockPos target) {
        // Migrate a V1 DomeSurvival archive compass to the selected building.
        // A player's unrelated Lodestone compass is never touched.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            CompoundTag tag = stack.getTag();

            if (stack.is(Items.COMPASS)
                    && tag != null
                    && tag.getBoolean(ARCHIVE_COMPASS_TAG)) {
                if (!archiveCompassPointsTo(stack, target)) {
                    bindArchiveCompass(stack, target);
                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();
                }

                if (!QuestGlobalSyncService.isGlobalCompleted(player, COMPASS_BOUND_QUEST_ID)) {
                    grantAdvancement(player, "genetic_archive_compass_bound");
                }
                return;
            }
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.COMPASS)) {
                continue;
            }

            CompoundTag existing = stack.getTag();

            // Never overwrite a player's own Lodestone compass.
            if (existing != null
                    && existing.contains("LodestonePos", Tag.TAG_COMPOUND)) {
                continue;
            }

            bindArchiveCompass(stack, target);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();

            grantAdvancement(player, "genetic_archive_compass_bound");
            return;
        }
    }

    private static boolean archiveCompassPointsTo(ItemStack stack, BlockPos target) {
        CompoundTag tag = stack.getTag();
        if (tag == null
                || !tag.getBoolean(ARCHIVE_COMPASS_TAG)
                || !tag.contains("LodestonePos", Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag pos = tag.getCompound("LodestonePos");
        return pos.getInt("X") == target.getX()
                && pos.getInt("Y") == target.getY()
                && pos.getInt("Z") == target.getZ()
                && Level.OVERWORLD.location().toString().equals(tag.getString("LodestoneDimension"))
                && !tag.getBoolean("LodestoneTracked");
    }

    private static void bindArchiveCompass(ItemStack stack, BlockPos target) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("LodestonePos", NbtUtils.writeBlockPos(target));
        tag.putString("LodestoneDimension", Level.OVERWORLD.location().toString());
        tag.putBoolean("LodestoneTracked", false);
        tag.putBoolean(ARCHIVE_COMPASS_TAG, true);
    }
    private static void forceBridgeQuest(ServerPlayer player) {
        if (!ModList.get().isLoaded("ftbquests")) {
            return;
        }

        player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack().withSuppressedOutput(),
                "ftbquests change_progress "
                        + player.getScoreboardName()
                        + " complete "
                        + BRIDGE_QUEST_ID
        );
    }

    private static void grantAdvancement(ServerPlayer player, String path) {
        player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack().withSuppressedOutput(),
                "advancement grant "
                        + player.getScoreboardName()
                        + " only domesurvival:quest_actions/"
                        + path
        );
    }

    private static void playDiscoverySound(ServerPlayer player) {
        String command = String.format(
                Locale.ROOT,
                "playsound domesurvival:genetic_archive_signal master %s %.3f %.3f %.3f 0.85 1.0",
                player.getScoreboardName(),
                player.getX(),
                player.getY(),
                player.getZ()
        );

        player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack().withSuppressedOutput(),
                command
        );
    }
}
