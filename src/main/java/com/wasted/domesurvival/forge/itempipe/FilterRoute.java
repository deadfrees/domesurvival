package com.wasted.domesurvival.forge.itempipe;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public enum FilterRoute {
    NORTH(Direction.NORTH, 0xFF3D7DCE, "blue"),
    SOUTH(Direction.SOUTH, 0xFFC64A42, "red"),
    WEST(Direction.WEST, 0xFF4C9A57, "green"),
    EAST(Direction.EAST, 0xFFD4B83E, "yellow"),
    UP(Direction.UP, 0xFFE1E4E6, "white"),
    DOWN(Direction.DOWN, 0xFF8A59B5, "purple"),
    NONE(null, 0xFF353A3D, "none"),
    ANY(null, 0xFF747C80, "any");

    @Nullable private final Direction direction;
    private final int argb;
    private final String id;

    FilterRoute(@Nullable Direction direction, int argb, String id) {
        this.direction = direction;
        this.argb = argb;
        this.id = id;
    }

    @Nullable public Direction direction() { return direction; }
    public int argb() { return argb; }
    public String id() { return id; }

    public static FilterRoute filterByIndex(int index) {
        if (index < 0 || index > NONE.ordinal()) return NONE;
        return values()[index];
    }

    public static FilterRoute defaultByIndex(int index) {
        if (index < 0 || index >= values().length) return ANY;
        FilterRoute route = values()[index];
        return route == NONE ? ANY : route;
    }
}
