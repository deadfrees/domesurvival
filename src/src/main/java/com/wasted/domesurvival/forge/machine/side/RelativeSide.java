package com.wasted.domesurvival.forge.machine.side;

import net.minecraft.core.Direction;

/**
 * Human-facing machine sides resolved relative to a horizontally facing block.
 */
public enum RelativeSide {
    TOP,
    BOTTOM,
    FRONT,
    BACK,
    LEFT,
    RIGHT;

    public Direction resolve(Direction machineFacing) {
        Direction horizontalFacing = machineFacing.getAxis().isHorizontal()
                ? machineFacing
                : Direction.NORTH;

        return switch (this) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case FRONT -> horizontalFacing;
            case BACK -> horizontalFacing.getOpposite();
            case LEFT -> horizontalFacing.getCounterClockWise();
            case RIGHT -> horizontalFacing.getClockWise();
        };
    }
}
