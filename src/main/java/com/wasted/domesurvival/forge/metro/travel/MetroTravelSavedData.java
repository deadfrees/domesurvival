package com.wasted.domesurvival.forge.metro.travel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent passenger-session source of truth.
 *
 * One player may have at most one active session. Sessions survive disconnects
 * and server restarts. No world/chunk state is stored here.
 */
public final class MetroTravelSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_metro_travel";
    private static final int CURRENT_VERSION = 1;

    private final Map<UUID, TravelSession> sessions = new LinkedHashMap<>();

    public static MetroTravelSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                MetroTravelSavedData::load,
                MetroTravelSavedData::new,
                DATA_NAME
        );
    }

    public static MetroTravelSavedData load(CompoundTag tag) {
        MetroTravelSavedData data = new MetroTravelSavedData();
        ListTag list = tag.getList("Sessions", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("SessionId") || !entry.hasUUID("PlayerId")) continue;

            String from = entry.getString("FromNode");
            String to = entry.getString("ToNode");
            if (from.isBlank() || to.isBlank()) continue;

            TravelSession session = new TravelSession(
                    entry.getUUID("SessionId"),
                    entry.getUUID("PlayerId"),
                    from,
                    to,
                    Math.max(0L, entry.getLong("StartedAt")),
                    Math.max(0L, entry.getLong("ArriveAt")),
                    Math.max(1, entry.getInt("DurationTicks"))
            );
            data.sessions.put(session.playerId(), session);
        }

        return data;
    }

    @Nullable
    public TravelSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public List<TravelSession> all() {
        return List.copyOf(new ArrayList<>(sessions.values()));
    }

    public boolean has(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public boolean begin(TravelSession session) {
        if (sessions.containsKey(session.playerId())) return false;
        sessions.put(session.playerId(), session);
        setDirty();
        return true;
    }

    @Nullable
    public TravelSession remove(UUID playerId) {
        TravelSession removed = sessions.remove(playerId);
        if (removed != null) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("DataVersion", CURRENT_VERSION);

        ListTag list = new ListTag();
        for (TravelSession session : sessions.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("SessionId", session.sessionId());
            entry.putUUID("PlayerId", session.playerId());
            entry.putString("FromNode", session.fromNodeId());
            entry.putString("ToNode", session.toNodeId());
            entry.putLong("StartedAt", session.startedAtGameTime());
            entry.putLong("ArriveAt", session.arriveAtGameTime());
            entry.putInt("DurationTicks", session.durationTicks());
            list.add(entry);
        }
        tag.put("Sessions", list);
        return tag;
    }

    public record TravelSession(
            UUID sessionId,
            UUID playerId,
            String fromNodeId,
            String toNodeId,
            long startedAtGameTime,
            long arriveAtGameTime,
            int durationTicks
    ) {
        public TravelSession {
            if (sessionId == null) sessionId = UUID.randomUUID();
            if (playerId == null) throw new IllegalArgumentException("playerId");
            if (fromNodeId == null || fromNodeId.isBlank()) throw new IllegalArgumentException("fromNodeId");
            if (toNodeId == null || toNodeId.isBlank()) throw new IllegalArgumentException("toNodeId");
            startedAtGameTime = Math.max(0L, startedAtGameTime);
            arriveAtGameTime = Math.max(startedAtGameTime + 1L, arriveAtGameTime);
            durationTicks = Math.max(1, durationTicks);
        }

        public int remainingTicks(long gameTime) {
            return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, arriveAtGameTime - gameTime));
        }
    }
}
