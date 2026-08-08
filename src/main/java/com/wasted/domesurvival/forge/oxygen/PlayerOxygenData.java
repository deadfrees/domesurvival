package com.wasted.domesurvival.forge.oxygen;

import com.wasted.domesurvival.core.oxygen.OxygenRules;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * Persistent server-side player oxygen state.
 *
 * Kept deliberately small: two integers. Equipment capacity is resolved separately
 * so future tanks/suits do not require an NBT format migration.
 */
public final class PlayerOxygenData {
    private static final String ROOT_KEY = DomeSurvival.MOD_ID + ":oxygen";
    private static final String OXYGEN_KEY = "oxygen";
    private static final String EMPTY_UPDATES_KEY = "empty_updates";

    // Transient sync cache fields. Stored in Forge persistent data, but not inside oxygen payload.
    private static final String LAST_BREATHABLE_SET_KEY = DomeSurvival.MOD_ID + ":oxygen_last_breathable_set";
    private static final String LAST_BREATHABLE_KEY = DomeSurvival.MOD_ID + ":oxygen_last_breathable";

    private PlayerOxygenData() {
    }

    public static int maxOxygen(Player player) {
        // Extension point for future suit/tank capacity.
        return OxygenRules.BASE_MAX_OXYGEN;
    }

    public static int oxygen(Player player) {
        int max = maxOxygen(player);
        CompoundTag tag = getOrCreate(player);
        if (!tag.contains(OXYGEN_KEY, Tag.TAG_INT)) {
            tag.putInt(OXYGEN_KEY, max);
        }
        int value = OxygenRules.clamp(tag.getInt(OXYGEN_KEY), 0, max);
        if (value != tag.getInt(OXYGEN_KEY)) {
            tag.putInt(OXYGEN_KEY, value);
        }
        return value;
    }

    public static int emptyUpdates(Player player) {
        CompoundTag tag = getOrCreate(player);
        return Math.max(0, tag.getInt(EMPTY_UPDATES_KEY));
    }

    public static void set(Player player, int oxygen, int emptyUpdates) {
        CompoundTag tag = getOrCreate(player);
        tag.putInt(OXYGEN_KEY, OxygenRules.clamp(oxygen, 0, maxOxygen(player)));
        tag.putInt(EMPTY_UPDATES_KEY, Math.max(0, emptyUpdates));
    }

    public static void reset(Player player) {
        set(player, maxOxygen(player), 0);
    }

    public static void copy(Player from, Player to) {
        CompoundTag sourceRoot = from.getPersistentData();
        if (sourceRoot.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            to.getPersistentData().put(ROOT_KEY, sourceRoot.getCompound(ROOT_KEY).copy());
        } else {
            reset(to);
        }
        clearLastBreathable(to);
    }

    /**
     * Returns true when the environmental state changed since the last server sync.
     */
    public static boolean updateLastBreathable(Player player, boolean breathable) {
        CompoundTag root = player.getPersistentData();
        boolean initialized = root.getBoolean(LAST_BREATHABLE_SET_KEY);
        boolean changed = !initialized || root.getBoolean(LAST_BREATHABLE_KEY) != breathable;
        root.putBoolean(LAST_BREATHABLE_SET_KEY, true);
        root.putBoolean(LAST_BREATHABLE_KEY, breathable);
        return changed;
    }

    public static void clearLastBreathable(Player player) {
        CompoundTag root = player.getPersistentData();
        root.remove(LAST_BREATHABLE_SET_KEY);
        root.remove(LAST_BREATHABLE_KEY);
    }

    private static CompoundTag getOrCreate(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag oxygen = new CompoundTag();
            oxygen.putInt(OXYGEN_KEY, maxOxygen(player));
            oxygen.putInt(EMPTY_UPDATES_KEY, 0);
            root.put(ROOT_KEY, oxygen);
        }
        return root.getCompound(ROOT_KEY);
    }
}
