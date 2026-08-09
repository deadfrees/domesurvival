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
 * Gas mask rework inspired by the uploaded M40 gas mask reference.
 *
 * Adapted for Minecraft readability and the DomeSurvival cyan/graphite palette:
 * - wider sealed visor;
 * - compact side housings;
 * - central front filter;
 * - no full helmet shell.
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

        // Main visor band: slightly taller than V3.2.4, still close to the face.
        head.addOrReplaceChild(
                "visor",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -5.55F, -4.15F, 8.0F, 3.10F, 0.35F),
                PartPose.ZERO
        );

        // Thin side arms/strap housings.
        head.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(3.75F, -5.00F, -3.65F, 0.30F, 1.60F, 6.20F),
                PartPose.ZERO
        );
        head.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-4.05F, -5.00F, -3.65F, 0.30F, 1.60F, 6.20F),
                PartPose.ZERO
        );

        // Side cheek seal blocks, reading closer to a gas mask body.
        head.addOrReplaceChild(
                "left_cheek",
                CubeListBuilder.create()
                        .texOffs(24, 0)
                        .addBox(2.10F, -3.45F, -4.75F, 1.30F, 1.60F, 1.35F),
                PartPose.ZERO
        );
        head.addOrReplaceChild(
                "right_cheek",
                CubeListBuilder.create()
                        .texOffs(24, 0)
                        .addBox(-3.40F, -3.45F, -4.75F, 1.30F, 1.60F, 1.35F),
                PartPose.ZERO
        );

        // Central mask body / intake section.
        head.addOrReplaceChild(
                "mask_body",
                CubeListBuilder.create()
                        .texOffs(24, 4)
                        .addBox(-2.00F, -3.20F, -5.05F, 4.00F, 2.35F, 1.75F),
                PartPose.ZERO
        );

        // Front filter cartridge, clearly volumetric.
        head.addOrReplaceChild(
                "filter",
                CubeListBuilder.create()
                        .texOffs(24, 9)
                        .addBox(-0.90F, -2.15F, -6.10F, 1.80F, 1.80F, 1.20F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
