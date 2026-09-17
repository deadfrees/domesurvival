package com.wasted.domesurvival.forge.gas;

import com.wasted.domesurvival.forge.capability.IGasStorage;
import com.wasted.domesurvival.forge.capability.IOxygenStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeBlock;
import com.wasted.domesurvival.forge.machine.passthrough.ServiceConduitKind;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughBlockEntity;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughTraversal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Pull transport for typed gases over the existing oxygen-pipe graph.
 * Pipes remain allocation-free blocks with no internal tank; gas type is
 * validated only by source/sink capabilities, so gases can never mix in a pipe.
 */
public final class GasPipeTransferService {
    private static final int MAX_VISITED_PIPES = 2048;

    private GasPipeTransferService() {
    }

    public static int pull(Level level, BlockPos sinkPos, IGasStorage sink, ResourceLocation gas,
                           int maxPull, Predicate<Direction> sinkSideAllowed) {
        if (level.isClientSide || gas == null || maxPull <= 0 || !sink.canReceiveGas(gas)) return 0;

        int budget = sink.receiveGas(gas, maxPull, true);
        if (budget <= 0) return 0;

        Map<BlockPos, Integer> bestPipeLimit = new HashMap<>();
        Map<Endpoint, Integer> endpoints = new HashMap<>();
        ArrayDeque<PathNode> queue = new ArrayDeque<>();

        for (Direction direction : Direction.values()) {
            if (!sinkSideAllowed.test(direction)) continue;
            BlockPos neighborPos = sinkPos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockState neighborState = level.getBlockState(neighborPos);
            int throughLimit = budget;

            if (level.getBlockEntity(neighborPos) instanceof ServicePassThroughBlockEntity) {
                ServicePassThroughTraversal.Exit exit = ServicePassThroughTraversal.resolve(
                        level, neighborPos, direction, ServiceConduitKind.OXYGEN);
                if (exit == null || !level.hasChunkAt(exit.pos())) continue;
                neighborPos = exit.pos();
                neighborState = level.getBlockState(neighborPos);
                throughLimit = Math.min(throughLimit, exit.transferLimit());
            }

            if (neighborState.getBlock() instanceof OxygenPipeBlock pipe) {
                if (!neighborState.getValue(OxygenPipeBlock.property(direction.getOpposite()))) continue;
                int pathLimit = Math.min(throughLimit, pipe.getTransferRate());
                bestPipeLimit.put(neighborPos, pathLimit);
                queue.addLast(new PathNode(neighborPos, pathLimit));
            } else if (level.getBlockEntity(neighborPos) != null) {
                endpoints.merge(new Endpoint(neighborPos, direction.getOpposite()), throughLimit, Math::max);
            }
        }

        while (!queue.isEmpty() && bestPipeLimit.size() <= MAX_VISITED_PIPES) {
            PathNode node = queue.removeFirst();
            BlockState nodeState = level.getBlockState(node.pos);
            for (Direction direction : Direction.values()) {
                if (nodeState.getBlock() instanceof OxygenPipeBlock
                        && !nodeState.getValue(OxygenPipeBlock.property(direction))) continue;
                BlockPos neighborPos = node.pos.relative(direction);
                if (neighborPos.equals(sinkPos) || !level.hasChunkAt(neighborPos)) continue;

                BlockState neighborState = level.getBlockState(neighborPos);
                int throughLimit = node.pathLimit;
                if (level.getBlockEntity(neighborPos) instanceof ServicePassThroughBlockEntity) {
                    ServicePassThroughTraversal.Exit exit = ServicePassThroughTraversal.resolve(
                            level, neighborPos, direction, ServiceConduitKind.OXYGEN);
                    if (exit == null || !level.hasChunkAt(exit.pos())) continue;
                    neighborPos = exit.pos();
                    neighborState = level.getBlockState(neighborPos);
                    throughLimit = Math.min(throughLimit, exit.transferLimit());
                }

                if (neighborState.getBlock() instanceof OxygenPipeBlock pipe) {
                    if (!neighborState.getValue(OxygenPipeBlock.property(direction.getOpposite()))) continue;
                    int nextLimit = Math.min(throughLimit, pipe.getTransferRate());
                    Integer previous = bestPipeLimit.get(neighborPos);
                    if ((previous == null || nextLimit > previous)
                            && (previous != null || bestPipeLimit.size() < MAX_VISITED_PIPES)) {
                        bestPipeLimit.put(neighborPos, nextLimit);
                        queue.addLast(new PathNode(neighborPos, nextLimit));
                    }
                } else if (level.getBlockEntity(neighborPos) != null) {
                    endpoints.merge(new Endpoint(neighborPos, direction.getOpposite()), throughLimit, Math::max);
                }
            }
        }

        int remaining = budget;
        int movedTotal = 0;
        for (Map.Entry<Endpoint, Integer> entry : endpoints.entrySet()) {
            if (remaining <= 0) break;
            Endpoint endpoint = entry.getKey();
            BlockEntity sourceEntity = level.getBlockEntity(endpoint.pos);
            if (sourceEntity == null) continue;

            int requested = Math.min(remaining, entry.getValue());
            int movable = simulateSource(sourceEntity, endpoint.side, gas, requested);
            if (movable <= 0) continue;
            movable = Math.min(movable, sink.receiveGas(gas, movable, true));
            if (movable <= 0) continue;

            int extracted = extractSource(sourceEntity, endpoint.side, gas, movable);
            if (extracted <= 0) continue;
            int inserted = sink.receiveGas(gas, extracted, false);
            if (inserted != extracted) {
                throw new IllegalStateException("Gas sink changed between simulation and execution");
            }
            movedTotal += inserted;
            remaining -= inserted;
        }
        return movedTotal;
    }

    private static int simulateSource(BlockEntity source, Direction side, ResourceLocation gas, int requested) {
        IGasStorage typed = source.getCapability(ModCapabilities.GAS, side).resolve().orElse(null);
        if (typed != null && typed.canExtractGas(gas)) return typed.extractGas(gas, requested, true);

        if (ModGases.OXYGEN.equals(gas)) {
            IOxygenStorage oxygen = source.getCapability(ModCapabilities.OXYGEN, side).resolve().orElse(null);
            if (oxygen != null && oxygen.canExtract()) return oxygen.extractOxygen(requested, true);
        }
        return 0;
    }

    private static int extractSource(BlockEntity source, Direction side, ResourceLocation gas, int requested) {
        IGasStorage typed = source.getCapability(ModCapabilities.GAS, side).resolve().orElse(null);
        if (typed != null && typed.canExtractGas(gas)) return typed.extractGas(gas, requested, false);

        if (ModGases.OXYGEN.equals(gas)) {
            IOxygenStorage oxygen = source.getCapability(ModCapabilities.OXYGEN, side).resolve().orElse(null);
            if (oxygen != null && oxygen.canExtract()) return oxygen.extractOxygen(requested, false);
        }
        return 0;
    }

    private record PathNode(BlockPos pos, int pathLimit) { }
    private record Endpoint(BlockPos pos, Direction side) { }
}
