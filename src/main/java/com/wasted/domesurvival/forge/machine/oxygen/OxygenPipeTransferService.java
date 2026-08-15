package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.machine.passthrough.ServiceConduitKind;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughBlockEntity;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughTraversal;

import com.wasted.domesurvival.forge.capability.IOxygenStorage;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Pull-based oxygen transport used by oxygen consumers.
 *
 * <p>No pipe BlockEntity is required. A consumer scans only when it actually has free
 * storage, follows at most 2048 connected pipe blocks, records the best bottleneck for
 * each endpoint, and then extracts oxygen directly from source capabilities.</p>
 */
public final class OxygenPipeTransferService {
    private static final int MAX_VISITED_PIPES = 2048;

    private OxygenPipeTransferService() {
    }

    public static int pull(Level level, BlockPos sinkPos, OxygenStorage sink, int maxPull,
                           Predicate<Direction> sinkSideAllowed) {
        return pull(level, sinkPos, (IOxygenStorage) sink, maxPull, sinkSideAllowed);
    }

    /**
     * Generic pull entry point used by shared storages such as the V63 universal reservoir.
     * Existing OxygenStorage callers keep the overload above, preserving the old API.
     */
    public static int pull(Level level, BlockPos sinkPos, IOxygenStorage sink, int maxPull,
                           Predicate<Direction> sinkSideAllowed) {
        if (level.isClientSide || maxPull <= 0 || !sink.canReceive()
                || sink.getOxygenStored() >= sink.getMaxOxygenStored()) {
            return 0;
        }

        int budget = Math.min(maxPull, sink.getMaxOxygenStored() - sink.getOxygenStored());
        if (budget <= 0) {
            return 0;
        }

        Map<BlockPos, Integer> bestPipeLimit = new HashMap<>();
        Map<Endpoint, Integer> endpoints = new HashMap<>();
        ArrayDeque<PathNode> queue = new ArrayDeque<>();

        for (Direction direction : Direction.values()) {
            if (!sinkSideAllowed.test(direction)) {
                continue;
            }
            BlockPos neighborPos = sinkPos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) {
                continue;
            }
            BlockState neighborState = level.getBlockState(neighborPos);
            int throughLimit = budget;

            if (level.getBlockEntity(neighborPos) instanceof ServicePassThroughBlockEntity) {
                ServicePassThroughTraversal.Exit exit =
                        ServicePassThroughTraversal.resolve(
                                level,
                                neighborPos,
                                direction,
                                ServiceConduitKind.OXYGEN
                        );
                if (exit == null || !level.hasChunkAt(exit.pos())) {
                    continue;
                }

                neighborPos = exit.pos();
                neighborState = level.getBlockState(neighborPos);
                throughLimit = Math.min(throughLimit, exit.transferLimit());
            }

            if (neighborState.getBlock() instanceof OxygenPipeBlock pipe) {
                if (!neighborState.getValue(OxygenPipeBlock.property(direction.getOpposite()))) {
                    continue;
                }
                int pathLimit = Math.min(throughLimit, pipe.getTransferRate());
                bestPipeLimit.put(neighborPos, pathLimit);
                queue.addLast(new PathNode(neighborPos, pathLimit));
            } else if (level.getBlockEntity(neighborPos) != null) {
                endpoints.merge(
                        new Endpoint(neighborPos, direction.getOpposite()),
                        throughLimit,
                        Math::max
                );
            }
        }

        while (!queue.isEmpty() && bestPipeLimit.size() <= MAX_VISITED_PIPES) {
            PathNode node = queue.removeFirst();
            BlockState nodeState = level.getBlockState(node.pos);
            for (Direction direction : Direction.values()) {
                if (nodeState.getBlock() instanceof OxygenPipeBlock
                        && !nodeState.getValue(OxygenPipeBlock.property(direction))) {
                    continue;
                }
                BlockPos neighborPos = node.pos.relative(direction);
                if (neighborPos.equals(sinkPos) || !level.hasChunkAt(neighborPos)) {
                    continue;
                }

                BlockState neighborState = level.getBlockState(neighborPos);
                int throughLimit = node.pathLimit;

                if (level.getBlockEntity(neighborPos) instanceof ServicePassThroughBlockEntity) {
                    ServicePassThroughTraversal.Exit exit =
                            ServicePassThroughTraversal.resolve(
                                    level,
                                    neighborPos,
                                    direction,
                                    ServiceConduitKind.OXYGEN
                            );
                    if (exit == null || !level.hasChunkAt(exit.pos())) {
                        continue;
                    }

                    neighborPos = exit.pos();
                    neighborState = level.getBlockState(neighborPos);
                    throughLimit = Math.min(throughLimit, exit.transferLimit());
                }

                if (neighborState.getBlock() instanceof OxygenPipeBlock pipe) {
                    if (!neighborState.getValue(OxygenPipeBlock.property(direction.getOpposite()))) {
                        continue;
                    }
                    int nextLimit = Math.min(throughLimit, pipe.getTransferRate());
                    Integer previous = bestPipeLimit.get(neighborPos);
                    if ((previous == null || nextLimit > previous)
                            && (previous != null || bestPipeLimit.size() < MAX_VISITED_PIPES)) {
                        bestPipeLimit.put(neighborPos, nextLimit);
                        queue.addLast(new PathNode(neighborPos, nextLimit));
                    }
                } else if (level.getBlockEntity(neighborPos) != null) {
                    endpoints.merge(
                            new Endpoint(neighborPos, direction.getOpposite()),
                            throughLimit,
                            Math::max
                    );
                }
            }
        }

        int remaining = budget;
        int movedTotal = 0;
        for (Map.Entry<Endpoint, Integer> entry : endpoints.entrySet()) {
            if (remaining <= 0) {
                break;
            }

            Endpoint endpoint = entry.getKey();
            BlockEntity sourceEntity = level.getBlockEntity(endpoint.pos);
            if (sourceEntity == null) {
                continue;
            }

            IOxygenStorage source = sourceEntity.getCapability(ModCapabilities.OXYGEN, endpoint.side)
                    .resolve().orElse(null);
            if (source == null || !source.canExtract()) {
                continue;
            }

            int requested = Math.min(remaining, entry.getValue());
            int simulatedExtract = source.extractOxygen(requested, true);
            if (simulatedExtract <= 0) {
                continue;
            }

            int simulatedReceive = sink.receiveOxygen(simulatedExtract, true);
            if (simulatedReceive <= 0) {
                continue;
            }

            int extracted = source.extractOxygen(simulatedReceive, false);
            if (extracted <= 0) {
                continue;
            }

            int inserted = sink.receiveOxygen(extracted, false);
            movedTotal += inserted;
            remaining -= inserted;
        }

        return movedTotal;
    }

    private record PathNode(BlockPos pos, int pathLimit) {
    }

    private record Endpoint(BlockPos pos, Direction side) {
    }
}
