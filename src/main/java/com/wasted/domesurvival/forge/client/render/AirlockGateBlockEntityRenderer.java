package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * V55.2 renderer.
 *
 * Formed gates use this one renderer in CLOSED, OPENING, OPEN and CLOSING.
 * We intentionally do not hand rendering back to the baked 25-block models at
 * the end of closing. That removes the previous one-frame flash and also keeps
 * the exact same lighting path for static and moving leaves.
 *
 * Each supported size uses pXY / pXY_back sprites scaled from the complete original 5x5 artwork.
 * Lighting is sampled per original 1x1 gate tile instead of reusing only the
 * master BlockEntity light value. Vanilla directional face shade is also
 * applied so movement does not make the gate visibly brighter.
 */
public final class AirlockGateBlockEntityRenderer
        implements BlockEntityRenderer<AirlockGateBlockEntity> {

    private static final ResourceLocation SIDE_TEXTURE = new ResourceLocation(
            DomeSurvival.MOD_ID,
            "block/airlock_gate/side"
    );

    private static final float Z_MIN = 5.5F / 16.0F;
    private static final float Z_MAX = 10.5F / 16.0F;

    private static final float CLIP_EPSILON = 1.0E-4F;

    public AirlockGateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            AirlockGateBlockEntity gate,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = gate.getBlockState();
        if (!state.hasProperty(AirlockGateBlock.FORMED)
                || !state.getValue(AirlockGateBlock.FORMED)
                || !state.getValue(AirlockGateBlock.MASTER)
                || !state.hasProperty(AirlockGateBlock.MOTION)) {
            return;
        }

        int size = gate.gateSize();
        int masterIndex = AirlockGateBlock.masterIndex(size);
        float progress = gate.renderProgress(partialTick);
        float slide = progress * AirlockGateBlock.maxSlideBlocks(size);
        float split = size / 2.0F;

        // Coordinates are expressed relative to the chosen master block.
        float pocketLeftX = -masterIndex;
        float pocketRightX = size - masterIndex;

        Level level = gate.getLevel();
        Lighting lighting = createLighting(level, state);

        poseStack.pushPose();
        rotateToFacing(
                poseStack,
                state.getValue(AirlockGateBlock.FACING),
                size
        );

        for (int row = 0; row < size; row++) {
            float y0 = row - masterIndex;
            float y1 = y0 + 1.0F;

            for (int col = 0; col < size; col++) {
                TextureAtlasSprite front = gateSprite(size, col, row, false);
                boolean mirroredFrontBack = size < AirlockGateBlock.MAX_GATE_SIZE;
                TextureAtlasSprite back = mirroredFrontBack
                        ? front
                        : gateSprite(size, col, row, true);
                int tileLight = tileLight(
                        gate,
                        state,
                        size,
                        col,
                        row,
                        packedLight
                );

                float tileDoorX0 = col;
                float tileDoorX1 = col + 1.0F;
                float baseX0 = tileDoorX0 - masterIndex;
                float baseX1 = tileDoorX1 - masterIndex;

                if (tileDoorX1 <= split + CLIP_EPSILON) {
                    // Entire tile belongs to the left leaf.
                    renderPiece(
                            poseStack, buffer, tileLight, packedOverlay, lighting,
                            front, back,
                            baseX0 - slide, baseX1 - slide,
                            y0, y1,
                            0.0F, 1.0F,
                            0.0F, 1.0F,
                            mirroredFrontBack,
                            col == 0,
                            slide > CLIP_EPSILON
                                    && Math.abs(tileDoorX1 - split) <= CLIP_EPSILON,
                            row == 0, row == size - 1,
                            pocketLeftX, pocketRightX
                    );
                    continue;
                }

                if (tileDoorX0 >= split - CLIP_EPSILON) {
                    // Entire tile belongs to the right leaf.
                    renderPiece(
                            poseStack, buffer, tileLight, packedOverlay, lighting,
                            front, back,
                            baseX0 + slide, baseX1 + slide,
                            y0, y1,
                            0.0F, 1.0F,
                            0.0F, 1.0F,
                            mirroredFrontBack,
                            slide > CLIP_EPSILON
                                    && Math.abs(tileDoorX0 - split) <= CLIP_EPSILON,
                            col == size - 1,
                            row == 0, row == size - 1,
                            pocketLeftX, pocketRightX
                    );
                    continue;
                }

                // Odd sizes (3x3/5x5) split the center source tile exactly at
                // the door center. Even sizes (2x2/4x4) never enter this path.
                float leftFraction = split - tileDoorX0;
                float rightFraction = 1.0F - leftFraction;
                boolean centerSeamExposed = slide > CLIP_EPSILON;

                renderPiece(
                        poseStack, buffer, tileLight, packedOverlay, lighting,
                        front, back,
                        baseX0 - slide,
                        baseX0 + leftFraction - slide,
                        y0, y1,
                        0.0F, leftFraction,
                        1.0F - leftFraction, 1.0F,
                        mirroredFrontBack,
                        col == 0, centerSeamExposed,
                        row == 0, row == size - 1,
                        pocketLeftX, pocketRightX
                );

                renderPiece(
                        poseStack, buffer, tileLight, packedOverlay, lighting,
                        front, back,
                        baseX0 + leftFraction + slide,
                        baseX1 + slide,
                        y0, y1,
                        leftFraction, 1.0F,
                        0.0F, rightFraction,
                        mirroredFrontBack,
                        centerSeamExposed, col == size - 1,
                        row == 0, row == size - 1,
                        pocketLeftX, pocketRightX
                );
            }
        }

        poseStack.popPose();
    }

    private static Lighting createLighting(Level level, BlockState state) {
        if (level == null) {
            return Lighting.FULL;
        }

        Direction.Axis axis = state.getValue(AirlockGateBlock.FACING).getAxis();

        // Opposite directions on the same axis have the same vanilla shade,
        // therefore axis selection is enough here.
        Direction frontBackDirection =
                axis == Direction.Axis.Z ? Direction.NORTH : Direction.WEST;
        Direction sideDirection =
                axis == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;

        return new Lighting(
                shadeByte(level.getShade(frontBackDirection, true)),
                shadeByte(level.getShade(sideDirection, true)),
                shadeByte(level.getShade(Direction.UP, true)),
                shadeByte(level.getShade(Direction.DOWN, true))
        );
    }

    private static int shadeByte(float shade) {
        int value = Math.round(255.0F * shade);
        return Math.max(0, Math.min(255, value));
    }

    private static int tileLight(
            AirlockGateBlockEntity gate,
            BlockState masterState,
            int size,
            int col,
            int row,
            int fallback
    ) {
        Level level = gate.getLevel();
        if (level == null) {
            return fallback;
        }

        int masterIndex = AirlockGateBlock.masterIndex(size);
        int horizontalOffset = col - masterIndex;
        int verticalOffset = row - masterIndex;
        BlockPos masterPos = gate.getBlockPos();

        BlockPos samplePos = masterState.getValue(AirlockGateBlock.FACING)
                .getAxis() == Direction.Axis.Z
                ? masterPos.offset(horizontalOffset, verticalOffset, 0)
                : masterPos.offset(0, verticalOffset, horizontalOffset);

        return LevelRenderer.getLightColor(level, samplePos);
    }

    private static TextureAtlasSprite gateSprite(
            int size,
            int col,
            int row,
            boolean back
    ) {
        /*
         * V60.3:
         *
         * The generated s2/s3/s4 *_back sprite sets already contain the correct
         * rear-side tile ordering. The renderer must therefore address rear
         * sprites with the SAME texture column index as the front side.
         *
         * V60.2 tried to special-case even widths by mirroring the rear column
         * lookup again in code. That produced no real improvement for 2x2/4x4
         * and also caused inconsistent rear composition across variable-size
         * gates. Keep the existing rear U-cropping logic, but stop remapping
         * the texture column itself.
         */
        int textureCol = col;

        String suffix = back ? "_back" : "";
        String path = size == AirlockGateBlock.MAX_GATE_SIZE
                ? "block/airlock_gate/p" + textureCol + row + suffix
                : "block/airlock_gate/s" + size + "/p" + textureCol + row + suffix;

        ResourceLocation id = new ResourceLocation(
                DomeSurvival.MOD_ID,
                path
        );

        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(id);
    }


    private static TextureAtlasSprite sideSprite() {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(SIDE_TEXTURE);
    }

    /**
     * Rotates the canonical north-facing mesh around the geometric center of
     * the complete gate, not around the master block center.
     *
     * Odd gates have a real center block, while even gates do not. Using the
     * geometric center prevents 2x2/4x4 south/east/west gates from shifting by
     * one block when the same master-based renderer is rotated.
     */
    private static void rotateToFacing(
            PoseStack poseStack,
            Direction facing,
            int size
    ) {
        int masterIndex = AirlockGateBlock.masterIndex(size);
        double horizontalCenter = (size / 2.0D) - masterIndex;

        double targetCenterX = facing.getAxis() == Direction.Axis.Z
                ? horizontalCenter
                : 0.5D;
        double targetCenterZ = facing.getAxis() == Direction.Axis.Z
                ? 0.5D
                : horizontalCenter;

        float degrees = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };

        poseStack.translate(targetCenterX, 0.0D, targetCenterZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-horizontalCenter, 0.0D, -0.5D);
    }

    private static void renderPiece(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay,
            Lighting lighting,
            TextureAtlasSprite front,
            TextureAtlasSprite back,
            float x0,
            float x1,
            float y0,
            float y1,
            float frontFractionU0,
            float frontFractionU1,
            float backFractionU0,
            float backFractionU1,
            boolean mirroredFrontBack,
            boolean renderLeftSide,
            boolean renderRightSide,
            boolean renderBottom,
            boolean renderTop,
            float pocketLeftX,
            float pocketRightX
    ) {
        float originalX0 = x0;
        float originalX1 = x1;
        float originalWidth = originalX1 - originalX0;
        if (originalWidth <= CLIP_EPSILON) {
            return;
        }

        float clippedX0 = Math.max(originalX0, pocketLeftX);
        float clippedX1 = Math.min(originalX1, pocketRightX);
        if (clippedX1 - clippedX0 <= CLIP_EPSILON) {
            return;
        }

        float clipT0 = (clippedX0 - originalX0) / originalWidth;
        float clipT1 = (clippedX1 - originalX0) / originalWidth;

        float originalFrontU0 = frontFractionU0;
        float originalFrontU1 = frontFractionU1;
        frontFractionU0 = lerp(originalFrontU0, originalFrontU1, clipT0);
        frontFractionU1 = lerp(originalFrontU0, originalFrontU1, clipT1);

        float originalBackU0 = backFractionU0;
        float originalBackU1 = backFractionU1;
        if (mirroredFrontBack) {
            /*
             * V60.7 hard rule for 2x2 / 3x3 / 4x4:
             * rear uses the SAME front sprite and the SAME world-U mapping.
             * A player looking from the opposite physical side therefore sees
             * the front artwork naturally mirrored, with no second tile remap.
             */
            backFractionU0 = frontFractionU0;
            backFractionU1 = frontFractionU1;
        } else {
            // Preserve the already-correct legacy 5x5 rear mapping exactly.
            backFractionU1 = lerp(originalBackU1, originalBackU0, clipT0);
            backFractionU0 = lerp(originalBackU1, originalBackU0, clipT1);
        }

        boolean leftEdgeClipped = clippedX0 > originalX0 + CLIP_EPSILON;
        boolean rightEdgeClipped = clippedX1 < originalX1 - CLIP_EPSILON;
        renderLeftSide = renderLeftSide && !leftEdgeClipped;
        renderRightSide = renderRightSide && !rightEdgeClipped;

        x0 = clippedX0;
        x1 = clippedX1;

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(Sheets.cutoutBlockSheet());
        TextureAtlasSprite side = sideSprite();

        float fu0 = lerp(front.getU0(), front.getU1(), frontFractionU0);
        float fu1 = lerp(front.getU0(), front.getU1(), frontFractionU1);
        float fv0 = front.getV0();
        float fv1 = front.getV1();

        float bu0 = lerp(back.getU0(), back.getU1(), backFractionU0);
        float bu1 = lerp(back.getU0(), back.getU1(), backFractionU1);
        float bv0 = back.getV0();
        float bv1 = back.getV1();

        solidQuad(
                consumer, pose,
                x0, y0, Z_MAX, fu0, fv1,
                x1, y0, Z_MAX, fu1, fv1,
                x1, y1, Z_MAX, fu1, fv0,
                x0, y1, Z_MAX, fu0, fv0,
                0.0F, 0.0F, 1.0F,
                lighting.frontBack(),
                light, overlay
        );

        if (mirroredFrontBack) {
            /*
             * Keep U attached to the same WORLD X coordinate as the front:
             *   x0 -> u0, x1 -> u1.
             * The camera is now on the opposite side of the plane, so the
             * complete image is seen as a true horizontal mirror of the front.
             */
            solidQuad(
                    consumer, pose,
                    x1, y0, Z_MIN, bu1, bv1,
                    x0, y0, Z_MIN, bu0, bv1,
                    x0, y1, Z_MIN, bu0, bv0,
                    x1, y1, Z_MIN, bu1, bv0,
                    0.0F, 0.0F, -1.0F,
                    lighting.frontBack(),
                    light, overlay
            );
        } else {
            solidQuad(
                    consumer, pose,
                    x1, y0, Z_MIN, bu0, bv1,
                    x0, y0, Z_MIN, bu1, bv1,
                    x0, y1, Z_MIN, bu1, bv0,
                    x1, y1, Z_MIN, bu0, bv0,
                    0.0F, 0.0F, -1.0F,
                    lighting.frontBack(),
                    light, overlay
            );
        }

        float su0 = side.getU0();
        float su1 = side.getU1();
        float sv0 = side.getV0();
        float sv1 = side.getV1();

        if (renderLeftSide) {
            solidQuad(
                    consumer, pose,
                    x0, y0, Z_MIN, su0, sv1,
                    x0, y0, Z_MAX, su1, sv1,
                    x0, y1, Z_MAX, su1, sv0,
                    x0, y1, Z_MIN, su0, sv0,
                    -1.0F, 0.0F, 0.0F,
                    lighting.side(),
                    light, overlay
            );
        }

        if (renderRightSide) {
            solidQuad(
                    consumer, pose,
                    x1, y0, Z_MAX, su0, sv1,
                    x1, y0, Z_MIN, su1, sv1,
                    x1, y1, Z_MIN, su1, sv0,
                    x1, y1, Z_MAX, su0, sv0,
                    1.0F, 0.0F, 0.0F,
                    lighting.side(),
                    light, overlay
            );
        }

        if (renderTop) {
            solidQuad(
                    consumer, pose,
                    x0, y1, Z_MAX, su0, sv0,
                    x1, y1, Z_MAX, su1, sv0,
                    x1, y1, Z_MIN, su1, sv1,
                    x0, y1, Z_MIN, su0, sv1,
                    0.0F, 1.0F, 0.0F,
                    lighting.top(),
                    light, overlay
            );
        }

        if (renderBottom) {
            solidQuad(
                    consumer, pose,
                    x0, y0, Z_MIN, su0, sv0,
                    x1, y0, Z_MIN, su1, sv0,
                    x1, y0, Z_MAX, su1, sv1,
                    x0, y0, Z_MAX, su0, sv1,
                    0.0F, -1.0F, 0.0F,
                    lighting.bottom(),
                    light, overlay
            );
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Renders both windings of a surface.
     *
     * The closed gate is a thin 5-pixel solid. A first-person camera can be
     * pushed slightly into that volume by camera/pose mods or by very close
     * collision contact. With ordinary back-face culling, the far face is then
     * viewed from its back side and disappears, which looks like the player can
     * see through the closed gate.
     *
     * Keeping both windings makes the visual shell closed from either side
     * without changing gate thickness, collision, textures, lighting or motion.
     */
    private static void solidQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz,
            int shade,
            int light,
            int overlay
    ) {
        quad(
                consumer, pose,
                x0, y0, z0, u0, v0,
                x1, y1, z1, u1, v1,
                x2, y2, z2, u2, v2,
                x3, y3, z3, u3, v3,
                nx, ny, nz,
                shade, light, overlay
        );

        quad(
                consumer, pose,
                x3, y3, z3, u3, v3,
                x2, y2, z2, u2, v2,
                x1, y1, z1, u1, v1,
                x0, y0, z0, u0, v0,
                -nx, -ny, -nz,
                shade, light, overlay
        );
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz,
            int shade,
            int light,
            int overlay
    ) {
        vertex(consumer, pose, x0, y0, z0, u0, v0, nx, ny, nz, shade, light, overlay);
        vertex(consumer, pose, x1, y1, z1, u1, v1, nx, ny, nz, shade, light, overlay);
        vertex(consumer, pose, x2, y2, z2, u2, v2, nx, ny, nz, shade, light, overlay);
        vertex(consumer, pose, x3, y3, z3, u3, v3, nx, ny, nz, shade, light, overlay);
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
            int shade,
            int light,
            int overlay
    ) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(shade, shade, shade, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(AirlockGateBlockEntity gate) {
        return true;
    }

    @Override
    public boolean shouldRender(AirlockGateBlockEntity gate, Vec3 cameraPos) {
        BlockPos pos = gate.getBlockPos();
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        return cameraPos.distanceToSqr(cx, cy, cz) < (96.0D * 96.0D);
    }

    private record Lighting(int frontBack, int side, int top, int bottom) {
        private static final Lighting FULL = new Lighting(255, 255, 255, 255);
    }
}
