package com.wasted.domesurvival.forge.metro.travel;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.metro.MetroStationRecord;
import com.wasted.domesurvival.forge.metro.MetroStationSavedData;
import com.wasted.domesurvival.forge.metro.StationState;
import com.wasted.domesurvival.forge.metro.dome.DomeMetroSavedData;
import com.wasted.domesurvival.forge.metro.network.MetroNetworkMenu;
import com.wasted.domesurvival.forge.metro.network.MetroNetworkSavedData;
import com.wasted.domesurvival.forge.metro.network.MetroNetworkService;
import com.wasted.domesurvival.forge.metro.network.MetroNode;
import com.wasted.domesurvival.forge.metro.network.MetroNodeState;
import com.wasted.domesurvival.forge.metro.network.MetroNodeType;
import com.wasted.domesurvival.forge.network.MetroTravelVisualPacket;
import com.wasted.domesurvival.forge.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative passenger transport.
 *
 * The client sends only destinationNodeId. The server derives the current node
 * from the open train console and validates the entire route before creating a
 * persistent session.
 */
public final class MetroTravelService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double CONSOLE_NODE_RADIUS = 80.0D;
    private static final int MIN_TRAVEL_TICKS = 80;
    private static final int MAX_TRAVEL_TICKS = 20 * 120;

    private MetroTravelService() {
    }

    public static void requestTravel(ServerPlayer player, String destinationNodeId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (!(player.containerMenu instanceof MetroNetworkMenu menu)) {
            fail(player, "\u041e\u0442\u043a\u0440\u043e\u0439\u0442\u0435 \u043a\u0430\u0440\u0442\u0443 \u0447\u0435\u0440\u0435\u0437 \u0442\u0440\u0430\u043d\u0441\u043f\u043e\u0440\u0442\u043d\u0443\u044e \u043a\u043e\u043d\u0441\u043e\u043b\u044c.");
            return;
        }

        if (!menu.stillValid(player)) {
            fail(player, "\u0412\u044b \u0441\u043b\u0438\u0448\u043a\u043e\u043c \u0434\u0430\u043b\u0435\u043a\u043e \u043e\u0442 \u0442\u0440\u0430\u043d\u0441\u043f\u043e\u0440\u0442\u043d\u043e\u0439 \u043a\u043e\u043d\u0441\u043e\u043b\u0438.");
            return;
        }

        if (!player.isAlive() || player.isSpectator() || player.isSleeping()) {
            fail(player, "\u041f\u043e\u0435\u0437\u0434\u043a\u0430 \u0441\u0435\u0439\u0447\u0430\u0441 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u0430.");
            return;
        }

        MetroTravelSavedData travelData = MetroTravelSavedData.get(server);
        if (travelData.has(player.getUUID())) {
            fail(player, "\u0423 \u0432\u0430\u0441 \u0443\u0436\u0435 \u0435\u0441\u0442\u044c \u0430\u043a\u0442\u0438\u0432\u043d\u0430\u044f \u043f\u043e\u0435\u0437\u0434\u043a\u0430.");
            return;
        }

        MetroNetworkService.synchronize(server);
        MetroNetworkSavedData network = MetroNetworkSavedData.get(server);

        MetroNode destination = network.node(destinationNodeId);
        if (destination == null || destination.state() != MetroNodeState.ACTIVE) {
            fail(player, "\u0421\u0442\u0430\u043d\u0446\u0438\u044f \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u0430.");
            return;
        }

        MetroNode current = resolveCurrentNode(
                network,
                player.serverLevel().dimension().location().toString(),
                menu.consolePos()
        );
        if (current == null || current.state() != MetroNodeState.ACTIVE) {
            fail(player, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0438\u0442\u044c \u0442\u0435\u043a\u0443\u0449\u0443\u044e \u0441\u0442\u0430\u043d\u0446\u0438\u044e.");
            return;
        }

        if (current.nodeId().equals(destination.nodeId())) {
            fail(player, "\u0412\u044b \u0443\u0436\u0435 \u043d\u0430 \u044d\u0442\u043e\u0439 \u0441\u0442\u0430\u043d\u0446\u0438\u0438.");
            return;
        }

        if (!validateDestinationSourceOfTruth(server, destination)) {
            MetroNetworkService.synchronize(server);
            fail(player, "\u0421\u0442\u0430\u043d\u0446\u0438\u044f \u0431\u043e\u043b\u044c\u0448\u0435 \u043d\u0435 \u0433\u043e\u0442\u043e\u0432\u0430 \u043a \u043f\u0440\u0438\u0451\u043c\u0443 \u043f\u043e\u0435\u0437\u0434\u0430.");
            return;
        }

        MetroNetworkService.PathResult path = MetroNetworkService.findPath(
                server,
                current.nodeId(),
                destination.nodeId()
        );
        if (path == null || path.routes().isEmpty()) {
            fail(player, "\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u043c\u0430\u0440\u0448\u0440\u0443\u0442 \u043c\u0435\u0436\u0434\u0443 \u0441\u0442\u0430\u043d\u0446\u0438\u044f\u043c\u0438 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d.");
            return;
        }

        int durationTicks = Math.max(
                MIN_TRAVEL_TICKS,
                Math.min(MAX_TRAVEL_TICKS, path.travelTimeTicks())
        );
        long now = server.overworld().getGameTime();

        MetroTravelSavedData.TravelSession session = new MetroTravelSavedData.TravelSession(
                UUID.randomUUID(),
                player.getUUID(),
                current.nodeId(),
                destination.nodeId(),
                now,
                now + durationTicks,
                durationTicks
        );

        if (!travelData.begin(session)) {
            fail(player, "\u0417\u0430\u043f\u0440\u043e\u0441 \u043f\u043e\u0435\u0437\u0434\u043a\u0438 \u0443\u0436\u0435 \u043e\u0431\u0440\u0430\u0431\u0430\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f.");
            return;
        }

        player.stopRiding();
        player.closeContainer();
        player.playNotifySound(SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.8F, 0.75F);
        player.playNotifySound(SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.65F, 0.95F);

        ModNetwork.sendTo(
                player,
                MetroTravelVisualPacket.start(destination.displayName(), durationTicks)
        );

        player.sendSystemMessage(
                Component.literal(
                        "\u041c\u0430\u0440\u0448\u0440\u0443\u0442: " + current.displayName()
                                + " \u2192 " + destination.displayName()
                ).withStyle(ChatFormatting.AQUA)
        );

        LOGGER.info(
                "Metro travel started: session={} player={} from={} to={} durationTicks={}",
                session.sessionId(),
                player.getGameProfile().getName(),
                current.nodeId(),
                destination.nodeId(),
                durationTicks
        );
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) return;

        MetroTravelSavedData data = MetroTravelSavedData.get(server);
        long now = server.overworld().getGameTime();

        for (MetroTravelSavedData.TravelSession session : data.all()) {
            if (session.arriveAtGameTime() > now) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId());
            if (player == null) {
                // Keep the session. It will finish safely when the player reconnects.
                continue;
            }

            complete(server, player, session);
        }
    }

    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        MetroTravelSavedData.TravelSession session =
                MetroTravelSavedData.get(server).get(player.getUUID());
        if (session == null) return;

        long now = server.overworld().getGameTime();
        if (session.arriveAtGameTime() <= now) {
            complete(server, player, session);
            return;
        }

        MetroNode destination = MetroNetworkSavedData.get(server).node(session.toNodeId());
        String name = destination == null
                ? "\u0421\u0442\u0430\u043d\u0446\u0438\u044f \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f"
                : destination.displayName();

        ModNetwork.sendTo(
                player,
                MetroTravelVisualPacket.start(name, session.remainingTicks(now))
        );
        player.sendSystemMessage(
                Component.literal(
                        "\u041f\u043e\u0435\u0437\u0434\u043a\u0430 \u0432 \u043c\u0435\u0442\u0440\u043e \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0435\u043d\u0430 \u043f\u043e\u0441\u043b\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f."
                ).withStyle(ChatFormatting.GRAY)
        );
    }

    public static void cancel(ServerPlayer player, String reason) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        MetroTravelSavedData.TravelSession removed =
                MetroTravelSavedData.get(server).remove(player.getUUID());
        if (removed == null) return;

        ModNetwork.sendTo(player, MetroTravelVisualPacket.cancel(reason));
        LOGGER.info(
                "Metro travel cancelled: session={} player={} reason={}",
                removed.sessionId(),
                player.getGameProfile().getName(),
                reason
        );
    }

    private static void complete(MinecraftServer server,
                                 ServerPlayer player,
                                 MetroTravelSavedData.TravelSession session) {
        MetroTravelSavedData data = MetroTravelSavedData.get(server);
        MetroTravelSavedData.TravelSession current = data.get(player.getUUID());
        if (current == null || !current.sessionId().equals(session.sessionId())) return;

        MetroNetworkService.synchronize(server);
        MetroNetworkSavedData network = MetroNetworkSavedData.get(server);
        MetroNode destination = network.node(session.toNodeId());

        if (destination == null
                || destination.state() != MetroNodeState.ACTIVE
                || !validateDestinationSourceOfTruth(server, destination)) {
            data.remove(player.getUUID());
            ModNetwork.sendTo(
                    player,
                    MetroTravelVisualPacket.cancel(
                            "\u0421\u0442\u0430\u043d\u0446\u0438\u044f \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u0430."
                    )
            );
            player.sendSystemMessage(
                    Component.literal(
                            "\u041f\u043e\u0435\u0437\u0434\u043a\u0430 \u043e\u0442\u043c\u0435\u043d\u0435\u043d\u0430: \u0441\u0442\u0430\u043d\u0446\u0438\u044f \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f \u0431\u043e\u043b\u044c\u0448\u0435 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u0430."
                    ).withStyle(ChatFormatting.RED)
            );
            return;
        }

        ServerLevel targetLevel = resolveLevel(server, destination.dimension());
        if (targetLevel == null) {
            data.remove(player.getUUID());
            ModNetwork.sendTo(
                    player,
                    MetroTravelVisualPacket.cancel(
                            "\u0418\u0437\u043c\u0435\u0440\u0435\u043d\u0438\u0435 \u0441\u0442\u0430\u043d\u0446\u0438\u0438 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e."
                    )
            );
            return;
        }

        BlockPos arrival = resolveArrival(server, destination);
        if (arrival == null) {
            data.remove(player.getUUID());
            ModNetwork.sendTo(
                    player,
                    MetroTravelVisualPacket.cancel(
                            "\u0422\u043e\u0447\u043a\u0430 \u043f\u0440\u0438\u0431\u044b\u0442\u0438\u044f \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u0430."
                    )
            );
            return;
        }

        BlockPos safe = boundedSafeArrival(targetLevel, arrival);
        if (safe == null) {
            data.remove(player.getUUID());
            ModNetwork.sendTo(
                    player,
                    MetroTravelVisualPacket.cancel(
                            "\u041f\u043b\u0430\u0442\u0444\u043e\u0440\u043c\u0430 \u043f\u0440\u0438\u0431\u044b\u0442\u0438\u044f \u0437\u0430\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043d\u0430."
                    )
            );
            return;
        }

        // Remove first: our own cross-dimension teleport must never be mistaken
        // for an external dimension change and cancelled by the event handler.
        data.remove(player.getUUID());

        player.teleportTo(
                targetLevel,
                safe.getX() + 0.5D,
                safe.getY(),
                safe.getZ() + 0.5D,
                player.getYRot(),
                0.0F
        );
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;

        player.playNotifySound(SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.9F, 1.0F);
        ModNetwork.sendTo(player, MetroTravelVisualPacket.arrive(destination.displayName()));

        player.sendSystemMessage(
                Component.literal(
                        "\u041f\u0440\u0438\u0431\u044b\u0442\u0438\u0435: " + destination.displayName()
                ).withStyle(ChatFormatting.GREEN)
        );

        LOGGER.info(
                "Metro travel completed: session={} player={} destination={} @ {} {}",
                session.sessionId(),
                player.getGameProfile().getName(),
                destination.nodeId(),
                destination.dimension(),
                safe.toShortString()
        );
    }

    @Nullable
    private static MetroNode resolveCurrentNode(MetroNetworkSavedData network,
                                                String dimension,
                                                BlockPos consolePos) {
        MetroNode nearest = null;
        double nearestDistance = CONSOLE_NODE_RADIUS * CONSOLE_NODE_RADIUS;

        for (MetroNode node : network.nodes()) {
            if (node.state() != MetroNodeState.ACTIVE) continue;
            if (!node.dimension().equals(dimension)) continue;

            double distance = node.position().distSqr(consolePos);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = node;
            }
        }
        return nearest;
    }

    private static boolean validateDestinationSourceOfTruth(MinecraftServer server, MetroNode destination) {
        if (destination.type() == MetroNodeType.DOME) {
            DomeMetroSavedData dome = DomeMetroSavedData.get(server);
            return dome.isBuilt()
                    && destination.dimension().equals(dome.dimension())
                    && dome.stationAnchor() != null;
        }

        if (destination.stationId() == null) return false;
        MetroStationRecord station =
                MetroStationSavedData.get(server).get(destination.stationId());
        return station != null
                && station.state() == StationState.RESTORED
                && station.restored()
                && station.dimension().equals(destination.dimension());
    }

    @Nullable
    private static BlockPos resolveArrival(MinecraftServer server, MetroNode destination) {
        if (destination.type() == MetroNodeType.DOME) {
            DomeMetroSavedData dome = DomeMetroSavedData.get(server);
            if (!dome.isBuilt() || dome.stationAnchor() == null) return null;

            Direction forward = dome.forward();
            if (!forward.getAxis().isHorizontal()) forward = Direction.WEST;

            // Central platform: one of the known smooth-stone platform strips,
            // two blocks above the station floor. No world scan is needed.
            return dome.stationAnchor()
                    .relative(forward.getOpposite(), 2)
                    .above(2);
        }

        if (destination.stationId() == null) return null;
        MetroStationRecord station =
                MetroStationSavedData.get(server).get(destination.stationId());
        if (station == null || !station.restored()) return null;

        /*
         * Stage-2 station geometry is fixed relative to the restoration console:
         *   console anchor = center(-13,+2,-6)
         * The north platform walking space at center(0,+2,-1) therefore equals
         * anchor(+13,0,+5). This is a known marker, not a runtime station scan.
         */
        return station.anchor().offset(13, 0, 5);
    }

    @Nullable
    private static ServerLevel resolveLevel(MinecraftServer server, String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return server.getLevel(key);
    }

    /**
     * Bounded safety validation around the authored arrival marker.
     * At most nine candidate positions are tested.
     */
    @Nullable
    private static BlockPos boundedSafeArrival(ServerLevel level, BlockPos preferred) {
        List<BlockPos> candidates = List.of(
                preferred,
                preferred.north(),
                preferred.south(),
                preferred.east(),
                preferred.west(),
                preferred.north().east(),
                preferred.north().west(),
                preferred.south().east(),
                preferred.south().west()
        );

        for (BlockPos candidate : candidates) {
            if (isSafeStandingPosition(level, candidate)) return candidate;
        }
        return null;
    }

    private static boolean isSafeStandingPosition(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight()
                || feet.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockState floor = level.getBlockState(feet.below());
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());

        return !floor.getCollisionShape(level, feet.below()).isEmpty()
                && body.getCollisionShape(level, feet).isEmpty()
                && head.getCollisionShape(level, feet.above()).isEmpty()
                && body.getFluidState().isEmpty()
                && head.getFluidState().isEmpty();
    }

    private static void fail(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }
}
