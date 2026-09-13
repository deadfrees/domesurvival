package com.wasted.domesurvival.forge.block;

import com.wasted.domesurvival.forge.lanos.LanosTrunkBlockEntity;
import com.wasted.domesurvival.forge.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Static decorative vehicle block used by both corrected Lanos models.
 *
 * <p>The supplied final models are exactly 7 blocks long, 3 blocks wide and
 * 2.5 blocks high. They intentionally render outside the anchor block.
 * Collision and selection follow the full vehicle volume so the model no
 * longer behaves like a single invisible anchor block.</p>
 */
public final class DecorativeLanosBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final ResourceLocation TRUNK_LOOT =
            new ResourceLocation("domesurvival", "chests/lanos_trunk");
    // North/south model bounds: X [-16, 32], Y [0, 40], Z [-48, 64].
    private static final VoxelShape NORTH_SOUTH_SHAPE =
            box(-16.0D, 0.0D, -48.0D, 32.0D, 40.0D, 64.0D);
    private static final VoxelShape EAST_WEST_SHAPE =
            box(-48.0D, 0.0D, -16.0D, 64.0D, 40.0D, 32.0D);

    public DecorativeLanosBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            ensureFootprint(level, pos, state);
            if (level.getBlockEntity(pos) instanceof LanosTrunkBlockEntity trunk) {
                trunk.ensureLootTable(level.random, TRUNK_LOOT);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LanosTrunkBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type != com.wasted.domesurvival.forge.registry.ModBlockEntities.LANOS_TRUNK.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) ->
                LanosTrunkBlockEntity.serverTick(
                        tickLevel, tickPos, tickState, (LanosTrunkBlockEntity) blockEntity);
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
            return openTrunk(level, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static InteractionResult openTrunk(Level level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DecorativeLanosBlock)) {
            return InteractionResult.FAIL;
        }
        LanosTrunkBlockEntity trunk = ensureTrunk(level, pos, state);
        if (trunk == null) return InteractionResult.FAIL;
        ensureFootprint(level, pos, state);
        trunk.ensureLootTable(level.random, TRUNK_LOOT);
        // ServerLevel#playSound excludes the supplied player. Send the opener
        // an explicit packet and use the world broadcast only for bystanders.
        level.playSound(player, pos, ModSounds.LANOS_TRUNK_OPEN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        player.playNotifySound(ModSounds.LANOS_TRUNK_OPEN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        net.minecraftforge.network.NetworkHooks.openScreen(player, trunk, pos);
        return InteractionResult.CONSUME;
    }

    public static void ensureFootprint(Level level, BlockPos controller, BlockState controllerState) {
        Direction facing = controllerState.getValue(FACING);
        for (int localY = 0; localY <= 2; localY++) {
            for (int localZ = 0; localZ <= 6; localZ++) {
                for (int localX = 0; localX <= 2; localX++) {
                    if (localX == 1 && localY == 0 && localZ == 3) continue;
                    int x = localX - 1;
                    int z = localZ - 3;
                    BlockPos partPos = controller.relative(facing.getClockWise(), x)
                            .relative(facing, z).above(localY);
                    BlockState current = level.getBlockState(partPos);
                    if (current.is(ModBlocks.LANOS_HITBOX_PART.get())
                            && LanosHitboxPartBlock.controllerPosition(partPos, current).equals(controller)) {
                        continue;
                    }
                    if (!current.isAir() && !current.canBeReplaced()) continue;
                    BlockState part = ModBlocks.LANOS_HITBOX_PART.get().defaultBlockState()
                            .setValue(LanosHitboxPartBlock.FACING, facing)
                            .setValue(LanosHitboxPartBlock.LOCAL_X, localX)
                            .setValue(LanosHitboxPartBlock.LOCAL_Y, localY)
                            .setValue(LanosHitboxPartBlock.LOCAL_Z, localZ);
                    level.setBlock(partPos, part, Block.UPDATE_ALL);
                }
            }
        }
    }

    private static void clearFootprint(Level level, BlockPos controller, BlockState controllerState) {
        Direction facing = controllerState.getValue(FACING);
        for (int localY = 0; localY <= 2; localY++) {
            for (int localZ = 0; localZ <= 6; localZ++) {
                for (int localX = 0; localX <= 2; localX++) {
                    if (localX == 1 && localY == 0 && localZ == 3) continue;
                    BlockPos partPos = controller.relative(facing.getClockWise(), localX - 1)
                            .relative(facing, localZ - 3).above(localY);
                    BlockState current = level.getBlockState(partPos);
                    if (current.is(ModBlocks.LANOS_HITBOX_PART.get())
                            && LanosHitboxPartBlock.controllerPosition(partPos, current).equals(controller)) {
                        level.removeBlock(partPos, false);
                    }
                }
            }
        }
    }

    /**
     * Vehicles placed by older builds have no block entity in their chunk.
     * Upgrade them in place the first time they are opened instead of forcing
     * the player to break and replace the whole model.
     */
    private static LanosTrunkBlockEntity ensureTrunk(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof LanosTrunkBlockEntity existing) {
            return existing;
        }
        LanosTrunkBlockEntity created = new LanosTrunkBlockEntity(pos, state);
        level.setBlockEntity(created);
        created.setLootTable(TRUNK_LOOT, level.random.nextLong());
        created.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        return created;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LanosTrunkBlockEntity trunk) {
            Containers.dropContents(level, pos, trunk);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            clearFootprint(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X
                ? EAST_WEST_SHAPE
                : NORTH_SOUTH_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The model extends outside its anchor block. Give the vehicle the same
        // full 7x3x2.5 volume for collision as it has for selection.
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X
                ? EAST_WEST_SHAPE
                : NORTH_SOUTH_SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
