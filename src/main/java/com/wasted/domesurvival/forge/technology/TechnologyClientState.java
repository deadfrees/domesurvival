package com.wasted.domesurvival.forge.technology;

import java.util.Collection;
import java.util.Set;

public final class TechnologyClientState {
    private static volatile Set<String> unlockedFlags = Set.of();

    private TechnologyClientState() {
    }

    public static void replace(Collection<String> flags) {
        unlockedFlags = Set.copyOf(flags);
    }

    public static boolean has(String flag) {
        return unlockedFlags.contains(flag);
    }
}
