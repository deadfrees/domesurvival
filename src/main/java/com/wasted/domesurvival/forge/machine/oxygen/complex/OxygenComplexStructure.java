package com.wasted.domesurvival.forge.machine.oxygen.complex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Constant-time formation logic for the fixed 2x2 Oxygen Complex.
 *
 * <p>V64.0.1 deliberately does not depend on placement order or the temporary
 * FACING value a part received when it was placed.  The physical 2x2 layout is
 * the source of truth.  Once all four required roles exist in the correct
 * positions, their common facing is derived from that layout and normalized on
 * all four blocks.</p>
 */
public final class OxygenComplexStructure {
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    private OxygenComplexStructure() {
    }

    /**
     * Position of a role relative to the lower-right OUTPUT/controller block,
     * viewed from the machine front.
     *
     * <p>For a block whose front points toward {@code facing}, the player's
     * visual LEFT while looking at that front is {@code facing.getClockWise()}.
     * This is the important correction from V64.0.</p>
     */
    public static BlockPos rolePos(BlockPos controllerPos, OxygenComplexRole role, Direction facing) {
        Direction visualLeft = normalizedFacing(facing).getClockWise();
        return switch (role) {
            case OUTPUT -> controllerPos;
            case COMPRESSION -> controllerPos.relative(visualLeft);
            case FILTRATION -> controllerPos.above();
            case AIR_INTAKE -> controllerPos.above().relative(visualLeft);
        };
    }

    /** Inverse of {@link #rolePos(BlockPos, OxygenComplexRole, Direction)}. */
    public static BlockPos controllerPos(BlockPos partPos, OxygenComplexRole role, Direction facing) {
        Direction visualRight = normalizedFacing(facing).getCounterClockWise();
        return switch (role) {
            case OUTPUT -> partPos;
            case COMPRESSION -> partPos.relative(visualRight);
            case FILTRATION -> partPos.below();
            case AIR_INTAKE -> partPos.below().relative(visualRight);
        };
    }

    /**
     * Called after any module is placed.
     *
     * <p>Only a tiny 3x3x3 neighborhood is searched for an OUTPUT controller.
     * For every candidate controller, at most four horizontal 2x2 layouts are
     * tested.  This stays bounded and is not a flood-fill.</p>
     */
    public static void refreshFrom(ServerLevel level, BlockPos partPos) {
        if (!(level.getBlockState(partPos).getBlock() instanceof OxygenComplexBlock)) {
            return;
        }

        // The farthest valid controller from any one part is one horizontal
        // block plus one vertical block away, so this bounded cube is enough.
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 1) {
                        continue;
                    }

                    BlockPos candidate = partPos.offset(dx, dy, dz);
                    if (!level.hasChunkAt(candidate)) {
                        continue;
                    }
                    if (isRole(level, candidate, OxygenComplexRole.OUTPUT)
                            && refreshController(level, candidate)) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Revalidates one OUTPUT controller.
     *
     * <p>Formation no longer requires pre-matching part facings.  The correct
     * orientation is detected from the actual four-role layout, then written
     * back to all four modules atomically enough for normal block-state use.</p>
     */
    public static boolean refreshController(ServerLevel level, BlockPos controllerPos) {
        if (!isRole(level, controllerPos, OxygenComplexRole.OUTPUT)) {
            return false;
        }

        BlockState outputState = level.getBlockState(controllerPos);
        Direction previousFacing = normalizedFacing(outputState.getValue(OxygenComplexBlock.FACING));
        Direction facing = detectFacing(level, controllerPos, previousFacing);

        if (facing == null) {
            // A previously formed structure always had all four facings
            // normalized, so the output's current facing identifies its old
            // footprint and lets us clear stale FORMED/ACTIVE flags safely.
            clearFootprint(level, controllerPos, previousFacing);
            return false;
        }

        for (OxygenComplexRole role : OxygenComplexRole.values()) {
            BlockPos pos = rolePos(controllerPos, role, facing);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof OxygenComplexBlock block) || block.role() != role) {
                return false;
            }

