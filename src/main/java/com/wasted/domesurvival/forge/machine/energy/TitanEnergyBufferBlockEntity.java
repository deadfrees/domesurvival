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
 * FE storage wired through the same UnifiedSideConfig + PortVisual flow used
 * by CoalGenerator/WaterPurifier/OxygenElectrolyzer.
 */
public final class TitanEnergyBufferBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int MAX_RECEIVE_PER_TICK = 2_048;
    public static final int MAX_OUTPUT_PER_TICK = 2_048;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_INPUT_RATE_LOW = 2;
    public static final int DATA_INPUT_RATE_HIGH = 3;
    public static final int DATA_OUTPUT_RATE_LOW = 4;
    public static final int DATA_OUTPUT_RATE_HIGH = 5;
    public static final int DATA_SIDES_START = 6;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

    private static final String NBT_ENERGY = "Energy";

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();
    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE_PER_TICK, MAX_OUTPUT_PER_TICK);

    // Live FE transfer telemetry. These values are intentionally transient: they
    // describe the current/previous server tick and do not belong in saved NBT.
    private long transferStatsTick = Long.MIN_VALUE;
    private int receivedThisTick;
    private int sentThisTick;
    private int lastReceivedPerTick;
    private int lastSentPerTick;

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = energyStorage.receiveEnergy(maxReceive, simulate);
            if (!simulate && accepted > 0) {
                recordInput(accepted);
                onEnergyChanged();
            }
            return accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IEnergyStorage energyOutputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = energyStorage.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                recordOutput(extracted);
                onEnergyChanged();
            }
            return extracted;
        }

        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    private final IEnergyStorage energyCombinedView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return energyInputView.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return energyOutputView.extractEnergy(maxExtract, simulate);
        }

        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    };

    private LazyOptional<IEnergyStorage> energyInputCapability = LazyOptional.of(() -> energyInputView);
    private LazyOptional<IEnergyStorage> energyOutputCapability = LazyOptional.of(() -> energyOutputView);
    private LazyOptional<IEnergyStorage> energyCombinedCapability = LazyOptional.of(() -> energyCombinedView);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_CAPACITY) return energyStorage.getMaxEnergyStored();
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

    public TitanEnergyBufferBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_BUFFER_TITAN.get(), pos, state);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, TitanEnergyBufferBlockEntity buffer) {
        buffer.rollTransferStats();
        boolean changed = buffer.pushEnergyToNeighbors(level, pos) > 0;
        buffer.syncEnergyLevel();
        if (changed) {
            buffer.setChanged();
            buffer.notifyComparator();
        }
    }

    private int pushEnergyToNeighbors(Level level, BlockPos pos) {
        int totalTransferred = 0;
        for (Direction direction : Direction.values()) {
            if (isFrontWorldSide(direction) || !sideConfig.allowsOutput(direction)) continue;

            int remainingOutput = MAX_OUTPUT_PER_TICK - totalTransferred;
            if (remainingOutput <= 0 || energyStorage.getEnergyStored() <= 0) break;

            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) continue;

            IEnergyStorage target = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (target == null || !target.canReceive()) continue;

            int available = energyStorage.extractEnergy(remainingOutput, true);
            if (available <= 0) break;

            int acceptedSimulation = target.receiveEnergy(available, true);
            if (acceptedSimulation <= 0) continue;

            int extracted = energyStorage.extractEnergy(acceptedSimulation, false);
            int acceptedActual = target.receiveEnergy(extracted, false);
            if (acceptedActual < extracted) {
                energyStorage.addEnergyInternal(extracted - acceptedActual);
            }
            if (acceptedActual > 0) recordOutput(acceptedActual);
            totalTransferred += acceptedActual;
        }
        return totalTransferred;
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

    private void onEnergyChanged() {
        setChanged();
        syncEnergyLevel();
        notifyComparator();
    }

    private int computeEnergyLevel() {
        int capacity = Math.max(1, energyStorage.getMaxEnergyStored());
        return Math.max(0, Math.min(4,
                (int) Math.round((energyStorage.getEnergyStored() * 4.0D) / capacity)));
    }

    private void syncEnergyLevel() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof TitanEnergyBufferBlock)) return;
        int energyLevel = computeEnergyLevel();
        if (state.getValue(TitanEnergyBufferBlock.ENERGY_LEVEL) != energyLevel) {
            level.setBlock(worldPosition, state.setValue(TitanEnergyBufferBlock.ENERGY_LEVEL, energyLevel), 3);
        }
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
     * Side configuration changes alter the externally visible FE capability.
     * Re-sync the BlockEntity only when the side config changes; regular FE
     * transfers are represented by the blockstate energy indicator / menu data.
     */
    private void syncClientState() {
        if (level == null || level.isClientSide) return;
        BlockState current = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, current, current, Block.UPDATE_CLIENTS);
    }

    /**
     * Some transport mods cache an adjacent capability. A disabled side returns
     * an empty LazyOptional, so there is nothing for that neighbour to listen to
     * when the side later becomes INPUT/OUTPUT. A vanilla neighbour notification
     * forces those transports to re-scan the face after the mode changes.
     */
    private void notifyNeighborConnections() {
        if (level == null || level.isClientSide) return;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof TitanEnergyBufferBlock)) return;
        PortVisual visual = isFrontWorldSide(direction)
                ? PortVisual.OFF
                : PortVisual.fromMode(sideConfig.getMode(direction));
        var property = TitanEnergyBufferBlock.portProperty(direction);
        if (state.getValue(property) != visual) {
            level.setBlock(worldPosition, state.setValue(property, visual), 3);
        }
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof TitanEnergyBufferBlock)) return;

        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction)
                    ? PortVisual.OFF
                    : PortVisual.fromMode(sideConfig.getMode(direction));
            updated = updated.setValue(TitanEnergyBufferBlock.portProperty(direction), visual);
        }
        updated = updated.setValue(TitanEnergyBufferBlock.ENERGY_LEVEL, computeEnergyLevel());

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
        return state.hasProperty(TitanEnergyBufferBlock.FACING)
                ? state.getValue(TitanEnergyBufferBlock.FACING)
                : Direction.NORTH;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        sideConfig.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));

        if (!sideConfig.load(tag)) {
            migrateLegacySideConfiguration(tag);
        }
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);
    }

    private void migrateLegacySideConfiguration(CompoundTag tag) {
        applyDefaultSideConfiguration();
        Direction facing = getMachineFacing();

        loadLegacyMode(tag, "ModeUp", Direction.UP);
        loadLegacyMode(tag, "ModeDown", Direction.DOWN);
        loadLegacyMode(tag, "ModeNorth", Direction.NORTH);
        loadLegacyMode(tag, "ModeSouth", Direction.SOUTH);
        loadLegacyMode(tag, "ModeWest", Direction.WEST);
        loadLegacyMode(tag, "ModeEast", Direction.EAST);

        // If no per-world-side NBT existed, preserve the legacy relative blockstate
        // defaults used by V33-V38.
        if (!hasAnyLegacyMode(tag)) {
            sideConfig.setMode(RelativeSide.LEFT.resolve(facing), SideMode.INPUT);
            sideConfig.setMode(RelativeSide.RIGHT.resolve(facing), SideMode.OUTPUT);
        }
    }

    private static boolean hasAnyLegacyMode(CompoundTag tag) {
        return tag.contains("ModeUp") || tag.contains("ModeDown")
                || tag.contains("ModeNorth") || tag.contains("ModeSouth")
                || tag.contains("ModeWest") || tag.contains("ModeEast");
    }

    private void loadLegacyMode(CompoundTag tag, String key, Direction direction) {
        if (!tag.contains(key)) return;
        int value = tag.getInt(key);
        SideMode mode = switch (value) {
            case 1 -> SideMode.INPUT;
            case 2 -> SideMode.OUTPUT;
            default -> SideMode.DISABLED;
        };
        sideConfig.setMode(direction, mode);
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

    private void notifyComparator() {
        if (level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
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
        return Component.translatable("block.domesurvival.energy_buffer_titan");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TitanEnergyBufferMenu(containerId, inventory, this);
    }
}
