package com.wasted.domesurvival.forge.progression;

import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * Restores the warehouse at its authored local position inside the movable dome.
 * A separate 13x19 floor is placed directly beneath it.
 */
public final class WorkshopUpgradeApplier {
    public enum ApplyResult {
        APPLIED,
        REBUILT,
        ALREADY_APPLIED,
        PROJECT_NOT_COMPLETE,
        STORAGE_NOT_EMPTY,
        TEMPLATE_MISSING,
        PLACEMENT_FAILED
    }

    public enum RemovalResult {
        REMOVED,
        STORAGE_NOT_EMPTY,
        TEMPLATE_MISSING,
        PLACEMENT_FAILED
    }

    private static final ResourceLocation BUILDING_ID =
            new ResourceLocation("domesurvival", "workshop/warehouse_17x11");
    private static final ResourceLocation FLOOR_ID =
            new ResourceLocation("domesurvival", "workshop/warehouse_floor_v9");
    private static final ResourceLocation CLEANUP_ID =
            new ResourceLocation("domesurvival", "workshop/warehouse_original_cleanup_v9");

    // Authored offsets from the source dome centre. They keep the workshop in
    // the same local position when /domestart moves the dome to a new site.
    private static final int WORKSHOP_OFFSET_X = -7;
    private static final int WORKSHOP_OFFSET_Z = -42;
    private static final int WORKSHOP_WIDTH = 13;
    private static final int WORKSHOP_DEPTH = 19;
    private static final int WORKSHOP_HEIGHT = 10;

    private WorkshopUpgradeApplier() {
    }

    public static ApplyResult applyIfNeeded(ServerLevel anyLevel) {
        ServerLevel level = anyLevel.getServer().overworld();
        DomeProgressSavedData data = DomeProgressSavedData.get(level);

        if (!data.workshopComplete()) {
            return ApplyResult.PROJECT_NOT_COMPLETE;
        }

        if (data.workshopUpgradeApplied()) {
            return ApplyResult.ALREADY_APPLIED;
        }

        ApplyResult result = placeRestoredWorkshop(level);
        if (result == ApplyResult.APPLIED) {
            data.markWorkshopUpgradeApplied();
        }
        return result;
    }

    public static ApplyResult adminApply(ServerLevel anyLevel) {
        return applyIfNeeded(anyLevel);
    }

    /**
     * Restores the warehouse even if the previous experimental rebuild left it damaged.
     * It never moves the building.
     */
    public static ApplyResult rebuild(ServerLevel anyLevel) {
        ServerLevel level = anyLevel.getServer().overworld();
        DomeProgressSavedData data = DomeProgressSavedData.get(level);

        if (!data.workshopComplete()) {
            return ApplyResult.PROJECT_NOT_COMPLETE;
        }

        if (hasStoredItems(level)) {
            return ApplyResult.STORAGE_NOT_EMPTY;
        }

        Optional<StructureTemplate> building = level.getStructureManager().get(BUILDING_ID);
        Optional<StructureTemplate> floor = level.getStructureManager().get(FLOOR_ID);
        Optional<StructureTemplate> cleanup = level.getStructureManager().get(CLEANUP_ID);

        if (building.isEmpty() || floor.isEmpty() || cleanup.isEmpty()) {
            return ApplyResult.TEMPLATE_MISSING;
        }

        // Remove only positions that belong to the canonical warehouse template.
        if (!place(level, cleanup.get(), buildingOrigin(level))) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        // Fill/repair the ground layer beneath the complete warehouse footprint.
        repairFloorBase(level);

        if (!place(level, floor.get(), floorOrigin(level))) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        if (!place(level, building.get(), buildingOrigin(level))) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        data.markWorkshopUpgradeApplied();
        return ApplyResult.REBUILT;
    }

    /**
     * Test-only rollback for WASTED_TEST.
     *
     * Removes the canonical workshop building using the same cleanup template
     * already used by rebuild(), restores the separate floor footprint to grass,
     * and clears the Java progression flag so Stage 02 can build it again.
     *
     * Stored container contents are protected: reset is refused while any
     * workshop container contains items.
     */
    public static RemovalResult removeForTesting(ServerLevel anyLevel) {
        ServerLevel level = anyLevel.getServer().overworld();

        if (hasStoredItems(level)) {
            return RemovalResult.STORAGE_NOT_EMPTY;
        }

        Optional<StructureTemplate> cleanup = level.getStructureManager().get(CLEANUP_ID);
        if (cleanup.isEmpty()) {
            return RemovalResult.TEMPLATE_MISSING;
        }

        if (!place(level, cleanup.get(), buildingOrigin(level))) {
            return RemovalResult.PLACEMENT_FAILED;
        }

        restoreTestGround(level);
        DomeProgressSavedData.get(level).resetWorkshopForTesting();
        return RemovalResult.REMOVED;
    }

