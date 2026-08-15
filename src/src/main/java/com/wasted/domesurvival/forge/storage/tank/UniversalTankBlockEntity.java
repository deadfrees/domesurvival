package com.wasted.domesurvival.forge.storage.tank;

import com.wasted.domesurvival.forge.capability.IOxygenStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeBlock;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeTransferService;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * One lightweight proxy cell of the V63 universal reservoir.
 *
 * <p>Only the master cell stores the actual fluid/oxygen. Every other cell keeps
 * only a master pointer, structure geometry and its own side connector modes.
 * Only the master ticks, and only to service the existing pull-based oxygen network;
 * proxy cells never tick.</p>
 */
public final class UniversalTankBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY_PER_BLOCK = 4_000;
    private static final int MAX_OXYGEN_NETWORK_INPUT_PER_TICK = 480;

    public static final int DATA_KIND = 0;
    public static final int DATA_AMOUNT = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_BLOCK_COUNT = 3;
    public static final int DATA_SIZE_X = 4;
    public static final int DATA_SIZE_Y = 5;
    public static final int DATA_SIZE_Z = 6;
    public static final int DATA_UNIFIED = 7;
    public static final int DATA_SIDES_START = 8;
    public static final int DATA_COUNT = DATA_SIDES_START + 6;

    private static final String NBT_MASTER = "TankMaster";
    private static final String NBT_MIN = "TankMin";
    private static final String NBT_MAX = "TankMax";
    private static final String NBT_BLOCK_COUNT = "TankBlockCount";
    private static final String NBT_UNIFIED = "TankUnified";
    private static final String NBT_KIND = "TankContentKind";
    private static final String NBT_FLUID = "TankFluid";
    private static final String NBT_OXYGEN = "TankOxygen";
    private static final String NBT_SIDE_DEFAULTS_VERSION = "TankSideDefaultsVersion";
    private static final int SIDE_DEFAULTS_VERSION = 1;

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

    private BlockPos masterPos;
    private BlockPos structureMin;
    private BlockPos structureMax;
    private int blockCount = 1;
    private boolean unifiedModel;

    // Valid only on the current master. Proxies intentionally keep these empty.
    private UniversalTankContentKind contentKind = UniversalTankContentKind.EMPTY;
    private FluidStack fluid = FluidStack.EMPTY;
    private int oxygen;

    // Transient handshake used only during Thermal/CoFH wrench dismantling.
    // It lives for at most one server tick so a clone/pick-block operation can
    // never leave a stale "preserve content" flag behind.
    private int pendingPortableExtractionAmount;
    private long pendingPortableExtractionGameTime = Long.MIN_VALUE;

    // Master-only transient cache of structure cells that currently expose at least one O2 input.
    // Rebuilt only after structure/connector/load changes; never rescans the full tank every tick.
    private final List<BlockPos> oxygenInputCells = new ArrayList<>();
    private boolean oxygenPortCacheDirty = true;
    private int oxygenInputCursor;
    private final IOxygenStorage networkOxygenSink = new OxygenView(null);

    private int lastSyncedVisualKey = Integer.MIN_VALUE;

    private LazyOptional<IFluidHandler> internalFluidCapability = LazyOptional.empty();
    private LazyOptional<IOxygenStorage> internalOxygenCapability = LazyOptional.empty();
    private final EnumMap<Direction, LazyOptional<IFluidHandler>> fluidCapabilities =
            new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IOxygenStorage>> oxygenCapabilities =
            new EnumMap<>(Direction.class);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            UniversalTankBlockEntity master = getMasterEntity();
            UniversalTankBlockEntity storage = master == null ? UniversalTankBlockEntity.this : master;

            if (index == DATA_KIND) return storage.contentKind.ordinal();
            if (index == DATA_AMOUNT) return storage.getLocalStoredAmount();
            if (index == DATA_CAPACITY) return storage.getLocalCapacity();
            if (index == DATA_BLOCK_COUNT) return storage.blockCount;
            if (index == DATA_SIZE_X) return storage.getStructureSizeX();
            if (index == DATA_SIZE_Y) return storage.getStructureSizeY();
            if (index == DATA_SIZE_Z) return storage.getStructureSizeZ();
            if (index == DATA_UNIFIED) return storage.unifiedModel ? 1 : 0;

            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) {
                Direction direction = Direction.values()[index - DATA_SIDES_START];
                return getSideMode(direction).ordinal();
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public UniversalTankBlockEntity(BlockPos pos, BlockState state) {
        super(UniversalTankRegistry.UNIVERSAL_TANK_BLOCK_ENTITY.get(), pos, state);
        this.masterPos = pos.immutable();
        this.structureMin = pos.immutable();
        this.structureMax = pos.immutable();
        applyDefaultSideConfiguration();
        rebuildCapabilityViews();
    }

    private void applyDefaultSideConfiguration() {
        // V63.1: every connector is OFF until the player explicitly enables it.
        sideConfig.reset();
    }

    private void migrateLegacyDefaultSideConfiguration(CompoundTag tag) {
        if (tag.contains(NBT_SIDE_DEFAULTS_VERSION)) return;

        boolean legacyAllInput = true;
        for (Direction direction : Direction.values()) {
            if (sideConfig.getMode(direction) != SideMode.INPUT) {
                legacyAllInput = false;
                break;
            }
        }

        // V63 initially created every face as INPUT. Reset only that exact legacy
        // default; any intentionally customized old configuration is preserved.
        if (legacyAllInput) sideConfig.reset();
    }

    void serverMasterTick() {
        if (level == null || level.isClientSide || !isMaster()) return;
        if (contentKind == UniversalTankContentKind.FLUID) return;
        if (getLocalStoredAmount() >= getLocalCapacity()) return;

        pullOxygenFromConfiguredPort();
    }

    private void pullOxygenFromConfiguredPort() {
        if (level == null || level.isClientSide || !isMaster()) return;

        if (oxygenPortCacheDirty) rebuildOxygenInputCellCache();
        if (oxygenInputCells.isEmpty()) return;

        // Exactly one configured input cell is serviced per tick. This keeps a 12x12x12
        // reservoir from multiplying the oxygen network scan cost by its surface area.
        if (oxygenInputCursor >= oxygenInputCells.size()) oxygenInputCursor = 0;
        BlockPos inputPos = oxygenInputCells.get(oxygenInputCursor++);
        if (!level.hasChunkAt(inputPos)) return;

        BlockEntity blockEntity = level.getBlockEntity(inputPos);
        if (!(blockEntity instanceof UniversalTankBlockEntity inputCell)
                || !inputCell.masterPos.equals(worldPosition)) {
            oxygenPortCacheDirty = true;
            return;
        }

        OxygenPipeTransferService.pull(
                level,
                inputPos,
                networkOxygenSink,
                MAX_OXYGEN_NETWORK_INPUT_PER_TICK,
                side -> inputCell.isConnectorFace(side)
                        && inputCell.getSideMode(side).allowsInput()
        );
    }

    private void rebuildOxygenInputCellCache() {
        oxygenInputCells.clear();
        oxygenInputCursor = 0;
        oxygenPortCacheDirty = false;

        if (level == null || level.isClientSide || !isMaster()) return;

        if (unifiedModel) {
            // A formed multiblock has exactly four logical connector positions:
            // one centered cell on each horizontal face. Never scan its whole skin.
            for (Direction direction : new Direction[]{
                    Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
            }) {
                BlockPos portPos = unifiedPortPosition(direction);
                if (portPos == null || !level.hasChunkAt(portPos)) continue;

                BlockEntity blockEntity = level.getBlockEntity(portPos);
                if (!(blockEntity instanceof UniversalTankBlockEntity cell)
                        || !cell.masterPos.equals(worldPosition)) {
                    continue;
                }

                if (getSideMode(direction).allowsInput()
                        && cell.hasOxygenInputConnection(direction)
                        && !oxygenInputCells.contains(portPos)) {
                    oxygenInputCells.add(portPos.immutable());
                }
            }
            return;
        }

        for (int x = structureMin.getX(); x <= structureMax.getX(); x++) {
            for (int y = structureMin.getY(); y <= structureMax.getY(); y++) {
                for (int z = structureMin.getZ(); z <= structureMax.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (!(blockEntity instanceof UniversalTankBlockEntity cell)
                            || !cell.masterPos.equals(worldPosition)) {
                        continue;
                    }

                    boolean hasConnectedInput = false;
                    for (Direction direction : Direction.values()) {
                        if (cell.isConnectorFace(direction)
                                && cell.getSideMode(direction).allowsInput()
                                && cell.hasOxygenInputConnection(direction)) {
                            hasConnectedInput = true;
                            break;
                        }
                    }
                    if (hasConnectedInput) oxygenInputCells.add(pos.immutable());
                }
            }
        }
    }

    private boolean hasOxygenInputConnection(Direction direction) {
        if (level == null || direction == null) return false;
        BlockPos neighborPos = worldPosition.relative(direction);
        if (!level.hasChunkAt(neighborPos)) return false;

        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof OxygenPipeBlock) return true;

        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor == null) return false;

        return neighbor.getCapability(
                ModCapabilities.OXYGEN,
                direction.getOpposite()
        ).isPresent();
    }

    void markOxygenPortCacheDirty() {
        if (isMaster()) oxygenPortCacheDirty = true;
    }

    void markMasterOxygenPortCacheDirty() {
        UniversalTankBlockEntity master = getMasterEntity();
        if (master != null) master.markOxygenPortCacheDirty();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            UniversalTankBlockEntity master = getMasterEntity();
            if (master != null) {
                master.markOxygenPortCacheDirty();
                UniversalTankEvents.trackMaster(serverLevel, master.getBlockPos());
            }
        }
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    private Direction getLocalFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(UniversalTankBlock.FACING)
                ? state.getValue(UniversalTankBlock.FACING)
                : Direction.NORTH;
    }

    public Direction getFacing() {
        if (unifiedModel && !isMaster()) {
            UniversalTankBlockEntity master = getMasterEntity();
            if (master != null) return master.getLocalFacing();
        }
        return getLocalFacing();
    }

    /**
     * Same machine-relative side resolver used by every other DomeSurvival machine.
     * LEFT/RIGHT front-view projection is a client-screen concern and is intentionally
     * handled by UniversalTankScreen.machineSideForVisualSide().
     */
    public static Direction resolveRelativeSide(RelativeSide side, Direction facing) {
        Direction horizontalFacing = facing != null && facing.getAxis().isHorizontal()
                ? facing
                : Direction.NORTH;
        return side.resolve(horizontalFacing);
    }

    public SideMode getSideMode(Direction direction) {
        if (direction == null) return SideMode.DISABLED;

        UniversalTankBlockEntity master = getMasterEntity();
        if (master != null && master.unifiedModel) {
            if (!direction.getAxis().isHorizontal()) return SideMode.DISABLED;
            return master.sideConfig.getMode(direction);
        }

        return sideConfig.getMode(direction);
    }

    public SideMode getSideMode(RelativeSide side) {
        return getSideMode(resolveRelativeSide(side, getFacing()));
    }

    public SideMode cycleSideMode(RelativeSide side) {
        Direction worldSide = resolveRelativeSide(side, getFacing());
        UniversalTankBlockEntity master = getMasterEntity();

        if (master != null && master.unifiedModel) {
            if (!worldSide.getAxis().isHorizontal()) return SideMode.DISABLED;

            SideMode next = master.sideConfig.cycleMode(worldSide);
            master.setChanged();
            master.markOxygenPortCacheDirty();
            master.syncClientState(true);

            BlockPos portPos = master.unifiedPortPosition(worldSide);
            if (portPos != null && master.level != null && master.level.hasChunkAt(portPos)) {
                BlockEntity blockEntity = master.level.getBlockEntity(portPos);
                if (blockEntity instanceof UniversalTankBlockEntity portCell) {
                    portCell.refreshCapabilities();
                    portCell.syncClientState(true);
                    portCell.notifyNeighborConnections();
                }
            }
            return next;
        }

        SideMode next = sideConfig.cycleMode(worldSide);
        setChanged();
        refreshCapabilities();
        if (master != null) master.markOxygenPortCacheDirty();
        syncClientState(true);
        notifyNeighborConnections();
        return next;
    }

    public boolean isExternalFace(Direction side) {
        if (level == null || side == null) return true;
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(side));
        return !(neighbor instanceof UniversalTankBlockEntity other)
                || !other.masterPos.equals(masterPos);
    }

    /**
     * Actual connection surface. A unified reservoir exposes exactly four ports:
     * one centered block on NORTH/SOUTH/WEST/EAST. Modular cells retain their
     * normal external-face behavior.
     */
    public boolean isConnectorFace(Direction side) {
        if (side == null) return false;

        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null || !master.unifiedModel) {
            return isExternalFace(side);
        }
        if (!side.getAxis().isHorizontal()) return false;

        BlockPos portPos = master.unifiedPortPosition(side);
        return portPos != null && worldPosition.equals(portPos);
    }

    @Nullable
    private BlockPos unifiedPortPosition(Direction side) {
        if (!unifiedModel || side == null || !side.getAxis().isHorizontal()) return null;

        int centerX = (structureMin.getX() + structureMax.getX()) >> 1;
        int centerY = (structureMin.getY() + structureMax.getY()) >> 1;
        int centerZ = (structureMin.getZ() + structureMax.getZ()) >> 1;

        return switch (side) {
            case NORTH -> new BlockPos(centerX, centerY, structureMin.getZ());
            case SOUTH -> new BlockPos(centerX, centerY, structureMax.getZ());
            case WEST -> new BlockPos(structureMin.getX(), centerY, centerZ);
            case EAST -> new BlockPos(structureMax.getX(), centerY, centerZ);
            default -> null;
        };
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public BlockPos getStructureMin() {
        return structureMin;
    }

    public BlockPos getStructureMax() {
        return structureMax;
    }

    public int getStructureBlockCount() {
        UniversalTankBlockEntity master = getMasterEntity();
        return master == null ? blockCount : master.blockCount;
    }

    public int getStructureCapacity() {
        UniversalTankBlockEntity master = getMasterEntity();
        return master == null ? getLocalCapacity() : master.getLocalCapacity();
    }

    public int getStoredAmount() {
        UniversalTankBlockEntity master = getMasterEntity();
        return master == null ? 0 : master.getLocalStoredAmount();
    }

    public UniversalTankContentKind getContentKind() {
        UniversalTankBlockEntity master = getMasterEntity();
        return master == null ? UniversalTankContentKind.EMPTY : master.contentKind;
    }

    public FluidStack getVisibleFluidStack() {
        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null || master.contentKind != UniversalTankContentKind.FLUID || master.fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return master.fluid.copy();
    }

    public int getVisibleOxygen() {
        UniversalTankBlockEntity master = getMasterEntity();
        return master != null && master.contentKind == UniversalTankContentKind.OXYGEN
                ? master.oxygen : 0;
    }

    public float getFillFraction() {
        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null) return 0.0F;
        int capacity = master.getLocalCapacity();
        return capacity <= 0 ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, master.getLocalStoredAmount() / (float) capacity));
    }

    public boolean isMaster() {
        return worldPosition.equals(masterPos);
    }

    public boolean isUnifiedModel() {
        UniversalTankBlockEntity master = getMasterEntity();
        return master != null && master.unifiedModel;
    }

    public boolean isUnifiedMaster() {
        return isMaster() && unifiedModel;
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Unified structures are rendered entirely by the master block entity.
        // The default one-block AABB caused the whole reservoir to disappear when
        // the camera stopped seeing the master's origin even while other cells
        // remained on screen.
        final double margin = 0.125D;

        if (unifiedModel) {
            return new AABB(
                    structureMin.getX() - margin,
                    structureMin.getY() - margin,
                    structureMin.getZ() - margin,
                    structureMax.getX() + 1.0D + margin,
                    structureMax.getY() + 1.0D + margin,
                    structureMax.getZ() + 1.0D + margin
            );
        }

        return new AABB(
                worldPosition.getX() - margin,
                worldPosition.getY() - margin,
                worldPosition.getZ() - margin,
                worldPosition.getX() + 1.0D + margin,
                worldPosition.getY() + 1.0D + margin,
                worldPosition.getZ() + 1.0D + margin
        );
    }

    @Nullable
    public UniversalTankBlockEntity getMasterEntity() {
        if (level == null) return isMaster() ? this : null;
        if (isMaster()) return this;
        if (!level.hasChunkAt(masterPos)) return null;

        BlockEntity blockEntity = level.getBlockEntity(masterPos);
        if (blockEntity instanceof UniversalTankBlockEntity tank
                && tank.masterPos.equals(masterPos)) {
            return tank;
        }
        return null;
    }

    int getLocalStoredAmount() {
        return switch (contentKind) {
            case EMPTY -> 0;
            case FLUID -> fluid.isEmpty() ? 0 : fluid.getAmount();
            case OXYGEN -> Math.max(0, oxygen);
        };
    }

    int getLocalCapacity() {
        long capacity = (long) Math.max(1, blockCount) * CAPACITY_PER_BLOCK;
        return capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    int getStructureSizeX() {
        return Math.max(1, structureMax.getX() - structureMin.getX() + 1);
    }

    int getStructureSizeY() {
        return Math.max(1, structureMax.getY() - structureMin.getY() + 1);
    }

    int getStructureSizeZ() {
        return Math.max(1, structureMax.getZ() - structureMin.getZ() + 1);
    }

    boolean applyStructureData(BlockPos newMaster, BlockPos min, BlockPos max,
                               int newBlockCount, boolean newUnifiedModel) {
        BlockPos immutableMaster = newMaster.immutable();
        BlockPos immutableMin = min.immutable();
        BlockPos immutableMax = max.immutable();

        boolean changed = !masterPos.equals(immutableMaster)
                || !structureMin.equals(immutableMin)
                || !structureMax.equals(immutableMax)
                || blockCount != newBlockCount
                || unifiedModel != newUnifiedModel;

        masterPos = immutableMaster;
        structureMin = immutableMin;
        structureMax = immutableMax;
        blockCount = Math.max(1, newBlockCount);
        unifiedModel = newUnifiedModel;

        if (!isMaster()) {
            clearLocalContent();
        }

        if (changed) {
            setChanged();
            refreshCapabilities();
            if (isMaster()) markOxygenPortCacheDirty();
        }
        return changed;
    }

    /**
     * Writes one portable 4,000 mB cell worth of shared reservoir content into
     * the dismantled BlockItem. Structure coordinates are intentionally omitted,
     * so placing the item always starts as a safe standalone cell before normal
     * merge logic runs.
     */
    void prepareEngineerWrenchClone(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null) return;

        int portableAmount = Math.min(CAPACITY_PER_BLOCK, master.getLocalStoredAmount());

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.putInt(NBT_BLOCK_COUNT, 1);
        blockEntityTag.putBoolean(NBT_UNIFIED, false);
        blockEntityTag.putInt(NBT_KIND, master.contentKind.ordinal());

        if (portableAmount > 0 && master.contentKind == UniversalTankContentKind.FLUID && !master.fluid.isEmpty()) {
            FluidStack portableFluid = master.fluid.copy();
            portableFluid.setAmount(portableAmount);
            blockEntityTag.put(NBT_FLUID, portableFluid.writeToNBT(new CompoundTag()));
        } else if (portableAmount > 0 && master.contentKind == UniversalTankContentKind.OXYGEN) {
            blockEntityTag.putInt(NBT_OXYGEN, portableAmount);
        }

        // Preserve connector configuration of the physical cell being removed.
        sideConfig.save(blockEntityTag);
        blockEntityTag.putInt(NBT_SIDE_DEFAULTS_VERSION, SIDE_DEFAULTS_VERSION);

        stack.addTagElement("BlockEntityTag", blockEntityTag);

        pendingPortableExtractionAmount = portableAmount;
        pendingPortableExtractionGameTime = level == null
                ? Long.MIN_VALUE
                : level.getGameTime();
    }

    int consumeEngineerWrenchExtraction() {
        int result = 0;

        if (pendingPortableExtractionAmount > 0
                && level != null
                && pendingPortableExtractionGameTime != Long.MIN_VALUE) {
            long age = level.getGameTime() - pendingPortableExtractionGameTime;
            if (age >= 0L && age <= 1L) {
                result = pendingPortableExtractionAmount;
            }
        }

        pendingPortableExtractionAmount = 0;
        pendingPortableExtractionGameTime = Long.MIN_VALUE;
        return result;
    }

    void clearLocalContent() {
        contentKind = UniversalTankContentKind.EMPTY;
        fluid = FluidStack.EMPTY;
        oxygen = 0;
    }

    void setLocalContent(UniversalTankStructure.ContentSnapshot snapshot) {
        clearLocalContent();
        if (snapshot == null || snapshot.amount() <= 0) {
            onStorageChanged(true);
            return;
        }

        if (snapshot.kind() == UniversalTankContentKind.FLUID && !snapshot.fluid().isEmpty()) {
            contentKind = UniversalTankContentKind.FLUID;
            fluid = snapshot.fluid().copy();
            fluid.setAmount(Math.min(snapshot.amount(), getLocalCapacity()));
        } else if (snapshot.kind() == UniversalTankContentKind.OXYGEN) {
            contentKind = UniversalTankContentKind.OXYGEN;
            oxygen = Math.min(snapshot.amount(), getLocalCapacity());
        }

        onStorageChanged(true);
    }

    UniversalTankStructure.ContentSnapshot snapshotLocalContent() {
        if (contentKind == UniversalTankContentKind.FLUID && !fluid.isEmpty()) {
            return new UniversalTankStructure.ContentSnapshot(
                    UniversalTankContentKind.FLUID,
                    fluid.copy(),
                    fluid.getAmount()
            );
        }
        if (contentKind == UniversalTankContentKind.OXYGEN && oxygen > 0) {
            return new UniversalTankStructure.ContentSnapshot(
                    UniversalTankContentKind.OXYGEN,
                    FluidStack.EMPTY,
                    oxygen
            );
        }
        return UniversalTankStructure.ContentSnapshot.empty();
    }

    private int fillFluid(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty() || resource.getAmount() <= 0) return 0;

        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null) return 0;
        if (master.contentKind == UniversalTankContentKind.OXYGEN) return 0;
        if (master.contentKind == UniversalTankContentKind.FLUID
                && !master.fluid.isEmpty()
                && !master.fluid.isFluidEqual(resource)) {
            return 0;
        }

        int free = Math.max(0, master.getLocalCapacity() - master.getLocalStoredAmount());
        int accepted = Math.min(resource.getAmount(), free);
        if (accepted <= 0 || simulate) return accepted;

        boolean typeChanged = master.contentKind != UniversalTankContentKind.FLUID;
        if (typeChanged || master.fluid.isEmpty()) {
            master.contentKind = UniversalTankContentKind.FLUID;
            master.fluid = resource.copy();
            master.fluid.setAmount(accepted);
            master.oxygen = 0;
        } else {
            master.fluid.grow(accepted);
        }

        master.onStorageChanged(typeChanged);
        return accepted;
    }

    private FluidStack drainFluid(@Nullable FluidStack requested, int maxDrain, boolean simulate) {
        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null
                || master.contentKind != UniversalTankContentKind.FLUID
                || master.fluid.isEmpty()
                || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }

        if (requested != null && !requested.isEmpty() && !master.fluid.isFluidEqual(requested)) {
            return FluidStack.EMPTY;
        }

        int amount = Math.min(maxDrain, master.fluid.getAmount());
        FluidStack drained = master.fluid.copy();
        drained.setAmount(amount);

        if (!simulate) {
            master.fluid.shrink(amount);
            boolean typeChanged = master.fluid.isEmpty() || master.fluid.getAmount() <= 0;
            if (typeChanged) {
                master.clearLocalContent();
            }
            master.onStorageChanged(typeChanged);
        }
        return drained;
    }

    private int receiveOxygen(int amount, boolean simulate) {
        if (amount <= 0) return 0;

        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null || master.contentKind == UniversalTankContentKind.FLUID) return 0;

        int free = Math.max(0, master.getLocalCapacity() - master.getLocalStoredAmount());
        int accepted = Math.min(amount, free);
        if (accepted <= 0 || simulate) return accepted;

        boolean typeChanged = master.contentKind != UniversalTankContentKind.OXYGEN;
        master.contentKind = UniversalTankContentKind.OXYGEN;
        master.fluid = FluidStack.EMPTY;
        master.oxygen += accepted;
        master.onStorageChanged(typeChanged);
        return accepted;
    }

    private int extractOxygen(int amount, boolean simulate) {
        if (amount <= 0) return 0;

        UniversalTankBlockEntity master = getMasterEntity();
        if (master == null
                || master.contentKind != UniversalTankContentKind.OXYGEN
                || master.oxygen <= 0) {
            return 0;
        }

        int extracted = Math.min(amount, master.oxygen);
        if (!simulate) {
            master.oxygen -= extracted;
            boolean typeChanged = master.oxygen <= 0;
            if (typeChanged) {
                master.clearLocalContent();
            }
            master.onStorageChanged(typeChanged);
        }
        return extracted;
    }

    private void onStorageChanged(boolean forceClientSync) {
        if (!isMaster()) return;
        setChanged();

        if (level != null && !level.isClientSide) {
            int capacity = Math.max(1, getLocalCapacity());
            int visualLevel = Math.max(0, Math.min(64,
                    (int) (((long) getLocalStoredAmount() * 64L) / capacity)));
            int visualKey = contentKind.ordinal() * 1000 + visualLevel;

            if (forceClientSync || visualKey != lastSyncedVisualKey) {
                lastSyncedVisualKey = visualKey;
                syncClientState(false);
            }
        }
    }

    void syncStructureStateToClient() {
        syncClientState(true);
        notifyNeighborConnections();
    }

    private void syncClientState(boolean force) {
        if (level == null || level.isClientSide) return;
        if (force) lastSyncedVisualKey = Integer.MIN_VALUE;

        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private void notifyNeighborConnections() {
        if (level == null || level.isClientSide) return;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    private boolean allowsInput(Direction side) {
        return side == null || (isConnectorFace(side) && getSideMode(side).allowsInput());
    }

    private boolean allowsOutput(Direction side) {
        return side == null || (isConnectorFace(side) && getSideMode(side).allowsOutput());
    }

    private void rebuildCapabilityViews() {
        internalFluidCapability = LazyOptional.of(() -> new FluidView(null));
        internalOxygenCapability = LazyOptional.of(() -> new OxygenView(null));

        fluidCapabilities.clear();
        oxygenCapabilities.clear();
        for (Direction direction : Direction.values()) {
            fluidCapabilities.put(direction, LazyOptional.of(() -> new FluidView(direction)));
            oxygenCapabilities.put(direction, LazyOptional.of(() -> new OxygenView(direction)));
        }
    }

    private void refreshCapabilities() {
        invalidateOwnCapabilities();
        rebuildCapabilityViews();
    }

    private void invalidateOwnCapabilities() {
        internalFluidCapability.invalidate();
        internalOxygenCapability.invalidate();
        for (LazyOptional<IFluidHandler> optional : fluidCapabilities.values()) optional.invalidate();
        for (LazyOptional<IOxygenStorage> optional : oxygenCapabilities.values()) optional.invalidate();
        fluidCapabilities.clear();
        oxygenCapabilities.clear();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                      @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side == null) return internalFluidCapability.cast();
            if (!isConnectorFace(side) || getSideMode(side) == SideMode.DISABLED) {
                return super.getCapability(cap, side);
            }
            LazyOptional<IFluidHandler> optional = fluidCapabilities.get(side);
            return optional == null ? super.getCapability(cap, side) : optional.cast();
        }

        if (cap == ModCapabilities.OXYGEN) {
            if (side == null) return internalOxygenCapability.cast();
            if (!isConnectorFace(side) || getSideMode(side) == SideMode.DISABLED) {
                return super.getCapability(cap, side);
            }
            LazyOptional<IOxygenStorage> optional = oxygenCapabilities.get(side);
            return optional == null ? super.getCapability(cap, side) : optional.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invalidateOwnCapabilities();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        rebuildCapabilityViews();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putLong(NBT_MASTER, masterPos.asLong());
        tag.putLong(NBT_MIN, structureMin.asLong());
        tag.putLong(NBT_MAX, structureMax.asLong());
        tag.putInt(NBT_BLOCK_COUNT, blockCount);
        tag.putBoolean(NBT_UNIFIED, unifiedModel);
        tag.putInt(NBT_KIND, contentKind.ordinal());

        if (isMaster() && contentKind == UniversalTankContentKind.FLUID && !fluid.isEmpty()) {
            tag.put(NBT_FLUID, fluid.writeToNBT(new CompoundTag()));
        } else if (isMaster() && contentKind == UniversalTankContentKind.OXYGEN && oxygen > 0) {
            tag.putInt(NBT_OXYGEN, oxygen);
        }

        sideConfig.save(tag);
        tag.putInt(NBT_SIDE_DEFAULTS_VERSION, SIDE_DEFAULTS_VERSION);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        masterPos = tag.contains(NBT_MASTER) ? BlockPos.of(tag.getLong(NBT_MASTER)) : worldPosition.immutable();
        structureMin = tag.contains(NBT_MIN) ? BlockPos.of(tag.getLong(NBT_MIN)) : worldPosition.immutable();
        structureMax = tag.contains(NBT_MAX) ? BlockPos.of(tag.getLong(NBT_MAX)) : worldPosition.immutable();
        blockCount = Math.max(1, tag.getInt(NBT_BLOCK_COUNT));
        unifiedModel = tag.getBoolean(NBT_UNIFIED);

        clearLocalContent();
        if (isMaster()) {
            contentKind = UniversalTankContentKind.byOrdinal(tag.getInt(NBT_KIND));
            if (contentKind == UniversalTankContentKind.FLUID && tag.contains(NBT_FLUID)) {
                fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(NBT_FLUID));
                if (fluid.isEmpty()) {
                    contentKind = UniversalTankContentKind.EMPTY;
                } else {
                    fluid.setAmount(Math.min(fluid.getAmount(), getLocalCapacity()));
                }
            } else if (contentKind == UniversalTankContentKind.OXYGEN) {
                oxygen = Math.max(0, Math.min(getLocalCapacity(), tag.getInt(NBT_OXYGEN)));
                if (oxygen <= 0) contentKind = UniversalTankContentKind.EMPTY;
            }
        }

        if (!sideConfig.load(tag)) {
            applyDefaultSideConfiguration();
        } else {
            migrateLegacyDefaultSideConfiguration(tag);
        }

        lastSyncedVisualKey = Integer.MIN_VALUE;
        oxygenPortCacheDirty = true;
        oxygenInputCursor = 0;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
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
    public Component getDisplayName() {
        return Component.translatable("block.domesurvival.universal_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new UniversalTankMenu(containerId, playerInventory, this);
    }

    private final class FluidView implements IFluidHandler {
        @Nullable
        private final Direction side;

        private FluidView(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? getVisibleFluidStack() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? getStructureCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || !allowsInput(side)) return false;
            UniversalTankBlockEntity master = getMasterEntity();
            if (master == null || master.contentKind == UniversalTankContentKind.OXYGEN) return false;
            return master.contentKind != UniversalTankContentKind.FLUID
                    || master.fluid.isEmpty()
                    || master.fluid.isFluidEqual(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!allowsInput(side)) return 0;
            return fillFluid(resource, action.simulate());
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (!allowsOutput(side) || resource.isEmpty()) return FluidStack.EMPTY;
            return drainFluid(resource, resource.getAmount(), action.simulate());
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowsOutput(side)) return FluidStack.EMPTY;
            return drainFluid(null, maxDrain, action.simulate());
        }
    }

    private final class OxygenView implements IOxygenStorage {
        @Nullable
        private final Direction side;

        private OxygenView(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int receiveOxygen(int maxReceive, boolean simulate) {
            if (!allowsInput(side)) return 0;
            return UniversalTankBlockEntity.this.receiveOxygen(maxReceive, simulate);
        }

        @Override
        public int extractOxygen(int maxExtract, boolean simulate) {
            if (!allowsOutput(side)) return 0;
            return UniversalTankBlockEntity.this.extractOxygen(maxExtract, simulate);
        }

        @Override
        public int getOxygenStored() {
            return getVisibleOxygen();
        }

        @Override
        public int getMaxOxygenStored() {
            return getStructureCapacity();
        }

        @Override
        public boolean canReceive() {
            UniversalTankBlockEntity master = getMasterEntity();
            return allowsInput(side)
                    && master != null
                    && master.contentKind != UniversalTankContentKind.FLUID
                    && master.getLocalStoredAmount() < master.getLocalCapacity();
        }

        @Override
        public boolean canExtract() {
            UniversalTankBlockEntity master = getMasterEntity();
            return allowsOutput(side)
                    && master != null
                    && master.contentKind == UniversalTankContentKind.OXYGEN
                    && master.oxygen > 0;
        }
    }
}
