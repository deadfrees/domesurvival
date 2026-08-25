package com.wasted.domesurvival.forge.quest;

import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-global campaign completion ledger.
 *
 * Schema v2 additionally records the one global reward winner for each quest.
 * Schema v1 worlds load safely; older already-completed entries simply have no
 * winner and are never rewarded retroactively.
 */
public final class GlobalQuestProgressSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_global_quest_progress_v1";
    private static final int SCHEMA_VERSION = 2;

    private final Map<String, Integer> completedRevision = new LinkedHashMap<>();
    private final Map<String, UUID> winnerByQuest = new LinkedHashMap<>();
    private final Map<String, String> winnerNameByQuest = new LinkedHashMap<>();
    private final Map<UUID, Integer> playerSyncRevision = new LinkedHashMap<>();
    private int revision;

    public static GlobalQuestProgressSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                GlobalQuestProgressSavedData::load,
                GlobalQuestProgressSavedData::new,
                DATA_NAME
        );
    }

    public static GlobalQuestProgressSavedData load(CompoundTag tag) {
        GlobalQuestProgressSavedData data = new GlobalQuestProgressSavedData();
        data.revision = Math.max(0, tag.getInt("Revision"));

        ListTag completed = tag.getList("Completed", Tag.TAG_COMPOUND);
        for (int i = 0; i < completed.size(); i++) {
            CompoundTag entry = completed.getCompound(i);
            String quest = entry.getString("Quest").toUpperCase();
            int rev = entry.getInt("Revision");

            if (!QuestGlobalRegistry.isKnownQuest(quest) || rev <= 0) {
                continue;
            }

            data.completedRevision.put(quest, rev);
            data.revision = Math.max(data.revision, rev);

            if (entry.contains("Winner", Tag.TAG_STRING)) {
                try {
                    data.winnerByQuest.put(
                            quest,
                            UUIDTypeAdapter.fromString(entry.getString("Winner"))
                    );
                } catch (IllegalArgumentException ignored) {
                    // Legacy/malformed winner is ignored; completion remains.
                }
            }

            String winnerName = entry.getString("WinnerName");
            if (!winnerName.isBlank()) {
                data.winnerNameByQuest.put(quest, winnerName);
            }
        }

        CompoundTag players = tag.getCompound("PlayerSync");
        for (String key : players.getAllKeys()) {
            try {
                UUID id = UUIDTypeAdapter.fromString(key);
                data.playerSyncRevision.put(id, Math.max(0, players.getInt(key)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy/offline entries safely.
            }
        }

        return data;
    }

    public boolean isCompleted(String questId) {
        return completedRevision.containsKey(normalize(questId));
    }

    /**
     * Atomically reserves global completion and the single reward winner.
     */
    public CompletionResult completeOnce(String questId, UUID winnerId, String winnerName) {
        String id = normalize(questId);
        Integer existing = completedRevision.get(id);

        if (existing != null) {
            return new CompletionResult(existing, false);
        }

        revision++;
        completedRevision.put(id, revision);
        winnerByQuest.put(id, winnerId);
        winnerNameByQuest.put(id, winnerName == null ? "" : winnerName);
        setDirty();
        return new CompletionResult(revision, true);
    }

    public Optional<UUID> winner(String questId) {
        return Optional.ofNullable(winnerByQuest.get(normalize(questId)));
    }

    public String winnerName(String questId) {
        return winnerNameByQuest.getOrDefault(normalize(questId), "");
    }

    public int currentRevision() {
        return revision;
    }

    public int playerRevision(UUID playerId) {
        return playerSyncRevision.getOrDefault(playerId, 0);
    }

    public void markPlayerRevision(UUID playerId, int value) {
        int clamped = Math.max(0, Math.min(value, revision));
        if (playerSyncRevision.getOrDefault(playerId, 0) != clamped) {
            playerSyncRevision.put(playerId, clamped);
            setDirty();
        }
    }

    public void resetPlayerRevision(UUID playerId) {
        if (playerSyncRevision.remove(playerId) != null) {
            setDirty();
        }
    }

    public List<Completion> completionsAfter(int afterRevision) {
        List<Completion> result = new ArrayList<>();
        completedRevision.forEach((id, rev) -> {
            if (rev > afterRevision) {
                result.add(new Completion(id, rev));
            }
        });
        result.sort(Comparator.comparingInt(Completion::revision));
        return result;
    }

    public List<String> allCompletedIds() {
        return completedRevision.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("SchemaVersion", SCHEMA_VERSION);
        tag.putInt("Revision", revision);

        ListTag completed = new ListTag();
        completedRevision.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    CompoundTag row = new CompoundTag();
                    String quest = entry.getKey();
                    row.putString("Quest", quest);
                    row.putInt("Revision", entry.getValue());

                    UUID winner = winnerByQuest.get(quest);
                    if (winner != null) {
                        row.putString("Winner", UUIDTypeAdapter.fromUUID(winner));
                    }

                    String winnerName = winnerNameByQuest.getOrDefault(quest, "");
                    if (!winnerName.isBlank()) {
                        row.putString("WinnerName", winnerName);
                    }

                    completed.add(row);
                });
        tag.put("Completed", completed);

        CompoundTag players = new CompoundTag();
        playerSyncRevision.forEach((uuid, rev) ->
                players.putInt(UUIDTypeAdapter.fromUUID(uuid), rev));
        tag.put("PlayerSync", players);

        return tag;
    }

    private static String normalize(String questId) {
        return questId == null ? "" : questId.trim().toUpperCase();
    }

    public record Completion(String questId, int revision) {
    }

    public record CompletionResult(int revision, boolean newlyCompleted) {
    }
}
