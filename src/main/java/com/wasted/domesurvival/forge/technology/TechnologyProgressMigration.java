package com.wasted.domesurvival.forge.technology;

import com.wasted.domesurvival.forge.quest.QuestGlobalSyncService;
import com.wasted.domesurvival.forge.quest.QuestProgressSavedData;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Grants technology flags earned before the technology system was installed. */
public final class TechnologyProgressMigration {
    private static final Map<String, List<String>> QUEST_FLAGS = new LinkedHashMap<>();

    static {
        map("1A7234374FC8891A", "FOOD_SYSTEM_ESTABLISHED", "FOOD_INFRASTRUCTURE_COMPLETE");

        map("613FF462EA9C75DC", "POWER_PROGRAM_STARTED");
        map("4D5E49001CD7D6CE", "POWER_FUEL_RESERVE_READY");
        map("473AE864B22D87AB", "PULSE_MATRIX_AVAILABLE");
        map("27D329DD824D350B", "BASIC_POWER_GRID_STARTED");
        map("74335F61233E9EB2", "FIRST_POWERED_MACHINE_ONLINE");
        map("3CE842358BCF4A85", "POWER_TRANSMISSION_TECH_KNOWN");
        map("78B0EF958AD81A29", "POWER_STORAGE_TECH_KNOWN");
        map("40D4CDA4A2445247", "POWER_STAGE_1");
        map("267C7B7925B99855", "POWER_INFRASTRUCTURE_ESTABLISHED", "DOME_POWER_ONLINE");

        map("6AB2D7F0B4A14E41", "WATER_PURIFICATION_TECH_KNOWN");
        map("7925344428A5F173", "OXYGEN_ELECTROLYSIS_TECH_KNOWN");
        map("6C2401658492032F", "OXYGEN_PRODUCTION_ONLINE", "OXYGEN_DISTRIBUTION_TECH_KNOWN");
        map("22DDCEABD0E620B4", "OXYGEN_FILLING_TECH_KNOWN");
        map("444141AA2A3ACB3E", "OXYGEN_FILLING_STATION_ONLINE", "PORTABLE_OXYGEN_TECH_KNOWN");
    }

    private TechnologyProgressMigration() {
    }

    public static int apply(ServerPlayer player) {
        QuestProgressSavedData progress = QuestProgressSavedData.get(player.serverLevel());
        int changed = 0;

        // Legacy completed power chapter saves already had this flag, but not
        // the more precise technology milestone introduced in v10.
        if (progress.hasFlag("POWER_INFRASTRUCTURE_ESTABLISHED")
                && progress.setFlag("DOME_POWER_ONLINE")) {
            changed++;
        }

        for (Map.Entry<String, List<String>> entry : QUEST_FLAGS.entrySet()) {
            if (!QuestGlobalSyncService.isGlobalCompleted(player, entry.getKey())) {
                continue;
            }
            for (String flag : entry.getValue()) {
                if (progress.setFlag(flag)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    private static void map(String questId, String... flags) {
        QUEST_FLAGS.put(questId, List.of(flags));
    }
}
