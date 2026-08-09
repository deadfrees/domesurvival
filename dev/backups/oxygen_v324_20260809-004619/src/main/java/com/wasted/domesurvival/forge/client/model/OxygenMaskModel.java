package com.wasted.domesurvival.forge.client.model;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Thin protective goggles only.
 *
 * No helmet cube, respirator box, chest, arms or shoulders are rendered.
 * The visor sits almost flush with the vanilla face to avoid the oversized-mask look.
 */
public final class OxygenMaskModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_mask_v323"), "main");

    public OxygenMaskModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = OxygenModelMesh.emptyHumanoid();
        PartDefinition head = mesh.getRoot().getChild("head");

        // 8 px wide, 2 px high, only 0.25 px deep.
        // Front face of the vanilla head is z=-4.0; this is only 0.10 px outward.
        head.addOrReplaceChild(
                "visor",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -4.0F, -5.25F, -4.10F,
                                8.0F, 2.25F, 0.25F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.ZERO
        );

        // Thin side arms, like safety glasses. They remain within the head silhouette.
        head.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(0, 4)
                        .addBox(3.80F, -4.80F, -3.75F, 0.20F, 1.20F, 6.50F),
                PartPose.ZERO
        );
        head.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 4)
                        .addBox(-4.00F, -4.80F, -3.75F, 0.20F, 1.20F, 6.50F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
