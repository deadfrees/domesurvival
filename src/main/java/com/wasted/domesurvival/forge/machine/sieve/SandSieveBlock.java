package com.wasted.domesurvival.forge.machine.sieve;

import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import com.wasted.domesurvival.forge.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public final class SandSieveBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Shapes.or(
            box(1, 0, 1, 15, 4, 15),
            box(1, 4, 1, 3, 13, 3), box(13, 4, 1, 15, 13, 3),
            box(1, 4, 13, 3, 13, 15), box(13, 4, 13, 15, 13, 15),
            box(1, 10, 1, 15, 13, 3), box(1, 10, 13, 15, 13, 15),
            box(1, 10, 3, 3, 13, 13), box(13, 10, 3, 15, 13, 13)
    );

    public SandSieveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The authored model's control face points south in its north variant,
        // so using the player's look direction (without inversion) presents it
        // toward the player when placed.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(2) != 0) return;
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                pos.getX() + 0.28D + random.nextDouble() * 0.44D,
                pos.getY() + 0.68D,
                pos.getZ() + 0.28D + random.nextDouble() * 0.44D,
                (random.nextDouble() - 0.5D) * 0.025D,
                -0.035D,
                (random.nextDouble() - 0.5D) * 0.025D);
        if (random.nextInt(3) == 0) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    pos.getX() + 0.36D + random.nextDouble() * 0.28D,
                    pos.getY() + 0.40D,
                    pos.getZ() + 0.36D + random.nextDouble() * 0.28D,
                    0.0D, -0.022D, 0.0D);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SandSieveBlockEntity sieve)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (FluidUtil.getFluidHandler(held).isPresent()) {
            if (!level.isClientSide) {
                FluidUtil.interactWithFluidHandler(player, hand, sieve.getWaterTank());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Sneaking always opens maintenance access. A normal click starts a
            // prepared cycle; when inputs are incomplete it opens the GUI instead.
            if (!player.isShiftKeyDown() && sieve.tryStartCycle()) {
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.45F, 0.85F);
                level.playSound(null, pos, ModSounds.SAND_SIEVE_PROCESS.get(), SoundSource.BLOCKS,
                        0.72F, 1.0F);
            } else {
                NetworkHooks.openScreen(serverPlayer, sieve, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SandSieveBlockEntity sieve) {
            for (int slot = 0; slot < sieve.getInventory().getSlots(); slot++) {
                ItemStack stack = sieve.getInventory().getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D, stack.copy());
                    level.addFreshEntity(entity);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SandSieveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.SAND_SIEVE.get(), SandSieveBlockEntity::serverTick);
    }
}
