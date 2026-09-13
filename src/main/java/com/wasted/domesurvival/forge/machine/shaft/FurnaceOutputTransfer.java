package com.wasted.domesurvival.forge.machine.shaft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/** Small, lossless exporter shared by the two solid-fuel furnaces. */
final class FurnaceOutputTransfer {
    private FurnaceOutputTransfer() {
    }

    static boolean push(Level level, ItemStackHandler source, int sourceSlot,
                        BlockPos targetPos, Direction targetFace) {
        if (!level.hasChunkAt(targetPos)) return false;
        BlockEntity targetEntity = level.getBlockEntity(targetPos);
        if (targetEntity == null) return false;

        IItemHandler target = targetEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER, targetFace)
                .resolve().orElse(null);
        if (target == null) {
            target = targetEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                    .resolve().orElse(null);
        }
        if (target == null) return false;

        ItemStack available = source.getStackInSlot(sourceSlot);
        if (available.isEmpty()) return false;
        ItemStack simulatedRemainder = ItemHandlerHelper.insertItemStacked(target, available.copy(), true);
        int accepted = available.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) return false;

        ItemStack extracted = source.extractItem(sourceSlot, accepted, false);
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, extracted, false);
        if (!remainder.isEmpty()) {
            ItemStack current = source.getStackInSlot(sourceSlot);
            if (current.isEmpty()) {
                source.setStackInSlot(sourceSlot, remainder);
            } else if (ItemStack.isSameItemSameTags(current, remainder)) {
                current.grow(remainder.getCount());
            }
        }
        return remainder.getCount() < extracted.getCount();
    }
}
