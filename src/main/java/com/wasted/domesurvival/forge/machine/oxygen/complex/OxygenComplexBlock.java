package com.wasted.domesurvival.forge.machine.oxygen.complex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** One physical module of the fixed 2x2 Oxygen Complex. */
public final class OxygenComplexBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final OxygenComplexRole role;

    public OxygenComplexBlock(OxygenComplexRole role, Properties properties) {
        super(properties);
        this.role = role;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(ACTIVE, false));
    }

    public OxygenComplexRole role() {
        return role;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(ACTIVE, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, FORMED, ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenComplexBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            OxygenComplexStructure.refreshFrom(serverLevel, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            OxygenComplexStructure.invalidateBeforeRemoval(serverLevel, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof OxygenComplexBlockEntity part) {
                OxygenComplexBlockEntity controller = part.getControllerEntity();
                if (controller != null) {
                    NetworkHooks.openScreen(serverPlayer, controller, controller.getBlockPos());
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.domesurvival.oxygen_complex.incomplete"),
                            true
                    );
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }


    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        if (role != OxygenComplexRole.AIR_INTAKE
                || !state.getValue(FORMED)
                || !state.getValue(ACTIVE)) {
            return;
        }

        // Keep the effect readable but cheap: about one suction puff every two
        // client ticks. Particles start outside the intake face and travel
        // toward the grille.
        if (!random.nextBoolean()) {
            return;
        }

        Direction front = state.getValue(FACING);
        double frontX = front.getStepX();
        double frontZ = front.getStepZ();

        // Perpendicular vector used to spread the particles across the intake.
        double sideX = -frontZ;
        double sideZ = frontX;

        double lateral = (random.nextDouble() - 0.5D) * 0.62D;
        double distance = 0.82D + random.nextDouble() * 0.42D;

        double x = pos.getX() + 0.5D + frontX * distance + sideX * lateral;
        double y = pos.getY() + 0.34D + random.nextDouble() * 0.38D;
        double z = pos.getZ() + 0.5D + frontZ * distance + sideZ * lateral;

        double inward = 0.045D + random.nextDouble() * 0.018D;
        double vx = -frontX * inward + sideX * (random.nextDouble() - 0.5D) * 0.004D;
        double vy = (random.nextDouble() - 0.5D) * 0.004D;
        double vz = -frontZ * inward + sideZ * (random.nextDouble() - 0.5D) * 0.004D;

        level.addParticle(ParticleTypes.CLOUD, x, y, z, vx, vy, vz);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || role != OxygenComplexRole.OUTPUT) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                OxygenComplexRegistry.BLOCK_ENTITY.get(),
                OxygenComplexBlockEntity::serverTick
        );
    }
}
