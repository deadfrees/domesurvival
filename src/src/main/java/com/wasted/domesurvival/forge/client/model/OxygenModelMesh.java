package com.wasted.domesurvival.forge.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Creates the node structure HumanoidModel expects, but with NO vanilla body cubes.
 *
 * V3.2.2 used HumanoidModel.createMesh(), which brought vanilla armor/body cubes
 * into the custom equipment model. That is why a chest shell and shoulder blocks
 * appeared even though we only wanted goggles or a back tank.
 */
public final class OxygenModelMesh {
    private OxygenModelMesh() {
    }

    public static MeshDefinition emptyHumanoid() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return mesh;
    }
}
