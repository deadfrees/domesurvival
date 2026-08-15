package com.wasted.domesurvival.forge.transport.fluid;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class FluidPipeBlockEntityRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {
    public FluidPipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidPipeBlockEntity pipe, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // V50.6: intentionally unused. Fluid pipes are opaque and have no dynamic internal renderer.
    }
}
