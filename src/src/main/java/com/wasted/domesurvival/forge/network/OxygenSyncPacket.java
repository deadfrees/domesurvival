package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.core.oxygen.OxygenSource;
import com.wasted.domesurvival.forge.client.ClientOxygenState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Small S2C HUD packet:
 * - displayed oxygen (VarInt)
 * - displayed capacity (VarInt)
 * - ambient breathability (boolean)
 * - active source (byte)
 *
 * It is still sent only when authoritative state changes, never every game tick.
 */
public record OxygenSyncPacket(
        int oxygen,
        int maxOxygen,
        boolean breathable,
        OxygenSource source
) {
    public static void encode(OxygenSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.oxygen);
        buffer.writeVarInt(packet.maxOxygen);
        buffer.writeBoolean(packet.breathable);
        buffer.writeByte(packet.source.networkId());
    }

    public static OxygenSyncPacket decode(FriendlyByteBuf buffer) {
        return new OxygenSyncPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                OxygenSource.fromNetworkId(buffer.readUnsignedByte())
        );
    }

    public static void handle(OxygenSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                ClientOxygenState.update(
                        packet.oxygen,
                        packet.maxOxygen,
                        packet.breathable,
                        packet.source
                )
        );
        context.setPacketHandled(true);
    }
}
