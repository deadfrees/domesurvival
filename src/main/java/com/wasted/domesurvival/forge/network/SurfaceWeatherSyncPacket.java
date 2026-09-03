package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.forge.client.weather.ClientSurfaceWeatherState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Compact S2C weather/exposure state packet. Sent only when a player's visible state changes. */
public record SurfaceWeatherSyncPacket(
        SurfaceWeatherType weather,
        boolean exposed,
        boolean solarActive,
        boolean solarExposed,
        int secondsRemaining,
        int domeCenterX,
        int domeBaseY,
        int domeCenterZ
) {
    public static void encode(SurfaceWeatherSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.weather.ordinal());
        buffer.writeBoolean(packet.exposed);
        buffer.writeBoolean(packet.solarActive);
        buffer.writeBoolean(packet.solarExposed);
        buffer.writeVarInt(Math.max(0, packet.secondsRemaining));
        buffer.writeInt(packet.domeCenterX);
        buffer.writeInt(packet.domeBaseY);
        buffer.writeInt(packet.domeCenterZ);
    }

    public static SurfaceWeatherSyncPacket decode(FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        SurfaceWeatherType[] values = SurfaceWeatherType.values();
        SurfaceWeatherType weather = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : SurfaceWeatherType.CLEAR;
        return new SurfaceWeatherSyncPacket(
                weather,
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt()
        );
    }

    public static void handle(SurfaceWeatherSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientSurfaceWeatherState.update(
                packet.weather,
                packet.exposed,
                packet.solarActive,
                packet.solarExposed,
                packet.secondsRemaining,
                packet.domeCenterX,
                packet.domeBaseY,
                packet.domeCenterZ
        ));
        context.setPacketHandled(true);
    }
}
