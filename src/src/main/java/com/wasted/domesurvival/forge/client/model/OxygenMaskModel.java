package com.wasted.domesurvival.forge.client.model;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Intentionally empty in V3.2.6.
 *
 * OxygenMaskItem is still an ArmorItem for the HEAD equipment slot, but the actual
 * visible mask is rendered by M40MaskRenderLayer from the exact converted GLB mesh.
 */
public final class OxygenMaskModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_mask_v326_empty"),
                    "main"
            );

    public OxygenMaskModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = OxygenModelMesh.emptyHumanoid();
        return LayerDefinition.create(mesh, 64, 32);
    }
}
