package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.capability.IOxygenStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import com.wasted.domesurvival.forge.sound.MachineAmbientSoundService;
import com.wasted.domesurvival.forge.particle.ModParticles;
import com.wasted.domesurvival.forge.oxygen.room.SealedRoomManager;
import com.wasted.domesurvival.forge.oxygen.room.RoomAtmosphereRules;
import com.wasted.domesurvival.forge.oxygen.room.RoomAtmosphereSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores oxygen from the pipe network, fills DomeSurvival tanks and pressurizes sealed rooms.
 * V62 never compensates an active leak: the room must be resealed before oxygen input resumes.
 */
public final class OxygenFillerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_ENERGY_INPUT_PER_TICK = 200;
    public static final int ENERGY_PER_FILL_TICK = 5;
    public static final int OXYGEN_CAPACITY = 6_000;
    public static final int MAX_OXYGEN_INPUT_PER_TICK = 120;
    public static final int MAX_OXYGEN_OUTPUT_PER_TICK = 120;
    public static final int OXYGEN_FILL_PER_TICK = 1;
    public static final int SLOT_TANK = 0;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_OXYGEN = 2;
    public static final int DATA_OXYGEN_CAPACITY = 3;
    public static final int DATA_TANK_OXYGEN = 4;
    public static final int DATA_TANK_CAPACITY = 5;
    public static final int DATA_STATUS = 6;
    public static final int DATA_MODE = 7;
    public static final int DATA_ROOM_STATE = 8;
    public static final int DATA_ROOM_VOLUME = 9;
    public static final int DATA_ROOM_OXYGEN = 10;
    public static final int DATA_ROOM_OXYGEN_REQUIRED = 11;
    public static final int DATA_SIDES_START = 12;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_FILLING = 1;
    public static final int STATUS_NO_TANK = 2;
    public static final int STATUS_TANK_FULL = 3;
    public static final int STATUS_NO_OXYGEN = 4;
    public static final int STATUS_NO_ENERGY = 5;
    public static final int STATUS_VENTILATING = 6;
    public static final int STATUS_VENT_OUTLET_BLOCKED = 7;
    public static final int STATUS_VENT_ROOM_OPEN = 8;
    public static final int STATUS_VENT_ROOM_TOO_LARGE = 9;
    public static final int STATUS_VENT_ROOM_UNLOADED = 10;
    public static final int STATUS_VENT_ROOM_FULL = 11;
    public static final int STATUS_VENT_ROOM_LEAKING = 12;
    public static final int STATUS_VENT_ROOM_DEPRESSURIZED = 13;

    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_OXYGEN = "Oxygen";
    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_MODE = "OperatingMode";
    private static final int VENTILATION_PARTICLE_INTERVAL = 12;

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();
    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT_PER_TICK, 0);
    private final OxygenStorage oxygenStorage =
            new OxygenStorage(OXYGEN_CAPACITY, MAX_OXYGEN_INPUT_PER_TICK, 0);

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_TANK && stack.getItem() instanceof OxygenTankItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IEnergyStorage energyInputView = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = energyStorage.receiveEnergy(maxReceive, simulate);
            if (!simulate && accepted > 0) setChanged();
            return accepted;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IOxygenStorage oxygenInputView = new IOxygenStorage() {
        @Override public int receiveOxygen(int maxReceive, boolean simulate) {
            int accepted = oxygenStorage.receiveOxygen(maxReceive, simulate);
            if (!simulate && accepted > 0) setChanged();
            return accepted;
        }
        @Override public int extractOxygen(int maxExtract, boolean simulate) { return 0; }
        @Override public int getOxygenStored() { return oxygenStorage.getOxygenStored(); }
        @Override public int getMaxOxygenStored() { return oxygenStorage.getMaxOxygenStored(); }
        @Override public boolean canReceive() { return true; }
        @Override public boolean canExtract() { return false; }
    };

    /**
     * Sided oxygen output exposed only on connector faces configured as OUTPUT.
     *
     * Oxygen pipes in DomeSurvival are pull-based, so downstream machines query
     * this capability and extract directly from the filler buffer.
     */
    private final IOxygenStorage oxygenOutputView = new IOxygenStorage() {
        @Override public int receiveOxygen(int maxReceive, boolean simulate) { return 0; }

        @Override
        public int extractOxygen(int maxExtract, boolean simulate) {
            return extractOxygenForNetwork(maxExtract, simulate);
        }

        @Override public int getOxygenStored() { return oxygenStorage.getOxygenStored(); }
        @Override public int getMaxOxygenStored() { return oxygenStorage.getMaxOxygenStored(); }
        @Override public boolean canReceive() { return false; }
        @Override public boolean canExtract() { return true; }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyInputView);
    private LazyOptional<IOxygenStorage> oxygenInputCapability = LazyOptional.of(() -> oxygenInputView);
    private LazyOptional<IOxygenStorage> oxygenOutputCapability = LazyOptional.of(() -> oxygenOutputView);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> inventory);

    private long oxygenOutputBudgetGameTime = Long.MIN_VALUE;
    private int oxygenOutputUsedThisTick;

    private OxygenFillerMode operatingMode = OxygenFillerMode.TANK_FILLING;
    private int status = STATUS_IDLE;
    private SealedRoomManager.RoomState roomState = SealedRoomManager.RoomState.UNKNOWN;
    private int roomVolume;
    private int roomOxygen;
    private int roomOxygenRequired;
    private int ambientSoundTick;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_ENERGY_CAPACITY) return energyStorage.getMaxEnergyStored();
            if (index == DATA_OXYGEN) return oxygenStorage.getOxygenStored();
            if (index == DATA_OXYGEN_CAPACITY) return oxygenStorage.getMaxOxygenStored();
            if (index == DATA_TANK_OXYGEN) return getTankOxygen();
            if (index == DATA_TANK_CAPACITY) return getTankCapacity();
            if (index == DATA_STATUS) return status;
            if (index == DATA_MODE) return operatingMode.ordinal();
            if (index == DATA_ROOM_STATE) return roomState.ordinal();
            if (index == DATA_ROOM_VOLUME) return roomVolume;
            if (index == DATA_ROOM_OXYGEN) return roomOxygen;
            if (index == DATA_ROOM_OXYGEN_REQUIRED) return roomOxygenRequired;
            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) {
                Direction direction = Direction.values()[index - DATA_SIDES_START];
                return getDisplayedSideMode(direction).ordinal();
            }
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public OxygenFillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_FILLER.get(), pos, state);
        applyDefaultSideConfiguration();
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();
        for (RelativeSide relative : RelativeSide.values()) {
            Direction worldSide = relative.resolve(facing);
            sideConfig.setMode(worldSide, relative == RelativeSide.FRONT ? SideMode.DISABLED : SideMode.INPUT);
        }
    }

    public static boolean isConfigurableSide(RelativeSide side) {
        return side != RelativeSide.FRONT;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenFillerBlockEntity machine) {
        machine.syncAllPortStates();
        boolean changed = false;

        machine.status = machine.calculateStatus();
        if (machine.shouldPullOxygenForCurrentMode()
                && machine.oxygenStorage.getOxygenStored() < machine.oxygenStorage.getMaxOxygenStored()) {
            int pulled = OxygenPipeTransferService.pull(
                    level,
                    pos,
                    machine.oxygenStorage,
                    MAX_OXYGEN_INPUT_PER_TICK,
                    machine::allowsOxygenInputOn
            );
            if (pulled > 0) {
                changed = true;
                machine.status = machine.calculateStatus();
            }
        }

        boolean working = false;

        if (machine.status == STATUS_FILLING) {
            ItemStack stack = machine.inventory.getStackInSlot(SLOT_TANK);
            if (stack.getItem() instanceof OxygenTankItem tank) {
                int missing = tank.capacity() - tank.getOxygen(stack);
                int amount = Math.min(OXYGEN_FILL_PER_TICK,
                        Math.min(missing, machine.oxygenStorage.getOxygenStored()));
                if (amount > 0
                        && machine.energyStorage.removeEnergyInternal(ENERGY_PER_FILL_TICK) == ENERGY_PER_FILL_TICK) {
                    machine.oxygenStorage.removeInternal(amount);
                    tank.setOxygen(stack, tank.getOxygen(stack) + amount);
                    working = true;
                    changed = true;
                }
            }
        } else if (machine.status == STATUS_VENTILATING && level instanceof ServerLevel serverLevel) {
            int transferred = machine.fillSealedRoom(serverLevel);
            if (transferred > 0) {
                working = true;
                changed = true;
            }
        }

        machine.status = machine.calculateStatus();
        machine.ambientSoundTick = MachineAmbientSoundService.tick(
                level, pos, working, machine.ambientSoundTick,
                MachineAmbientSoundService.MachineType.OXYGEN_FILLER
        );

        if (state.getValue(OxygenFillerBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(OxygenFillerBlock.LIT, working), 3);
            changed = true;
        }

        if (changed) {
            machine.setChanged();
        }
    }

    private boolean shouldPullOxygenForCurrentMode() {
        if (operatingMode != OxygenFillerMode.VENTILATION) {
            return true;
        }
        return status == STATUS_VENTILATING
                || status == STATUS_NO_OXYGEN
                || status == STATUS_NO_ENERGY;
    }

    private int calculateStatus() {
        if (operatingMode == OxygenFillerMode.VENTILATION) {
            if (!isVentilationOutletClear()) {
                clearRoomDisplay();
                return STATUS_VENT_OUTLET_BLOCKED;
            }
            if (!(level instanceof ServerLevel serverLevel)) {
                return STATUS_IDLE;
            }

            SealedRoomManager.RoomSnapshot room = SealedRoomManager.getOrDiscover(
                    serverLevel,
                    worldPosition.above()
            );
            roomState = room.state();
            roomVolume = room.volume();

            if (!room.sealed()) {
                RoomAtmosphereSavedData atmosphereData = RoomAtmosphereSavedData.get(serverLevel);
                RoomAtmosphereSavedData.LeakAtmosphereSnapshot leak = atmosphereData.getLeakByOutlet(
                        worldPosition.above().asLong(),
                        serverLevel.getGameTime()
                );
                if (leak != null) {
                    roomOxygen = leak.oxygen();
                    roomOxygenRequired = leak.required();
                    roomVolume = RoomAtmosphereRules.volumeFromRequiredOxygen(leak.required());
                    if (leak.leaking()) {
                        roomState = SealedRoomManager.RoomState.LEAKING;
                        return STATUS_VENT_ROOM_LEAKING;
                    }
                    roomState = SealedRoomManager.RoomState.DEPRESSURIZED;
                    return STATUS_VENT_ROOM_DEPRESSURIZED;
                }

                roomOxygen = 0;
                roomOxygenRequired = 0;
                return switch (room.state()) {
                    case OPEN -> STATUS_VENT_ROOM_OPEN;
                    case TOO_LARGE -> STATUS_VENT_ROOM_TOO_LARGE;
                    case UNLOADED -> STATUS_VENT_ROOM_UNLOADED;
                    case UNKNOWN, SEALED, LEAKING, DEPRESSURIZED -> STATUS_IDLE;
                };
            }

            RoomAtmosphereSavedData.AtmosphereSnapshot atmosphere =
                    RoomAtmosphereSavedData.get(serverLevel).reconcileSealed(
                            room, worldPosition.above().asLong(), serverLevel.getGameTime()
                    );
            roomOxygen = atmosphere.oxygen();
            roomOxygenRequired = atmosphere.required();

            if (atmosphere.full()) return STATUS_VENT_ROOM_FULL;
            if (oxygenStorage.getOxygenStored() <= 0) return STATUS_NO_OXYGEN;
            if (energyStorage.getEnergyStored() < RoomAtmosphereRules.ENERGY_FE_PER_OXYGEN_MB) {
                return STATUS_NO_ENERGY;
            }
            return STATUS_VENTILATING;
        }

        clearRoomDisplay();
        ItemStack stack = inventory.getStackInSlot(SLOT_TANK);
        if (stack.isEmpty() || !(stack.getItem() instanceof OxygenTankItem tank)) return STATUS_NO_TANK;
        if (tank.getOxygen(stack) >= tank.capacity()) return STATUS_TANK_FULL;
        if (oxygenStorage.getOxygenStored() <= 0) return STATUS_NO_OXYGEN;
        if (energyStorage.getEnergyStored() < ENERGY_PER_FILL_TICK) return STATUS_NO_ENERGY;
        return STATUS_FILLING;
    }

    private int fillSealedRoom(ServerLevel serverLevel) {
        SealedRoomManager.RoomSnapshot room = SealedRoomManager.getOrDiscover(
                serverLevel,
                worldPosition.above()
        );
        if (!room.sealed()) return 0;

        RoomAtmosphereSavedData atmosphereData = RoomAtmosphereSavedData.get(serverLevel);
        RoomAtmosphereSavedData.AtmosphereSnapshot atmosphere = atmosphereData.reconcileSealed(
                room, worldPosition.above().asLong(), serverLevel.getGameTime()
        );
        int maxByEnergy = energyStorage.getEnergyStored() / RoomAtmosphereRules.ENERGY_FE_PER_OXYGEN_MB;
        int fillBudget = RoomAtmosphereRules.ventilationFillBudget(serverLevel.getGameTime());
        int amount = Math.min(
                fillBudget,
                Math.min(
                        atmosphere.missing(),
                        Math.min(oxygenStorage.getOxygenStored(), maxByEnergy)
                )
        );
        if (amount <= 0) return 0;

        int accepted = atmosphereData.addOxygen(room, amount);
        if (accepted <= 0) return 0;

        oxygenStorage.removeInternal(accepted);
        energyStorage.removeEnergyInternal(accepted * RoomAtmosphereRules.ENERGY_FE_PER_OXYGEN_MB);
        return accepted;
    }

    private void clearRoomDisplay() {
        roomState = SealedRoomManager.RoomState.UNKNOWN;
        roomVolume = 0;
        roomOxygen = 0;
        roomOxygenRequired = 0;
    }

    private boolean isVentilationOutletClear() {
        if (level == null) return false;
        BlockPos outletPos = worldPosition.above();
        BlockState outletState = level.getBlockState(outletPos);
        return outletState.getCollisionShape(level, outletPos).isEmpty();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, OxygenFillerBlockEntity machine) {
        if (!level.isClientSide
                || machine.operatingMode != OxygenFillerMode.VENTILATION
                || !state.getValue(OxygenFillerBlock.LIT)) {
            return;
        }

        long phase = Math.floorMod(pos.asLong(), (long) VENTILATION_PARTICLE_INTERVAL);
        if (Math.floorMod(level.getGameTime() + phase, (long) VENTILATION_PARTICLE_INTERVAL) != 0L) {
            return;
        }

        double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.10D;
        double y = pos.getY() + 1.035D;
        double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.10D;
        level.addParticle(
                ModParticles.VENTILATION_BUBBLE.get(),
                x, y, z,
                0.0D, 0.012D, 0.0D
        );
    }

    private int getTankOxygen() {
        ItemStack stack = inventory.getStackInSlot(SLOT_TANK);
        return stack.getItem() instanceof OxygenTankItem tank ? tank.getOxygen(stack) : 0;
    }

    private int getTankCapacity() {
        ItemStack stack = inventory.getStackInSlot(SLOT_TANK);
        return stack.getItem() instanceof OxygenTankItem tank ? tank.capacity() : ModItems.SMALL_TANK_CAPACITY;
    }

    private boolean allowsOxygenInputOn(Direction direction) {
        if (operatingMode == OxygenFillerMode.VENTILATION && direction == Direction.UP) return false;
        return !isFrontWorldSide(direction) && sideConfig.allowsInput(direction);
    }

    private boolean allowsOxygenOutputOn(Direction direction) {
        if (operatingMode == OxygenFillerMode.VENTILATION && direction == Direction.UP) return false;
        return !isFrontWorldSide(direction) && sideConfig.allowsOutput(direction);
    }

    /**
     * Enforces one shared 120 mB/t output budget across all OUTPUT faces.
     * This prevents several downstream consumers from multiplying the configured
     * machine output rate during the same server tick.
     */
    private int extractOxygenForNetwork(int maxExtract, boolean simulate) {
        if (maxExtract <= 0 || oxygenStorage.getOxygenStored() <= 0) return 0;

        int remainingBudget = MAX_OXYGEN_OUTPUT_PER_TICK;
        long gameTime = Long.MIN_VALUE;

        if (level != null) {
            gameTime = level.getGameTime();
            if (oxygenOutputBudgetGameTime == gameTime) {
                remainingBudget -= oxygenOutputUsedThisTick;
            }
        }

        if (remainingBudget <= 0) return 0;

        int extracted = Math.min(
                Math.min(maxExtract, remainingBudget),
                oxygenStorage.getOxygenStored()
        );
        if (extracted <= 0 || simulate) return extracted;

        int removed = oxygenStorage.removeInternal(extracted);
        if (removed <= 0) return 0;

        if (level != null) {
            if (oxygenOutputBudgetGameTime != gameTime) {
                oxygenOutputBudgetGameTime = gameTime;
                oxygenOutputUsedThisTick = 0;
            }
            oxygenOutputUsedThisTick += removed;
        }

        setChanged();
        return removed;
    }

    private SideMode getDisplayedSideMode(Direction direction) {
        if (operatingMode == OxygenFillerMode.VENTILATION && direction == Direction.UP) {
            return SideMode.DISABLED;
        }
        return sideConfig.getMode(direction);
    }

    public OxygenFillerMode getOperatingMode() {
        return operatingMode;
    }

    public OxygenFillerMode cycleOperatingMode() {
        operatingMode = operatingMode.next();
        refreshCapabilities();
        syncAllPortStates();
        status = calculateStatus();
        setChanged();
        syncOperatingModeToClient();
        return operatingMode;
    }

    private void syncOperatingModeToClient() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) return SideMode.DISABLED;
        Direction worldSide = relativeSide.resolve(getMachineFacing());
        if (operatingMode == OxygenFillerMode.VENTILATION && worldSide == Direction.UP) {
            return SideMode.DISABLED;
        }

        // Standard machine routing:
        // DISABLED -> INPUT -> OUTPUT -> DISABLED.
        SideMode next = sideConfig.cycleMode(worldSide);
        refreshCapabilities();
        syncPortState(worldSide);
        setChanged();
        return next;
    }

    private boolean isFrontWorldSide(Direction side) {
        return side == getMachineFacing();
    }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof OxygenFillerBlock)) return;

        PortVisual visual = isFrontWorldSide(direction)
                ? PortVisual.OFF
                : PortVisual.fromMode(getDisplayedSideMode(direction));

        var property = OxygenFillerBlock.portProperty(direction);
        if (state.getValue(property) != visual) {
            level.setBlock(worldPosition, state.setValue(property, visual), 3);
        }
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof OxygenFillerBlock)) return;
        BlockState updated = state;

        for (Direction direction : Direction.values()) {
            PortVisual visual = isFrontWorldSide(direction)
                    ? PortVisual.OFF
                    : PortVisual.fromMode(getDisplayedSideMode(direction));
            updated = updated.setValue(OxygenFillerBlock.portProperty(direction), visual);
        }

        if (!updated.equals(state)) {
            level.setBlock(worldPosition, updated, 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        clearRoomDisplay();
        syncAllPortStates();
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(OxygenFillerBlock.FACING)
                ? state.getValue(OxygenFillerBlock.FACING) : Direction.NORTH;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(NBT_OXYGEN, oxygenStorage.getOxygenStored());
        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_MODE, operatingMode.ordinal());
        sideConfig.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));
        oxygenStorage.setStoredInternal(tag.getInt(NBT_OXYGEN));
        operatingMode = OxygenFillerMode.byOrdinal(tag.getInt(NBT_MODE));
        if (tag.contains(NBT_INVENTORY)) {
            inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        }
        if (!sideConfig.load(tag)) {
            applyDefaultSideConfiguration();
        }
        Direction facing = getMachineFacing();
        sideConfig.setMode(facing, SideMode.DISABLED);
        // INPUT and OUTPUT are both persistent valid modes. UnifiedSideConfig
        // already sanitizes legacy BOTH values to OUTPUT while loading.
        clearRoomDisplay();
        status = STATUS_IDLE;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_MODE, operatingMode.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains(NBT_MODE)) {
            operatingMode = OxygenFillerMode.byOrdinal(tag.getInt(NBT_MODE));
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Null is the existing unsided/internal access path and remains input-only.
        boolean inputAllowed = side == null || allowsOxygenInputOn(side);

        if (cap == ForgeCapabilities.ENERGY && inputAllowed) {
            return energyCapability.cast();
        }

        if (cap == ForgeCapabilities.ITEM_HANDLER && inputAllowed) {
            return itemCapability.cast();
        }

        if (cap == ModCapabilities.OXYGEN) {
            if (side == null || allowsOxygenInputOn(side)) {
                return oxygenInputCapability.cast();
            }
            if (allowsOxygenOutputOn(side)) {
                return oxygenOutputCapability.cast();
            }
            return LazyOptional.empty();
        }

        return super.getCapability(cap, side);
    }

    private void refreshCapabilities() {
        energyCapability.invalidate();
        oxygenInputCapability.invalidate();
        oxygenOutputCapability.invalidate();
        itemCapability.invalidate();

        energyCapability = LazyOptional.of(() -> energyInputView);
        oxygenInputCapability = LazyOptional.of(() -> oxygenInputView);
        oxygenOutputCapability = LazyOptional.of(() -> oxygenOutputView);
        itemCapability = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
        oxygenInputCapability.invalidate();
        oxygenOutputCapability.invalidate();
        itemCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        refreshCapabilities();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.oxygen_filler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OxygenFillerMenu(containerId, playerInventory, this);
    }
}
