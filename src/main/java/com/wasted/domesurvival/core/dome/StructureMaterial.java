package com.wasted.domesurvival.core.dome;

public enum StructureMaterial {
    GLASS,
    FRAME,
    FOUNDATION,
    AIR,
    SAND,
    COARSE_DIRT,
    AIRLOCK_DOOR,
    AIRLOCK_PANEL,
    /** Remove only the known author-made decorative blocks in the cleanup box. */
    CLEAR_AUTHOR_BUILD,
    /** Remove only the obsolete external V1.3 dome foundation and restore terrain. */
    CLEAR_LEGACY_FOUNDATION
}
