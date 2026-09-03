package com.wasted.domesurvival.forge.network;

import com.wasted.domesurvival.forge.bio.BioLootData;
import com.wasted.domesurvival.forge.bio.BioModuleClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record BioModuleRegistrySyncPacket(List<BioLootData.Species> species) {
    private static final int MAX_SPECIES = 512;

    public BioModuleRegistrySyncPacket {
        species = List.copyOf(species);
        if (species.size() > MAX_SPECIES) {
            throw new IllegalArgumentException("Too many biological species");
        }
    }

    public static void encode(BioModuleRegistrySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.species.size());
        for (BioLootData.Species value : packet.species) {
            buffer.writeResourceLocation(value.entityId());
            buffer.writeUtf(value.rarity(), 32);
            buffer.writeUtf(value.lootGroup(), 64);
            buffer.writeVarInt(value.weight());
            buffer.writeResourceLocation(value.feedItem());
            buffer.writeVarInt(value.feedCount());
            buffer.writeVarInt(value.waterMb());
            buffer.writeVarInt(value.energyPerTick());
            buffer.writeVarInt(value.processTicks());
        }
    }

    public static BioModuleRegistrySyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_SPECIES) {
            throw new IllegalArgumentException("Invalid biological species count: " + size);
        }
        List<BioLootData.Species> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation entityId = buffer.readResourceLocation();
            String rarity = buffer.readUtf(32);
            String group = buffer.readUtf(64);
            int weight = buffer.readVarInt();
            ResourceLocation feed = buffer.readResourceLocation();
            int feedCount = buffer.readVarInt();
            int waterMb = buffer.readVarInt();
            int energyPerTick = buffer.readVarInt();
            int processTicks = buffer.readVarInt();
            values.add(new BioLootData.Species(entityId, rarity, group, weight,
                    feed, feedCount, waterMb, energyPerTick, processTicks));
        }
        return new BioModuleRegistrySyncPacket(values);
    }

    public static void handle(BioModuleRegistrySyncPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> BioModuleClientState.replace(packet.species));
        context.setPacketHandled(true);
    }
}
