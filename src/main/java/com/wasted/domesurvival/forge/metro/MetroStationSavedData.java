package com.wasted.domesurvival.forge.metro;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Global source of truth for generated metro stations and their deposited materials. */
public final class MetroStationSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_metro_stations";
    public static final int CURRENT_VERSION = 4;

    /*
     * Human-readable station names. Unicode escapes keep this source ASCII-only,
     * so Windows PowerShell 5.1 cannot corrupt the Java file while installing it.
     */
    private static final List<String> STATION_NAMES = List.of(
            "\u0421\u0435\u0432\u0435\u0440\u043d\u0430\u044f",
            "\u041f\u0440\u043e\u043c\u044b\u0448\u043b\u0435\u043d\u043d\u0430\u044f",
            "\u0417\u0430\u0432\u043e\u0434\u0441\u043a\u0430\u044f",
            "\u042d\u043d\u0435\u0440\u0433\u0435\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f",
            "\u0428\u0430\u0445\u0442\u0451\u0440\u0441\u043a\u0430\u044f",
            "\u0413\u0435\u043e\u043b\u043e\u0433\u0438\u0447\u0435\u0441\u043a\u0430\u044f",
            "\u0421\u0442\u0430\u043b\u0435\u043b\u0438\u0442\u0435\u0439\u043d\u0430\u044f",
            "\u0420\u0443\u0431\u0435\u0436",
            "\u041c\u0430\u044f\u043a",
            "\u0420\u0430\u0441\u0441\u0432\u0435\u0442",
            "\u0413\u043e\u0440\u0438\u0437\u043e\u043d\u0442",
            "\u0412\u043e\u0441\u0442\u043e\u0447\u043d\u0430\u044f",
            "\u0417\u0430\u043f\u0430\u0434\u043d\u0430\u044f",
            "\u042e\u0436\u043d\u0430\u044f",
            "\u0422\u0435\u0445\u043d\u0438\u0447\u0435\u0441\u043a\u0430\u044f",
            "\u041a\u0430\u0440\u044c\u0435\u0440\u043d\u0430\u044f",
            "\u0414\u0435\u043f\u043e",
            "\u0422\u0440\u0430\u043d\u0441\u043f\u043e\u0440\u0442\u043d\u0430\u044f",
            "\u0420\u0443\u0434\u043d\u0430\u044f",
            "\u0421\u043a\u043b\u0430\u0434\u0441\u043a\u0430\u044f",
            "\u0411\u0443\u043d\u043a\u0435\u0440\u043d\u0430\u044f",
            "\u0420\u0435\u0430\u043a\u0442\u043e\u0440\u043d\u0430\u044f",
            "\u041f\u0435\u0440\u0438\u043c\u0435\u0442\u0440",
            "\u0410\u0432\u0430\u043d\u043f\u043e\u0441\u0442"
    );

    private final Map<UUID, MetroStationRecord> records = new LinkedHashMap<>();
    private int dataVersion = CURRENT_VERSION;

    public static MetroStationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                MetroStationSavedData::load,
                MetroStationSavedData::new,
                DATA_NAME
        );
    }

    public static MetroStationSavedData load(CompoundTag tag) {
        MetroStationSavedData data = new MetroStationSavedData();
        data.dataVersion = Math.max(0, tag.getInt("DataVersion"));
        boolean migrated = data.dataVersion < CURRENT_VERSION;
        Set<String> usedNames = new LinkedHashSet<>();

        ListTag list = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("StationId") || !entry.contains("Dimension") || !entry.contains("Anchor")) continue;

            UUID stationId = entry.getUUID("StationId");
            String dimension = entry.getString("Dimension");
            BlockPos anchor = BlockPos.of(entry.getLong("Anchor"));

            String storedName = entry.contains("StationName") ? entry.getString("StationName") : "";
            String stationName = storedName;
            if (stationName.isBlank() || isTechnicalName(stationName)) {
                stationName = nextRussianName(usedNames);
                migrated = true;
            } else if (usedNames.contains(stationName)) {
                stationName = nextRussianName(usedNames);
                migrated = true;
            }
            usedNames.add(stationName);

            StationState state = StationState.byName(entry.getString("State"));
            boolean discovered = entry.getBoolean("Discovered");
            boolean restored = entry.contains("Restored") ? entry.getBoolean("Restored") : state == StationState.RESTORED;
            UUID restoredBy = entry.hasUUID("RestoredBy") ? entry.getUUID("RestoredBy") : null;
            long discoveredAt = Math.max(0L, entry.getLong("DiscoveredAt"));
            long restoredAt = Math.max(0L, entry.getLong("RestoredAt"));

            Map<String, Integer> deposits = new LinkedHashMap<>();
            int removedTinDeposit = 0;
            if (entry.contains("RestorationDeposits", Tag.TAG_COMPOUND)) {
                CompoundTag depositTag = entry.getCompound("RestorationDeposits");
                for (String key : depositTag.getAllKeys()) {
                    int value = Math.max(0, depositTag.getInt(key));
                    if (value <= 0) continue;
                    if ("tin_rod".equals(key) || "tin_wire".equals(key)) {
                        removedTinDeposit += value;
                        migrated = true;
                    } else {
                        deposits.put(key, value);
                    }
                }
            }
            if (removedTinDeposit > 0) {
                int existing = deposits.getOrDefault("steel_wire", 0);
                deposits.put("steel_wire", Math.min(16, existing + removedTinDeposit));
            }

            if (restored) state = StationState.RESTORED;
            data.records.put(stationId, new MetroStationRecord(
                    stationId,
                    dimension,
                    anchor,
                    stationName,
                    state,
                    discovered,
                    restored,
                    restoredBy,
                    discoveredAt,
                    restoredAt,
                    deposits
            ));
        }

        if (migrated) {
            data.dataVersion = CURRENT_VERSION;
            data.setDirty();
        }
        return data;
    }

    public UUID claim(@Nullable UUID requestedId, String dimension, BlockPos anchor) {
        MetroStationRecord atLocation = findByAnchor(dimension, anchor);
        if (atLocation != null) return atLocation.stationId();

        UUID resolved = requestedId;
        if (resolved == null || records.containsKey(resolved)) {
            do { resolved = UUID.randomUUID(); } while (records.containsKey(resolved));
        }

        Set<String> usedNames = new LinkedHashSet<>();
        for (MetroStationRecord existing : records.values()) usedNames.add(existing.stationName());

        MetroStationRecord record = new MetroStationRecord(
                resolved,
                dimension,
                anchor,
                nextRussianName(usedNames),
                StationState.ABANDONED,
                false,
                false,
                null,
                0L,
                0L,
                Map.of()
        );
        records.put(resolved, record);
        setDirty();
        return resolved;
    }

    public boolean markDiscovered(UUID stationId, long gameTime) {
        MetroStationRecord record = records.get(stationId);
        if (record == null || !record.markDiscovered(gameTime)) return false;
        setDirty();
        return true;
    }

    public boolean setState(UUID stationId, StationState expected, StationState next) {
        MetroStationRecord record = records.get(stationId);
        if (record == null || record.state() != expected || expected == next) return false;
        record.setState(next);
        setDirty();
        return true;
    }

    public boolean markRestored(UUID stationId, @Nullable UUID playerId, long gameTime) {
        MetroStationRecord record = records.get(stationId);
        if (record == null || record.state() == StationState.RESTORED) return false;
        record.markRestored(playerId, gameTime);
        setDirty();
        return true;
    }

    public int addRestorationDeposit(UUID stationId, String requirementId, int amount, int maximum) {
        MetroStationRecord record = records.get(stationId);
        if (record == null || record.state() != StationState.ABANDONED) return 0;
        int accepted = record.addRestorationDeposit(requirementId, amount, maximum);
        if (accepted > 0) setDirty();
        return accepted;
    }

    public void replaceRestorationDeposits(UUID stationId, Map<String, Integer> deposits) {
        MetroStationRecord record = records.get(stationId);
        if (record == null) return;
        record.replaceRestorationDeposits(deposits);
        setDirty();
    }

    public boolean resetRestoration(UUID stationId) {
        MetroStationRecord record = records.get(stationId);
        if (record == null) return false;
        record.resetRestoration();
        setDirty();
        return true;
    }

    @Nullable
    public MetroStationRecord get(UUID stationId) {
        return records.get(stationId);
    }

    @Nullable
    public MetroStationRecord findByAnchor(String dimension, BlockPos anchor) {
        for (MetroStationRecord record : records.values()) {
            if (record.dimension().equals(dimension) && record.anchor().equals(anchor)) return record;
        }
        return null;
    }

    public List<MetroStationRecord> all() {
        ArrayList<MetroStationRecord> result = new ArrayList<>(records.values());
        result.sort(Comparator.comparing(MetroStationRecord::stationName));
        return List.copyOf(result);
    }

    public int size() {
        return records.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("DataVersion", CURRENT_VERSION);
        ListTag list = new ListTag();
        for (MetroStationRecord record : records.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("StationId", record.stationId());
            entry.putString("Dimension", record.dimension());
            entry.putLong("Anchor", record.anchor().asLong());
            entry.putString("StationName", record.stationName());
            entry.putString("State", record.state().name());
            entry.putBoolean("Discovered", record.discovered());
            entry.putBoolean("Restored", record.restored());
            if (record.restoredBy() != null) entry.putUUID("RestoredBy", record.restoredBy());
            entry.putLong("DiscoveredAt", record.discoveredAt());
            entry.putLong("RestoredAt", record.restoredAt());

            CompoundTag deposits = new CompoundTag();
            record.restorationDeposits().forEach((key, value) -> {
                if (value != null && value > 0) deposits.putInt(key, value);
            });
            entry.put("RestorationDeposits", deposits);
            list.add(entry);
        }
        tag.put("Stations", list);
        return tag;
    }

    private static boolean isTechnicalName(String name) {
        if (name == null) return true;
        String value = name.trim().toUpperCase(Locale.ROOT);
        return value.startsWith("METRO-") || value.startsWith("METRO_") || value.startsWith("STATION-");
    }

    private static String nextRussianName(Set<String> usedNames) {
        for (String candidate : STATION_NAMES) {
            if (!usedNames.contains(candidate)) return candidate;
        }
        return "\u0421\u0442\u0430\u043d\u0446\u0438\u044f " + (usedNames.size() + 1);
    }
}
