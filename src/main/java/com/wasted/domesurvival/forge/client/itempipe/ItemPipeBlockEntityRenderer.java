package com.wasted.domesurvival.forge.client.itempipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.itempipe.ItemConnectorMode;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ItemPipeBlockEntityRenderer implements BlockEntityRenderer<ItemPipeBlockEntity> {
    private static final ResourceLocation FRAME = texture("connector_frame.png");
    private static final ResourceLocation INPUT = texture("connector_input.png");
    private static final ResourceLocation OUTPUT = texture("connector_output.png");
    private static final ResourceLocation DISABLED = texture("connector_disabled.png");

    // Compact coupling around the normal 3px pipe arm. The dimensions are kept
    // intentionally close to the existing GOTEICRAFT machine connector style.
    private static final float OUTER_MIN = 5.25F / 16.0F;
    private static final float OUTER_MAX = 10.75F / 16.0F;
    private static final float INNER_MIN = 6.15F / 16.0F;
    private static final float INNER_MAX = 9.85F / 16.0F;
    private static final float DEPTH = 1.65F / 16.0F;
    private static final float FACE_INSET = 0.20F / 16.0F;

    public ItemPipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(ItemPipeBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light, int overlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        for (Direction direction : Direction.values()) {
            if (!ItemPipeBlock.hasObjectConnector(level, blockEntity.getBlockPos(), direction)) continue;

            ItemConnectorMode mode = blockEntity.getConnectorMode(direction);
            ResourceLocation faceTexture = switch (mode) {
                case INPUT -> INPUT;
                case OUTPUT -> OUTPUT;
                case DISABLED -> DISABLED;
            };

            VertexConsumer frame = bufferSource.getBuffer(RenderType.entityCutoutNoCull(FRAME));
            renderConnectorFrame(frame, poseStack.last(), direction, light, overlay);

            VertexConsumer face = bufferSource.getBuffer(RenderType.entityCutoutNoCull(faceTexture));
            renderModeFace(face, poseStack.last(), direction, light, overlay);
        }
    }

    private static void renderConnectorFrame(VertexConsumer c, PoseStack.Pose pose,
                                             Direction direction, int light, int overlay) {
        // A shallow 3D collar placed fully inside the pipe block. This prevents
        // z-fighting with the connected chest/machine while making the exact
        // configurable connection visually obvious.
        switch (direction) {
            case NORTH -> box(c, pose, OUTER_MIN, OUTER_MIN, 0.0F,
                    OUTER_MAX, OUTER_MAX, DEPTH, light, overlay);
            case SOUTH -> box(c, pose, OUTER_MIN, OUTER_MIN, 1.0F - DEPTH,
                    OUTER_MAX, OUTER_MAX, 1.0F, light, overlay);
            case WEST -> box(c, pose, 0.0F, OUTER_MIN, OUTER_MIN,
                    DEPTH, OUTER_MAX, OUTER_MAX, light, overlay);
            case EAST -> box(c, pose, 1.0F - DEPTH, OUTER_MIN, OUTER_MIN,
                    1.0F, OUTER_MAX, OUTER_MAX, light, overlay);
            case DOWN -> box(c, pose, OUTER_MIN, 0.0F, OUTER_MIN,
                    OUTER_MAX, DEPTH, OUTER_MAX, light, overlay);
            case UP -> box(c, pose, OUTER_MIN, 1.0F - DEPTH, OUTER_MIN,
                    OUTER_MAX, 1.0F, OUTER_MAX, light, overlay);
        }
    }

    private static void renderModeFace(VertexConsumer c, PoseStack.Pose pose,
                                       Direction direction, int light, int overlay) {
        float n = FACE_INSET;
        float p = 1.0F - FACE_INSET;
        switch (direction) {
            case NORTH -> quad(c, pose,
                    INNER_MIN, INNER_MIN, n, 1,1,
                    INNER_MAX, INNER_MIN, n, 0,1,
                    INNER_MAX, INNER_MAX, n, 0,0,
                    INNER_MIN, INNER_MAX, n, 1,0,
                    0,0,-1, light, overlay);
            case SOUTH -> quad(c, pose,
                    INNER_MAX, INNER_MIN, p, 1,1,
                    INNER_MIN, INNER_MIN, p, 0,1,
                    INNER_MIN, INNER_MAX, p, 0,0,
                    INNER_MAX, INNER_MAX, p, 1,0,
                    0,0,1, light, overlay);
            case WEST -> quad(c, pose,
                    n, INNER_MIN, INNER_MAX, 1,1,
                    n, INNER_MIN, INNER_MIN, 0,1,
                    n, INNER_MAX, INNER_MIN, 0,0,
                    n, INNER_MAX, INNER_MAX, 1,0,
                    -1,0,0, light, overlay);
            case EAST -> quad(c, pose,
                    p, INNER_MIN, INNER_MIN, 1,1,
                    p, INNER_MIN, INNER_MAX, 0,1,
                    p, INNER_MAX, INNER_MAX, 0,0,
                    p, INNER_MAX, INNER_MIN, 1,0,
                    1,0,0, light, overlay);
            case DOWN -> quad(c, pose,
                    INNER_MIN, n, INNER_MIN, 0,0,
                    INNER_MAX, n, INNER_MIN, 1,0,
                    INNER_MAX, n, INNER_MAX, 1,1,
                    INNER_MIN, n, INNER_MAX, 0,1,
                    0,-1,0, light, overlay);
            case UP -> quad(c, pose,
                    INNER_MIN, p, INNER_MAX, 0,1,
                    INNER_MAX, p, INNER_MAX, 1,1,
                    INNER_MAX, p, INNER_MIN, 1,0,
                    INNER_MIN, p, INNER_MIN, 0,0,
                    0,1,0, light, overlay);
        }
    }

    private static void box(VertexConsumer c, PoseStack.Pose pose,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            int light, int overlay) {
        // north / south
        quad(c, pose, x1,y0,z0, 1,1, x0,y0,z0, 0,1, x0,y1,z0, 0,0, x1,y1,z0, 1,0, 0,0,-1, light,overlay);
        quad(c, pose, x0,y0,z1, 1,1, x1,y0,z1, 0,1, x1,y1,z1, 0,0, x0,y1,z1, 1,0, 0,0,1, light,overlay);
        // west / east
        quad(c, pose, x0,y0,z0, 1,1, x0,y0,z1, 0,1, x0,y1,z1, 0,0, x0,y1,z0, 1,0, -1,0,0, light,overlay);
        quad(c, pose, x1,y0,z1, 1,1, x1,y0,z0, 0,1, x1,y1,z0, 0,0, x1,y1,z1, 1,0, 1,0,0, light,overlay);
        // down / up
        quad(c, pose, x0,y0,z1, 0,1, x1,y0,z1, 1,1, x1,y0,z0, 1,0, x0,y0,z0, 0,0, 0,-1,0, light,overlay);
        quad(c, pose, x0,y1,z0, 0,0, x1,y1,z0, 1,0, x1,y1,z1, 1,1, x0,y1,z1, 0,1, 0,1,0, light,overlay);
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose,
                             float x1,float y1,float z1,float u1,float v1,
                             float x2,float y2,float z2,float u2,float v2,
                             float x3,float y3,float z3,float u3,float v3,
                             float x4,float y4,float z4,float u4,float v4,
                             float nx,float ny,float nz,int light,int overlay) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(c,matrix,normal,x1,y1,z1,u1,v1,nx,ny,nz,light,overlay);
        vertex(c,matrix,normal,x2,y2,z2,u2,v2,nx,ny,nz,light,overlay);
        vertex(c,matrix,normal,x3,y3,z3,u3,v3,nx,ny,nz,light,overlay);
        vertex(c,matrix,normal,x4,y4,z4,u4,v4,nx,ny,nz,light,overlay);
    }

    private static void vertex(VertexConsumer c, Matrix4f matrix, Matrix3f normal,
                               float x,float y,float z,float u,float v,
                               float nx,float ny,float nz,int light,int overlay) {
        c.vertex(matrix,x,y,z)
                .color(255,255,255,255)
                .uv(u,v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal,nx,ny,nz)
                .endVertex();
    }

    private static ResourceLocation texture(String file) {
        return new ResourceLocation(DomeSurvival.MOD_ID, "textures/block/item_pipe/" + file);
    }
}
