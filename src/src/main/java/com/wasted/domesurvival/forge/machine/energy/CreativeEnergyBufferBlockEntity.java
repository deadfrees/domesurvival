package com.wasted.domesurvival.forge.machine.energy;

import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Infinite creative FE source/sink.
 *
 * OUTPUT/BOTH sides can provide any amount requested through the FE
 * capability. Automatic neighbour pushing is intentionally uncapped per
 * configured output side, making this useful for stress-testing machines and
 * cable throughput. INPUT/BOTH sides accept and discard incoming FE.
 */
public final class CreativeEnergyBufferBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int DISPLAY_ENERGY = Integer.MAX_VALUE;
    public static final int DISPLAY_CAPACITY = Integer.MAX_VALUE;
    public static final int MAX_RECEIVE_PER_TICK = Integer.MAX_VALUE;
    public static final int MAX_OUTPUT_PER_TICK = Integer.MAX_VALUE;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_INPUT_RATE_LOW = 2;
    public static final int DATA_INPUT_RATE_HIGH = 3;
    public static final int DATA_OUTPUT_RATE_LOW = 4;
    public static final int DATA_OUTPUT_RATE_HIGH = 5;
    public static final int DATA_SIDES_START = 6;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

    private long transferStatsTick = Long.MIN_VALUE;
    private int receivedThisTick;
    private int sentThisTick;
    private int lastReceivedPerTick;
    private int lastSentPerTick;

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.max(0, maxReceive);
            if (!simulate && accepted > 0) recordInput(accepted);
            return accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return DISPLAY_ENERGY; }
        @Override public int getMaxEnergyStored() { return DISPLAY_CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IEnergyStorage energyOutputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.max(0, maxExtract);
            if (!simulate && extracted > 0) recordOutput(extracted);
            return extracted;
        }

        @Override public int getEnergyStored() { return DISPLAY_ENERGY; }
        @Override public int getMaxEnergyStored() { return DISPLAY_CAPACITY; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private final IEnergyStorage energyCombinedView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.max(0, maxReceive);
            if (!simulate && accepted > 0) recordInput(accepted);
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.max(0, maxExtract);
            if (!simulate && extracted > 0) recordOutput(extracted);
            return extracted;
        }

        @Override public int getEnergyStored() { return DISPLAY_ENERGY; }
        @Override public int getMaxEnergyStored() { return DISPLAY_CAPACITY; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> energyInputCapability = LazyOptional.of(() -> energyInputView);
    private LazyOptional<IEnergyStorage> energyOutputCapability = LazyOptional.of(() -> energyOutputView);
    private LazyOptional<IEnergyStorage> energyCombinedCapability = LazyOptional.of(() -> energyCombinedView);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_ENERGY) return DISPLAY_ENERGY;
            if (index == DATA_CAPACITY) return DISPLAY_CAPACITY;
            int inputRate = getDisplayedInputPerTick();
            int outputRate = getDisplayedOutputPerTick();
            if (index == DATA_INPUT_RATE_LOW) return inputRate & 0xFFFF;
            if (index == DATA_INPUT_RATE_HIGH) return (inputRate >>> 16) & 0xFFFF;
            if (index == DATA_OUTPUT_RATE_LOW) return outputRate & 0xFFFF;
            if (index == DATA_OUTPUT_RATE_HIGH) return (outputRate >>> 16) & 0xFFFF;
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) {
                Direction direction = Direction.values()[index - DATA_SIDES_START];
                return sideConfig.getMode(direction).ordinal();
            }
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public CreativeEnergyBufferBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_BUFFER_CREATIVE.get(), pos, state);
        applyDefaultSideConfiguration();
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        sideConfig.setMode(RelativeSide.LEFT.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.RIGHT.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.TOP.resolve(facing), SideMode.DISABLED);
        sideConfig.setMode(RelativeSide.BOTTOM.resolve(facing), SideMode.DISABLED);
        sideConfig.setMode(RelativeSide.BACK.resolve(facing), SideMode.DISABLED);
        sideConfig.setMode(RelativeSide.FRONT.resolve(facing), SideMode.DISABLED);
    }

    public static boolean isConfigurableSide(RelativeSide side) {
        return side != RelativeSide.FRONT;
    }

    private boolean isFrontWorldSide(Direction side) {
        return side == getMachineFacing();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeEnergyBufferBlockEntity buffer) {
        buffer.rollTransferStats();
        buffer.pushEnergyToNeighbors(level, pos);
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isFrontWorldSide(direction) || !sideConfig.allowsOutput(direction)) continue;

            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) continue;

            IEnergyStorage target = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (target == null || !target.canReceive()) continue;

            int acceptedSimulation = target.receiveEnergy(Integer.MAX_VALUE, true);
            if (acceptedSimulation > 0) {
                int acceptedActual = target.receiveEnergy(acceptedSimulation, false);
                if (acceptedActual > 0) recordOutput(acceptedActual);
            }
        }
    }

    private void rollTransferStats() {
        if (level == null || level.isClientSide) return;
        long gameTime = level.getGameTime();
        if (transferStatsTick == Long.MIN_VALUE) {
            transferStatsTick = gameTime;
            return;
        }
        if (gameTime != transferStatsTick) {
            lastReceivedPerTick = receivedThisTick;
            lastSentPerTick = sentThisTick;
            receivedThisTick = 0;
            sentThisTick = 0;
            transferStatsTick = gameTime;
        }
    }

    private static int saturatingAdd(int current, int amount) {
        if (amount <= 0) return current;
        return (int) Math.min(Integer.MAX_VALUE, (long) current + amount);
    }

    private void recordInput(int amount) {
        if (amount <= 0 || level == null || level.isClientSide) return;
        rollTransferStats();
        receivedThisTick = saturatingAdd(receivedThisTick, amount);
    }

    private void recordOutput(int amount) {
        if (amount <= 0 || level == null || level.isClientSide) return;
        rollTransferStats();
        sentThisTick = saturatingAdd(sentThisTick, amount);
    }

    private int getDisplayedInputPerTick() {
        rollTransferStats();
        return receivedThisTick > 0 ? receivedThisTick : lastReceivedPerTick;
    }

    private int getDisplayedOutputPerTick() {
        rollTransferStats();
        return sentThisTick > 0 ? sentThisTick : lastSentPerTick;
    }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction worldSide = relativeSide.resolve(getMachineFacing());
        SideMode mode = sideConfig.cycleMode(worldSide);
        refreshCapabilities();
        syncPortState(worldSide);
        setChanged();
        syncClientState();
        notifyNeighborConnections();
        return mode;
    }

    /**
     * Sync side configuration to the client only when it changes. This keeps
     * client-side capability queries consistent without sending packets for the
     * infinite energy value every tick.
     */
    private void syncClientState() {
        if (level == null || level.isClientSide) return;
        BlockState current = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, current, current, Block.UPDATE_CLIENTS);
    }

    /**
     * Transport mods may cache an empty capability while a face is disabled.
     * Invalidating our non-empty LazyOptionals is not enough for that case, so
     * notify adjacent blocks after a side mode change and force a fresh scan.
     */
    private void notifyNeighborConnections() {
        if (level == null || level.isClientSide) return;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof CreativeEnergyBufferBlock)) return;

        PortVisual visual = isFrontWorldSide(direction)
                ? PortVisual.OFF
                : PortVisual.fromMode(sideConfig.getMode(direction));
        var property = CreativeEnergyBufferBlock.portProperty(direction);
        if (state.getValue(property) != visual) {
            level.setBlock(worldPosition, state.setValue(property, visual), 3);
        }
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof CreativeEnergyBufferBlock)) return;

        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction)
                    ? PortVisual.OFF
                    : PortVisual.fromMode(sideConfig.getMode(direction));
            updated = updated.setValue(CreativeEnergyBufferBlock.portProperty(direction), visual);
        }

        if (!updated.equals(state)) {
            level.setBlock(worldPosition, updated, 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncAllPortStates();
        notifyNeighborConnections();
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(CreativeEnergyBufferBlock.FACING)
                ? state.getValue(CreativeEnergyBufferBlock.FACING)
                : Direction.NORTH;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public int getEnergyStored() {
        return DISPLAY_ENERGY;
    }

    public int getEnergyCapacity() {
        return DISPLAY_CAPACITY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        sideConfig.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (!sideConfig.load(tag)) {
            applyDefaultSideConfiguration();
        }
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            if (side == null) return energyCombinedCapability.cast();
            if (isFrontWorldSide(side)) return LazyOptional.empty();

            boolean input = sideConfig.allowsInput(side);
            boolean output = sideConfig.allowsOutput(side);

            if (input && output) return energyCombinedCapability.cast();
            if (input) return energyInputCapability.cast();
            if (output) return energyOutputCapability.cast();
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    private void refreshCapabilities() {
        energyInputCapability.invalidate();
        energyOutputCapability.invalidate();
        energyCombinedCapability.invalidate();
        energyInputCapability = LazyOptional.of(() -> energyInputView);
        energyOutputCapability = LazyOptional.of(() -> energyOutputView);
        energyCombinedCapability = LazyOptional.of(() -> energyCombinedView);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyInputCapability.invalidate();
        energyOutputCapability.invalidate();
        energyCombinedCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        refreshCapabilities();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.energy_buffer_creative");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CreativeEnergyBufferMenu(containerId, inventory, this);
    }
}
