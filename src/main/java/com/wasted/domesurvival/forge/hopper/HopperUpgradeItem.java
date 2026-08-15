package com.wasted.domesurvival.forge.hopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class HopperUpgradeItem extends Item {
    public enum Path {
        VANILLA_TO_COPPER,
        COPPER_TO_STEEL,
        STEEL_TO_DESH
    }

    private final Path path;

    public HopperUpgradeItem(Path path, Properties properties) {
        super(properties);
        this.path = path;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState sourceState = level.getBlockState(pos);

        Block expectedSource = expectedSource();
        TieredHopperBlock target = targetBlock();

        if (!sourceState.is(expectedSource)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity sourceEntity = level.getBlockEntity(pos);
        if (!(sourceEntity instanceof Container sourceContainer)) {
            return InteractionResult.FAIL;
        }

        Direction facing = sourceState.getValue(HopperBlock.FACING);
        boolean enabled = sourceState.getValue(HopperBlock.ENABLED);

        List<ItemStack> contents = new ArrayList<>(sourceContainer.getContainerSize());
        for (int slot = 0; slot < sourceContainer.getContainerSize(); slot++) {
            contents.add(sourceContainer.getItem(slot).copy());
            sourceContainer.setItem(slot, ItemStack.EMPTY);
        }

        sourceEntity.setChanged();

        BlockState targetState = target.defaultBlockState()
                .setValue(TieredHopperBlock.FACING, facing)
                .setValue(TieredHopperBlock.ENABLED, enabled);

        if (!level.setBlock(pos, targetState, Block.UPDATE_ALL)) {
            restoreContents(sourceContainer, contents);
            return InteractionResult.FAIL;
        }

        BlockEntity targetEntity = level.getBlockEntity(pos);
        if (!(targetEntity instanceof Container targetContainer)) {
            dropContents(level, pos, contents);
            return InteractionResult.FAIL;
        }

        int copyCount = Math.min(contents.size(), targetContainer.getContainerSize());
        for (int slot = 0; slot < copyCount; slot++) {
            targetContainer.setItem(slot, contents.get(slot));
        }

        if (targetEntity != null) {
            targetEntity.setChanged();
        }

        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        level.playSound(
                null,
                pos,
                SoundEvents.ANVIL_USE,
                SoundSource.BLOCKS,
                0.65F,
                1.25F
        );

        return InteractionResult.CONSUME;
    }

    private Block expectedSource() {
        return switch (path) {
            case VANILLA_TO_COPPER -> Blocks.HOPPER;
            case COPPER_TO_STEEL -> HopperRegistryEvents.COPPER_HOPPER.get();
            case STEEL_TO_DESH -> HopperRegistryEvents.STEEL_HOPPER.get();
        };
    }

    private TieredHopperBlock targetBlock() {
        return switch (path) {
            case VANILLA_TO_COPPER -> HopperRegistryEvents.COPPER_HOPPER.get();
            case COPPER_TO_STEEL -> HopperRegistryEvents.STEEL_HOPPER.get();
            case STEEL_TO_DESH -> HopperRegistryEvents.DESH_HOPPER.get();
        };
    }

    private static void restoreContents(Container container, List<ItemStack> contents) {
        int count = Math.min(container.getContainerSize(), contents.size());
        for (int slot = 0; slot < count; slot++) {
            container.setItem(slot, contents.get(slot));
        }
    }

    private static void dropContents(Level level, BlockPos pos, List<ItemStack> contents) {
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) continue;

            net.minecraft.world.Containers.dropItemStack(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    stack
            );
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                switch (path) {
                    case VANILLA_TO_COPPER -> "item.domesurvival.hopper_upgrade_vanilla_to_copper";
                    case COPPER_TO_STEEL -> "item.domesurvival.hopper_upgrade_copper_to_steel";
                    case STEEL_TO_DESH -> "item.domesurvival.hopper_upgrade_steel_to_desh";
                }
        );
    }
}