            BlockState updated = state;
            if (updated.getValue(OxygenComplexBlock.FACING) != facing) {
                updated = updated.setValue(OxygenComplexBlock.FACING, facing);
            }
            if (!updated.getValue(OxygenComplexBlock.FORMED)) {
                updated = updated.setValue(OxygenComplexBlock.FORMED, true);
            }
            if (!updated.equals(state)) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof OxygenComplexBlockEntity part) {
                part.setStructureLink(controllerPos, true);
            }
        }

        BlockEntity controller = level.getBlockEntity(controllerPos);
        if (controller instanceof OxygenComplexBlockEntity complexController) {
            complexController.onStructureFormed();
        }
        return true;
    }

    /**
     * Detects the 2x2 layout independently of the placement direction of each
     * module.  The OUTPUT is lower-right and FILTRATION is directly above it;
     * COMPRESSION may be on any horizontal side, with AIR_INTAKE above it.
     */
    @Nullable
    private static Direction detectFacing(ServerLevel level, BlockPos controllerPos, Direction preferredFacing) {
        // Prefer the already stored orientation when it still matches. This
        // prevents needless facing changes on an already formed machine.
        Direction preferredLeft = normalizedFacing(preferredFacing).getClockWise();
        if (matchesPhysicalLayout(level, controllerPos, preferredLeft)) {
            return normalizedFacing(preferredFacing);
        }

        for (Direction visualLeft : HORIZONTAL_DIRECTIONS) {
            if (visualLeft == preferredLeft) {
                continue;
            }
            if (matchesPhysicalLayout(level, controllerPos, visualLeft)) {
                // visualLeft = facing.getClockWise(), therefore inverse is CCW.
                return visualLeft.getCounterClockWise();
            }
        }
        return null;
    }

    private static boolean matchesPhysicalLayout(ServerLevel level, BlockPos controllerPos, Direction visualLeft) {
        BlockPos filtration = controllerPos.above();
        BlockPos compression = controllerPos.relative(visualLeft);
        BlockPos intake = compression.above();

        return isRole(level, controllerPos, OxygenComplexRole.OUTPUT)
                && isRole(level, filtration, OxygenComplexRole.FILTRATION)
                && isRole(level, compression, OxygenComplexRole.COMPRESSION)
                && isRole(level, intake, OxygenComplexRole.AIR_INTAKE);
    }

    private static boolean isRole(ServerLevel level, BlockPos pos, OxygenComplexRole expectedRole) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof OxygenComplexBlock block
                && block.role() == expectedRole;
    }

    public static void invalidateBeforeRemoval(ServerLevel level, BlockPos partPos, BlockState state) {
        if (!(state.getBlock() instanceof OxygenComplexBlock block)) {
            return;
        }
        Direction facing = state.getValue(OxygenComplexBlock.FACING);
        BlockPos controllerPos = controllerPos(partPos, block.role(), facing);

        BlockEntity removedEntity = level.getBlockEntity(partPos);
        boolean linkedToController = removedEntity instanceof OxygenComplexBlockEntity part
                && part.isLinkedTo(controllerPos);
        if (block.role() != OxygenComplexRole.OUTPUT
                && !state.getValue(OxygenComplexBlock.FORMED)
                && !linkedToController) {
            return;
        }

        BlockEntity controllerEntity = level.getBlockEntity(controllerPos);
        if (controllerEntity instanceof OxygenComplexBlockEntity controller
                && controller.role() == OxygenComplexRole.OUTPUT) {
            controller.backupControllerState();
        }

        clearFootprint(level, controllerPos, facing);

        // Run after the block replacement completes. If all four parts are truly gone,
        // there is no reason to keep an orphan controller snapshot forever.
        level.getServer().execute(() -> cleanupBackupIfFullyDismantled(level, controllerPos, facing));
    }

    public static void setStageActivity(ServerLevel level, BlockPos controllerPos, Direction facing,
                                        boolean intake, boolean filtration,
                                        boolean compression, boolean output) {
        setActive(level, rolePos(controllerPos, OxygenComplexRole.AIR_INTAKE, facing), intake);
        setActive(level, rolePos(controllerPos, OxygenComplexRole.FILTRATION, facing), filtration);
        setActive(level, rolePos(controllerPos, OxygenComplexRole.COMPRESSION, facing), compression);
        setActive(level, rolePos(controllerPos, OxygenComplexRole.OUTPUT, facing), output);
    }

    private static void clearFootprint(ServerLevel level, BlockPos controllerPos, Direction facing) {
        for (OxygenComplexRole role : OxygenComplexRole.values()) {
            BlockPos pos = rolePos(controllerPos, role, facing);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            boolean linkedToController = blockEntity instanceof OxygenComplexBlockEntity part
                    && part.isLinkedTo(controllerPos);
            BlockState state = level.getBlockState(pos);
            boolean expectedPart = state.getBlock() instanceof OxygenComplexBlock complexBlock
                    && complexBlock.role() == role;
            if (!linkedToController && !expectedPart) {
                continue;
            }
            if (blockEntity instanceof OxygenComplexBlockEntity part) {
                part.setStructureLink(controllerPos, false);
            }
            if (state.getBlock() instanceof OxygenComplexBlock) {
                BlockState updated = state;
                if (updated.getValue(OxygenComplexBlock.FORMED)) {
                    updated = updated.setValue(OxygenComplexBlock.FORMED, false);
                }
                if (updated.getValue(OxygenComplexBlock.ACTIVE)) {
                    updated = updated.setValue(OxygenComplexBlock.ACTIVE, false);
                }
                if (!updated.equals(state)) {
                    level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void setActive(ServerLevel level, BlockPos pos, boolean active) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof OxygenComplexBlock
                && state.getValue(OxygenComplexBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(OxygenComplexBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    private static void cleanupBackupIfFullyDismantled(ServerLevel level, BlockPos controllerPos, Direction facing) {
        for (OxygenComplexRole role : OxygenComplexRole.values()) {
            BlockPos pos = rolePos(controllerPos, role, facing);
            if (!level.hasChunkAt(pos)) {
                return;
            }
            if (level.getBlockState(pos).getBlock() instanceof OxygenComplexBlock) {
                return;
            }
        }
        OxygenComplexSavedData.get(level).remove(controllerPos);
    }

    private static Direction normalizedFacing(Direction facing) {
        return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
    }
}
