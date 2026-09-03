package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.machine.sieve.SandSieveBlock;
import com.wasted.domesurvival.forge.machine.sieve.SandSieveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * The sieve tray is transformed every rendered frame. This deliberately uses
 * partial ticks and trigonometric motion instead of a stepped texture animation.
 */
public final class SandSieveBlockEntityRenderer implements BlockEntityRenderer<SandSieveBlockEntity> {
    public SandSieveBlockEntityRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(SandSieveBlockEntity sieve, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack mesh = sieve.meshForRendering();
        boolean active = sieve.getBlockState().getValue(SandSieveBlock.ACTIVE);
        double time = sieve.getLevel() == null ? partialTick
                : sieve.getLevel().getGameTime() + partialTick;
        float phase = (float) (time * 0.72D);
        float slideX = active ? (float) Math.sin(phase * 2.1F) * 0.050F : 0.0F;
        float slideZ = active ? (float) Math.sin(phase * 1.37F + 0.8F) * 0.026F : 0.0F;
        float tilt = active ? (float) Math.sin(phase * 1.63F) * 1.7F : 0.0F;

        if (!mesh.isEmpty()) {
            pose.pushPose();
            // The mesh is visible in the upper tray again, but remains clearly
            // separated from both the wooden rim and the sand surface.
            pose.translate(0.5F + slideX, 0.735F, 0.5F + slideZ);
            pose.mulPose(Axis.YP.rotationDegrees(sieve.getBlockState().getValue(SandSieveBlock.FACING).toYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(90.0F + tilt));
            pose.scale(0.72F, 0.72F, 0.72F);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    mesh, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    pose, buffers, sieve.getLevel(), (int) sieve.getBlockPos().asLong()
            );
            pose.popPose();
        }

        if (sieve.hasSandForRendering()) {
            float progress = active ? sieve.animationProgress(partialTick) : 1.0F;
            float eased = progress * progress * (3.0F - 2.0F * progress);
            float sandY = active ? 0.82F + (0.30F - 0.82F) * eased : 0.30F;
            float sandScale = active ? 0.50F - 0.10F * eased : 0.40F;
            pose.pushPose();
            // A complete cube is loaded through the upper tray, then descends
            // smoothly into the open frame instead of resting above the machine.
            pose.translate(0.5F + slideX, sandY, 0.5F + slideZ);
            pose.mulPose(Axis.ZP.rotationDegrees(tilt * 0.35F));
            pose.translate(-sandScale * 0.5F, 0.0F, -sandScale * 0.5F);
            pose.scale(sandScale, sandScale, sandScale);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    Blocks.SAND.defaultBlockState(), pose, buffers, packedLight, OverlayTexture.NO_OVERLAY
            );
            pose.popPose();
        }
    }
}
