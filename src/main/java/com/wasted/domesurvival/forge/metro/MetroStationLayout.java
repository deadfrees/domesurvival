package com.wasted.domesurvival.forge.metro;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Stable relative markers for the generated station.
 * Stage 3 replaces only these known positions instead of scanning the station.
 * All offsets are relative to the Restoration Console anchor.
 */
public final class MetroStationLayout {
    /** Train console inside the static car. */
    public static final BlockPos TRAIN_CONSOLE = new BlockPos(13, 1, 8);

    /** Ceiling fixtures intentionally generated in their damaged/off state. */
    public static final List<BlockPos> BROKEN_LIGHTS = List.of(
            new BlockPos(3, 7, 6),
            new BlockPos(8, 7, 6),
            new BlockPos(13, 7, 6),
            new BlockPos(18, 7, 6),
            new BlockPos(23, 7, 6),
            new BlockPos(26, 7, 6),
            new BlockPos(3, 7, 12),
            new BlockPos(8, 7, 12),
            new BlockPos(13, 7, 12),
            new BlockPos(18, 7, 12),
            new BlockPos(23, 7, 12),
            new BlockPos(26, 7, 12)
    );

    /** Known damaged display positions for Stage 3 visual restoration. */
    public static final List<BlockPos> DAMAGED_DISPLAYS = List.of(
            new BlockPos(4, 2, 2),
            new BlockPos(24, 2, 13)
    );

    private MetroStationLayout() {
    }

    public static BlockPos at(BlockPos anchor, BlockPos offset) {
        return anchor.offset(offset.getX(), offset.getY(), offset.getZ());
    }
}