    /**
     * Removes the authored late-game workshop from a freshly transferred dome.
     * Unlike the admin rollback, this is run before players can use containers,
     * so it deliberately does not reject the cleanup because of template loot.
     */
    public static RemovalResult prepareInitialState(ServerLevel anyLevel) {
        ServerLevel level = anyLevel.getServer().overworld();
        Optional<StructureTemplate> cleanup = level.getStructureManager().get(CLEANUP_ID);
        if (cleanup.isEmpty()) {
            return RemovalResult.TEMPLATE_MISSING;
        }
        if (!place(level, cleanup.get(), buildingOrigin(level))) {
            return RemovalResult.PLACEMENT_FAILED;
        }

        restoreTestGround(level);
        DomeProgressSavedData.get(level).resetWorkshopForTesting();
        return RemovalResult.REMOVED;
    }

    /**
     * For a clean repeatable test run, restore the exact 13x19 footprint to
     * the living starter soil that exists before the workshop quest is done.
     */
    private static void restoreTestGround(ServerLevel level) {
        BlockPos floorOrigin = floorOrigin(level);
        for (int dx = 0; dx < WORKSHOP_WIDTH; dx++) {
            for (int dz = 0; dz < WORKSHOP_DEPTH; dz++) {
                BlockPos top = floorOrigin.offset(dx, 0, dz);
                level.setBlockAndUpdate(top, Blocks.GRASS_BLOCK.defaultBlockState());
                BlockPos soil = top.below();
                if (level.getBlockState(soil).isAir() || level.getBlockState(soil).is(Blocks.SAND)) {
                    level.setBlockAndUpdate(soil, Blocks.DIRT.defaultBlockState());
                }
            }
        }
    }

    private static ApplyResult placeRestoredWorkshop(ServerLevel level) {
        Optional<StructureTemplate> building = level.getStructureManager().get(BUILDING_ID);
        Optional<StructureTemplate> floor = level.getStructureManager().get(FLOOR_ID);

        if (building.isEmpty() || floor.isEmpty()) {
            return ApplyResult.TEMPLATE_MISSING;
        }

        repairFloorBase(level);

        if (!place(level, floor.get(), floorOrigin(level))) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        if (!place(level, building.get(), buildingOrigin(level))) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        return ApplyResult.APPLIED;
    }

    /**
     * Prevent losing items if a player has started using containers inside the warehouse.
     */
    private static boolean hasStoredItems(ServerLevel level) {
        BlockPos origin = floorOrigin(level);
        for (int dx = 0; dx < WORKSHOP_WIDTH; dx++) {
            for (int dy = 0; dy < WORKSHOP_HEIGHT; dy++) {
                for (int dz = 0; dz < WORKSHOP_DEPTH; dz++) {
                    if (level.getBlockEntity(origin.offset(dx, dy, dz)) instanceof Container container) {
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            if (!container.getItem(slot).isEmpty()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Repairs holes left by earlier experiments and ensures the floor has support.
     * Only air/natural terrain is changed; dome/foundation blocks are untouched.
     * The portable starter dome uses living soil, so no sand is reintroduced
     * when the quest-built workshop appears at its centre-relative position.
     */
    private static void repairFloorBase(ServerLevel level) {
        BlockPos origin = floorOrigin(level);
        for (int dx = 0; dx < WORKSHOP_WIDTH; dx++) {
            for (int dz = 0; dz < WORKSHOP_DEPTH; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);

                if (state.isAir()
                        || state.is(Blocks.SAND)
                        || state.is(Blocks.SANDSTONE)
                        || state.is(Blocks.CUT_SANDSTONE)
                        || state.is(Blocks.SMOOTH_SANDSTONE)
                        || state.is(Blocks.DEAD_BUSH)
                        || state.is(Blocks.RED_WOOL)
                        || state.is(Blocks.SMOOTH_STONE)) {
                    level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                }
            }
        }
    }

    private static boolean place(ServerLevel level, StructureTemplate template, BlockPos origin) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);

        return template.placeInWorld(
                level,
                origin,
                origin,
                settings,
                level.getRandom(),
                2
        );
    }

    private static BlockPos buildingOrigin(ServerLevel level) {
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        return new BlockPos(
                spec.centerX() + WORKSHOP_OFFSET_X,
                spec.baseY() + 1,
                spec.centerZ() + WORKSHOP_OFFSET_Z
        );
    }

    private static BlockPos floorOrigin(ServerLevel level) {
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        return new BlockPos(
                spec.centerX() + WORKSHOP_OFFSET_X,
                spec.baseY(),
                spec.centerZ() + WORKSHOP_OFFSET_Z
        );
    }
}
