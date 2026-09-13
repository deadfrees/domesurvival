package com.wasted.domesurvival.forge.machine.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class SolarPanelBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape NORTH_SHAPE = createNorthShape();
    private static final VoxelShape EAST_SHAPE = rotateY(NORTH_SHAPE, 1);
    private static final VoxelShape SOUTH_SHAPE = rotateY(NORTH_SHAPE, 2);
    private static final VoxelShape WEST_SHAPE = rotateY(NORTH_SHAPE, 3);

    private final SolarPanelTier tier;

    public SolarPanelBlock(Properties properties, SolarPanelTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public SolarPanelTier getTier() {
        return tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SolarPanelBlockEntity panel) {
                NetworkHooks.openScreen(serverPlayer, panel, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(
                type,
                SolarPanelRegistry.blockEntityType(),
                SolarPanelBlockEntity::serverTick
        );
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    private static VoxelShape createNorthShape() {
        VoxelShape result = Shapes.empty();

        // Main base footprint.
        result = Shapes.or(result, Shapes.box(
                -0.163D, -0.005D, 0.096D,
                 1.148D,  0.146D, 1.407D
        ));

        // Compact bottom connector: included in the clickable / collision shape.
        result = Shapes.or(result, Shapes.box(
                0.34D, -0.030D, 0.34D,
                0.66D,  0.030D, 0.66D
        ));
        result = Shapes.or(result, Shapes.box(
                0.395D, -0.055D, 0.395D,
                0.605D, -0.030D, 0.605D
        ));

        // Central mast / support.
        result = Shapes.or(result, Shapes.box(
                0.406D, 0.146D, 0.665D,
                0.579D, 1.456D, 0.838D
        ));

        // Upper transverse brace.
        result = Shapes.or(result, Shapes.box(
                -0.075D, 1.427D, 0.700D,
                 1.061D, 1.529D, 0.802D
        ));

        /*
         * The collector is made clickable across the whole visible surface
         * using many thin overlapping slabs. This keeps the hitbox close to
         * the real model without a giant air-filled prism.
         */
        final int segments = 24;
        final double zStart = -0.407D;
        final double zEnd = 1.180D;
        final double segmentDepth = (zEnd - zStart) / segments;
        final double thickness = 0.135D;
        final double overlap = 0.012D;

        for (int i = 0; i < segments; i++) {
            double z0 = zStart + segmentDepth * i - overlap;
            double z1 = zStart + segmentDepth * (i + 1) + overlap;
            double zMid = (z0 + z1) * 0.5D;

            double yMid = 1.43934546D * zMid + 1.09179146D;
            yMid = Math.max(0.53D, Math.min(2.72D, yMid));

            result = Shapes.or(result, Shapes.box(
                    -1.000D, yMid - thickness * 0.5D, z0,
                     2.000D, yMid + thickness * 0.5D, z1
            ));
        }

        return result.optimize();
    }

    private static VoxelShape rotateY(VoxelShape source, int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        VoxelShape result = source;

        for (int turn = 0; turn < turns; turn++) {
            VoxelShape rotated = Shapes.empty();

            for (AABB box : result.toAabbs()) {
                double x0 = 1.0D - box.maxZ;
                double x1 = 1.0D - box.minZ;
                double z0 = box.minX;
                double z1 = box.maxX;

                rotated = Shapes.or(
                        rotated,
                        Shapes.box(
                                Math.min(x0, x1),
                                box.minY,
                                Math.min(z0, z1),
                                Math.max(x0, x1),
                                box.maxY,
                                Math.max(z0, z1)
                        )
                );
            }

            result = rotated.optimize();
        }

        return result;
    }
}
