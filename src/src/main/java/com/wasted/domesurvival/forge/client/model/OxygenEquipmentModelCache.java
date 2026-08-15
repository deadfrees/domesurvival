package com.wasted.domesurvival.forge.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/** Client-only lazy cache. No server/gameplay state is stored here. */
public final class OxygenEquipmentModelCache {
    private static HumanoidModel<LivingEntity> maskModel;
    private static HumanoidModel<LivingEntity> smallTankModel;
    private static HumanoidModel<LivingEntity> mediumTankModel;
    private static HumanoidModel<LivingEntity> largeTankModel;

    private OxygenEquipmentModelCache() {
    }

    public static HumanoidModel<LivingEntity> maskModel() {
        if (maskModel == null) {
            maskModel = new OxygenMaskModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(OxygenMaskModel.LAYER_LOCATION)
            );
        }
        return maskModel;
    }

    public static HumanoidModel<LivingEntity> tankModel() {
        return smallTankModel();
    }

    public static HumanoidModel<LivingEntity> smallTankModel() {
        if (smallTankModel == null) {
            smallTankModel = new OxygenTankModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(OxygenTankModel.SMALL_LAYER_LOCATION)
            );
        }
        return smallTankModel;
    }

    public static HumanoidModel<LivingEntity> mediumTankModel() {
        if (mediumTankModel == null) {
            mediumTankModel = new OxygenTankModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(OxygenTankModel.MEDIUM_LAYER_LOCATION)
            );
        }
        return mediumTankModel;
    }

    public static HumanoidModel<LivingEntity> largeTankModel() {
        if (largeTankModel == null) {
            largeTankModel = new OxygenTankModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(OxygenTankModel.LARGE_LAYER_LOCATION)
            );
        }
        return largeTankModel;
    }
}
