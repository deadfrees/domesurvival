package com.wasted.domesurvival.forge.machine.oxygen.complex;

import com.wasted.domesurvival.forge.capability.IOxygenStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.sound.MachineAmbientSoundService;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenStorage;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One lightweight BE type shared by all four modules.
 * Only the OUTPUT role executes the production tick and owns resources.
 */
public final class OxygenComplexBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MAX_ENERGY_INPUT = 512;
    public static final int COLLECTED_AIR_CAPACITY = 8_000;
    public static final int FILTERED_AIR_CAPACITY = 8_000;
    public static final int COMPRESSED_FEED_CAPACITY = 8_000;
    public static final int OXYGEN_CAPACITY = 16_000;
    public static final int MAX_OXYGEN_OUTPUT = 120;

    public static final int INTAKE_AIR_PER_TICK = 16;
    public static final int FILTER_INPUT_PER_TICK = 16;
    public static final int FILTER_OUTPUT_PER_TICK = 12;
    public static final int COMPRESS_INPUT_PER_TICK = 12;
    public static final int COMPRESS_OUTPUT_PER_TICK = 8;
    public static final int OXYGEN_INPUT_PER_TICK = 8;
    public static final int OXYGEN_OUTPUT_PER_TICK = 8;

    /**
     * Fixed operating draw for the complete 2x2 Oxygen Complex.
     * If at least one processing stage runs during a server tick, the
     * multiblock consumes exactly 512 FE once for that tick.
     */
    public static final int OPERATING_ENERGY_PER_TICK = 256;

    public static final int FILTER_WEAR_INTERVAL_TICKS = 200;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_COLLECTED_AIR = 2;
    public static final int DATA_COLLECTED_AIR_CAPACITY = 3;
    public static final int DATA_FILTERED_AIR = 4;
    public static final int DATA_FILTERED_AIR_CAPACITY = 5;
    public static final int DATA_COMPRESSED = 6;
    public static final int DATA_COMPRESSED_CAPACITY = 7;
    public static final int DATA_OXYGEN = 8;
    public static final int DATA_OXYGEN_CAPACITY = 9;
    public static final int DATA_STATUS = 10;
    public static final int DATA_CURRENT_FE_T = 11;
    public static final int DATA_FORMED = 12;
    public static final int DATA_INTAKE_ACTIVE = 13;
    public static final int DATA_FILTER_ACTIVE = 14;
    public static final int DATA_COMPRESS_ACTIVE = 15;
    public static final int DATA_OUTPUT_ACTIVE = 16;
    public static final int DATA_ATMOSPHERE = 17;
    public static final int DATA_FILTER_PRESENT = 18;
    public static final int DATA_SIDE_TOP = 19;
    public static final int DATA_SIDE_BOTTOM = 20;
    public static final int DATA_SIDE_FRONT = 21;
    public static final int DATA_SIDE_BACK = 22;
    public static final int DATA_SIDE_LEFT = 23;
    public static final int DATA_SIDE_RIGHT = 24;
    public static final int DATA_FILTER_DAMAGE = 25;
    public static final int DATA_FILTER_MAX_DAMAGE = 26;
    public static final int DATA_COUNT = 27;

    private static final int STATE_VERSION = 3;
    private static final String NBT_STATE_VERSION = "OxygenComplexStateVersion";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_COLLECTED = "CollectedAir";
    private static final String NBT_FILTERED = "FilteredAir";
    private static final String NBT_COMPRESSED = "CompressedFeed";
    private static final String NBT_OXYGEN = "Oxygen";
    private static final String NBT_CONTROLLER = "ControllerPos";
    private static final String NBT_HAS_CONTROLLER = "HasController";
    private static final String NBT_FILTER_INVENTORY = "AirFilterInventory";
    private static final String NBT_FILTER_WEAR_TICKS = "AirFilterWearTicks";

    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0);
    private final OxygenStorage oxygenStorage =
            new OxygenStorage(OXYGEN_CAPACITY, 0, MAX_OXYGEN_OUTPUT);
    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();
    private final ItemStackHandler filterInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            OxygenComplexBlockEntity.this.setChanged();
        }
    };

    private int collectedAir;
    private int filteredAir;
    private int compressedFeed;
    private int currentEnergyUse;
    private OxygenComplexStatus status = OxygenComplexStatus.INCOMPLETE;

    private boolean intakeActive;
    private boolean filterActive;
    private boolean compressionActive;
    private boolean outputActive;
    private boolean atmosphereAvailable;
    private int atmosphereCheckCooldown;
    private int structureCheckCooldown;
    private int filterWearTicks;
    private int ambientSoundTick;

    @Nullable
    private BlockPos controllerPos;
    private boolean linkedFormed;
    private boolean persistentControllerStateLoaded;

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = energyStorage.receiveEnergy(maxReceive, simulate);
            if (!simulate && accepted > 0) {
                setChanged();
            }
            return accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IOxygenStorage oxygenOutputView = new IOxygenStorage() {
        @Override public int receiveOxygen(int maxReceive, boolean simulate) { return 0; }

        @Override
        public int extractOxygen(int maxExtract, boolean simulate) {
            int extracted = oxygenStorage.extractOxygen(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }

        @Override public int getOxygenStored() { return oxygenStorage.getOxygenStored(); }
        @Override public int getMaxOxygenStored() { return oxygenStorage.getMaxOxygenStored(); }
        @Override public boolean canReceive() { return false; }
        @Override public boolean canExtract() { return true; }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyInputView);
    private LazyOptional<IOxygenStorage> oxygenCapability = LazyOptional.of(() -> oxygenOutputView);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> ENERGY_CAPACITY;
                case DATA_COLLECTED_AIR -> collectedAir;
                case DATA_COLLECTED_AIR_CAPACITY -> COLLECTED_AIR_CAPACITY;
                case DATA_FILTERED_AIR -> filteredAir;
                case DATA_FILTERED_AIR_CAPACITY -> FILTERED_AIR_CAPACITY;
                case DATA_COMPRESSED -> compressedFeed;
                case DATA_COMPRESSED_CAPACITY -> COMPRESSED_FEED_CAPACITY;
                case DATA_OXYGEN -> oxygenStorage.getOxygenStored();
                case DATA_OXYGEN_CAPACITY -> OXYGEN_CAPACITY;
                case DATA_STATUS -> status.ordinal();
                case DATA_CURRENT_FE_T -> currentEnergyUse;
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_INTAKE_ACTIVE -> intakeActive ? 1 : 0;
                case DATA_FILTER_ACTIVE -> filterActive ? 1 : 0;
                case DATA_COMPRESS_ACTIVE -> compressionActive ? 1 : 0;
                case DATA_OUTPUT_ACTIVE -> outputActive ? 1 : 0;
                case DATA_ATMOSPHERE -> atmosphereAvailable ? 1 : 0;
                case DATA_FILTER_PRESENT -> hasAirFilter() ? 1 : 0;
                case DATA_SIDE_TOP -> getSideMode(RelativeSide.TOP).ordinal();
                case DATA_SIDE_BOTTOM -> getSideMode(RelativeSide.BOTTOM).ordinal();
                case DATA_SIDE_FRONT -> getSideMode(RelativeSide.FRONT).ordinal();
                case DATA_SIDE_BACK -> getSideMode(RelativeSide.BACK).ordinal();
                case DATA_SIDE_LEFT -> getSideMode(RelativeSide.LEFT).ordinal();
                case DATA_SIDE_RIGHT -> getSideMode(RelativeSide.RIGHT).ordinal();
                case DATA_FILTER_DAMAGE -> getAirFilterDamage();
                case DATA_FILTER_MAX_DAMAGE -> getAirFilterMaxDamage();
                default -> 0;
            };
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public OxygenComplexBlockEntity(BlockPos pos, BlockState state) {
        super(OxygenComplexRegistry.BLOCK_ENTITY.get(), pos, state);
        applyDefaultSideConfiguration();
    }

    public OxygenComplexRole role() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof OxygenComplexBlock block
                ? block.role()
                : OxygenComplexRole.OUTPUT;
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(OxygenComplexBlock.FACING)
                ? state.getValue(OxygenComplexBlock.FACING)
                : Direction.NORTH;
    }

    public boolean isController() {
        return role() == OxygenComplexRole.OUTPUT;
    }

    public boolean isFormed() {
        BlockState state = getBlockState();
        return linkedFormed
                && state.hasProperty(OxygenComplexBlock.FORMED)
                && state.getValue(OxygenComplexBlock.FORMED);
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public ItemStackHandler getFilterInventory() {
        return filterInventory;
    }

    public boolean hasAirFilter() {
        return OxygenComplexFilters.isAirFilter(filterInventory.getStackInSlot(0));
    }

    public int getAirFilterDamage() {
        ItemStack stack = filterInventory.getStackInSlot(0);
        return hasAirFilter() && stack.isDamageableItem() ? stack.getDamageValue() : 0;
    }

    public int getAirFilterMaxDamage() {
        ItemStack stack = filterInventory.getStackInSlot(0);
        return hasAirFilter() && stack.isDamageableItem() ? stack.getMaxDamage() : 0;
    }

    private void tickAirFilterWear() {
        ItemStack stack = filterInventory.getStackInSlot(0);
        if (!OxygenComplexFilters.isAirFilter(stack) || !stack.isDamageableItem()) {
            filterWearTicks = 0;
            return;
        }

        if (++filterWearTicks < FILTER_WEAR_INTERVAL_TICKS) {
            return;
        }

        filterWearTicks = 0;
        int nextDamage = stack.getDamageValue() + 1;
        if (nextDamage >= stack.getMaxDamage()) {
            filterInventory.setStackInSlot(0, ItemStack.EMPTY);
        } else {
            stack.setDamageValue(nextDamage);
            filterInventory.setStackInSlot(0, stack);
        }
        setChanged();
    }

    public SideMode getSideMode(RelativeSide side) {
        if (!isController()) {
            OxygenComplexBlockEntity controller = getControllerEntity();
            return controller == null ? SideMode.DISABLED : controller.getSideMode(side);
        }
        return sideConfig.getMode(side.resolve(getMachineFacing()));
    }

    public SideMode cycleSideMode(RelativeSide side) {
        if (!OxygenComplexPortLayout.isPhysicalPort(side)) {
            return SideMode.DISABLED;
        }
        if (!isController()) {
            OxygenComplexBlockEntity controller = getControllerEntity();
            return controller == null ? SideMode.DISABLED : controller.cycleSideMode(side);
        }

        SideMode next = sideConfig.cycleMode(side.resolve(getMachineFacing()));
        setChanged();
        syncToClient();
        return next;
    }

    boolean isLinkedTo(BlockPos expectedControllerPos) {
        return controllerPos != null && controllerPos.equals(expectedControllerPos);
    }

    public void setStructureLink(BlockPos controllerPos, boolean formed) {
        boolean changed = !controllerPos.equals(this.controllerPos) || linkedFormed != formed;
        this.controllerPos = controllerPos.immutable();
        this.linkedFormed = formed;
        if (!formed && isController()) {
            status = OxygenComplexStatus.INCOMPLETE;
            updateStageActivity(false, false, false, false);
        }
        if (changed) {
            setChanged();
        }
    }

    @Nullable
    public OxygenComplexBlockEntity getControllerEntity() {
        if (level == null) {
            return isController() ? this : null;
        }
        if (isController()) {
            return this;
        }

        BlockPos target = controllerPos;
        if (target == null) {
            target = OxygenComplexStructure.controllerPos(worldPosition, role(), getMachineFacing());
        }
        if (!level.hasChunkAt(target)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof OxygenComplexBlockEntity controller && controller.isController()) {
            return controller;
        }
        return null;
    }

    public void onStructureFormed() {
        if (!isController() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!persistentControllerStateLoaded) {
            CompoundTag backup = OxygenComplexSavedData.get(serverLevel).getSnapshot(worldPosition);
            if (backup != null) {
                readControllerState(backup);
            }
            persistentControllerStateLoaded = true;
        }
        structureCheckCooldown = 20;
        atmosphereCheckCooldown = 0;
        setChanged();
        syncToClient();
    }

    public void backupControllerState() {
        if (!isController() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        OxygenComplexSavedData.get(serverLevel).store(worldPosition, writeControllerState());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenComplexBlockEntity machine) {
        if (!(level instanceof ServerLevel serverLevel) || !machine.isController()) {
            return;
        }

        if (--machine.structureCheckCooldown <= 0) {
            OxygenComplexStructure.refreshController(serverLevel, pos);
            machine.structureCheckCooldown = 20;
        }

        if (!machine.isFormed()) {
            machine.currentEnergyUse = 0;
            machine.ambientSoundTick = 0;
            machine.status = OxygenComplexStatus.INCOMPLETE;
            machine.updateStageActivity(false, false, false, false);
            return;
        }

        if (--machine.atmosphereCheckCooldown <= 0) {
            BlockPos intakePos = OxygenComplexStructure.rolePos(
                    pos, OxygenComplexRole.AIR_INTAKE, machine.getMachineFacing());
            machine.atmosphereAvailable = OxygenComplexAtmosphereRules.hasExternalAtmosphere(
                    serverLevel, intakePos, machine.getMachineFacing());
            machine.atmosphereCheckCooldown = 10;
        }

        int energyUse = 0;
        boolean outputRan = false;
        boolean compressionRan = false;
        boolean filterRan = false;
        boolean intakeRan = false;

        // Process downstream first so upstream stages never starve already-collected material.
        if (machine.compressedFeed >= OXYGEN_INPUT_PER_TICK
                && machine.oxygenStorage.getMaxOxygenStored() - machine.oxygenStorage.getOxygenStored() >= OXYGEN_OUTPUT_PER_TICK
                && machine.energyStorage.getEnergyStored() >= OPERATING_ENERGY_PER_TICK) {
            machine.compressedFeed -= OXYGEN_INPUT_PER_TICK;
            machine.oxygenStorage.addInternal(OXYGEN_OUTPUT_PER_TICK);
            outputRan = true;
        }

        if (machine.filteredAir >= COMPRESS_INPUT_PER_TICK
                && COMPRESSED_FEED_CAPACITY - machine.compressedFeed >= COMPRESS_OUTPUT_PER_TICK
                && machine.energyStorage.getEnergyStored() >= OPERATING_ENERGY_PER_TICK) {
            machine.filteredAir -= COMPRESS_INPUT_PER_TICK;
            machine.compressedFeed += COMPRESS_OUTPUT_PER_TICK;
            compressionRan = true;
        }

        if (machine.hasAirFilter()
                && machine.collectedAir >= FILTER_INPUT_PER_TICK
                && FILTERED_AIR_CAPACITY - machine.filteredAir >= FILTER_OUTPUT_PER_TICK
                && machine.energyStorage.getEnergyStored() >= OPERATING_ENERGY_PER_TICK) {
            machine.collectedAir -= FILTER_INPUT_PER_TICK;
            machine.filteredAir += FILTER_OUTPUT_PER_TICK;
            machine.tickAirFilterWear();
            filterRan = true;
        }

        if (machine.atmosphereAvailable
                && COLLECTED_AIR_CAPACITY - machine.collectedAir >= INTAKE_AIR_PER_TICK
                && machine.energyStorage.getEnergyStored() >= OPERATING_ENERGY_PER_TICK) {
            machine.collectedAir += INTAKE_AIR_PER_TICK;
            intakeRan = true;
        }

        boolean anyStageRan = intakeRan || filterRan || compressionRan || outputRan;
        if (anyStageRan) {
            machine.energyStorage.removeEnergyInternal(OPERATING_ENERGY_PER_TICK);
            energyUse = OPERATING_ENERGY_PER_TICK;
        }

        machine.currentEnergyUse = energyUse;
        machine.status = machine.calculateStatus(intakeRan, filterRan, compressionRan, outputRan);
        machine.updateStageActivity(intakeRan, filterRan, compressionRan, outputRan);

        machine.ambientSoundTick = MachineAmbientSoundService.tick(
                serverLevel,
                pos,
                anyStageRan,
                machine.ambientSoundTick,
                MachineAmbientSoundService.MachineType.OXYGEN_COMPLEX
        );

        if (energyUse > 0) {
            machine.setChanged();
        }
    }

    private OxygenComplexStatus calculateStatus(boolean intakeRan, boolean filterRan,
                                                boolean compressionRan, boolean outputRan) {
        if (!isFormed()) return OxygenComplexStatus.INCOMPLETE;
        if (outputRan) return OxygenComplexStatus.PRODUCING;
        if (compressionRan) return OxygenComplexStatus.COMPRESSING;
        if (filterRan) return OxygenComplexStatus.FILTERING;
        if (intakeRan) return OxygenComplexStatus.INTAKING;
        if (oxygenStorage.getOxygenStored() >= OXYGEN_CAPACITY) return OxygenComplexStatus.OXYGEN_FULL;

        boolean needsEnergy = (compressedFeed >= OXYGEN_INPUT_PER_TICK
                && oxygenStorage.getMaxOxygenStored() - oxygenStorage.getOxygenStored() >= OXYGEN_OUTPUT_PER_TICK)
                || (filteredAir >= COMPRESS_INPUT_PER_TICK
                && COMPRESSED_FEED_CAPACITY - compressedFeed >= COMPRESS_OUTPUT_PER_TICK)
                || (hasAirFilter()
                && collectedAir >= FILTER_INPUT_PER_TICK
                && FILTERED_AIR_CAPACITY - filteredAir >= FILTER_OUTPUT_PER_TICK)
                || (atmosphereAvailable && COLLECTED_AIR_CAPACITY - collectedAir >= INTAKE_AIR_PER_TICK);
        if (needsEnergy && energyStorage.getEnergyStored() < OPERATING_ENERGY_PER_TICK) {
            return OxygenComplexStatus.LOW_ENERGY;
        }
        if (!atmosphereAvailable && collectedAir < FILTER_INPUT_PER_TICK) {
            return OxygenComplexStatus.NO_ATMOSPHERE;
        }
        if (collectedAir > 0 || filteredAir > 0 || compressedFeed > 0 || oxygenStorage.getOxygenStored() > 0) {
            return OxygenComplexStatus.OPERATIONAL;
        }
        return OxygenComplexStatus.IDLE;
    }

    private void updateStageActivity(boolean intake, boolean filtration,
                                     boolean compression, boolean output) {
        boolean changed = intakeActive != intake
                || filterActive != filtration
                || compressionActive != compression
                || outputActive != output;
        intakeActive = intake;
        filterActive = filtration;
        compressionActive = compression;
        outputActive = output;

        if (changed && level instanceof ServerLevel serverLevel && isController()) {
            OxygenComplexStructure.setStageActivity(
                    serverLevel, worldPosition, getMachineFacing(),
                    intake, filtration, compression, output
            );
        }
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        sideConfig.setMode(RelativeSide.BACK.resolve(facing), SideMode.INPUT);
        sideConfig.setMode(RelativeSide.BOTTOM.resolve(facing), SideMode.OUTPUT);
        sideConfig.setMode(RelativeSide.TOP.resolve(facing), SideMode.DISABLED);
        sideConfig.setMode(RelativeSide.FRONT.resolve(facing), SideMode.DISABLED);
        sideConfig.setMode(RelativeSide.LEFT.resolve(facing), SideMode.DISABLED);
        sideConfig.setMode(RelativeSide.RIGHT.resolve(facing), SideMode.DISABLED);
    }

    /**
     * V64.2I migration: only TOP/BOTTOM/BACK remain physical.
     * Existing LEFT input is moved to BACK if BACK is free; existing RIGHT
     * output is moved to BOTTOM if BOTTOM is free. Hidden sides are disabled.
     */
    private void migrateLegacyConnectorLayout(int savedVersion) {
        if (savedVersion >= 3) {
            return;
        }

        Direction facing = getMachineFacing();
        Direction back = RelativeSide.BACK.resolve(facing);
        Direction bottom = RelativeSide.BOTTOM.resolve(facing);
        Direction left = RelativeSide.LEFT.resolve(facing);
        Direction right = RelativeSide.RIGHT.resolve(facing);
        Direction front = RelativeSide.FRONT.resolve(facing);

        if (sideConfig.getMode(back) == SideMode.DISABLED
                && sideConfig.getMode(left) == SideMode.INPUT) {
            sideConfig.setMode(back, SideMode.INPUT);
        }
        if (sideConfig.getMode(bottom) == SideMode.DISABLED
                && sideConfig.getMode(right) == SideMode.OUTPUT) {
            sideConfig.setMode(bottom, SideMode.OUTPUT);
        }

        sideConfig.setMode(front, SideMode.DISABLED);
        sideConfig.setMode(left, SideMode.DISABLED);
        sideConfig.setMode(right, SideMode.DISABLED);
    }

    private CompoundTag writeControllerState() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_STATE_VERSION, STATE_VERSION);
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(NBT_COLLECTED, collectedAir);
        tag.putInt(NBT_FILTERED, filteredAir);
        tag.putInt(NBT_COMPRESSED, compressedFeed);
        tag.putInt(NBT_OXYGEN, oxygenStorage.getOxygenStored());
        tag.put(NBT_FILTER_INVENTORY, filterInventory.serializeNBT());
        tag.putInt(NBT_FILTER_WEAR_TICKS, filterWearTicks);
        sideConfig.save(tag);
        return tag;
    }

    private void readControllerState(CompoundTag tag) {
        int savedVersion = tag.contains(NBT_STATE_VERSION) ? tag.getInt(NBT_STATE_VERSION) : 0;
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        collectedAir = clamp(tag.getInt(NBT_COLLECTED), COLLECTED_AIR_CAPACITY);
        filteredAir = clamp(tag.getInt(NBT_FILTERED), FILTERED_AIR_CAPACITY);
        compressedFeed = clamp(tag.getInt(NBT_COMPRESSED), COMPRESSED_FEED_CAPACITY);
        oxygenStorage.setStoredInternal(tag.getInt(NBT_OXYGEN));
        if (tag.contains(NBT_FILTER_INVENTORY)) {
            filterInventory.deserializeNBT(tag.getCompound(NBT_FILTER_INVENTORY));
        } else {
            filterInventory.setStackInSlot(0, ItemStack.EMPTY);
        }
        filterWearTicks = Math.max(0, tag.getInt(NBT_FILTER_WEAR_TICKS));
        if (!sideConfig.load(tag)) {
            applyDefaultSideConfiguration();
        } else {
            migrateLegacyConnectorLayout(savedVersion);
        }
        refreshCapabilities();
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                if (!isRemoved() && serverLevel.hasChunkAt(worldPosition)) {
                    OxygenComplexStructure.refreshFrom(serverLevel, worldPosition);
                }
            });
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (controllerPos != null) {
            tag.putBoolean(NBT_HAS_CONTROLLER, true);
            tag.putLong(NBT_CONTROLLER, controllerPos.asLong());
        }
        if (isController()) {
            tag.merge(writeControllerState());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        controllerPos = tag.getBoolean(NBT_HAS_CONTROLLER)
                ? BlockPos.of(tag.getLong(NBT_CONTROLLER))
                : null;
        linkedFormed = getBlockState().hasProperty(OxygenComplexBlock.FORMED)
                && getBlockState().getValue(OxygenComplexBlock.FORMED);

        if (isController() && tag.contains(NBT_STATE_VERSION)) {
            readControllerState(tag);
            persistentControllerStateLoaded = true;
        } else if (isController()) {
            applyDefaultSideConfiguration();
            persistentControllerStateLoaded = false;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
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

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (side == null) {
            if (isController()) {
                if (cap == ForgeCapabilities.ENERGY) {
                    return energyCapability.cast();
                }
                if (cap == ModCapabilities.OXYGEN) {
                    return oxygenCapability.cast();
                }
            }
            return super.getCapability(cap, null);
        }

        RelativeSide relativeSide = OxygenComplexPortLayout.relativeSide(side, getMachineFacing());
        if (relativeSide == null
                || !OxygenComplexPortLayout.isPhysicalPort(relativeSide)
                || OxygenComplexPortLayout.hostRole(relativeSide) != role()) {
            return super.getCapability(cap, side);
        }

        OxygenComplexBlockEntity controller = getControllerEntity();
        if (controller == null || !controller.isFormed()) {
            return super.getCapability(cap, side);
        }

        SideMode mode = controller.getSideMode(relativeSide);
        if (cap == ForgeCapabilities.ENERGY && mode == SideMode.INPUT) {
            return controller.energyCapability.cast();
        }
        if (cap == ModCapabilities.OXYGEN && mode == SideMode.OUTPUT) {
            return controller.oxygenCapability.cast();
        }

        return super.getCapability(cap, side);
    }

    private void refreshCapabilities() {
        energyCapability.invalidate();
        oxygenCapability.invalidate();
        energyCapability = LazyOptional.of(() -> energyInputView);
        oxygenCapability = LazyOptional.of(() -> oxygenOutputView);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
        oxygenCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        refreshCapabilities();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.domesurvival.oxygen_complex.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!isController()) {
            return null;
        }
        return new OxygenComplexMenu(containerId, playerInventory, this);
    }
}
