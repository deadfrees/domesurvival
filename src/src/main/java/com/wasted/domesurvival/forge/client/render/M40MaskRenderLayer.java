package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.item.ModItems;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Renders the project-provided M40 mesh as a true triangle mesh attached to the
 * animated player head. No GLB parser is needed at runtime: the source GLB was
 * converted to static vertex/index data at development time.
 */
public final class M40MaskRenderLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(
                    DomeSurvival.MOD_ID,
                    "textures/models/armor/m40_gasmask_domesurvival.png"
            );

    public M40MaskRenderLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent
    ) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK.get())) {
            return;
        }

        poseStack.pushPose();

        // Follow the exact animated head transform: yaw, pitch, crouch/swim model pose, etc.
        getParentModel().head.translateAndRotate(poseStack);

        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        M40MaskMesh.render(poseStack, consumer, packedLight);

        poseStack.popPose();
    }
}
