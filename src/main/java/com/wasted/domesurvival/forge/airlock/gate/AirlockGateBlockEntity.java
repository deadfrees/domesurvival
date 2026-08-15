package com.wasted.domesurvival.forge.airlock.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * One lightweight render/sync anchor for the entire formed 2x2..5x5 gate.
 *
 * There is no BlockEntity ticker. The server only schedules one completion
 * block tick when an animation starts. Client smoothness comes from BER
 * partial-tick interpolation, not from extra server ticks or per-part ticking BEs.
 *
 * V57 also stores one optional reciprocal interlock partner master position.
 * Two paired gates are therefore protected server-side even when several
 * control panels point to either gate.
 */
public final class AirlockGateBlockEntity extends BlockEntity {
    private static final String TAG_INTERLOCK_POS = "InterlockGatePos";
    private static final String TAG_GATE_SIZE = "GateSize";

    private int gateSize = 5;
    private long animationStartTick = -1L;
    private int animationDurationTicks = 40;
    private boolean opening = true;

    @Nullable
    private BlockPos interlockGatePos;

    public AirlockGateBlockEntity(BlockPos pos, BlockState state) {
        super(AirlockGateRegistry.AIRLOCK_GATE_BLOCK_ENTITY.get(), pos, state);
    }

    public int gateSize() {
        return AirlockGateBlock.normalizeGateSize(gateSize);
    }

    public void setGateSize(int size) {
        int normalized = AirlockGateBlock.normalizeGateSize(size);
        if (gateSize == normalized) {
            return;
        }

        gateSize = normalized;
        setChanged();
        syncToClient();
    }

    public void beginAnimation(boolean opening, long startTick, int durationTicks) {
        this.opening = opening;
        this.animationStartTick = startTick;
        this.animationDurationTicks = Math.max(1, durationTicks);
        setChanged();
        syncToClient();
    }

    public float renderProgress(float partialTick) {
        BlockState state = getBlockState();
        if (!state.hasProperty(AirlockGateBlock.MOTION)) {
            return 0.0F;
        }

        AirlockGateMotion motion = state.getValue(AirlockGateBlock.MOTION);
        if (motion == AirlockGateMotion.CLOSED) {
            return 0.0F;
        }
        if (motion == AirlockGateMotion.OPEN) {
            return 1.0F;
        }

        if (level == null || animationStartTick < 0L) {
            return motion.opening() ? 0.0F : 1.0F;
        }

        double raw = ((double) level.getGameTime() + partialTick - animationStartTick)
                / (double) Math.max(1, animationDurationTicks);
        float t = (float) Math.max(0.0D, Math.min(1.0D, raw));

        // Quintic smootherstep: zero velocity and zero acceleration at both ends.
        float smooth = t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
        return opening ? smooth : 1.0F - smooth;
    }

    public boolean isOpeningAnimation() {
        return opening;
    }

    public long animationStartTick() {
        return animationStartTick;
    }

    public int animationDurationTicks() {
        return animationDurationTicks;
    }

    public boolean hasInterlockGate() {
        return interlockGatePos != null;
    }

    @Nullable
    public BlockPos interlockGatePos() {
        return interlockGatePos;
    }

    public void setInterlockGate(BlockPos gateMasterPos) {
        interlockGatePos = gateMasterPos.immutable();
        setChanged();
        syncToClient();
    }

    public void clearInterlockGate() {
        if (interlockGatePos == null) {
            return;
        }

        interlockGatePos = null;
        setChanged();
        syncToClient();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeSyncData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        gateSize = tag.contains(TAG_GATE_SIZE)
                ? AirlockGateBlock.normalizeGateSize(tag.getInt(TAG_GATE_SIZE))
                : 5;
        animationStartTick = tag.getLong("GateAnimStart");
        animationDurationTicks = Math.max(1, tag.getInt("GateAnimDuration"));
        opening = tag.getBoolean("GateAnimOpening");
        interlockGatePos = tag.contains(TAG_INTERLOCK_POS)
                ? BlockPos.of(tag.getLong(TAG_INTERLOCK_POS))
                : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeSyncData(tag);
        return tag;
    }

    private void writeSyncData(CompoundTag tag) {
        tag.putInt(TAG_GATE_SIZE, gateSize());
        tag.putLong("GateAnimStart", animationStartTick);
        tag.putInt("GateAnimDuration", animationDurationTicks);
        tag.putBoolean("GateAnimOpening", opening);

        if (interlockGatePos != null) {
            tag.putLong(TAG_INTERLOCK_POS, interlockGatePos.asLong());
        } else {
            tag.remove(TAG_INTERLOCK_POS);
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        int size = gateSize();
        int masterIndex = AirlockGateBlock.masterIndex(size);
        double margin = 0.25D;

        BlockState state = getBlockState();
        Direction facing = state.hasProperty(AirlockGateBlock.FACING)
                ? state.getValue(AirlockGateBlock.FACING)
                : Direction.NORTH;

        double minX;
        double minY = worldPosition.getY() - masterIndex - margin;
        double minZ;
        double maxX;
        double maxY = worldPosition.getY() + (size - masterIndex) + margin;
        double maxZ;

        if (facing.getAxis() == Direction.Axis.Z) {
            minX = worldPosition.getX() - masterIndex - margin;
            maxX = worldPosition.getX() + (size - masterIndex) + margin;
            minZ = worldPosition.getZ() - margin;
            maxZ = worldPosition.getZ() + 1.0D + margin;
        } else {
            minX = worldPosition.getX() - margin;
            maxX = worldPosition.getX() + 1.0D + margin;
            minZ = worldPosition.getZ() - masterIndex - margin;
            maxZ = worldPosition.getZ() + (size - masterIndex) + margin;
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
