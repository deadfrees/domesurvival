package com.wasted.domesurvival.forge.airlock;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Lightweight controller: finds the nearest vanilla-style openable airlock and toggles its connected group. */
public final class AirlockDoorController {
    private static final int SEARCH_RADIUS = 10;
    private static final int MAX_GROUP_BLOCKS = 64;

    private AirlockDoorController() { }

    public static boolean setNearestAirlockOpen(Level level, BlockPos panelPos, boolean open) {
        BlockPos seed = findNearestOpenable(level, panelPos);
        if (seed == null) return false;

        BlockState seedState = level.getBlockState(seed);
        Object seedBlock = seedState.getBlock();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(seed);

        boolean changed = false;
        while (!queue.isEmpty() && visited.size() < MAX_GROUP_BLOCKS) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current)) continue;

            BlockState state = level.getBlockState(current);
            if (state.getBlock() != seedBlock || !state.hasProperty(BlockStateProperties.OPEN)) continue;

            if (state.getValue(BlockStateProperties.OPEN) != open) {
                level.setBlock(current, state.setValue(BlockStateProperties.OPEN, open), 3);
                changed = true;
            }

            queue.add(current.above());
            queue.add(current.below());
            queue.add(current.north());
            queue.add(current.south());
            queue.add(current.east());
            queue.add(current.west());
        }

        if (changed) {
            level.playSound(null, seed, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return changed || seedState.getValue(BlockStateProperties.OPEN) == open;
    }

    private static BlockPos findNearestOpenable(Level level, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (distance >= bestDistance) continue;
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (isOpenable(state)) {
                        bestDistance = distance;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static boolean isOpenable(BlockState state) {
        if (!state.hasProperty(BlockStateProperties.OPEN)) return false;
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock;
    }
}
