package com.wasted.domesurvival.forge.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/** Client-only lazy cache. No server/gameplay state is stored here. */
public final class OxygenEquipmentModelCache {
    private static HumanoidModel<LivingEntity> maskModel;
    private static HumanoidModel<LivingEntity> tankModel;

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
        if (tankModel == null) {
            tankModel = new OxygenTankModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(OxygenTankModel.LAYER_LOCATION)
            );
        }
        return tankModel;
    }
}
