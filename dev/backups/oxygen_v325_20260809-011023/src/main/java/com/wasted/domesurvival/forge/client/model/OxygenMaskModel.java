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
 * Compact protective goggles plus a small respirator cartridge under the visor.
 *
 * Design constraints:
 * - goggles stay thin and close to the face;
 * - the respirator is volumetric, but much smaller than the failed V3.2.2 face box;
 * - no helmet shell around the head.
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

        // Thin visor, almost flush with the face.
        head.addOrReplaceChild(
                "visor",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -5.25F, -4.10F, 8.0F, 2.25F, 0.25F),
                PartPose.ZERO
        );

        // Thin arms of the goggles, inside the head silhouette.
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

        // Main respirator body under the visor.
        head.addOrReplaceChild(
                "respirator",
                CubeListBuilder.create()
                        .texOffs(24, 0)
                        .addBox(-1.75F, -2.55F, -5.10F, 3.50F, 1.75F, 1.60F),
                PartPose.ZERO
        );

        // Lower cartridge/lip so the respirator reads as a separate volume.
        head.addOrReplaceChild(
                "respirator_lower",
                CubeListBuilder.create()
                        .texOffs(24, 5)
                        .addBox(-1.25F, -1.00F, -5.00F, 2.50F, 0.80F, 1.25F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
