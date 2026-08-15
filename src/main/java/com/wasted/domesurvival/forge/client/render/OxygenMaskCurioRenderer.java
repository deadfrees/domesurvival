package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Curios renderer for the exact project-provided M40 mask mesh.
 *
 * Curios invokes this only while the mask is actually equipped, avoiding a capability lookup
 * for every rendered player. The mesh follows the wearer's animated HumanoidModel head.
 */
public final class OxygenMaskCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(
                    DomeSurvival.MOD_ID,
                    "textures/models/armor/m40_gasmask_domesurvival.png"
            );

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource bufferSource,
            int packedLight,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        poseStack.pushPose();
        humanoidModel.head.translateAndRotate(poseStack);

        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        M40MaskMesh.render(poseStack, consumer, packedLight);

        poseStack.popPose();
    }
}
