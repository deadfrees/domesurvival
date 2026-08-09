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

public final class OxygenMaskModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_mask"), "main");

    public OxygenMaskModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.0F), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");

        head.addOrReplaceChild(
                "goggles_band",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -5.0F, -4.0F, 8.0F, 3.0F, 1.0F, new CubeDeformation(-0.2F)),
                PartPose.ZERO
        );

        head.addOrReplaceChild(
                "strap_left",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(3.0F, -4.5F, -3.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(-0.2F)),
                PartPose.ZERO
        );

        head.addOrReplaceChild(
                "strap_right",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .mirror()
                        .addBox(-4.0F, -4.5F, -3.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(-0.2F))
                        .mirror(false),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
