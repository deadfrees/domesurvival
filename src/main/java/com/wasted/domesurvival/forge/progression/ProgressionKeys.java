package com.wasted.domesurvival.forge.progression;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Stable progression identifiers shared by native code, FTB Quests command rewards,
 * CustomNPCs scripts and future KubeJS integration.
 *
 * <p>The manager intentionally accepts custom identifiers too. The constants below
 * are the current DomeSurvival vocabulary, not a hard-coded quest tree.</p>
 */
public final class ProgressionKeys {
    public static final String SURVIVAL = "survival";
    public static final String LIFE_SUPPORT = "life_support";
    public static final String EXPLORATION = "exploration";
    public static final String INDUSTRIAL = "industrial";
    public static final String ADVANCED_INDUSTRIAL = "advanced_industrial";
    public static final String SPACE = "space";
    public static final String LUNAR = "lunar";
    public static final String ADVANCED_SPACE = "advanced_space";

    private static final Pattern VALID_ID =
            Pattern.compile("[a-z0-9_.:/-]+");

    private static final List<String> KNOWN = List.of(
            SURVIVAL,
            LIFE_SUPPORT,
            EXPLORATION,
            INDUSTRIAL,
            ADVANCED_INDUSTRIAL,
            SPACE,
            LUNAR,
            ADVANCED_SPACE
    );

    private ProgressionKeys() {
    }

    public static List<String> known() {
        return KNOWN;
    }

    @Nullable
    public static String normalize(@Nullable String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !VALID_ID.matcher(normalized).matches()) {
            return null;
        }

        return normalized;
    }
}
