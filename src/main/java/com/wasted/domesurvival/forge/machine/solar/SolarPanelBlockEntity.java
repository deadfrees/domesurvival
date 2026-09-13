package com.wasted.domesurvival.forge.machine.solar;

import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SolarPanelBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_GENERATION = 2;
    public static final int DATA_STATE = 3;
    public static final int DATA_MAX_OUTPUT = 4;
    public static final int DATA_COUNT = 5;

    private static final String NBT_ENERGY = "Energy";

    private final SolarPanelTier tier;
    private final MachineEnergyStorage energyStorage;
    private final IEnergyStorage energyOutputView;
    private final ContainerData dataAccess;
    private LazyOptional<IEnergyStorage> energyCapability;
    private SolarPanelState solarState = SolarPanelState.NIGHT;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(SolarPanelRegistry.blockEntityType(), pos, state);

        this.tier = state.getBlock() instanceof SolarPanelBlock panel
                ? panel.getTier()
                : SolarPanelTier.MK1;
        this.energyStorage = new MachineEnergyStorage(
                tier.energyCapacity(),
                0,
                tier.maxOutputPerTick()
        );
        this.energyOutputView = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return 0;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return energyStorage.extractEnergy(maxExtract, simulate);
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
                return true;
            }

            @Override
            public boolean canReceive() {
                return false;
            }
        };
        this.energyCapability = LazyOptional.of(() -> energyOutputView);

        this.dataAccess = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_ENERGY -> energyStorage.getEnergyStored();
                    case DATA_CAPACITY -> energyStorage.getMaxEnergyStored();
                    case DATA_GENERATION -> tier.generationPerTick();
                    case DATA_STATE -> solarState.networkId();
                    case DATA_MAX_OUTPUT -> tier.maxOutputPerTick();
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
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SolarPanelBlockEntity panel
    ) {
        boolean changed = false;

        SolarPanelState generationState = panel.determineState(level, pos);
        if (generationState == SolarPanelState.GENERATING) {
            if (panel.energyStorage.addEnergyInternal(panel.tier.generationPerTick()) > 0) {
                changed = true;
            }
        }

        if (panel.pushEnergyDown(level, pos) > 0) {
            changed = true;
        }

        SolarPanelState newState = panel.determineState(level, pos);
        if (newState != panel.solarState) {
            panel.solarState = newState;
            changed = true;
        }

        if (changed) {
            panel.setChanged();
        }
    }

    private SolarPanelState determineState(Level level, BlockPos pos) {
        if (!level.isDay()) {
            return SolarPanelState.NIGHT;
        }
        if (!level.canSeeSky(pos.above())) {
            return SolarPanelState.NO_SKY;
        }
        if (energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) {
            return SolarPanelState.BUFFER_FULL;
        }
        return SolarPanelState.GENERATING;
    }

    private int pushEnergyDown(Level level, BlockPos pos) {
        if (energyStorage.getEnergyStored() <= 0) {
            return 0;
        }

        BlockEntity neighbor = level.getBlockEntity(pos.below());
        if (neighbor == null) {
            return 0;
        }

        IEnergyStorage target = neighbor
                .getCapability(ForgeCapabilities.ENERGY, Direction.UP)
                .orElse(null);
        if (target == null || !target.canReceive()) {
            return 0;
        }

        int available = energyStorage.extractEnergy(tier.maxOutputPerTick(), true);
        if (available <= 0) {
            return 0;
        }

        int acceptedSimulation = target.receiveEnergy(available, true);
        if (acceptedSimulation <= 0) {
            return 0;
        }

        int extracted = energyStorage.extractEnergy(acceptedSimulation, false);
        if (extracted <= 0) {
            return 0;
        }

        int acceptedActual = target.receiveEnergy(extracted, false);
        if (acceptedActual < extracted) {
            energyStorage.addEnergyInternal(extracted - acceptedActual);
        }
        return acceptedActual;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public SolarPanelTier getTier() {
        return tier;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new SolarPanelMenu(containerId, playerInventory, this);
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
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side
    ) {
        if (cap == ForgeCapabilities.ENERGY && side == Direction.DOWN) {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }
}
