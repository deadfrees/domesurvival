package com.wasted.domesurvival.forge.progression;

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
 * Stage 3 V9 — restore the warehouse to its original successful placement.
 *
 * No X/Z movement anymore.
 * The building is restored at the original V5 origin.
 * A separate 13x19 floor is placed directly beneath it at Y=62.
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

    // Original successful V5 building position.
    private static final BlockPos BUILDING_ORIGIN = new BlockPos(-513, 63, -683);

    // Floor directly beneath the building.
    private static final BlockPos FLOOR_ORIGIN = new BlockPos(-513, 62, -683);

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
        if (!place(level, cleanup.get(), BUILDING_ORIGIN)) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        // Fill/repair the ground layer beneath the complete warehouse footprint.
        repairFloorBase(level);

        if (!place(level, floor.get(), FLOOR_ORIGIN)) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        if (!place(level, building.get(), BUILDING_ORIGIN)) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        data.markWorkshopUpgradeApplied();
        return ApplyResult.REBUILT;
    }

    /**
     * Test-only rollback for WASTED_TEST.
     *
     * Removes the canonical workshop building using the same cleanup template
     * already used by rebuild(), restores the separate floor footprint to sand,
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

        if (!place(level, cleanup.get(), BUILDING_ORIGIN)) {
            return RemovalResult.PLACEMENT_FAILED;
        }

        restoreTestGround(level);
        DomeProgressSavedData.get(level).resetWorkshopForTesting();
        return RemovalResult.REMOVED;
    }

    /**
     * The workshop has a dedicated 13x19 floor at Y=62. For a clean repeatable
     * test run, restore that exact footprint to the desert sand that existed
     * before the workshop floor template was placed.
     */
    private static void restoreTestGround(ServerLevel level) {
        for (int x = -513; x <= -501; x++) {
            for (int z = -683; z <= -665; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 62, z), Blocks.SAND.defaultBlockState());
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

        if (!place(level, floor.get(), FLOOR_ORIGIN)) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        if (!place(level, building.get(), BUILDING_ORIGIN)) {
            return ApplyResult.PLACEMENT_FAILED;
        }

        return ApplyResult.APPLIED;
    }

    /**
     * Prevent losing items if a player has started using containers inside the warehouse.
     */
    private static boolean hasStoredItems(ServerLevel level) {
        for (int x = -513; x <= -501; x++) {
            for (int y = 62; y <= 71; y++) {
                for (int z = -683; z <= -665; z++) {
                    if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof Container container) {
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
     * Only air/natural desert terrain is changed; dome/foundation blocks are untouched.
     */
    private static void repairFloorBase(ServerLevel level) {
        for (int x = -513; x <= -501; x++) {
            for (int z = -683; z <= -665; z++) {
                BlockPos pos = new BlockPos(x, 62, z);
                BlockState state = level.getBlockState(pos);

                if (state.isAir()
                        || state.is(Blocks.SAND)
                        || state.is(Blocks.SANDSTONE)
                        || state.is(Blocks.CUT_SANDSTONE)
                        || state.is(Blocks.SMOOTH_SANDSTONE)
                        || state.is(Blocks.DEAD_BUSH)
                        || state.is(Blocks.RED_WOOL)
                        || state.is(Blocks.SMOOTH_STONE)) {
                    level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
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
}
