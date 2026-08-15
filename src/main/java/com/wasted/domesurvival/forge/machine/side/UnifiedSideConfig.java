package com.wasted.domesurvival.forge.machine.side;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;

/**
 * Unified machine routing: one mode per physical face, shared by every capability the
 * concrete machine supports on that face. INPUT is blue, OUTPUT is orange, DISABLED is off.
 * Resource-specific rules stay inside the machine (for example, a generator does not accept FE).
 */
public final class UnifiedSideConfig {
    public static final String NBT_ROOT = "UnifiedSideConfig";

    private final EnumMap<Direction, SideMode> modes = new EnumMap<>(Direction.class);

    public UnifiedSideConfig() {
        reset();
    }

    public SideMode getMode(Direction side) {
        return side == null ? SideMode.DISABLED : modes.getOrDefault(side, SideMode.DISABLED);
    }

    public boolean setMode(Direction side, SideMode mode) {
        if (side == null || mode == null) {
            return false;
        }
        SideMode sanitized = sanitize(mode);
        SideMode previous = modes.put(side, sanitized);
        return previous != sanitized;
    }

    public SideMode cycleMode(Direction side) {
        SideMode next = switch (getMode(side)) {
            case DISABLED -> SideMode.INPUT;
            case INPUT -> SideMode.OUTPUT;
            case OUTPUT, BOTH -> SideMode.DISABLED;
        };
        setMode(side, next);
        return next;
    }

    public boolean allowsInput(Direction side) {
        return getMode(side) == SideMode.INPUT;
    }

    public boolean allowsOutput(Direction side) {
        return getMode(side) == SideMode.OUTPUT;
    }

    public void reset() {
        for (Direction direction : Direction.values()) {
            modes.put(direction, SideMode.DISABLED);
        }
    }

    public void save(CompoundTag parentTag) {
        CompoundTag root = new CompoundTag();
        for (Direction direction : Direction.values()) {
            root.putString(direction.getName(), getMode(direction).getSerializedName());
        }
        parentTag.put(NBT_ROOT, root);
    }

    public boolean load(CompoundTag parentTag) {
        reset();
        if (!parentTag.contains(NBT_ROOT)) {
            return false;
        }
        CompoundTag root = parentTag.getCompound(NBT_ROOT);
        for (Direction direction : Direction.values()) {
            if (root.contains(direction.getName())) {
                setMode(direction, SideMode.fromSerializedName(root.getString(direction.getName())));
            }
        }
        return true;
    }

    private static SideMode sanitize(SideMode mode) {
        // Unified routing intentionally has no BOTH mode. Legacy BOTH data is converted
        // conservatively to OUTPUT because extraction must never silently become insertion.
        return mode == SideMode.BOTH ? SideMode.OUTPUT : mode;
    }
}
