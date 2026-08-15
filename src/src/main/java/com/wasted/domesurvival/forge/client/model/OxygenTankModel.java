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

/** Three Curios back-tank sizes. V23 intentionally swaps small/medium visuals. */
public final class OxygenTankModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation SMALL_LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_tank_small_v23"), "main");
    public static final ModelLayerLocation MEDIUM_LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_tank_medium_v23"), "main");
    public static final ModelLayerLocation LARGE_LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_tank_large_v23"), "main");
    public static final ModelLayerLocation LAYER_LOCATION = SMALL_LAYER_LOCATION;

    public OxygenTankModel(ModelPart root) { super(root); }

    public static LayerDefinition createBodyLayer() { return createSmallBodyLayer(); }

    // Small item uses the old medium visual.
    public static LayerDefinition createSmallBodyLayer() {
        return createBodyLayer(5.25F, 8.75F, 3.05F, 1.15F);
    }

    // Medium item uses the old small visual.
    public static LayerDefinition createMediumBodyLayer() {
        return createBodyLayer(4.0F, 7.0F, 2.75F, 2.0F);
    }

    // Large is deliberately a little larger than before.
    public static LayerDefinition createLargeBodyLayer() {
        return createBodyLayer(7.0F, 11.25F, 3.70F, -0.05F);
    }

    private static LayerDefinition createBodyLayer(float bottleWidth, float bottleHeight, float bottleDepth, float bottleY) {
        MeshDefinition mesh = OxygenModelMesh.emptyHumanoid();
        PartDefinition body = mesh.getRoot().getChild("body");
        float left = -bottleWidth / 2.0F;
        float frontZ = 2.25F;

        body.addOrReplaceChild("tank_body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(left, bottleY, frontZ, bottleWidth, bottleHeight, bottleDepth),
                PartPose.ZERO);

        body.addOrReplaceChild("tank_valve",
                CubeListBuilder.create().texOffs(20, 0)
                        .addBox(-0.75F, Math.max(0.15F, bottleY - 1.25F), frontZ + 0.75F, 1.5F, 1.25F, 1.25F),
                PartPose.ZERO);

        float upperY = bottleY + 0.45F;
        float lowerY = bottleY + bottleHeight - 1.3F;
        float bracketWidth = bottleWidth + 1.0F;
        body.addOrReplaceChild("upper_bracket",
                CubeListBuilder.create().texOffs(20, 4)
                        .addBox(-bracketWidth / 2.0F, upperY, 2.05F, bracketWidth, 0.60F, 0.45F),
                PartPose.ZERO);
        body.addOrReplaceChild("lower_bracket",
                CubeListBuilder.create().texOffs(20, 6)
                        .addBox(-bracketWidth / 2.0F, lowerY, 2.05F, bracketWidth, 0.60F, 0.45F),
                PartPose.ZERO);

        body.addOrReplaceChild("left_back_strap",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(1.90F, 0.40F, 1.95F, 0.35F, 9.40F, 0.35F), PartPose.ZERO);
        body.addOrReplaceChild("right_back_strap",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-2.25F, 0.40F, 1.95F, 0.35F, 9.40F, 0.35F), PartPose.ZERO);
        body.addOrReplaceChild("left_front_strap",
                CubeListBuilder.create().texOffs(36, 0)
                        .addBox(1.85F, 0.35F, -2.18F, 0.40F, 10.60F, 0.28F), PartPose.ZERO);
        body.addOrReplaceChild("right_front_strap",
                CubeListBuilder.create().texOffs(36, 0)
                        .addBox(-2.25F, 0.35F, -2.18F, 0.40F, 10.60F, 0.28F), PartPose.ZERO);
        body.addOrReplaceChild("upper_harness_bridge",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(-2.25F, 0.15F, -2.00F, 4.50F, 0.28F, 4.10F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }
}
