package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.forge.client.metro.MetroTravelClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MetroTravelVisualPacket(
        Phase phase,
        String destinationName,
        int durationTicks,
        String detail
) {
    public enum Phase {
        START,
        ARRIVE,
        CANCEL
    }

    public MetroTravelVisualPacket {
        if (phase == null) phase = Phase.CANCEL;
        if (destinationName == null) destinationName = "";
        if (detail == null) detail = "";
        durationTicks = Math.max(0, durationTicks);
    }

    public static MetroTravelVisualPacket start(String destinationName, int durationTicks) {
        return new MetroTravelVisualPacket(Phase.START, destinationName, durationTicks, "");
    }

    public static MetroTravelVisualPacket arrive(String destinationName) {
        return new MetroTravelVisualPacket(Phase.ARRIVE, destinationName, 0, "");
    }

    public static MetroTravelVisualPacket cancel(String detail) {
        return new MetroTravelVisualPacket(Phase.CANCEL, "", 0, detail);
    }

    public static void encode(MetroTravelVisualPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.phase());
        buf.writeUtf(packet.destinationName(), 512);
        buf.writeVarInt(packet.durationTicks());
        buf.writeUtf(packet.detail(), 512);
    }

    public static MetroTravelVisualPacket decode(FriendlyByteBuf buf) {
        return new MetroTravelVisualPacket(
                buf.readEnum(Phase.class),
                buf.readUtf(512),
                buf.readVarInt(),
                buf.readUtf(512)
        );
    }

    public static void handle(MetroTravelVisualPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> MetroTravelClientState.handle(packet)
                )
        );
        context.setPacketHandled(true);
    }
}
