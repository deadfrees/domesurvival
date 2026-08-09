package com.wasted.domesurvival.forge.client.model;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Small 3D oxygen bottle mounted entirely behind the torso.
 *
 * There is deliberately nothing on the chest or shoulders.
 */
public final class OxygenTankModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_tank_v323"), "main");

    public OxygenTankModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = OxygenModelMesh.emptyHumanoid();
        PartDefinition body = mesh.getRoot().getChild("body");

        // Main bottle: narrow and clearly behind the player's torso.
        body.addOrReplaceChild(
                "tank_body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 2.0F, 2.25F, 4.0F, 7.0F, 2.75F),
                PartPose.ZERO
        );

        // Valve/cap.
        body.addOrReplaceChild(
                "tank_valve",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-0.75F, 0.75F, 3.0F, 1.5F, 1.25F, 1.25F),
                PartPose.ZERO
        );

        // Two short rear brackets. They never wrap around the chest.
        body.addOrReplaceChild(
                "upper_bracket",
                CubeListBuilder.create()
                        .texOffs(16, 4)
                        .addBox(-2.5F, 2.4F, 2.05F, 5.0F, 0.60F, 0.45F),
                PartPose.ZERO
        );
        body.addOrReplaceChild(
                "lower_bracket",
                CubeListBuilder.create()
                        .texOffs(16, 6)
                        .addBox(-2.5F, 7.7F, 2.05F, 5.0F, 0.60F, 0.45F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
