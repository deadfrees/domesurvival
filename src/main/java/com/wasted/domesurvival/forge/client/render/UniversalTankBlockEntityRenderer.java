package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.storage.tank.UniversalTankBlockEntity;
import com.wasted.domesurvival.forge.storage.tank.UniversalTankContentKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** V63.1 seamless reservoir renderer for both single cells and unified structures. */
public final class UniversalTankBlockEntityRenderer
        implements BlockEntityRenderer<UniversalTankBlockEntity> {

    private static final ResourceLocation METAL_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "block/airlock_gate_unformed_dark");
    private static final ResourceLocation GLASS_TEXTURE =
            new ResourceLocation("minecraft", "block/glass");
    // Bright neutral gas texture; transparency is applied through the translucent block sheet.
    private static final ResourceLocation OXYGEN_TEXTURE =
            new ResourceLocation("minecraft", "block/white_stained_glass");
    private static final ResourceLocation VALVE_INPUT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "block/universal_tank/valve_input");
    private static final ResourceLocation VALVE_OUTPUT_TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "block/universal_tank/valve_output");

    private static final float FRAME = 3.0F / 16.0F;
    private static final float GLASS_GAP = 0.010F;
    private static final float CONTENT_INSET = 0.025F;

    public UniversalTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            UniversalTankBlockEntity tank,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (tank.isUnifiedModel()) {
            if (tank.isMaster()) {
                renderUnifiedBody(tank, poseStack, buffer, packedLight, packedOverlay);
            }
        } else {
            renderStandaloneBody(tank, poseStack, buffer, packedLight, packedOverlay);
        }

        renderConnectorPorts(tank, poseStack, buffer, packedLight, packedOverlay);
    }

    private static void renderStandaloneBody(
            UniversalTankBlockEntity tank,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        TextureAtlasSprite metal = sprite(METAL_TEXTURE);
        TextureAtlasSprite glass = sprite(GLASS_TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();

        renderNonOverlappingFrame(consumer, pose, metal,
                0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, light, overlay);

        float gx0 = FRAME + GLASS_GAP;
        float gy0 = FRAME + GLASS_GAP;
        float gz0 = FRAME + GLASS_GAP;
        float gx1 = 1.0F - FRAME - GLASS_GAP;
        float gy1 = 1.0F - FRAME - GLASS_GAP;
        float gz1 = 1.0F - FRAME - GLASS_GAP;

        cuboid(consumer, pose, glass, gx0, gy0, gz0, gx1, gy1, gz1,
                255, 255, 255, 255, light, overlay);
        renderContent(tank, buffer, pose,
                gx0 + CONTENT_INSET, gy0 + CONTENT_INSET, gz0 + CONTENT_INSET,
                gx1 - CONTENT_INSET, gy1 - CONTENT_INSET, gz1 - CONTENT_INSET,
                light, overlay);
    }

    private static void renderUnifiedBody(
            UniversalTankBlockEntity tank,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        BlockPos master = tank.getBlockPos();
        BlockPos min = tank.getStructureMin();
        BlockPos max = tank.getStructureMax();

        float x0 = min.getX() - master.getX();
        float y0 = min.getY() - master.getY();
        float z0 = min.getZ() - master.getZ();
        float x1 = max.getX() - master.getX() + 1.0F;
        float y1 = max.getY() - master.getY() + 1.0F;
        float z1 = max.getZ() - master.getZ() + 1.0F;

        TextureAtlasSprite metal = sprite(METAL_TEXTURE);
        TextureAtlasSprite glass = sprite(GLASS_TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();

        renderNonOverlappingFrame(consumer, pose, metal, x0, y0, z0, x1, y1, z1, light, overlay);

        // Glass is inset from the inner frame planes so no coplanar faces can flicker.
        float gx0 = x0 + FRAME + GLASS_GAP;
        float gy0 = y0 + FRAME + GLASS_GAP;
        float gz0 = z0 + FRAME + GLASS_GAP;
        float gx1 = x1 - FRAME - GLASS_GAP;
        float gy1 = y1 - FRAME - GLASS_GAP;
        float gz1 = z1 - FRAME - GLASS_GAP;

        if (gx1 > gx0 && gy1 > gy0 && gz1 > gz0) {
            cuboid(consumer, pose, glass, gx0, gy0, gz0, gx1, gy1, gz1,
                    255, 255, 255, 255, light, overlay);
            renderContent(tank, buffer, pose,
                    gx0 + CONTENT_INSET, gy0 + CONTENT_INSET, gz0 + CONTENT_INSET,
                    gx1 - CONTENT_INSET, gy1 - CONTENT_INSET, gz1 - CONTENT_INSET,
                    light, overlay);
        }
    }

    private static void renderNonOverlappingFrame(
            VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite metal,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int light, int overlay
    ) {
        int c = 255;

        // Four vertical posts own the corners for the full height.
        cuboid(consumer, pose, metal, x0, y0, z0, x0 + FRAME, y1, z0 + FRAME, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x1 - FRAME, y0, z0, x1, y1, z0 + FRAME, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x0, y0, z1 - FRAME, x0 + FRAME, y1, z1, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x1 - FRAME, y0, z1 - FRAME, x1, y1, z1, c,c,c,255,light,overlay);

        // X beams stop at the vertical posts: no overlapping corner surfaces.
        cuboid(consumer, pose, metal, x0 + FRAME, y0, z0, x1 - FRAME, y0 + FRAME, z0 + FRAME, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x0 + FRAME, y0, z1 - FRAME, x1 - FRAME, y0 + FRAME, z1, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x0 + FRAME, y1 - FRAME, z0, x1 - FRAME, y1, z0 + FRAME, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x0 + FRAME, y1 - FRAME, z1 - FRAME, x1 - FRAME, y1, z1, c,c,c,255,light,overlay);

        // Z beams likewise stop at the vertical posts and only touch X beams at boundaries.
        cuboid(consumer, pose, metal, x0, y0, z0 + FRAME, x0 + FRAME, y0 + FRAME, z1 - FRAME, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x1 - FRAME, y0, z0 + FRAME, x1, y0 + FRAME, z1 - FRAME, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x0, y1 - FRAME, z0 + FRAME, x0 + FRAME, y1, z1 - FRAME, c,c,c,255,light,overlay);
        cuboid(consumer, pose, metal, x1 - FRAME, y1 - FRAME, z0 + FRAME, x1, y1, z1 - FRAME, c,c,c,255,light,overlay);
    }

    private static void renderContent(
            UniversalTankBlockEntity tank, MultiBufferSource buffer, PoseStack.Pose pose,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int light, int overlay
    ) {
        float fraction = tank.getFillFraction();
        if (fraction <= 0.0F || x1 <= x0 || y1 <= y0 || z1 <= z0) return;

        float filledY1 = y0 + (y1 - y0) * fraction;
        if (filledY1 <= y0) return;

        TextureAtlasSprite contentSprite;
        VertexConsumer contentConsumer;
        int r;
        int g;
        int b;
        int a;

        if (tank.getContentKind() == UniversalTankContentKind.OXYGEN) {
            contentSprite = sprite(OXYGEN_TEXTURE);
            contentConsumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());

            // Oxygen Filler uses a neutral gray palette. Use its lighter end here
            // and partial alpha so the stored oxygen reads visually as gas.
            r = 210;
            g = 216;
            b = 220;
            a = 160;
        } else if (tank.getContentKind() == UniversalTankContentKind.FLUID) {
            FluidStack stack = tank.getVisibleFluidStack();
            if (stack.isEmpty()) return;

            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluid());
            ResourceLocation texture = extensions.getStillTexture(stack);
            if (texture == null) return;

            contentSprite = sprite(texture);
            contentConsumer = buffer.getBuffer(Sheets.cutoutBlockSheet());
            int tint = extensions.getTintColor(stack);
            r = (tint >> 16) & 0xFF;
            g = (tint >> 8) & 0xFF;
            b = tint & 0xFF;
            a = 255;
        } else {
            return;
        }

        cuboid(contentConsumer, pose, contentSprite, x0, y0, z0, x1, filledY1, z1,
                r, g, b, a, light, overlay);
    }

    /**
     * V63.2 compact reservoir valves.
     *
     * The old connector was a large 0.4-block colored plate floating outside the
     * surface. The new connector grows from the glass plane: a dark-metal neck
     * crosses the glass/frame depth and a small canonical input/output valve cap
     * sits outside the reservoir.
     */
    private static void renderConnectorPorts(
            UniversalTankBlockEntity tank, PoseStack poseStack, MultiBufferSource buffer,
            int light, int overlay
    ) {
        TextureAtlasSprite metal = sprite(METAL_TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();

        for (Direction direction : Direction.values()) {
            SideMode mode = tank.getSideMode(direction);
            if (mode == SideMode.DISABLED || !tank.isConnectorFace(direction)) {
                continue;
            }

            TextureAtlasSprite valve = sprite(
                    mode == SideMode.INPUT
                            ? VALVE_INPUT_TEXTURE
                            : VALVE_OUTPUT_TEXTURE
            );

            renderValve(consumer, pose, metal, valve, direction, light, overlay);
        }
    }

    private static void renderValve(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            TextureAtlasSprite metal,
            TextureAtlasSprite valve,
            Direction direction,
            int light,
            int overlay
    ) {
        // Neck: 3.5 px square. Cap: 5.5 px square.
        // The inner end starts at the same inset plane as the visible glass,
        // so the valve visually emerges from the glass rather than floating.
        final float neck0 = 0.390625F;
        final float neck1 = 0.609375F;
        final float cap0 = 0.328125F;
        final float cap1 = 0.671875F;

        final float innerLow = FRAME + GLASS_GAP - 0.010F;
        final float innerHigh = 1.0F - FRAME - GLASS_GAP + 0.010F;

        switch (direction) {
            case NORTH -> {
                cuboid(consumer, pose, metal,
                        neck0, neck0, -0.020F,
                        neck1, neck1, innerLow,
                        255,255,255,255,light,overlay);
                cuboid(consumer, pose, valve,
                        cap0, cap0, -0.045F,
                        cap1, cap1, -0.012F,
                        255,255,255,255,light,overlay);
            }
            case SOUTH -> {
                cuboid(consumer, pose, metal,
                        neck0, neck0, innerHigh,
                        neck1, neck1, 1.020F,
                        255,255,255,255,light,overlay);
                cuboid(consumer, pose, valve,
                        cap0, cap0, 1.012F,
                        cap1, cap1, 1.045F,
                        255,255,255,255,light,overlay);
            }
            case WEST -> {
                cuboid(consumer, pose, metal,
                        -0.020F, neck0, neck0,
                        innerLow, neck1, neck1,
                        255,255,255,255,light,overlay);
                cuboid(consumer, pose, valve,
                        -0.045F, cap0, cap0,
                        -0.012F, cap1, cap1,
                        255,255,255,255,light,overlay);
            }
            case EAST -> {
                cuboid(consumer, pose, metal,
                        innerHigh, neck0, neck0,
                        1.020F, neck1, neck1,
                        255,255,255,255,light,overlay);
                cuboid(consumer, pose, valve,
                        1.012F, cap0, cap0,
                        1.045F, cap1, cap1,
                        255,255,255,255,light,overlay);
            }
            case DOWN -> {
                cuboid(consumer, pose, metal,
                        neck0, -0.020F, neck0,
                        neck1, innerLow, neck1,
                        255,255,255,255,light,overlay);
                cuboid(consumer, pose, valve,
                        cap0, -0.045F, cap0,
                        cap1, -0.012F, cap1,
                        255,255,255,255,light,overlay);
            }
            case UP -> {
                cuboid(consumer, pose, metal,
                        neck0, innerHigh, neck0,
                        neck1, 1.020F, neck1,
                        255,255,255,255,light,overlay);
                cuboid(consumer, pose, valve,
                        cap0, 1.012F, cap0,
                        cap1, 1.045F, cap1,
                        255,255,255,255,light,overlay);
            }
        }
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(id);
    }

    private static void cuboid(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int r, int g, int b, int a,
            int light,
            int overlay
    ) {
        if (x1 <= x0 || y1 <= y0 || z1 <= z0) return;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // North (-Z)
        quad(consumer, pose,
                x1, y0, z0, u0, v1,
                x0, y0, z0, u1, v1,
                x0, y1, z0, u1, v0,
                x1, y1, z0, u0, v0,
                0.0F, 0.0F, -1.0F, r, g, b, a, light, overlay);

        // South (+Z)
        quad(consumer, pose,
                x0, y0, z1, u0, v1,
                x1, y0, z1, u1, v1,
                x1, y1, z1, u1, v0,
                x0, y1, z1, u0, v0,
                0.0F, 0.0F, 1.0F, r, g, b, a, light, overlay);

        // West (-X)
        quad(consumer, pose,
                x0, y0, z0, u0, v1,
                x0, y0, z1, u1, v1,
                x0, y1, z1, u1, v0,
                x0, y1, z0, u0, v0,
                -1.0F, 0.0F, 0.0F, r, g, b, a, light, overlay);

        // East (+X)
        quad(consumer, pose,
                x1, y0, z1, u0, v1,
                x1, y0, z0, u1, v1,
                x1, y1, z0, u1, v0,
                x1, y1, z1, u0, v0,
                1.0F, 0.0F, 0.0F, r, g, b, a, light, overlay);

        // Down (-Y)
        quad(consumer, pose,
                x0, y0, z1, u0, v1,
                x0, y0, z0, u0, v0,
                x1, y0, z0, u1, v0,
                x1, y0, z1, u1, v1,
                0.0F, -1.0F, 0.0F, r, g, b, a, light, overlay);

        // Up (+Y)
        quad(consumer, pose,
                x0, y1, z0, u0, v1,
                x0, y1, z1, u0, v0,
                x1, y1, z1, u1, v0,
                x1, y1, z0, u1, v1,
                0.0F, 1.0F, 0.0F, r, g, b, a, light, overlay);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz,
            int r, int g, int b, int a,
            int light,
            int overlay
    ) {
        vertex(consumer, pose, x0, y0, z0, u0, v0, nx, ny, nz, r, g, b, a, light, overlay);
        vertex(consumer, pose, x1, y1, z1, u1, v1, nx, ny, nz, r, g, b, a, light, overlay);
        vertex(consumer, pose, x2, y2, z2, u2, v2, nx, ny, nz, r, g, b, a, light, overlay);
        vertex(consumer, pose, x3, y3, z3, u3, v3, nx, ny, nz, r, g, b, a, light, overlay);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x, float y, float z,
            float u, float v,
            float nx, float ny, float nz,
            int r, int g, int b, int a,
            int light,
            int overlay
    ) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(UniversalTankBlockEntity tank) {
        // One master BER draws the complete multiblock. Keep it eligible while any
        // part of the reservoir is visible, even if the master origin leaves the frustum.
        return tank.isUnifiedModel();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
