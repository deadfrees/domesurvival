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
 * Small 3D back bottle with thin harness straps.
 *
 * Front stays almost clean: only narrow strap lines may be visible near the torso edges.
 * There is still no chest plate and no shoulder wing geometry.
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

        // Main bottle.
        body.addOrReplaceChild(
                "tank_body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 2.0F, 2.25F, 4.0F, 7.0F, 2.75F),
                PartPose.ZERO
        );

        // Valve / top connector.
        body.addOrReplaceChild(
                "tank_valve",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-0.75F, 0.75F, 3.0F, 1.5F, 1.25F, 1.25F),
                PartPose.ZERO
        );

        // Rear brackets holding the tank to the harness.
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

        // Thin visible rear straps framing the tank.
        body.addOrReplaceChild(
                "left_back_strap",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(1.90F, 0.40F, 1.95F, 0.35F, 9.40F, 0.35F),
                PartPose.ZERO
        );
        body.addOrReplaceChild(
                "right_back_strap",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-2.25F, 0.40F, 1.95F, 0.35F, 9.40F, 0.35F),
                PartPose.ZERO
        );

        // Thin front straps near torso edges (not a chest plate).
        body.addOrReplaceChild(
                "left_front_strap",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(1.85F, 0.55F, -2.15F, 0.35F, 9.00F, 0.25F),
                PartPose.ZERO
        );
        body.addOrReplaceChild(
                "right_front_strap",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(-2.20F, 0.55F, -2.15F, 0.35F, 9.00F, 0.25F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
