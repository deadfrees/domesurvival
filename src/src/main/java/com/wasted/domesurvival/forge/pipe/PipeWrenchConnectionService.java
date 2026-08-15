package com.wasted.domesurvival.forge.pipe;

import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlockEntity;
import com.wasted.domesurvival.forge.itempipe.ItemPipeNetworkManager;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeBlock;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeConnectionData;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlock;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlockEntity;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeSideMode;
import com.wasted.domesurvival.forge.transport.fluid.FluidPipeBlock;
import com.wasted.domesurvival.forge.transport.fluid.FluidPipeBlockEntity;
import com.wasted.domesurvival.forge.transport.fluid.FluidPipeSideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PipeWrenchConnectionService {
    private PipeWrenchConnectionService() { }

    public static boolean hasSameFamilyPipe(Level level, BlockPos pos, Direction side) {
        Block a = level.getBlockState(pos).getBlock();
        Block b = level.getBlockState(pos.relative(side)).getBlock();
        return sameFamily(a, b);
    }


    @Nullable
    public static Direction findSameFamilySide(Level level, BlockPos pos, BlockHitResult hit) {
        Direction direct = hit.getDirection();
        if (hasSameFamilyPipe(level, pos, direct)) return direct;

        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.values()) {
            if (!hasSameFamilyPipe(level, pos, direction)) continue;
            double distance = switch (direction) {
                case NORTH -> local.z;
                case SOUTH -> 1.0D - local.z;
                case WEST -> local.x;
                case EAST -> 1.0D - local.x;
                case DOWN -> local.y;
                case UP -> 1.0D - local.y;
            };
            if (distance < bestDistance) {
                bestDistance = distance;
                best = direction;
            }
        }
        return best;
    }

    @Nullable
    public static Direction findOnlySameFamilySide(Level level, BlockPos pos, @Nullable Direction preferred) {
        if (preferred != null && hasSameFamilyPipe(level, pos, preferred)) return preferred;
        Direction only = null;
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (!hasSameFamilyPipe(level, pos, direction)) continue;
            only = direction;
            count++;
            if (count > 1) return null;
        }
        return only;
    }

    private static boolean sameFamily(Block a, Block b) {
        if (a instanceof EnergyPipeBlock) return b instanceof EnergyPipeBlock;
        if (a instanceof FluidPipeBlock) return b instanceof FluidPipeBlock;
        if (a instanceof OxygenPipeBlock) return b instanceof OxygenPipeBlock;
        if (a instanceof ItemPipeBlock) return b instanceof ItemPipeBlock;
        return false;
    }

    public static boolean toggle(ServerLevel level, BlockPos pos, Direction side, Player player) {
        if (!hasSameFamilyPipe(level, pos, side)) return false;

        boolean disconnected;
        Block block = level.getBlockState(pos).getBlock();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (block instanceof EnergyPipeBlock && blockEntity instanceof EnergyPipeBlockEntity pipe) {
            disconnected = pipe.getSideMode(side) != EnergyPipeSideMode.DISABLED;
            pipe.setSideMode(side, disconnected ? EnergyPipeSideMode.DISABLED : EnergyPipeSideMode.AUTO);
        } else if (block instanceof FluidPipeBlock && blockEntity instanceof FluidPipeBlockEntity pipe) {
            disconnected = pipe.getSideMode(side) != FluidPipeSideMode.DISABLED;
            pipe.setSideMode(side, disconnected ? FluidPipeSideMode.DISABLED : FluidPipeSideMode.AUTO);
        } else if (block instanceof ItemPipeBlock && blockEntity instanceof ItemPipeBlockEntity pipe) {
            disconnected = pipe.toggleManualDisconnect(side);
            refreshItemPair(level, pos, side);
            ItemPipeNetworkManager.markDirty(level);
        } else if (block instanceof OxygenPipeBlock) {
            disconnected = OxygenPipeConnectionData.get(level).toggle(pos, side);
            refreshOxygenPair(level, pos, side);
        } else {
            return false;
        }

        player.displayClientMessage(
                Component.translatable(disconnected
                        ? "message.domesurvival.pipe.connection_cut"
                        : "message.domesurvival.pipe.connection_restored"),
                true
        );
        return true;
    }

    private static void refreshItemPair(ServerLevel level, BlockPos pos, Direction side) {
        refreshItem(level, pos);
        refreshItem(level, pos.relative(side));
    }

    private static void refreshItem(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ItemPipeBlock)) return;
        BlockState refreshed = ItemPipeBlock.refreshConnections(level, pos, state);
        if (!refreshed.equals(state)) {
            level.setBlock(pos, refreshed, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
        } else {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void refreshOxygenPair(ServerLevel level, BlockPos pos, Direction side) {
        refreshOxygen(level, pos);
        refreshOxygen(level, pos.relative(side));
    }

    private static void refreshOxygen(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof OxygenPipeBlock)) return;
        BlockState refreshed = OxygenPipeBlock.refreshConnections(level, pos, state);
        if (!refreshed.equals(state)) {
            level.setBlock(pos, refreshed, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
        } else {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
