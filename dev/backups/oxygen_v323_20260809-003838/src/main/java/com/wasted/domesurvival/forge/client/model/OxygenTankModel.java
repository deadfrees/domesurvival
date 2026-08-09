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

public final class OxygenTankModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(DomeSurvival.MOD_ID, "oxygen_tank"), "main");

    public OxygenTankModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.0F), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");

        body.addOrReplaceChild(
                "back_tank",
                CubeListBuilder.create()
                        .texOffs(24, 0)
                        .addBox(-2.0F, 2.0F, 2.2F, 4.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        body.addOrReplaceChild(
                "tank_cap",
                CubeListBuilder.create()
                        .texOffs(38, 0)
                        .addBox(-1.0F, 0.8F, 2.6F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        body.addOrReplaceChild(
                "left_back_strap",
                CubeListBuilder.create()
                        .texOffs(46, 0)
                        .addBox(1.6F, 1.0F, 1.6F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.1F)),
                PartPose.ZERO
        );

        body.addOrReplaceChild(
                "right_back_strap",
                CubeListBuilder.create()
                        .texOffs(46, 0)
                        .mirror()
                        .addBox(-2.6F, 1.0F, 1.6F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.1F))
                        .mirror(false),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
