package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.forge.technology.TechnologyClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record TechnologySyncPacket(List<String> unlockedFlags) {
    private static final int MAX_FLAGS = 512;

    public TechnologySyncPacket {
        unlockedFlags = List.copyOf(unlockedFlags);
        if (unlockedFlags.size() > MAX_FLAGS) {
            throw new IllegalArgumentException("Too many technology progress flags");
        }
    }

    public static void encode(TechnologySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.unlockedFlags.size());
        packet.unlockedFlags.forEach(flag -> buffer.writeUtf(flag, 96));
    }

    public static TechnologySyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_FLAGS) {
            throw new IllegalArgumentException("Invalid technology flag count: " + size);
        }
        List<String> flags = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            flags.add(buffer.readUtf(96));
        }
        return new TechnologySyncPacket(flags);
    }

    public static void handle(TechnologySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> TechnologyClientState.replace(packet.unlockedFlags));
        context.setPacketHandled(true);
    }
}
