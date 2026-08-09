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
 * V3.2.5:
 * Small back bottle plus continuous backpack-style harness.
 *
 * Goal from review:
 * - keep the successful back bottle;
 * - continue the straps so they clearly read as backpack straps;
 * - still no chest plate and no bulky shoulder shells.
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

        // Valve / connector.
        body.addOrReplaceChild(
                "tank_valve",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-0.75F, 0.75F, 3.0F, 1.5F, 1.25F, 1.25F),
                PartPose.ZERO
        );

        // Rear brackets.
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

        // Rear visible vertical straps.
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

        // Continuous front straps, now longer and visually clearer.
        body.addOrReplaceChild(
                "left_front_strap",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(1.85F, 0.35F, -2.18F, 0.40F, 10.60F, 0.28F),
                PartPose.ZERO
        );
        body.addOrReplaceChild(
                "right_front_strap",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(-2.25F, 0.35F, -2.18F, 0.40F, 10.60F, 0.28F),
                PartPose.ZERO
        );

        // Top bridge over shoulders, keeps the front/back straps reading as one harness.
        body.addOrReplaceChild(
                "upper_harness_bridge",
                CubeListBuilder.create()
                        .texOffs(40, 0)
                        .addBox(-2.25F, 0.15F, -2.00F, 4.50F, 0.28F, 4.10F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
