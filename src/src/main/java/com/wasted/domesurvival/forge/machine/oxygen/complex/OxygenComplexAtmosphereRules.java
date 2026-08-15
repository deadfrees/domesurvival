package com.wasted.domesurvival.forge.machine.oxygen.complex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * V64.0 atmosphere rule: the intake must have a clear air channel leading to sky.
 * This is deliberately bounded and never force-loads chunks.
 */
public final class OxygenComplexAtmosphereRules {
    public static final int MAX_INTAKE_TRACE = 8;

    private OxygenComplexAtmosphereRules() {
    }

    public static boolean hasExternalAtmosphere(ServerLevel level, BlockPos intakePos, Direction facing) {
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return false;
        }

        Direction front = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        for (int distance = 1; distance <= MAX_INTAKE_TRACE; distance++) {
            BlockPos checkPos = intakePos.relative(front, distance);
            if (!level.hasChunkAt(checkPos)) {
                return false;
            }

            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir()) {
                return false;
            }

            if (level.canSeeSky(checkPos)) {
                return true;
            }
        }
        return false;
    }
}
