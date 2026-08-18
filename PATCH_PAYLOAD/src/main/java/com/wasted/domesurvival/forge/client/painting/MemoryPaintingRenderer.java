package com.wasted.domesurvival.forge.client.painting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wasted.domesurvival.forge.entity.MemoryPaintingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Delegates rendering to Minecraft's own PaintingRenderer.
 * This keeps lighting, atlas lookup, UV layout and back texture vanilla.
 */
public final class MemoryPaintingRenderer extends EntityRenderer<MemoryPaintingEntity> {
    private final PaintingRenderer delegate;

    public MemoryPaintingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.delegate = new PaintingRenderer(context);
    }

    @Override
    public void render(
            MemoryPaintingEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        this.delegate.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MemoryPaintingEntity entity) {
        return this.delegate.getTextureLocation(entity);
    }
}
