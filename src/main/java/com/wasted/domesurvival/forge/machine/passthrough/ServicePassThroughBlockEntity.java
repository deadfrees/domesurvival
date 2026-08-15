package com.wasted.domesurvival.forge.machine.passthrough;

import com.wasted.domesurvival.forge.capability.IOxygenStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeBlock;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlock;
import com.wasted.domesurvival.forge.transport.fluid.FluidPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class ServicePassThroughBlockEntity extends BlockEntity {
    private static final String NBT_CONDUIT = "InstalledConduit";
    private static final String NBT_KIND = "ConduitKind";
    private static final int MAX_CHAIN_LENGTH = 32;

    private ItemStack installedConduit = ItemStack.EMPTY;
    private ServiceConduitKind conduitKind = ServiceConduitKind.EMPTY;

    // Only the two active opposite faces can ever expose a bridge.
    // LazyOptionals are cached and invalidated on configuration/lifecycle changes.
    private LazyOptional<IEnergyStorage> negativeEnergy = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> positiveEnergy = LazyOptional.empty();
    private LazyOptional<IFluidHandler> negativeFluid = LazyOptional.empty();
    private LazyOptional<IFluidHandler> positiveFluid = LazyOptional.empty();
    private LazyOptional<IItemHandler> negativeItems = LazyOptional.empty();
    private LazyOptional<IItemHandler> positiveItems = LazyOptional.empty();
    private LazyOptional<IOxygenStorage> negativeOxygen = LazyOptional.empty();
    private LazyOptional<IOxygenStorage> positiveOxygen = LazyOptional.empty();

    public ServicePassThroughBlockEntity(BlockPos pos, BlockState state) {
        super(ServicePassThroughRegistry.BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isEmpty() {
        return installedConduit.isEmpty() || conduitKind == ServiceConduitKind.EMPTY;
    }

    public ServiceConduitKind getConduitKind() {
        return conduitKind;
    }

    public ItemStack getInstalledConduit() {
        return installedConduit.copy();
    }

    @Nullable
    public Block getInstalledConduitBlock() {
        return installedConduit.getItem() instanceof BlockItem blockItem
                ? blockItem.getBlock()
                : null;
    }

    public boolean installConduit(ItemStack source, ServiceConduitKind kind) {
        if (!isEmpty() || source.isEmpty() || kind == ServiceConduitKind.EMPTY) {
            return false;
        }

        invalidateBridgeCaps();
        installedConduit = source.copyWithCount(1);
        conduitKind = kind;
        setOccupiedState(true);
        markDirtyAndSync();
        return true;
    }

    public void removeInstalledConduit(Player player) {
        if (isEmpty()) {
            return;
        }

        ItemStack returned = installedConduit.copy();
        clearInstalled();

        if (!player.addItem(returned)) {
            player.drop(returned, false);
        }
    }

    public void dropInstalledConduit() {
        if (isEmpty() || level == null) {
            return;
        }

        ItemStack dropped = installedConduit.copy();
        invalidateBridgeCaps();
        installedConduit = ItemStack.EMPTY;
        conduitKind = ServiceConduitKind.EMPTY;
        setChanged();
        Block.popResource(level, worldPosition, dropped);
    }

    public void onAxisChanged() {
        invalidateBridgeCaps();
        markDirtyAndSync();
    }

    private void clearInstalled() {
        invalidateBridgeCaps();
        installedConduit = ItemStack.EMPTY;
        conduitKind = ServiceConduitKind.EMPTY;
        setOccupiedState(false);
        markDirtyAndSync();
    }

    private void setOccupiedState(boolean occupied) {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (state.getBlock() == ServicePassThroughRegistry.BLOCK.get()
                && state.hasProperty(ServicePassThroughBlock.OCCUPIED)
                && state.getValue(ServicePassThroughBlock.OCCUPIED) != occupied) {
            level.setBlock(
                    worldPosition,
                    state.setValue(ServicePassThroughBlock.OCCUPIED, occupied),
                    Block.UPDATE_ALL
            );
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            setOccupiedState(!isEmpty());
        }
    }

    public boolean isAxisCompatible(Direction direction) {
        return getBlockState().getValue(ServicePassThroughBlock.AXIS) == direction.getAxis();
    }

    public int installedTransferLimit() {
        Block block = getInstalledConduitBlock();
        if (block instanceof EnergyPipeBlock energyPipe) {
            return energyPipe.tier().transferPerTick();
        }
        if (block instanceof FluidPipeBlock fluidPipe) {
            return fluidPipe.tier().transferPerTick();
        }
        if (block instanceof OxygenPipeBlock oxygenPipe) {
            return oxygenPipe.getTransferRate();
        }
        return Integer.MAX_VALUE;
    }

    private int capAmount(int requested) {
        if (requested <= 0) {
            return 0;
        }
        int limit = installedTransferLimit();
        return limit == Integer.MAX_VALUE ? requested : Math.min(requested, limit);
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, state.getBlock());
        }
    }

    @Nullable
    private BlockEntity findOppositeEndpoint(Direction queriedSide) {
        if (level == null || !isAxisCompatible(queriedSide)) {
            return null;
        }

        Direction travel = queriedSide.getOpposite();
        BlockPos cursor = worldPosition.relative(travel);

        for (int i = 0; i < MAX_CHAIN_LENGTH && level.hasChunkAt(cursor); i++) {
            BlockEntity candidate = level.getBlockEntity(cursor);
            if (candidate instanceof ServicePassThroughBlockEntity pass) {
                if (pass.isEmpty()
                        || !pass.isAxisCompatible(travel)
                        || pass.conduitKind != conduitKind) {
                    return null;
                }
                cursor = cursor.relative(travel);
                continue;
            }
            return candidate;
        }

        return null;
    }

    @Nullable
    private <T> T targetCapability(Capability<T> capability, Direction queriedSide) {
        BlockEntity endpoint = findOppositeEndpoint(queriedSide);
        if (endpoint == null) {
            return null;
        }

        // queriedSide is also the face of the remote endpoint facing back toward
        // this pass-through chain.
        return endpoint.getCapability(capability, queriedSide).resolve().orElse(null);
    }

    private boolean isNegative(Direction side) {
        return side.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
    }

    private LazyOptional<IEnergyStorage> energyBridge(Direction side) {
        if (isNegative(side)) {
            if (!negativeEnergy.isPresent()) {
                negativeEnergy = LazyOptional.of(() -> new SidedEnergyBridge(this, side));
            }
            return negativeEnergy;
        }

        if (!positiveEnergy.isPresent()) {
            positiveEnergy = LazyOptional.of(() -> new SidedEnergyBridge(this, side));
        }
        return positiveEnergy;
    }

    private LazyOptional<IFluidHandler> fluidBridge(Direction side) {
        if (isNegative(side)) {
            if (!negativeFluid.isPresent()) {
                negativeFluid = LazyOptional.of(() -> new SidedFluidBridge(this, side));
            }
            return negativeFluid;
        }

        if (!positiveFluid.isPresent()) {
            positiveFluid = LazyOptional.of(() -> new SidedFluidBridge(this, side));
        }
        return positiveFluid;
    }

    private LazyOptional<IItemHandler> itemBridge(Direction side) {
        if (isNegative(side)) {
            if (!negativeItems.isPresent()) {
                negativeItems = LazyOptional.of(() -> new SidedItemBridge(this, side));
            }
            return negativeItems;
        }

        if (!positiveItems.isPresent()) {
            positiveItems = LazyOptional.of(() -> new SidedItemBridge(this, side));
        }
        return positiveItems;
    }

    private LazyOptional<IOxygenStorage> oxygenBridge(Direction side) {
        if (isNegative(side)) {
            if (!negativeOxygen.isPresent()) {
                negativeOxygen = LazyOptional.of(() -> new SidedOxygenBridge(this, side));
            }
            return negativeOxygen;
        }

        if (!positiveOxygen.isPresent()) {
            positiveOxygen = LazyOptional.of(() -> new SidedOxygenBridge(this, side));
        }
        return positiveOxygen;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (side == null || isEmpty() || !isAxisCompatible(side)) {
            return super.getCapability(cap, side);
        }

        if (conduitKind == ServiceConduitKind.ENERGY && cap == ForgeCapabilities.ENERGY) {
            return energyBridge(side).cast();
        }
        if (conduitKind == ServiceConduitKind.FLUID && cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidBridge(side).cast();
        }
        if (conduitKind == ServiceConduitKind.ITEM && cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemBridge(side).cast();
        }
        if (conduitKind == ServiceConduitKind.OXYGEN && cap == ModCapabilities.OXYGEN) {
            return oxygenBridge(side).cast();
        }

        return super.getCapability(cap, side);
    }

    private void invalidateBridgeCaps() {
        negativeEnergy.invalidate();
        positiveEnergy.invalidate();
        negativeFluid.invalidate();
        positiveFluid.invalidate();
        negativeItems.invalidate();
        positiveItems.invalidate();
        negativeOxygen.invalidate();
        positiveOxygen.invalidate();

        negativeEnergy = LazyOptional.empty();
        positiveEnergy = LazyOptional.empty();
        negativeFluid = LazyOptional.empty();
        positiveFluid = LazyOptional.empty();
        negativeItems = LazyOptional.empty();
        positiveItems = LazyOptional.empty();
        negativeOxygen = LazyOptional.empty();
        positiveOxygen = LazyOptional.empty();
    }

    @Override
    public void invalidateCaps() {
        invalidateBridgeCaps();
        super.invalidateCaps();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!installedConduit.isEmpty()) {
            tag.put(NBT_CONDUIT, installedConduit.save(new CompoundTag()));
        }
        tag.putString(NBT_KIND, conduitKind.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        invalidateBridgeCaps();

        installedConduit = tag.contains(NBT_CONDUIT)
                ? ItemStack.of(tag.getCompound(NBT_CONDUIT))
                : ItemStack.EMPTY;

        try {
            conduitKind = ServiceConduitKind.valueOf(tag.getString(NBT_KIND));
        } catch (IllegalArgumentException ignored) {
            conduitKind = ServiceConduitKind.detect(installedConduit);
        }

        if (installedConduit.isEmpty()) {
            conduitKind = ServiceConduitKind.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    private static final class SidedEnergyBridge implements IEnergyStorage {
        private final ServicePassThroughBlockEntity owner;
        private final Direction side;

        private SidedEnergyBridge(ServicePassThroughBlockEntity owner, Direction side) {
            this.owner = owner;
            this.side = side;
        }

        private IEnergyStorage target() {
            return owner.targetCapability(ForgeCapabilities.ENERGY, side);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            IEnergyStorage target = target();
            return target == null
                    ? 0
                    : target.receiveEnergy(owner.capAmount(maxReceive), simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            IEnergyStorage target = target();
            return target == null
                    ? 0
                    : target.extractEnergy(owner.capAmount(maxExtract), simulate);
        }

        @Override
        public int getEnergyStored() {
            IEnergyStorage target = target();
            return target == null ? 0 : target.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            IEnergyStorage target = target();
            return target == null ? 0 : target.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            IEnergyStorage target = target();
            return target != null && target.canExtract();
        }

        @Override
        public boolean canReceive() {
            IEnergyStorage target = target();
            return target != null && target.canReceive();
        }
    }

    private static final class SidedFluidBridge implements IFluidHandler {
        private final ServicePassThroughBlockEntity owner;
        private final Direction side;

        private SidedFluidBridge(ServicePassThroughBlockEntity owner, Direction side) {
            this.owner = owner;
            this.side = side;
        }

        private IFluidHandler target() {
            return owner.targetCapability(ForgeCapabilities.FLUID_HANDLER, side);
        }

        @Override
        public int getTanks() {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            IFluidHandler target = target();
            return target != null && target.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            IFluidHandler target = target();
            if (target == null || resource.isEmpty()) {
                return 0;
            }

            FluidStack limited = resource.copy();
            limited.setAmount(owner.capAmount(resource.getAmount()));
            return target.fill(limited, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            IFluidHandler target = target();
            if (target == null || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }

            FluidStack limited = resource.copy();
            limited.setAmount(owner.capAmount(resource.getAmount()));
            return target.drain(limited, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler target = target();
            return target == null
                    ? FluidStack.EMPTY
                    : target.drain(owner.capAmount(maxDrain), action);
        }
    }

    private static final class SidedItemBridge implements IItemHandler {
        private final ServicePassThroughBlockEntity owner;
        private final Direction side;

        private SidedItemBridge(ServicePassThroughBlockEntity owner, Direction side) {
            this.owner = owner;
            this.side = side;
        }

        private IItemHandler target() {
            return owner.targetCapability(ForgeCapabilities.ITEM_HANDLER, side);
        }

        @Override
        public int getSlots() {
            IItemHandler target = target();
            return target == null ? 0 : target.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler target = target();
            return target == null ? ItemStack.EMPTY : target.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler target = target();
            return target == null ? stack : target.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler target = target();
            return target == null ? ItemStack.EMPTY : target.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler target = target();
            return target == null ? 0 : target.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler target = target();
            return target != null && target.isItemValid(slot, stack);
        }
    }

    private static final class SidedOxygenBridge implements IOxygenStorage {
        private final ServicePassThroughBlockEntity owner;
        private final Direction side;

        private SidedOxygenBridge(ServicePassThroughBlockEntity owner, Direction side) {
            this.owner = owner;
            this.side = side;
        }

        private IOxygenStorage target() {
            return owner.targetCapability(ModCapabilities.OXYGEN, side);
        }

        @Override
        public int receiveOxygen(int maxReceive, boolean simulate) {
            IOxygenStorage target = target();
            return target == null
                    ? 0
                    : target.receiveOxygen(owner.capAmount(maxReceive), simulate);
        }

        @Override
        public int extractOxygen(int maxExtract, boolean simulate) {
            IOxygenStorage target = target();
            return target == null
                    ? 0
                    : target.extractOxygen(owner.capAmount(maxExtract), simulate);
        }

        @Override
        public int getOxygenStored() {
            IOxygenStorage target = target();
            return target == null ? 0 : target.getOxygenStored();
        }

        @Override
        public int getMaxOxygenStored() {
            IOxygenStorage target = target();
            return target == null ? 0 : target.getMaxOxygenStored();
        }

        @Override
        public boolean canReceive() {
            IOxygenStorage target = target();
            return target != null && target.canReceive();
        }

        @Override
        public boolean canExtract() {
            IOxygenStorage target = target();
            return target != null && target.canExtract();
        }
    }
}
