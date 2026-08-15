package com.wasted.domesurvival.forge.airlock;

import com.wasted.domesurvival.forge.airlock.gate.AirlockGateBlock;
import com.wasted.domesurvival.forge.airlock.gate.AirlockGateMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent V56 link state for one wall-mounted airlock control panel.
 *
 * The panel stores exactly one gate master position. Many panel entities may
 * point at the same gate, so one gate naturally supports 1..N control panels.
 * No ticking is used; loaded panels are indexed only for event-driven visual
 * synchronization when the gate changes motion state.
 */
public final class AirlockControlPanelBlockEntity extends BlockEntity {
    private static final String TAG_LINK_DIMENSION = "LinkedGateDimension";
    private static final String TAG_LINK_POS = "LinkedGatePos";

    @Nullable
    private ResourceKey<Level> linkedGateDimension;
    @Nullable
    private BlockPos linkedGatePos;

    public AirlockControlPanelBlockEntity(BlockPos pos, BlockState state) {
        super(AirlockPanelRegistry.AIRLOCK_CONTROL_PANEL_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean hasBinding() {
        return linkedGateDimension != null && linkedGatePos != null;
    }

    @Nullable
    public ResourceKey<Level> linkedGateDimension() {
        return linkedGateDimension;
    }

    @Nullable
    public BlockPos linkedGatePos() {
        return linkedGatePos;
    }

    public boolean isLinkedTo(ResourceKey<Level> dimension, BlockPos gateMasterPos) {
        return hasBinding()
                && linkedGateDimension.equals(dimension)
                && linkedGatePos.equals(gateMasterPos);
    }

    public void bind(ServerLevel level, BlockPos gateMasterPos) {
        unregisterCurrentLink();
        linkedGateDimension = level.dimension();
        linkedGatePos = gateMasterPos.immutable();
        setChanged();
        AirlockPanelLinkIndex.register(level, linkedGatePos, worldPosition);
        syncVisualFromGate(level);
    }

    public void clearBinding() {
        unregisterCurrentLink();
        linkedGateDimension = null;
        linkedGatePos = null;
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            AirlockPanelLinkIndex.setPanelActive(serverLevel, worldPosition, false);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!(level instanceof ServerLevel serverLevel) || !hasBinding()) {
            return;
        }

        if (serverLevel.dimension().equals(linkedGateDimension)) {
            AirlockPanelLinkIndex.register(serverLevel, linkedGatePos, worldPosition);
            syncVisualFromGate(serverLevel);
        }
    }

    @Override
    public void setRemoved() {
        unregisterCurrentLink();
        super.setRemoved();
    }

    private void unregisterCurrentLink() {
        if (!(level instanceof ServerLevel serverLevel)
                || linkedGateDimension == null
                || linkedGatePos == null
                || !serverLevel.dimension().equals(linkedGateDimension)) {
            return;
        }
        AirlockPanelLinkIndex.unregister(serverLevel, linkedGatePos, worldPosition);
    }

    private void syncVisualFromGate(ServerLevel level) {
        if (!hasBinding() || !level.dimension().equals(linkedGateDimension)) {
            AirlockPanelLinkIndex.setPanelActive(level, worldPosition, false);
            return;
        }

        BlockState gateState = level.getBlockState(linkedGatePos);
        boolean active = gateState.getBlock() instanceof AirlockGateBlock gate
                && gate.isValidMaster(level, linkedGatePos)
                && gateState.getValue(AirlockGateBlock.MOTION) != AirlockGateMotion.CLOSED;

        AirlockPanelLinkIndex.setPanelActive(level, worldPosition, active);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!hasBinding()) {
            return;
        }

        tag.putString(TAG_LINK_DIMENSION, linkedGateDimension.location().toString());
        tag.putLong(TAG_LINK_POS, linkedGatePos.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedGateDimension = null;
        linkedGatePos = null;

        if (!tag.contains(TAG_LINK_DIMENSION) || !tag.contains(TAG_LINK_POS)) {
            return;
        }

        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(TAG_LINK_DIMENSION));
        if (dimensionId == null) {
            return;
        }

        linkedGateDimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        linkedGatePos = BlockPos.of(tag.getLong(TAG_LINK_POS));
    }
}
