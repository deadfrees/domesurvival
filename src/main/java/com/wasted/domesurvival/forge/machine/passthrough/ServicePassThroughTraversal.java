package com.wasted.domesurvival.forge.machine.passthrough;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class ServicePassThroughTraversal {
    private static final int MAX_CHAIN_LENGTH = 32;

    private ServicePassThroughTraversal() {
    }

    @Nullable
    public static Exit resolve(Level level, BlockPos entryPos,
                               Direction travelDirection, ServiceConduitKind kind) {
        BlockPos cursor = entryPos;
        int transferLimit = Integer.MAX_VALUE;
        boolean crossed = false;

        for (int i = 0; i < MAX_CHAIN_LENGTH && level.hasChunkAt(cursor); i++) {
            if (!(level.getBlockEntity(cursor) instanceof ServicePassThroughBlockEntity pass)) {
                return crossed ? new Exit(cursor, transferLimit) : null;
            }

            if (pass.isEmpty()
                    || pass.getConduitKind() != kind
                    || !pass.isAxisCompatible(travelDirection)) {
                return null;
            }

            crossed = true;
            transferLimit = Math.min(transferLimit, pass.installedTransferLimit());
            cursor = cursor.relative(travelDirection);
        }

        return null;
    }

    public record Exit(BlockPos pos, int transferLimit) {
    }
}
