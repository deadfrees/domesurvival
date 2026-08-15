package com.wasted.domesurvival.forge.airlock.gate;

import com.wasted.domesurvival.forge.airlock.AirlockPanelLinkIndex;
import com.wasted.domesurvival.forge.airlock.AirlockPanelRegistry;
import com.wasted.domesurvival.forge.sound.ModSounds;
import com.wasted.domesurvival.forge.oxygen.room.SealedRoomManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Craftable square 2x2..5x5 multiblock airlock gate.
 *
 * V55D:
 * - ALL 25 segments participate in opening;
 * - the full formed aperture becomes collision-free in OPEN;
 * - two half-width leaves slide into the side pockets;
 * - leaves never pop/disappear at OPEN; side pockets clip them smoothly;
 * - one master BlockEntity provides per-frame partial-tick interpolation;
 * - no BlockEntity ticker; only one scheduled completion tick per movement;
 * - closing is cancelled/reversed if a living entity occupies the actual aperture.
 *
 * Direct RMB toggle is temporary for testing V55. V56 will move normal control
 * to the link-key + airlock-panel system.
 */
public final class AirlockGateBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty MASTER = BooleanProperty.create("master");
    public static final EnumProperty<AirlockGatePart> PART =
            EnumProperty.create("part", AirlockGatePart.class);
    public static final EnumProperty<AirlockGateMotion> MOTION =
            EnumProperty.create("motion", AirlockGateMotion.class);

    public static final int MIN_GATE_SIZE = 2;
    public static final int MAX_GATE_SIZE = 5;

    private static final int LEGACY_SIZE = 5;
    private static final int ANIMATION_DURATION_TICKS = 40;

    /** 5 px gate thickness = 5/16 block. */
    private static final VoxelShape NORTH_SOUTH_SHAPE =
            Block.box(0.0D, 0.0D, 5.5D, 16.0D, 16.0D, 10.5D);
    private static final VoxelShape EAST_WEST_SHAPE =
            Block.box(5.5D, 0.0D, 0.0D, 10.5D, 16.0D, 16.0D);

    public AirlockGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(MASTER, false)
                .setValue(PART, AirlockGatePart.P22)
                .setValue(MOTION, AirlockGateMotion.CLOSED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, MASTER, PART, MOTION);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only the center/master of a formed gate needs a render anchor.
        if (state.getValue(FORMED) && state.getValue(MASTER)) {
            return new AirlockGateBlockEntity(pos, state);
        }
        return null;
    }

    /**
     * V55.2: every formed gate is rendered exclusively by the one master BER.
     * This removes the baked-model <-> BER handoff at the first/last animation
     * frame, which was the source of the one-frame flash when closing.
     *
     * Unformed construction blocks still use their normal baked block model.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Nullable
    private AirlockGateBlockEntity ensureMasterEntity(Level level, BlockPos masterPos) {
        BlockState state = level.getBlockState(masterPos);
        if (!state.is(this)
                || !state.getValue(FORMED)
                || !state.getValue(MASTER)) {
            return null;
        }

        BlockEntity existing = level.getBlockEntity(masterPos);
        if (existing instanceof AirlockGateBlockEntity gateEntity) {
            return gateEntity;
        }

        AirlockGateBlockEntity created = new AirlockGateBlockEntity(masterPos, state);
        level.setBlockEntity(created);
        created.setGateSize(
                inferGateSizeFromStructure(level, masterPos, state, false)
        );
        return created;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(MASTER, false)
                .setValue(PART, AirlockGatePart.P22)
                .setValue(MOTION, AirlockGateMotion.CLOSED);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction preferred = placer == null
                ? Direction.NORTH
                : placer.getDirection().getOpposite();

        Formation formation = tryFindLegacy5x5Formation(level, pos, preferred);
        if (formation == null) return;

        form(level, formation);

        if (placer instanceof ServerPlayer player) {
            player.displayClientMessage(
                    Component.literal("Ворота шлюза сформированы: 5×5"),
                    true
            );
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }

        if (player.getItemInHand(hand).is(AirlockPanelRegistry.AIRLOCK_BINDING_KEY.get())) {
            // PASS lets AirlockBindingKeyItem.useOn resolve any of the 25 parts
            // back to the one master controller.
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // V56: normal operation is panel-driven. Direct RMB remains disabled so
        // a player cannot bypass the future V57 two-gate interlock.
        player.displayClientMessage(
                Component.translatable("message.domesurvival.airlock_gate.use_panel"),
                true
        );
        return InteractionResult.CONSUME;
    }

    @Nullable
    public BlockPos resolveMasterPos(Level level, BlockPos partPos, BlockState partState) {
        if (!partState.is(this) || !partState.getValue(FORMED)) {
            return null;
        }

        AirlockGatePart part = partState.getValue(PART);
        Direction facing = partState.getValue(FACING);

        // No gate-size BlockState property is used. That would multiply the
        // already-large gate state table by four. Instead, the one master BE
        // stores the size and a part resolves it with at most four master checks.
        for (int size = MAX_GATE_SIZE; size >= MIN_GATE_SIZE; size--) {
            if (part.column() >= size || part.row() >= size) {
                continue;
            }

            int master = masterIndex(size);
            BlockPos candidate = facing.getAxis() == Direction.Axis.Z
                    ? partPos.offset(
                            master - part.column(),
                            master - part.row(),
                            0
                    )
                    : partPos.offset(
                            0,
                            master - part.row(),
                            master - part.column()
                    );

            if (!level.hasChunkAt(candidate)) {
                continue;
            }

            BlockState masterState = level.getBlockState(candidate);
            if (!masterState.is(this)
                    || !masterState.getValue(FORMED)
                    || !masterState.getValue(MASTER)) {
                continue;
            }

            int actualSize = gateSizeForMaster(level, candidate, masterState);
            if (actualSize == size) {
                return candidate;
            }
        }

        return null;
    }

    public boolean isValidMaster(Level level, BlockPos masterPos) {
        BlockState masterState = level.getBlockState(masterPos);
        if (!masterState.is(this)
                || !masterState.getValue(FORMED)
                || !masterState.getValue(MASTER)) {
            return false;
        }

        int size = gateSizeForMaster(level, masterPos, masterState);
        int master = masterIndex(size);
        AirlockGatePart part = masterState.getValue(PART);
        return part.column() == master && part.row() == master;
    }

    public int gateSizeForMaster(Level level, BlockPos masterPos) {
        BlockState state = level.getBlockState(masterPos);
        if (!state.is(this)
                || !state.getValue(FORMED)
                || !state.getValue(MASTER)) {
            return LEGACY_SIZE;
        }
        return gateSizeForMaster(level, masterPos, state);
    }

    private int gateSizeForMaster(Level level,
                                  BlockPos masterPos,
                                  BlockState masterState) {
        BlockEntity raw = level.getBlockEntity(masterPos);
        if (raw instanceof AirlockGateBlockEntity gateEntity) {
            return gateEntity.gateSize();
        }

        return inferGateSizeFromStructure(level, masterPos, masterState, false);
    }

    private int inferGateSizeFromStructure(Level level,
                                           BlockPos masterPos,
                                           BlockState masterState,
                                           boolean masterAlreadyRemoved) {
        if (!masterState.is(this)
                || !masterState.getValue(FORMED)
                || !masterState.getValue(MASTER)) {
            return LEGACY_SIZE;
        }

        AirlockGatePart masterPart = masterState.getValue(PART);
        Direction facing = masterState.getValue(FACING);

        for (int size = MAX_GATE_SIZE; size >= MIN_GATE_SIZE; size--) {
            int master = masterIndex(size);
            if (masterPart.column() != master || masterPart.row() != master) {
                continue;
            }

            boolean matches = true;
            for (int row = 0; row < size && matches; row++) {
                for (int col = 0; col < size; col++) {
                    if (masterAlreadyRemoved && col == master && row == master) {
                        continue;
                    }

                    BlockPos partPos = facing.getAxis() == Direction.Axis.Z
                            ? masterPos.offset(col - master, row - master, 0)
                            : masterPos.offset(0, row - master, col - master);
                    if (!level.hasChunkAt(partPos)) {
                        matches = false;
                        break;
                    }

                    BlockState state = level.getBlockState(partPos);

                    if (!state.is(this)
                            || !state.getValue(FORMED)
                            || state.getValue(FACING) != facing
                            || state.getValue(PART) != AirlockGatePart.at(col, row)
                            || state.getValue(MASTER) != (col == master && row == master)) {
                        matches = false;
                        break;
                    }
                }
            }

            if (matches) {
                return size;
            }
        }

        // Existing V55-V60 worlds have no GateSize NBT and are always 5x5.
        return LEGACY_SIZE;
    }

    @Nullable
    private GateLayout layoutFromMaster(Level level,
                                        BlockPos masterPos,
                                        BlockState masterState) {
        if (!masterState.is(this)
                || !masterState.getValue(FORMED)
                || !masterState.getValue(MASTER)) {
            return null;
        }

        int size = gateSizeForMaster(level, masterPos, masterState);
        return GateLayout.from(masterPos, masterState, size);
    }

    @Nullable
    private GateLayout layoutForRemovedPart(Level level,
                                            BlockPos removedPos,
                                            BlockState removedState) {
        if (!removedState.is(this) || !removedState.getValue(FORMED)) {
            return null;
        }

        if (removedState.getValue(MASTER)) {
            int size;
            BlockEntity raw = level.getBlockEntity(removedPos);
            if (raw instanceof AirlockGateBlockEntity gateEntity) {
                size = gateEntity.gateSize();
            } else {
                size = inferGateSizeFromStructure(
                        level,
                        removedPos,
                        removedState,
                        true
                );
            }
            return GateLayout.from(removedPos, removedState, size);
        }

        BlockPos masterPos = resolveMasterPos(level, removedPos, removedState);
        if (masterPos == null) {
            return null;
        }

        BlockState masterState = level.getBlockState(masterPos);
        return layoutFromMaster(level, masterPos, masterState);
    }

    public ToggleResult requestToggle(ServerLevel level, BlockPos masterPos) {
        if (!isValidMaster(level, masterPos)) {
            return ToggleResult.INVALID;
        }

        BlockState masterState = level.getBlockState(masterPos);
        AirlockGateMotion motion = masterState.getValue(MOTION);
        if (motion.moving()) {
            return ToggleResult.MOVING;
        }

        GateLayout layout = layoutFromMaster(level, masterPos, masterState);
        if (layout == null) {
            return ToggleResult.INVALID;
        }

        if (motion == AirlockGateMotion.CLOSED) {
            InterlockCheck interlockCheck = checkInterlockBeforeOpening(level, masterPos);
            if (interlockCheck == InterlockCheck.BLOCKED) {
                return ToggleResult.INTERLOCK_BLOCKED;
            }
            if (interlockCheck == InterlockCheck.INVALID) {
                return ToggleResult.INTERLOCK_INVALID;
            }

            startOpening(level, layout);
            return ToggleResult.OPENING;
        }
        if (motion == AirlockGateMotion.OPEN) {
            // Closing is always allowed. The safety rule only prevents a second
            // gate from starting to OPEN before its paired gate is fully CLOSED.
            startClosing(level, layout);
            return ToggleResult.CLOSING;
        }
        return ToggleResult.MOVING;
    }

    private InterlockCheck checkInterlockBeforeOpening(ServerLevel level, BlockPos masterPos) {
        AirlockGateBlockEntity gateEntity = ensureMasterEntity(level, masterPos);
        if (gateEntity == null || !gateEntity.hasInterlockGate()) {
            return InterlockCheck.CLEAR;
        }

        BlockPos otherMasterPos = gateEntity.interlockGatePos();
        if (otherMasterPos == null
                || otherMasterPos.equals(masterPos)
                || !isValidMaster(level, otherMasterPos)) {
            return InterlockCheck.INVALID;
        }

        BlockEntity otherEntityRaw = level.getBlockEntity(otherMasterPos);
        if (!(otherEntityRaw instanceof AirlockGateBlockEntity otherEntity)
                || otherEntity.interlockGatePos() == null
                || !masterPos.equals(otherEntity.interlockGatePos())) {
            return InterlockCheck.INVALID;
        }

        BlockState otherState = level.getBlockState(otherMasterPos);
        return otherState.getValue(MOTION) == AirlockGateMotion.CLOSED
                ? InterlockCheck.CLEAR
                : InterlockCheck.BLOCKED;
    }

    public InterlockPairResult pairInterlock(ServerLevel level,
                                             BlockPos firstMasterPos,
                                             BlockPos secondMasterPos) {
        if (firstMasterPos.equals(secondMasterPos)) {
            return InterlockPairResult.SAME_GATE;
        }
        if (!isValidMaster(level, firstMasterPos) || !isValidMaster(level, secondMasterPos)) {
            return InterlockPairResult.INVALID;
        }

        BlockState firstState = level.getBlockState(firstMasterPos);
        BlockState secondState = level.getBlockState(secondMasterPos);
        if (firstState.getValue(MOTION) != AirlockGateMotion.CLOSED
                || secondState.getValue(MOTION) != AirlockGateMotion.CLOSED) {
            return InterlockPairResult.NOT_CLOSED;
        }

        AirlockGateBlockEntity firstEntity = ensureMasterEntity(level, firstMasterPos);
        AirlockGateBlockEntity secondEntity = ensureMasterEntity(level, secondMasterPos);
        if (firstEntity == null || secondEntity == null) {
            return InterlockPairResult.INVALID;
        }

        // A gate belongs to only one two-door airlock pair. Re-pairing safely
        // removes any previous reciprocal relationship before writing the new one.
        clearInterlockPair(level, firstMasterPos);
        clearInterlockPair(level, secondMasterPos);

        firstEntity = ensureMasterEntity(level, firstMasterPos);
        secondEntity = ensureMasterEntity(level, secondMasterPos);
        if (firstEntity == null || secondEntity == null) {
            return InterlockPairResult.INVALID;
        }

        firstEntity.setInterlockGate(secondMasterPos);
        secondEntity.setInterlockGate(firstMasterPos);
        return InterlockPairResult.PAIRED;
    }

    public InterlockPairResult clearInterlockPair(ServerLevel level, BlockPos masterPos) {
        if (!isValidMaster(level, masterPos)) {
            return InterlockPairResult.INVALID;
        }

        BlockEntity raw = level.getBlockEntity(masterPos);
        if (!(raw instanceof AirlockGateBlockEntity gateEntity)
                || !gateEntity.hasInterlockGate()) {
            return InterlockPairResult.NOT_PAIRED;
        }

        BlockPos otherPos = gateEntity.interlockGatePos();
        gateEntity.clearInterlockGate();

        if (otherPos != null) {
            BlockEntity otherRaw = level.getBlockEntity(otherPos);
            if (otherRaw instanceof AirlockGateBlockEntity otherEntity
                    && masterPos.equals(otherEntity.interlockGatePos())) {
                otherEntity.clearInterlockGate();
            }
        }

        return InterlockPairResult.UNPAIRED;
    }

    private enum InterlockCheck {
        CLEAR,
        BLOCKED,
        INVALID
    }

    public enum ToggleResult {
        OPENING,
        CLOSING,
        MOVING,
        INTERLOCK_BLOCKED,
        INTERLOCK_INVALID,
        INVALID
    }

    public enum InterlockPairResult {
        PAIRED,
        UNPAIRED,
        NOT_PAIRED,
        NOT_CLOSED,
        SAME_GATE,
        INVALID
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this)
                || !state.getValue(FORMED)
                || !state.getValue(MASTER)) {
            return;
        }

        GateLayout layout = layoutFromMaster(level, pos, state);
        if (layout == null) {
            return;
        }

        AirlockGateMotion motion = state.getValue(MOTION);

        if (motion.opening()) {
            setMotion(level, layout, AirlockGateMotion.OPEN);
            return;
        }

        if (motion.closing()) {
            // Collision remains empty until the final closed state. If a living
            // entity occupies any part of the actual passage, immediately reverse.
            if (!isFullOpeningClear(level, layout)) {
                AirlockGateBlockEntity gateEntity =
                        ensureMasterEntity(level, layout.masterPos());
                if (gateEntity != null) {
                    gateEntity.beginAnimation(
                            true,
                            level.getGameTime(),
                            ANIMATION_DURATION_TICKS
                    );
                }

                setMotion(level, layout, AirlockGateMotion.OPENING_1);
                level.scheduleTick(
                        layout.masterPos(),
                        this,
                        ANIMATION_DURATION_TICKS
                );
                playGateSound(level, layout.masterPos(), true);
                return;
            }

            setMotion(level, layout, AirlockGateMotion.CLOSED);
        }
    }

    private void startOpening(ServerLevel level, GateLayout layout) {
        AirlockGateBlockEntity gateEntity =
                ensureMasterEntity(level, layout.masterPos());

        if (gateEntity != null) {
            // Sync timing first so the client has interpolation data before the
            // dynamic state becomes visible.
            gateEntity.beginAnimation(
                    true,
                    level.getGameTime(),
                    ANIMATION_DURATION_TICKS
            );
        }

        setMotion(level, layout, AirlockGateMotion.OPENING_1);
        level.scheduleTick(
                layout.masterPos(),
                this,
                ANIMATION_DURATION_TICKS
        );
        playGateSound(level, layout.masterPos(), true);
    }

    private void startClosing(ServerLevel level, GateLayout layout) {
        AirlockGateBlockEntity gateEntity =
                ensureMasterEntity(level, layout.masterPos());

        if (gateEntity != null) {
            gateEntity.beginAnimation(
                    false,
                    level.getGameTime(),
                    ANIMATION_DURATION_TICKS
            );
        }

        setMotion(level, layout, AirlockGateMotion.CLOSING_1);
        level.scheduleTick(
                layout.masterPos(),
                this,
                ANIMATION_DURATION_TICKS
        );
        playGateSound(level, layout.masterPos(), false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!level.isClientSide
                && state.getValue(FORMED)
                && !state.is(newState.getBlock())) {
            GateLayout layout = layoutForRemovedPart(level, pos, state);
            if (layout != null && level instanceof ServerLevel serverLevel) {
                clearInterlockPairIfPresent(serverLevel, layout.masterPos());
                AirlockPanelLinkIndex.syncGate(
                        serverLevel,
                        layout.masterPos(),
                        AirlockGateMotion.CLOSED
                );
            }
            if (layout != null) {
                disassembleRemaining(level, layout);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void clearInterlockPairIfPresent(ServerLevel level, BlockPos masterPos) {
        BlockEntity raw = level.getBlockEntity(masterPos);
        if (!(raw instanceof AirlockGateBlockEntity gateEntity)
                || !gateEntity.hasInterlockGate()) {
            return;
        }

        BlockPos otherPos = gateEntity.interlockGatePos();
        gateEntity.clearInterlockGate();

        if (otherPos != null) {
            BlockEntity otherRaw = level.getBlockEntity(otherPos);
            if (otherRaw instanceof AirlockGateBlockEntity otherEntity
                    && masterPos.equals(otherEntity.interlockGatePos())) {
                otherEntity.clearInterlockGate();
            }
        }
    }

    /**
     * Selection shape. While OPEN, only the invisible master remains targetable
     * so V55 can still be closed by RMB during testing. V56 control comes from
     * the linked wall panel instead.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        if (!state.getValue(FORMED)) {
            return Shapes.block();
        }

        AirlockGateMotion motion = state.getValue(MOTION);
        if (motion == AirlockGateMotion.OPEN) {
            return state.getValue(MASTER) ? thinShape(state) : Shapes.empty();
        }

        return thinShape(state);
    }

    /**
     * Smooth BER motion is sub-tick, so we intentionally avoid fake stepped
     * collision. CLOSED blocks the plane; moving/open states leave the entire
     * formed passage collision-free until the final close completes.
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext context) {
        if (!state.getValue(FORMED)) {
            return Shapes.block();
        }

        return state.getValue(MOTION) == AirlockGateMotion.CLOSED
                ? thinShape(state)
                : Shapes.empty();
    }

    private VoxelShape thinShape(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z
                ? NORTH_SOUTH_SHAPE
                : EAST_WEST_SHAPE;
    }

    private boolean isFullOpeningClear(ServerLevel level, GateLayout layout) {
        int size = layout.size();
        AABB passage;

        if (layout.axis() == Direction.Axis.Z) {
            passage = new AABB(
                    layout.minX(),
                    layout.minY(),
                    layout.constant() - 0.75D,
                    layout.minX() + size,
                    layout.minY() + size,
                    layout.constant() + 1.75D
            );
        } else {
            passage = new AABB(
                    layout.constant() - 0.75D,
                    layout.minY(),
                    layout.minZ(),
                    layout.constant() + 1.75D,
                    layout.minY() + size,
                    layout.minZ() + size
            );
        }

        return level.getEntitiesOfClass(
                LivingEntity.class,
                passage,
                LivingEntity::isAlive
        ).isEmpty();
    }

    private void playGateSound(Level level, BlockPos pos, boolean opening) {
        level.playSound(
                null,
                pos,
                opening ? ModSounds.AIRLOCK_OPEN.get() : ModSounds.AIRLOCK_CLOSE.get(),
                SoundSource.BLOCKS,
                1.15F,
                1.0F
        );
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Legacy placement behavior is intentionally kept only for a complete 5x5.
     * Smaller gates are explicitly commissioned with the System Control Key so
     * a planned 4x4 cannot auto-form early as soon as a 2x2 corner exists.
     */
    @Nullable
    private Formation tryFindLegacy5x5Formation(Level level,
                                                 BlockPos placedPos,
                                                 Direction preferredFacing) {
        return findFormationOfSize(level, placedPos, preferredFacing, LEGACY_SIZE);
    }

    /**
     * System Control Key commissioning path for 2x2..5x5.
     * Search is largest-first by design.
     */
    @Nullable
    public CommissionedGate commissionLargestSquare(ServerLevel level,
                                                     BlockPos clickedPos,
                                                     Direction preferredFacing) {
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!clickedState.is(this) || clickedState.getValue(FORMED)) {
            return null;
        }

        for (int size = MAX_GATE_SIZE; size >= MIN_GATE_SIZE; size--) {
            Formation formation = findFormationOfSize(
                    level,
                    clickedPos,
                    preferredFacing,
                    size
            );
            if (formation == null) {
                continue;
            }

            form(level, formation);
            BlockPos masterPos = formation.masterPos();
            return isValidMaster(level, masterPos)
                    ? new CommissionedGate(masterPos, size)
                    : null;
        }

        return null;
    }

    @Nullable
    private Formation findFormationOfSize(Level level,
                                          BlockPos placedPos,
                                          Direction preferredFacing,
                                          int size) {
        if (!isSupportedGateSize(size)) {
            return null;
        }

        Direction.Axis preferredAxis = preferredFacing.getAxis();

        Formation preferred = preferredAxis == Direction.Axis.Z
                ? findXY(level, placedPos, preferredFacing, size)
                : findZY(level, placedPos, preferredFacing, size);
        if (preferred != null) {
            return preferred;
        }

        return preferredAxis == Direction.Axis.Z
                ? findZY(level, placedPos, Direction.EAST, size)
                : findXY(level, placedPos, Direction.SOUTH, size);
    }

    @Nullable
    private Formation findXY(Level level,
                             BlockPos placedPos,
                             Direction preferredFacing,
                             int size) {
        for (int rowOffset = 0; rowOffset < size; rowOffset++) {
            int minY = placedPos.getY() - rowOffset;

            for (int colOffset = 0; colOffset < size; colOffset++) {
                int minX = placedPos.getX() - colOffset;
                int z = placedPos.getZ();

                if (!isUnformedRectangleXY(level, minX, minY, z, size)) {
                    continue;
                }

                Direction facing = preferredFacing.getAxis() == Direction.Axis.Z
                        ? preferredFacing
                        : Direction.SOUTH;
                return new Formation(Direction.Axis.Z, minX, minY, z, facing, size);
            }
        }
        return null;
    }

    @Nullable
    private Formation findZY(Level level,
                             BlockPos placedPos,
                             Direction preferredFacing,
                             int size) {
        for (int rowOffset = 0; rowOffset < size; rowOffset++) {
            int minY = placedPos.getY() - rowOffset;

            for (int colOffset = 0; colOffset < size; colOffset++) {
                int minZ = placedPos.getZ() - colOffset;
                int x = placedPos.getX();

                if (!isUnformedRectangleZY(level, x, minY, minZ, size)) {
                    continue;
                }

                Direction facing = preferredFacing.getAxis() == Direction.Axis.X
                        ? preferredFacing
                        : Direction.EAST;
                return new Formation(Direction.Axis.X, x, minY, minZ, facing, size);
            }
        }
        return null;
    }

    private boolean isUnformedRectangleXY(Level level,
                                           int minX,
                                           int minY,
                                           int z,
                                           int size) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BlockState state = level.getBlockState(
                        new BlockPos(minX + col, minY + row, z)
                );
                if (!state.is(this) || state.getValue(FORMED)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isUnformedRectangleZY(Level level,
                                           int x,
                                           int minY,
                                           int minZ,
                                           int size) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BlockState state = level.getBlockState(
                        new BlockPos(x, minY + row, minZ + col)
                );
                if (!state.is(this) || state.getValue(FORMED)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void form(Level level, Formation formation) {
        int size = formation.size();
        int master = masterIndex(size);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BlockPos partPos = formation.pos(col, row);
                BlockState oldState = level.getBlockState(partPos);

                BlockState formedState = oldState
                        .setValue(FACING, formation.facing())
                        .setValue(FORMED, true)
                        .setValue(MASTER, col == master && row == master)
                        .setValue(PART, AirlockGatePart.at(col, row))
                        .setValue(MOTION, AirlockGateMotion.CLOSED);

                level.setBlock(partPos, formedState, Block.UPDATE_CLIENTS);
            }
        }

        BlockPos masterPos = formation.masterPos();
        AirlockGateBlockEntity gateEntity = ensureMasterEntity(level, masterPos);
        if (gateEntity != null) {
            gateEntity.setGateSize(size);
        }
    }

    /**
     * Deterministic V58 structure-generation entry point.
     * Starter-dome gates remain 5x5 and keep their historical P22 master.
     */
    public boolean formGenerated5x5(ServerLevel level, BlockPos minCorner, Direction facing) {
        if (facing.getAxis() != Direction.Axis.Z) {
            return false;
        }
        if (!isUnformedRectangleXY(
                level,
                minCorner.getX(),
                minCorner.getY(),
                minCorner.getZ(),
                LEGACY_SIZE
        )) {
            return false;
        }

        Formation formation = new Formation(
                Direction.Axis.Z,
                minCorner.getX(),
                minCorner.getY(),
                minCorner.getZ(),
                facing,
                LEGACY_SIZE
        );
        form(level, formation);
        return isValidMaster(level, formation.masterPos());
    }

    private void setMotion(ServerLevel level,
                           GateLayout layout,
                           AirlockGateMotion motion) {
        int size = layout.size();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BlockPos partPos = layout.pos(col, row);
                BlockState state = level.getBlockState(partPos);

                if (!state.is(this) || !state.getValue(FORMED)) {
                    continue;
                }

                AirlockGatePart expectedPart = AirlockGatePart.at(col, row);
                if (state.getValue(PART) != expectedPart) {
                    continue;
                }

                if (state.getValue(MOTION) != motion) {
                    level.setBlock(
                            partPos,
                            state.setValue(MOTION, motion),
                            Block.UPDATE_CLIENTS
                    );
                }
            }
        }

        AirlockPanelLinkIndex.syncGate(level, layout.masterPos(), motion);

        // V60.1: gate state changes use UPDATE_CLIENTS. Explicitly invalidate
        // every actual NxN gate cell, not a hard-coded 5x5 area.
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                SealedRoomManager.invalidateGatePart(level, layout.pos(col, row));
            }
        }
    }

    private void disassembleRemaining(Level level, GateLayout layout) {
        int size = layout.size();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BlockPos partPos = layout.pos(col, row);
                unformAt(level, partPos);

                if (level instanceof ServerLevel serverLevel) {
                    SealedRoomManager.invalidateGatePart(serverLevel, partPos);
                }
            }
        }
    }

    private void unformAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || !state.getValue(FORMED)) {
            return;
        }

        if (state.getValue(MASTER)) {
            level.removeBlockEntity(pos);
        }

        level.setBlock(
                pos,
                state.setValue(FORMED, false)
                        .setValue(MASTER, false)
                        .setValue(PART, AirlockGatePart.P22)
                        .setValue(MOTION, AirlockGateMotion.CLOSED),
                Block.UPDATE_CLIENTS
        );
    }

    public static int normalizeGateSize(int size) {
        return isSupportedGateSize(size) ? size : LEGACY_SIZE;
    }

    public static int masterIndex(int size) {
        return normalizeGateSize(size) / 2;
    }

    public static float maxSlideBlocks(int size) {
        return (normalizeGateSize(size) / 2.0F) - (2.0F / 16.0F);
    }

    public static boolean isSupportedGateSize(int size) {
        return size >= MIN_GATE_SIZE && size <= MAX_GATE_SIZE;
    }

    public record CommissionedGate(BlockPos masterPos, int size) {
    }

    private record Formation(Direction.Axis axis,
                             int first,
                             int minY,
                             int second,
                             Direction facing,
                             int size) {
        BlockPos pos(int col, int row) {
            if (axis == Direction.Axis.Z) {
                return new BlockPos(first + col, minY + row, second);
            }
            return new BlockPos(first, minY + row, second + col);
        }

        BlockPos masterPos() {
            int master = masterIndex(size);
            return pos(master, master);
        }
    }

    private record GateLayout(Direction.Axis axis,
                              int minX,
                              int minY,
                              int minZ,
                              int constant,
                              Direction facing,
                              int size) {
        static GateLayout from(BlockPos pos, BlockState state, int size) {
            AirlockGatePart part = state.getValue(PART);
            Direction facing = state.getValue(FACING);

            if (facing.getAxis() == Direction.Axis.Z) {
                return new GateLayout(
                        Direction.Axis.Z,
                        pos.getX() - part.column(),
                        pos.getY() - part.row(),
                        pos.getZ(),
                        pos.getZ(),
                        facing,
                        normalizeGateSize(size)
                );
            }

            return new GateLayout(
                    Direction.Axis.X,
                    pos.getX(),
                    pos.getY() - part.row(),
                    pos.getZ() - part.column(),
                    pos.getX(),
                    facing,
                    normalizeGateSize(size)
            );
        }

        BlockPos pos(int col, int row) {
            if (axis == Direction.Axis.Z) {
                return new BlockPos(minX + col, minY + row, constant);
            }
            return new BlockPos(constant, minY + row, minZ + col);
        }

        BlockPos masterPos() {
            int master = masterIndex(size);
            return pos(master, master);
        }
    }
}
