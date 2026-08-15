package com.wasted.domesurvival.forge.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Global progression state for Dome Survival.
 * Stored in the Overworld so all players share one base state.
 */
public final class DomeProgressSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_progression";

    private int baseStage;

    private int workshopIron;
    private int workshopCopper;
    private int workshopRedstone;
    private boolean workshopComplete;
    private boolean workshopUpgradeApplied;

    public static DomeProgressSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                DomeProgressSavedData::load,
                DomeProgressSavedData::new,
                DATA_NAME
        );
    }

    public static DomeProgressSavedData load(CompoundTag tag) {
        DomeProgressSavedData data = new DomeProgressSavedData();

        data.baseStage = Math.max(0, tag.getInt("BaseStage"));

        CompoundTag workshop = tag.getCompound("Workshop");
        data.workshopIron = clamp(workshop.getInt("Iron"), 0, WorkshopProject.IRON_REQUIRED);
        data.workshopCopper = clamp(workshop.getInt("Copper"), 0, WorkshopProject.COPPER_REQUIRED);
        data.workshopRedstone = clamp(workshop.getInt("Redstone"), 0, WorkshopProject.REDSTONE_REQUIRED);
        data.workshopComplete = workshop.getBoolean("Complete");
        data.workshopUpgradeApplied = workshop.getBoolean("UpgradeApplied");

        // Migrates worlds completed on Stage 2.
        if (data.hasAllWorkshopResources()) {
            data.workshopComplete = true;
            data.baseStage = Math.max(data.baseStage, 1);
        }

        return data;
    }

    public int baseStage() {
        return baseStage;
    }

    public int workshopIron() {
        return workshopIron;
    }

    public int workshopCopper() {
        return workshopCopper;
    }

    public int workshopRedstone() {
        return workshopRedstone;
    }

    public boolean workshopComplete() {
        return workshopComplete;
    }

    public boolean workshopUpgradeApplied() {
        return workshopUpgradeApplied;
    }

    public int remainingIron() {
        return Math.max(0, WorkshopProject.IRON_REQUIRED - workshopIron);
    }

    public int remainingCopper() {
        return Math.max(0, WorkshopProject.COPPER_REQUIRED - workshopCopper);
    }

    public int remainingRedstone() {
        return Math.max(0, WorkshopProject.REDSTONE_REQUIRED - workshopRedstone);
    }

    public void addWorkshopContribution(int iron, int copper, int redstone) {
        if (workshopComplete) {
            return;
        }

        workshopIron = clamp(workshopIron + Math.max(0, iron), 0, WorkshopProject.IRON_REQUIRED);
        workshopCopper = clamp(workshopCopper + Math.max(0, copper), 0, WorkshopProject.COPPER_REQUIRED);
        workshopRedstone = clamp(workshopRedstone + Math.max(0, redstone), 0, WorkshopProject.REDSTONE_REQUIRED);

        if (hasAllWorkshopResources()) {
            workshopComplete = true;
            baseStage = Math.max(baseStage, 1);
        }

        setDirty();
    }

    public void markWorkshopUpgradeApplied() {
        if (workshopUpgradeApplied) {
            return;
        }
        workshopUpgradeApplied = true;
        setDirty();
    }

    /**
     * Test reset intentionally resets project resources, but never claims
     * an already-built physical upgrade is absent.
     */
    public void resetWorkshopProgress() {
        workshopIron = 0;
        workshopCopper = 0;
        workshopRedstone = 0;
        workshopComplete = false;

        if (!workshopUpgradeApplied && baseStage == 1) {
            baseStage = 0;
        }

        setDirty();
    }

    /**
     * Full test reset used together with physical workshop removal.
     * Unlike resetWorkshopProgress(), this also allows the upgrade to be
     * built again after the project is completed during the next test run.
     */
    public void resetWorkshopForTesting() {
        workshopIron = 0;
        workshopCopper = 0;
        workshopRedstone = 0;
        workshopComplete = false;
        workshopUpgradeApplied = false;
        baseStage = 0;
        setDirty();
    }

    private boolean hasAllWorkshopResources() {
        return workshopIron >= WorkshopProject.IRON_REQUIRED
                && workshopCopper >= WorkshopProject.COPPER_REQUIRED
                && workshopRedstone >= WorkshopProject.REDSTONE_REQUIRED;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("BaseStage", baseStage);

        CompoundTag workshop = new CompoundTag();
        workshop.putInt("Iron", workshopIron);
        workshop.putInt("Copper", workshopCopper);
        workshop.putInt("Redstone", workshopRedstone);
        workshop.putBoolean("Complete", workshopComplete);
        workshop.putBoolean("UpgradeApplied", workshopUpgradeApplied);
        tag.put("Workshop", workshop);

        return tag;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
