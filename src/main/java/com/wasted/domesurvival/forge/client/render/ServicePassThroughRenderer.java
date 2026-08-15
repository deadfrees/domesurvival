package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughBlock;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Locale;

public final class ServicePassThroughRenderer
        implements BlockEntityRenderer<ServicePassThroughBlockEntity> {

    // Axial-only inset: diameter remains exact; ends move inward by
    // 0.016 px each to prevent coplanar flicker at block boundaries.
    private static final float AXIAL_RENDER_SCALE = 0.996F;

    public ServicePassThroughRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ServicePassThroughBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        Block installed = blockEntity.getInstalledConduitBlock();
        if (installed == null) {
            return;
        }

        Direction.Axis axis = blockEntity.getBlockState().getValue(ServicePassThroughBlock.AXIS);
        BlockState renderState = createStraightState(installed.defaultBlockState(), axis);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        switch (axis) {
            case X -> poseStack.scale(AXIAL_RENDER_SCALE, 1.0F, 1.0F);
            case Y -> poseStack.scale(1.0F, AXIAL_RENDER_SCALE, 1.0F);
            case Z -> poseStack.scale(1.0F, 1.0F, AXIAL_RENDER_SCALE);
        }

        poseStack.translate(-0.5D, -0.5D, -0.5D);

        Minecraft.getInstance()
                .getBlockRenderer()
                .renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private static BlockState createStraightState(BlockState state, Direction.Axis axis) {
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            state = state.setValue(BlockStateProperties.AXIS, axis);
        }

        for (Property<?> property : state.getProperties()) {
            if (!(property instanceof BooleanProperty booleanProperty)) {
                continue;
            }

            Direction direction = directionForProperty(property.getName());
            if (direction != null) {
                state = state.setValue(booleanProperty, direction.getAxis() == axis);
            }
        }

        return state;
    }

    private static Direction directionForProperty(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "east" -> Direction.EAST;
            case "up" -> Direction.UP;
            case "down" -> Direction.DOWN;
            default -> null;
        };
    }
}
