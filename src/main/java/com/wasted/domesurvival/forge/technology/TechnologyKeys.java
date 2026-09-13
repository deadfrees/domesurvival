package com.wasted.domesurvival.forge.technology;

import java.util.Locale;
import java.util.Set;

/**
 * Stable technology identifiers used by gameplay, commands, JEI gates and quests.
 */
public final class TechnologyKeys {
    public static final String INDUSTRIAL_CRUSHING = "INDUSTRIAL_CRUSHING";
    public static final String MACHINE_MODULES = "MACHINE_MODULES";
    public static final String PORTABLE_POWER = "PORTABLE_POWER";
    public static final String OUTPOST_NETWORK = "OUTPOST_NETWORK";
    public static final String CARGO_TRANSIT = "CARGO_TRANSIT";
    public static final String PASSENGER_TRANSIT = "PASSENGER_TRANSIT";

    private static final Set<String> KNOWN = Set.of(
            INDUSTRIAL_CRUSHING,
            MACHINE_MODULES,
            PORTABLE_POWER,
            OUTPOST_NETWORK,
            CARGO_TRANSIT,
            PASSENGER_TRANSIT
    );

    private TechnologyKeys() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 64) {
            return null;
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (!(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '_') {
                return null;
            }
        }
        return normalized;
    }

    public static boolean isKnown(String id) {
        return KNOWN.contains(id);
    }

    public static Set<String> known() {
        return KNOWN;
    }
}
