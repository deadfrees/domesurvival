package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {
    /*
     * Protocol 5 adds the server-authoritative technology progress sync.
     *
     * Do not return this to protocol 2:
     * current main already contains SurfaceWeatherSyncPacket.
     */
    private static final String PROTOCOL_VERSION = "5";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(
                            DomeSurvival.MOD_ID,
                            "main"
                    ),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static int nextMessageId;
    private static boolean registered;

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        CHANNEL.registerMessage(
                nextMessageId++,
                OxygenSyncPacket.class,
                OxygenSyncPacket::encode,
                OxygenSyncPacket::decode,
                OxygenSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                nextMessageId++,
                SurfaceWeatherSyncPacket.class,
                SurfaceWeatherSyncPacket::encode,
                SurfaceWeatherSyncPacket::decode,
                SurfaceWeatherSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                nextMessageId++,
                TechnologySyncPacket.class,
                TechnologySyncPacket::encode,
                TechnologySyncPacket::decode,
                TechnologySyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendTo(
            ServerPlayer player,
            Object message
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                message
        );
    }
}
