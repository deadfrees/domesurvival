package com.wasted.domesurvival.forge.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TieredHopperBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items;
    private int transferCooldown = 0;

    private LazyOptional<IItemHandler> itemHandler =
            LazyOptional.of(() -> new InvWrapper(this));

    public TieredHopperBlockEntity(BlockPos pos, BlockState state) {
        super(HopperRegistryEvents.HOPPER_BLOCK_ENTITY.get(), pos, state);
        this.items = NonNullList.withSize(
                HopperRegistryEvents.inventorySize(state.getBlock()),
                ItemStack.EMPTY
        );
    }


    @Override
    protected Component getDefaultName() {
        BlockState state = getBlockState();

        if (state.is(HopperRegistryEvents.COPPER_HOPPER.get())) {
            return Component.translatable("container.domesurvival.copper_hopper");
        }
        if (state.is(HopperRegistryEvents.STEEL_HOPPER.get())) {
            return Component.translatable("container.domesurvival.steel_hopper");
        }
        return Component.translatable("container.domesurvival.desh_hopper");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new TieredHopperMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);

        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }

        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }

        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.items = NonNullList.withSize(
                HopperRegistryEvents.inventorySize(getBlockState().getBlock()),
                ItemStack.EMPTY
        );
        ContainerHelper.loadAllItems(tag, this.items);
        this.transferCooldown = tag.getInt("TransferCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("TransferCooldown", this.transferCooldown);
    }

    @Override
    public <T> LazyOptional<T> getCapability(
            Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TieredHopperBlockEntity hopper) {
        if (hopper.transferCooldown > 0) {
            hopper.transferCooldown--;
            return;
        }

        if (!state.getValue(TieredHopperBlock.ENABLED)) {
            return;
        }

        boolean moved = hopper.pushOneItem();
        if (!moved) moved = hopper.pullOneItem();
        if (!moved) moved = hopper.collectOneItemEntity();

        if (moved) {
            hopper.transferCooldown = 8;
            hopper.setChanged();
        }
    }

    private boolean pushOneItem() {
        if (level == null) return false;

        Direction facing = getBlockState().getValue(TieredHopperBlock.FACING);
        BlockEntity targetEntity = level.getBlockEntity(worldPosition.relative(facing));
        if (targetEntity == null) return false;

        IItemHandler target = targetEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite())
                .orElse(null);

        if (target == null) return false;

        for (int sourceSlot = 0; sourceSlot < items.size(); sourceSlot++) {
            ItemStack source = items.get(sourceSlot);
            if (source.isEmpty()) continue;

            ItemStack one = source.copy();
            one.setCount(1);

            for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
                ItemStack remainder = target.insertItem(targetSlot, one, true);
                if (!remainder.isEmpty()) continue;

                ItemStack executed = target.insertItem(targetSlot, one, false);
                if (!executed.isEmpty()) continue;

                source.shrink(1);
                if (source.isEmpty()) {
                    items.set(sourceSlot, ItemStack.EMPTY);
                }
                setChanged();
                return true;
            }
        }

        return false;
    }

    private boolean pullOneItem() {
        if (level == null) return false;

        BlockEntity sourceEntity = level.getBlockEntity(worldPosition.above());
        if (sourceEntity == null) return false;

        IItemHandler source = sourceEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
                .orElse(null);

        if (source == null) return false;

        for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
            ItemStack simulated = source.extractItem(sourceSlot, 1, true);
            if (simulated.isEmpty()) continue;

            int destination = findDestinationSlot(simulated);
            if (destination < 0) return false;

            ItemStack extracted = source.extractItem(sourceSlot, 1, false);
            if (extracted.isEmpty()) continue;

            ItemStack remainder = insertIntoSlot(destination, extracted);
            if (remainder.isEmpty()) {
                setChanged();
                return true;
            }

            // Defensive recovery for non-standard handlers.
            for (int restoreSlot = 0; restoreSlot < source.getSlots() && !remainder.isEmpty(); restoreSlot++) {
                remainder = source.insertItem(restoreSlot, remainder, false);
            }
            return false;
        }

        return false;
    }

    private boolean collectOneItemEntity() {
        if (level == null) return false;

        AABB pickupBox = new AABB(
                worldPosition.getX(),
                worldPosition.getY() + 1.0D,
                worldPosition.getZ(),
                worldPosition.getX() + 1.0D,
                worldPosition.getY() + 2.0D,
                worldPosition.getZ() + 1.0D
        );

        List<ItemEntity> entities = level.getEntitiesOfClass(
                ItemEntity.class,
                pickupBox,
                entity -> entity.isAlive() && !entity.getItem().isEmpty()
        );

        for (ItemEntity entity : entities) {
            ItemStack entityStack = entity.getItem();
            int destination = findDestinationSlot(entityStack);
            if (destination < 0) return false;

            ItemStack one = entityStack.copy();
            one.setCount(1);

            ItemStack remainder = insertIntoSlot(destination, one);
            if (!remainder.isEmpty()) continue;

            entityStack.shrink(1);
            if (entityStack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(entityStack);
            }

            setChanged();
            return true;
        }

        return false;
    }

    private int findDestinationSlot(ItemStack incoming) {
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack current = items.get(slot);

            if (current.isEmpty()) {
                return slot;
            }

            if (ItemStack.isSameItemSameTags(current, incoming)
                    && current.getCount() < Math.min(current.getMaxStackSize(), getMaxStackSize())) {
                return slot;
            }
        }

        return -1;
    }

    private ItemStack insertIntoSlot(int slot, ItemStack incoming) {
        ItemStack current = items.get(slot);

        if (current.isEmpty()) {
            items.set(slot, incoming.copy());
            return ItemStack.EMPTY;
        }

        if (!ItemStack.isSameItemSameTags(current, incoming)) {
            return incoming;
        }

        int max = Math.min(current.getMaxStackSize(), getMaxStackSize());
        int room = max - current.getCount();
        if (room <= 0) return incoming;

        int moved = Math.min(room, incoming.getCount());
        current.grow(moved);

        if (moved == incoming.getCount()) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = incoming.copy();
        remainder.shrink(moved);
        return remainder;
    }
}
