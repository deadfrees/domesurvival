package com.wasted.domesurvival.forge.machine.transformer;

import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlock;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeNetwork;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
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
 * Automatic two-port FE transformer.
 *
 * <p>DomeSurvival voltage is represented by energy-pipe transfer classes. The rear
 * blue port detects the input pipe tier, the front amber port detects the output pipe
 * tier. There is no manual mode selector.</p>
 */
public final class TransformerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 100_000;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_MODE = 2;
    public static final int DATA_INPUT_RATE = 3;
    public static final int DATA_OUTPUT_RATE = 4;
    public static final int DATA_INPUT_THIS_TICK = 5;
    public static final int DATA_OUTPUT_THIS_TICK = 6;
    public static final int DATA_ACTIVE = 7;
    public static final int DATA_VALID = 8;
    public static final int DATA_COUNT = 9;

    private static final String NBT_ENERGY = "Energy";

    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, 0, 0);

    @Nullable private TransformerMode detectedMode;
    private long budgetGameTime = Long.MIN_VALUE;
    private int receivedThisTick;
    private int extractedThisTick;
    private boolean active;

    private final IEnergyStorage inputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return receiveLimited(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return resolveMode(false) != null;
        }
    };

    private final IEnergyStorage outputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return extractLimited(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return resolveMode(false) != null;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private LazyOptional<IEnergyStorage> inputCapability =
            LazyOptional.of(() -> inputView);
    private LazyOptional<IEnergyStorage> outputCapability =
            LazyOptional.of(() -> outputView);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            refreshBudgets();
            TransformerMode mode = resolveMode(false);

            return switch (index) {
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_CAPACITY -> energyStorage.getMaxEnergyStored();
                case DATA_MODE -> mode == null ? -1 : mode.ordinal();
                case DATA_INPUT_RATE -> mode == null ? 0 : mode.inputRate();
                case DATA_OUTPUT_RATE -> mode == null ? 0 : mode.outputRate();
                case DATA_INPUT_THIS_TICK -> receivedThisTick;
                case DATA_OUTPUT_THIS_TICK -> extractedThisTick;
                case DATA_ACTIVE -> active ? 1 : 0;
                case DATA_VALID -> mode != null ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(TransformerRegistry.TRANSFORMER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TransformerBlockEntity transformer
    ) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        transformer.refreshBudgets();
        TransformerMode mode = transformer.resolveMode(true);

        if (mode == null) {
            transformer.active = false;
            return;
        }

        /*
         * Energy pipes are network nodes rather than batteries. Process the two
         * independent networks in deterministic order so the rear network can fill
         * the transformer and the front network can drain it in the same server tick.
         */
        transformer.primePipeNetwork(transformer.getInputSide());
        transformer.primePipeNetwork(transformer.getOutputSide());

        boolean wasActive = transformer.active;
        transformer.active =
                transformer.receivedThisTick > 0
                        || transformer.extractedThisTick > 0;

        if (transformer.active || wasActive != transformer.active) {
            transformer.setChanged();
        }
    }

    @Nullable
    private TransformerMode resolveMode(boolean refreshConnections) {
        TransformerMode resolved = TransformerMode.fromTiers(
                getAdjacentPipeTier(getInputSide()),
                getAdjacentPipeTier(getOutputSide())
        );

        if (resolved == detectedMode) {
            return detectedMode;
        }

        detectedMode = resolved;
        budgetGameTime = Long.MIN_VALUE;
        receivedThisTick = 0;
        extractedThisTick = 0;
        active = false;
        setChanged();

        if (refreshConnections && level != null && !level.isClientSide) {
            refreshAdjacentPipeConnections();
        }

        return detectedMode;
    }

    @Nullable
    private EnergyPipeTier getAdjacentPipeTier(Direction direction) {
        if (level == null) {
            return null;
        }

        BlockState state = level.getBlockState(worldPosition.relative(direction));
        return state.getBlock() instanceof EnergyPipeBlock pipe
                ? pipe.tier()
                : null;
    }

    private void primePipeNetwork(Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pipePos = worldPosition.relative(direction);
        if (serverLevel.getBlockState(pipePos).getBlock() instanceof EnergyPipeBlock) {
            EnergyPipeNetwork.tick(serverLevel, pipePos);
        }
    }

    private void refreshAdjacentPipeConnections() {
        if (level == null) {
            return;
        }

        for (Direction side : new Direction[]{getInputSide(), getOutputSide()}) {
            BlockPos pipePos = worldPosition.relative(side);
            BlockState oldState = level.getBlockState(pipePos);

            if (!(oldState.getBlock() instanceof EnergyPipeBlock)) {
                continue;
            }

            BlockState refreshed =
                    EnergyPipeBlock.refreshConnections(level, pipePos, oldState);

            if (!refreshed.equals(oldState)) {
                level.setBlock(
                        pipePos,
                        refreshed,
                        Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS
                );
            } else {
                level.sendBlockUpdated(
                        pipePos,
                        oldState,
                        oldState,
                        Block.UPDATE_CLIENTS
                );
            }
        }
    }

    private int receiveLimited(int maxReceive, boolean simulate) {
        TransformerMode mode = resolveMode(false);
        if (mode == null || maxReceive <= 0) {
            return 0;
        }

        refreshBudgets();

        int remainingRate =
                Math.max(0, mode.inputRate() - receivedThisTick);
        int freeSpace = Math.max(
                0,
                energyStorage.getMaxEnergyStored()
                        - energyStorage.getEnergyStored()
        );
        int accepted =
                Math.min(maxReceive, Math.min(remainingRate, freeSpace));

        if (!simulate && accepted > 0) {
            int stored = energyStorage.addEnergyInternal(accepted);
            receivedThisTick += stored;

            if (stored > 0) {
                active = true;
                setChanged();
            }

            return stored;
        }

        return accepted;
    }

    private int extractLimited(int maxExtract, boolean simulate) {
        TransformerMode mode = resolveMode(false);
        if (mode == null || maxExtract <= 0) {
            return 0;
        }

        refreshBudgets();

        int remainingRate =
                Math.max(0, mode.outputRate() - extractedThisTick);
        int available = energyStorage.getEnergyStored();
        int extracted =
                Math.min(maxExtract, Math.min(remainingRate, available));

        if (!simulate && extracted > 0) {
            int removed = energyStorage.removeEnergyInternal(extracted);
            extractedThisTick += removed;

            if (removed > 0) {
                active = true;
                setChanged();
            }

            return removed;
        }

        return extracted;
    }

    private void refreshBudgets() {
        long gameTime = level == null ? 0L : level.getGameTime();

        if (budgetGameTime != gameTime) {
            budgetGameTime = gameTime;
            receivedThisTick = 0;
            extractedThisTick = 0;
            active = false;
        }
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(TransformerBlock.FACING)
                ? state.getValue(TransformerBlock.FACING)
                : Direction.NORTH;
    }

    public Direction getInputSide() {
        return getMachineFacing().getOpposite();
    }

    public Direction getOutputSide() {
        return getMachineFacing();
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        detectedMode = null;
        budgetGameTime = Long.MIN_VALUE;
        receivedThisTick = 0;
        extractedThisTick = 0;
        active = false;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side
    ) {
        if (cap == ForgeCapabilities.ENERGY) {
            if (resolveMode(false) == null) {
                return LazyOptional.empty();
            }

            if (side == getInputSide()) {
                return inputCapability.cast();
            }

            if (side == getOutputSide()) {
                return outputCapability.cast();
            }

            return LazyOptional.empty();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCapability.invalidate();
        outputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inputCapability = LazyOptional.of(() -> inputView);
        outputCapability = LazyOptional.of(() -> outputView);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.transformer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new TransformerMenu(containerId, playerInventory, this);
    }
}
