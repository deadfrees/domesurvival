package com.wasted.domesurvival.forge.quest;

import java.util.List;
import java.util.Map;

/**
 * Stable FTB Quests campaign IDs.
 *
 * IMPORTANT: FTB Quests 2001.4.x reads string IDs with Long.parseLong(hex, 16),
 * therefore published IDs must stay in the positive signed-long range
 * 0000000000000002..7FFFFFFFFFFFFFFF.
 *
 * V2 is the first registry actually published into FTB SNBT. V1 IDs were only
 * displayed by /domequest registry and were never used by FTB quest data.
 */
public final class QuestCampaignRegistry {
    public static final List<ChapterSpec> CHAPTERS = List.of(
            new ChapterSpec(0, "5017105A9984A511", "Под Куполом", 14),
            new ChapterSpec(1, "38F6E366B367B563", "Первые дни", 17),
            new ChapterSpec(2, "643138B0A36D1017", "За воротами", 17),
            new ChapterSpec(3, "270E74565C81C894", "Здесь будут жить", 15),
            new ChapterSpec(4, "23B2052E4DA60D0E", "Не одним хлебом", 20),
            new ChapterSpec(5, "7E8BFDBBF2CA9449", "Всё своё не унесёшь", 14),
            new ChapterSpec(6, "649FCBF9F0251A82", "Пусть горит свет", 20),
            new ChapterSpec(7, "76CBABB04B110F16", "Промышленный район", 22),
            new ChapterSpec(8, "6337B45648BD526C", "Всё должно двигаться", 20),
            new ChapterSpec(9, "196E68C2962C3B35", "Мир оказался больше", 16),
            new ChapterSpec(10, "4AD0CB6A893E6C72", "Когда садится солнце", 14),
            new ChapterSpec(11, "6005DCEA27FE984A", "Мир под миром", 18),
            new ChapterSpec(12, "76A5C21355A7B525", "Большая индустрия", 22),
            new ChapterSpec(13, "536DFF5B20B5A853", "Предел реальности", 14),
            new ChapterSpec(14, "2AB55D6EE31F1278", "Программа «Исход»", 21),
            new ChapterSpec(15, "05CA25527DF36F80", "Первый запуск", 12),
            new ChapterSpec(16, "3D7991DCEE3B0E8E", "Планетарные операции", 22),
            new ChapterSpec(17, "3093ECA3AE9C0CE9", "Собирайте вещи", 14),
            new ChapterSpec(18, "500B57532F96332F", "Другой дом", 10)
    );

    public static final String JOSEPH_HIDDEN_BRIDGE_CHAPTER_ID = "11FF60B844BBED5B";

    public static final Map<String, String> JOSEPH_HIDDEN_BRIDGE_QUEST_IDS = Map.ofEntries(
            Map.entry("JOSEPH_STAGE_13_COMPLETE", "31C0FE8D1487EA37"),
            Map.entry("JOSEPH_STAGE_14_COMPLETE", "3EFBBB5CC946B65C"),
            Map.entry("JOSEPH_STAGE_15_COMPLETE", "2B5067C09FEEEDED"),
            Map.entry("JOSEPH_STAGE_16_COMPLETE", "702F331B702620A5"),
            Map.entry("JOSEPH_FINAL_COMPLETE", "643B5A663ED36317")
    );

    private QuestCampaignRegistry() {
    }

    /**
     * Sum of design slots, not the promised final quest count.
     * Alternative/optional slots are consolidated during SNBT authoring.
     */
    public static int nominalDesignSlotCount() {
        return CHAPTERS.stream().mapToInt(ChapterSpec::targetQuestCount).sum();
    }

    public static boolean isFtbSignedSafeId(String id) {
        if (id == null || !id.matches("[0-7][0-9A-F]{15}")) {
            return false;
        }
        long value = Long.parseLong(id, 16);
        return value > 1L;
    }

    public record ChapterSpec(int index, String id, String title, int targetQuestCount) {
    }
}
