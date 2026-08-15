package com.wasted.domesurvival.forge.machine.oxygen.complex;

import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Physical connector placement for the 2x2 Oxygen Complex.
 *
 * One low-profile port is rendered/exposed for each relative side of the full
 * multiblock instead of putting six ports on every individual module.
 */
public final class OxygenComplexPortLayout {
    private OxygenComplexPortLayout() {
    }

    public static OxygenComplexRole hostRole(RelativeSide side) {
        return switch (side) {
            case TOP -> OxygenComplexRole.FILTRATION;
            case BOTTOM -> OxygenComplexRole.OUTPUT;
            case BACK -> OxygenComplexRole.OUTPUT;
            case FRONT, LEFT, RIGHT -> OxygenComplexRole.OUTPUT;
        };
    }


    public static boolean isPhysicalPort(RelativeSide side) {
        return side == RelativeSide.TOP
                || side == RelativeSide.BOTTOM
                || side == RelativeSide.BACK;
    }

    @Nullable
    public static RelativeSide relativeSide(Direction worldSide, Direction machineFacing) {
        if (worldSide == null) {
            return null;
        }

        for (RelativeSide relativeSide : RelativeSide.values()) {
            if (relativeSide.resolve(machineFacing) == worldSide) {
                return relativeSide;
            }
        }
        return null;
    }

    public static boolean isPortFace(
            OxygenComplexRole role,
            Direction machineFacing,
            Direction worldSide
    ) {
        RelativeSide relativeSide = relativeSide(worldSide, machineFacing);
        return relativeSide != null && isPhysicalPort(relativeSide) && hostRole(relativeSide) == role;
    }
}
