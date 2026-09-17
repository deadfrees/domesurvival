package com.wasted.domesurvival.forge.machine.gasturbine;

import com.wasted.domesurvival.forge.capability.IGasStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.gas.GasPipeTransferService;
import com.wasted.domesurvival.forge.gas.GasStorage;
import com.wasted.domesurvival.forge.gas.ModGases;
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

public final class GasTurbineGeneratorBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int ENERGY_CAPACITY = 150_000;
    public static final int MAX_FE_OUTPUT = 256;
    public static final int GENERATION_PER_TICK = 160;
    public static final int GAS_CAPACITY = 8_000;
    public static final int GAS_PER_TICK = 1;
    public static final int GAS_PULL_RATE = 200;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_GAS = 2;
    public static final int DATA_GAS_CAPACITY = 3;
    public static final int DATA_STATUS = 4;
    public static final int DATA_GENERATION = 5;
    public static final int DATA_COUNT = 6;

    public static final int READY = 0;
    public static final int GENERATING = 1;
    public static final int NO_GAS = 2;
    public static final int ENERGY_FULL = 3;

    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_GAS = "GasTank";

    private final MachineEnergyStorage energy =
            new MachineEnergyStorage(ENERGY_CAPACITY, 0, MAX_FE_OUTPUT);

    private final GasStorage gasTank = new GasStorage(
            GAS_CAPACITY,
            GAS_PULL_RATE,
            0,
            ModGases.MINERAL_GAS::equals,
            this::setChanged
    );

    private final IGasStorage gasInput = new IGasStorage() {
        @Override
        public int receiveGas(net.minecraft.resources.ResourceLocation gas, int maxReceive, boolean simulate) {
            return gasTank.receiveGas(gas, maxReceive, simulate);
        }

        @Override
        public int extractGas(net.minecraft.resources.ResourceLocation gas, int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public @Nullable net.minecraft.resources.ResourceLocation getGasType() {
            return gasTank.getGasType();
        }

        @Override public int getGasStored() { return gasTank.getGasStored(); }
        @Override public int getMaxGasStored() { return gasTank.getMaxGasStored(); }
        @Override public boolean canReceiveGas(net.minecraft.resources.ResourceLocation gas) { return gasTank.canReceiveGas(gas); }
        @Override public boolean canExtractGas(net.minecraft.resources.ResourceLocation gas) { return false; }
    };

    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
    private LazyOptional<IGasStorage> gasCap = LazyOptional.of(() -> gasInput);
    private boolean active;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energy.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> energy.getMaxEnergyStored();
                case DATA_GAS -> gasTank.getGasStored();
                case DATA_GAS_CAPACITY -> gasTank.getMaxGasStored();
                case DATA_STATUS -> status();
                case DATA_GENERATION -> GENERATION_PER_TICK;
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

    public GasTurbineGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(GasTurbineGeneratorRegistry.GAS_TURBINE_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  GasTurbineGeneratorBlockEntity turbine) {
        boolean changed = false;

        int pushed = turbine.pushEnergy(level, pos);
        if (pushed > 0) changed = true;

        int pulled = GasPipeTransferService.pull(
                level,
                pos,
                turbine.gasTank,
                ModGases.MINERAL_GAS,
                GAS_PULL_RATE,
                direction -> true
        );
        if (pulled > 0) changed = true;

        boolean canGenerate = turbine.gasTank.getGasStored() >= GAS_PER_TICK
                && turbine.energy.getMaxEnergyStored() - turbine.energy.getEnergyStored() >= GENERATION_PER_TICK;

        turbine.active = canGenerate;
        if (canGenerate) {
            int consumed = turbine.gasTank.removeInternal(ModGases.MINERAL_GAS, GAS_PER_TICK, false);
            if (consumed == GAS_PER_TICK) {
                turbine.energy.addEnergyInternal(GENERATION_PER_TICK);
                changed = true;
            } else {
                turbine.active = false;
            }
        }

        if (state.getValue(GasTurbineGeneratorBlock.ACTIVE) != turbine.active) {
            level.setBlock(pos, state.setValue(GasTurbineGeneratorBlock.ACTIVE, turbine.active), 3);
            changed = true;
        }

        if (changed) turbine.setChanged();
    }

    private int pushEnergy(Level level, BlockPos pos) {
        int remaining = Math.min(MAX_FE_OUTPUT, energy.getEnergyStored());
        int moved = 0;
        if (remaining <= 0) return 0;

        for (Direction direction : Direction.values()) {
            if (remaining <= 0) break;
            BlockPos neighborPos = pos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) continue;

            IEnergyStorage target = neighbor.getCapability(
                    ForgeCapabilities.ENERGY,
                    direction.getOpposite()
            ).resolve().orElse(null);
            if (target == null || !target.canReceive()) continue;

            int accepted = target.receiveEnergy(remaining, false);
            if (accepted <= 0) continue;
            int extracted = energy.extractEnergy(accepted, false);
            moved += extracted;
            remaining -= extracted;
        }
        return moved;
    }

    private int status() {
        if (active) return GENERATING;
        if (gasTank.getGasStored() < GAS_PER_TICK) return NO_GAS;
        if (energy.getMaxEnergyStored() - energy.getEnergyStored() < GENERATION_PER_TICK) return ENERGY_FULL;
        return READY;
    }

    public ContainerData getDataAccess() {
        return data;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_ENERGY, energy.getEnergyStored());
        tag.put(NBT_GAS, gasTank.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        gasTank.deserializeNBT(tag.getCompound(NBT_GAS));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (cap == ModCapabilities.GAS) return gasCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        gasCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> energy);
        gasCap = LazyOptional.of(() -> gasInput);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.gas_turbine_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GasTurbineGeneratorMenu(id, inventory, this);
    }
}
