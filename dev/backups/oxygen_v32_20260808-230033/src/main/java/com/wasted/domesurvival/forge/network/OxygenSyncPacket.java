package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.forge.client.ClientOxygenState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Very small S2C packet: two VarInts + one boolean.
 * Sent only on oxygen/environment changes, not every game tick.
 */
public record OxygenSyncPacket(int oxygen, int maxOxygen, boolean breathable) {
    public static void encode(OxygenSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.oxygen);
        buffer.writeVarInt(packet.maxOxygen);
        buffer.writeBoolean(packet.breathable);
    }

    public static OxygenSyncPacket decode(FriendlyByteBuf buffer) {
        return new OxygenSyncPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(OxygenSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                ClientOxygenState.update(packet.oxygen, packet.maxOxygen, packet.breathable)
        );
        context.setPacketHandled(true);
    }
}
