package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexBlock;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexBlockEntity;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexPortLayout;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class OxygenComplexPortRenderer implements BlockEntityRenderer<OxygenComplexBlockEntity> {
    private static final ResourceLocation PORT_OFF =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/block/oxygen_complex/port_off.png");
    private static final ResourceLocation PORT_INPUT =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/block/oxygen_complex/port_input.png");
    private static final ResourceLocation PORT_OUTPUT =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/block/oxygen_complex/port_output.png");
    private static final ResourceLocation BACK_PORT_OFF =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/block/energy_buffer/back.png");
    private static final ResourceLocation BACK_PORT_INPUT =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/block/energy_buffer/side_input.png");
    private static final ResourceLocation BACK_PORT_OUTPUT =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/block/energy_buffer/side_output.png");

    private static final float MIN = 0.31F;
    private static final float MAX = 0.69F;
    private static final float EPS = 0.002F;

    public OxygenComplexPortRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            OxygenComplexBlockEntity part,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay
    ) {
        BlockState state = part.getBlockState();
        if (!(state.getBlock() instanceof OxygenComplexBlock)
                || !state.getValue(OxygenComplexBlock.FORMED)) {
            return;
        }

        OxygenComplexBlockEntity controller = part.getControllerEntity();
        if (controller == null || !controller.isFormed()) {
            return;
        }

        Direction facing = part.getMachineFacing();
        for (RelativeSide relativeSide : RelativeSide.values()) {
            if (!OxygenComplexPortLayout.isPhysicalPort(relativeSide)
                    || OxygenComplexPortLayout.hostRole(relativeSide) != part.role()) {
                continue;
            }

            SideMode mode = controller.getSideMode(relativeSide);

            if (relativeSide == RelativeSide.TOP) {
                ResourceLocation topTexture = switch (mode) {
                    case INPUT -> BACK_PORT_INPUT;
                    case OUTPUT, BOTH -> BACK_PORT_OUTPUT;
                    case DISABLED -> BACK_PORT_OFF;
                };

                VertexConsumer topConsumer =
                        bufferSource.getBuffer(RenderType.entityCutoutNoCull(topTexture));
                renderTopAsRearConnectorTexture(
                        topConsumer,
                        poseStack.last(),
                        combinedLight,
                        combinedOverlay
                );
                continue;
            }

            ResourceLocation texture = switch (mode) {
                case INPUT -> PORT_INPUT;
                case OUTPUT, BOTH -> PORT_OUTPUT;
                case DISABLED -> PORT_OFF;
            };

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
            Direction worldSide = relativeSide.resolve(facing);
            if (relativeSide == RelativeSide.FRONT) {
                renderFrontFace(
                        consumer,
                        poseStack.last(),
                        worldSide,
                        combinedLight,
                        combinedOverlay
                );
            } else {
                renderFace(
                        consumer,
                        poseStack.last(),
                        worldSide,
                        combinedLight,
                        combinedOverlay
                );
            }
        }
    }

    /**
     * The top port deliberately reuses the complete rear-connector texture.
     * It covers the full 1x1 top face, so the result is visually identical
     * to the rear Energy Buffer style instead of the old small blue square.
     */
    private static void renderTopAsRearConnectorTexture(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int light,
            int overlay
    ) {
        quad(
                consumer, pose,
                0F, 1F + EPS, 0F, 0F, 1F,
                1F, 1F + EPS, 0F, 1F, 1F,
                1F, 1F + EPS, 1F, 1F, 0F,
                0F, 1F + EPS, 1F, 0F, 0F,
                0F, 1F, 0F,
                light, overlay
        );
    }

    private static void renderFrontFace(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Direction side,
            int light,
            int overlay
    ) {
        final float h0 = 0.39F;
        final float h1 = 0.61F;
        final float y0 = 0.06F;
        final float y1 = 0.28F;

        switch (side) {
            case NORTH -> quad(
                    consumer, pose,
                    h0, y0, -EPS, 0F, 1F,
                    h1, y0, -EPS, 1F, 1F,
                    h1, y1, -EPS, 1F, 0F,
                    h0, y1, -EPS, 0F, 0F,
                    0F, 0F, -1F,
                    light, overlay
            );
            case SOUTH -> quad(
                    consumer, pose,
                    h1, y0, 1F + EPS, 0F, 1F,
                    h0, y0, 1F + EPS, 1F, 1F,
                    h0, y1, 1F + EPS, 1F, 0F,
                    h1, y1, 1F + EPS, 0F, 0F,
                    0F, 0F, 1F,
                    light, overlay
            );
            case WEST -> quad(
                    consumer, pose,
                    -EPS, y0, h1, 0F, 1F,
                    -EPS, y0, h0, 1F, 1F,
                    -EPS, y1, h0, 1F, 0F,
                    -EPS, y1, h1, 0F, 0F,
                    -1F, 0F, 0F,
                    light, overlay
            );
            case EAST -> quad(
                    consumer, pose,
                    1F + EPS, y0, h0, 0F, 1F,
                    1F + EPS, y0, h1, 1F, 1F,
                    1F + EPS, y1, h1, 1F, 0F,
                    1F + EPS, y1, h0, 0F, 0F,
                    1F, 0F, 0F,
                    light, overlay
            );
            default -> {
                // FRONT is always horizontal for this machine.
            }
        }
    }

    private static void renderFace(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Direction side,
            int light,
            int overlay
    ) {
        switch (side) {
            case NORTH -> quad(
                    consumer, pose,
                    MIN, MIN, -EPS, 0F, 1F,
                    MAX, MIN, -EPS, 1F, 1F,
                    MAX, MAX, -EPS, 1F, 0F,
                    MIN, MAX, -EPS, 0F, 0F,
                    0F, 0F, -1F,
                    light, overlay
            );
            case SOUTH -> quad(
                    consumer, pose,
                    MAX, MIN, 1F + EPS, 0F, 1F,
                    MIN, MIN, 1F + EPS, 1F, 1F,
                    MIN, MAX, 1F + EPS, 1F, 0F,
                    MAX, MAX, 1F + EPS, 0F, 0F,
                    0F, 0F, 1F,
                    light, overlay
            );
            case WEST -> quad(
                    consumer, pose,
                    -EPS, MIN, MAX, 0F, 1F,
                    -EPS, MIN, MIN, 1F, 1F,
                    -EPS, MAX, MIN, 1F, 0F,
                    -EPS, MAX, MAX, 0F, 0F,
                    -1F, 0F, 0F,
                    light, overlay
            );
            case EAST -> quad(
                    consumer, pose,
                    1F + EPS, MIN, MIN, 0F, 1F,
                    1F + EPS, MIN, MAX, 1F, 1F,
                    1F + EPS, MAX, MAX, 1F, 0F,
                    1F + EPS, MAX, MIN, 0F, 0F,
                    1F, 0F, 0F,
                    light, overlay
            );
            case UP -> quad(
                    consumer, pose,
                    MIN, 1F + EPS, MIN, 0F, 1F,
                    MAX, 1F + EPS, MIN, 1F, 1F,
                    MAX, 1F + EPS, MAX, 1F, 0F,
                    MIN, 1F + EPS, MAX, 0F, 0F,
                    0F, 1F, 0F,
                    light, overlay
            );
            case DOWN -> quad(
                    consumer, pose,
                    MIN, -EPS, MAX, 0F, 1F,
                    MAX, -EPS, MAX, 1F, 1F,
                    MAX, -EPS, MIN, 1F, 0F,
                    MIN, -EPS, MIN, 0F, 0F,
                    0F, -1F, 0F,
                    light, overlay
            );
        }
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz,
            int light,
            int overlay
    ) {
        vertex(consumer, pose, x0, y0, z0, u0, v0, nx, ny, nz, light, overlay);
        vertex(consumer, pose, x1, y1, z1, u1, v1, nx, ny, nz, light, overlay);
        vertex(consumer, pose, x2, y2, z2, u2, v2, nx, ny, nz, light, overlay);
        vertex(consumer, pose, x3, y3, z3, u3, v3, nx, ny, nz, light, overlay);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int light,
            int overlay
    ) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
