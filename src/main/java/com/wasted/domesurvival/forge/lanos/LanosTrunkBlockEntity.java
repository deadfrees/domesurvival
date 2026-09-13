package com.wasted.domesurvival.forge.lanos;

import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import com.wasted.domesurvival.forge.block.DecorativeLanosBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LanosTrunkBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SIZE = 6;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int footprintCooldown;

    public LanosTrunkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANOS_TRUNK.get(), pos, state);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos,
                                  BlockState state, LanosTrunkBlockEntity trunk) {
        if (trunk.footprintCooldown-- <= 0) {
            DecorativeLanosBlock.ensureFootprint(level, pos, state);
            trunk.footprintCooldown = 40;
        }
    }

    public void ensureLootTable(net.minecraft.util.RandomSource random,
                                net.minecraft.resources.ResourceLocation table) {
        if (lootTable == null && isEmpty()) {
            setLootTable(table, random.nextLong());
            setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.domesurvival.lanos_trunk");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new LanosTrunkMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items);
        }
    }
}
