package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.forge.metro.travel.MetroTravelService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MetroTravelRequestPacket(String destinationNodeId) {
    public MetroTravelRequestPacket {
        if (destinationNodeId == null) destinationNodeId = "";
        if (destinationNodeId.length() > 512) {
            destinationNodeId = destinationNodeId.substring(0, 512);
        }
    }

    public static void encode(MetroTravelRequestPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.destinationNodeId(), 512);
    }

    public static MetroTravelRequestPacket decode(FriendlyByteBuf buf) {
        return new MetroTravelRequestPacket(buf.readUtf(512));
    }

    public static void handle(MetroTravelRequestPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();

        context.enqueueWork(() -> {
            if (sender != null && !packet.destinationNodeId().isBlank()) {
                MetroTravelService.requestTravel(sender, packet.destinationNodeId());
            }
        });
        context.setPacketHandled(true);
    }
}
