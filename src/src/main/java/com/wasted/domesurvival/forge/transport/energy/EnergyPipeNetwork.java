package com.wasted.domesurvival.forge.transport.energy;

import com.wasted.domesurvival.forge.machine.passthrough.ServiceConduitKind;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughBlockEntity;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughTraversal;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class EnergyPipeNetwork {
    private static final Map<ServerLevel, TickState> STATES = new WeakHashMap<>();

    private EnergyPipeNetwork() { }

    public static void tick(ServerLevel level, BlockPos start) {
        TickState tickState = STATES.computeIfAbsent(level, ignored -> new TickState());
        long gameTime = level.getGameTime();

        if (tickState.gameTime != gameTime) {
            tickState.gameTime = gameTime;
            tickState.processed.clear();
        }

        long startKey = start.asLong();
        if (tickState.processed.contains(startKey)) {
            return;
        }

        Component component = collectComponent(level, start, tickState.processed);
        if (component.pipes.isEmpty() || component.transferLimit <= 0) {
            return;
        }

        transfer(level, component);
    }

    private static Component collectComponent(ServerLevel level, BlockPos start, LongOpenHashSet processed) {
        LongOpenHashSet pipes = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(start.asLong());

        int transferLimit = Integer.MAX_VALUE;

        while (!queue.isEmpty()) {
            long packed = queue.dequeueLong();
            if (!pipes.add(packed)) continue;

            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                pipes.remove(packed);
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof EnergyPipeBlock pipeBlock)) {
                pipes.remove(packed);
                continue;
            }

            BlockEntity currentEntity = level.getBlockEntity(pos);
            EnergyPipeBlockEntity currentPipe =
                    currentEntity instanceof EnergyPipeBlockEntity pipe ? pipe : null;

            processed.add(packed);
            transferLimit = Math.min(transferLimit, pipeBlock.tier().transferPerTick());

            for (Direction direction : Direction.values()) {
                if (currentPipe != null
                        && !currentPipe.getSideMode(direction).isConnectionEnabled()) {
                    continue;
                }

                BlockPos next = pos.relative(direction);
                if (!level.isLoaded(next)) continue;

                if (level.getBlockEntity(next) instanceof ServicePassThroughBlockEntity) {
                    ServicePassThroughTraversal.Exit exit =
                            ServicePassThroughTraversal.resolve(
                                    level, next, direction, ServiceConduitKind.ENERGY);
                    if (exit == null || !level.isLoaded(exit.pos())) continue;
                    transferLimit = Math.min(transferLimit, exit.transferLimit());
                    next = exit.pos();
                }

                if (!(level.getBlockState(next).getBlock() instanceof EnergyPipeBlock)) continue;

                BlockEntity nextEntity = level.getBlockEntity(next);
                if (nextEntity instanceof EnergyPipeBlockEntity nextPipe
                        && !nextPipe.getSideMode(direction.getOpposite()).isConnectionEnabled()) {
                    continue;
                }

                long nextKey = next.asLong();
                if (!pipes.contains(nextKey)) {
                    queue.enqueue(nextKey);
                }
            }
        }

        if (transferLimit == Integer.MAX_VALUE) transferLimit = 0;
        return new Component(pipes, transferLimit);
    }

    private static void transfer(ServerLevel level, Component component) {
        List<Endpoint> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();
        HashSet<EndpointKey> seenEndpoints = new HashSet<>();

        for (long packed : component.pipes) {
            BlockPos pipePos = BlockPos.of(packed);
            BlockEntity pipeEntity = level.getBlockEntity(pipePos);
            EnergyPipeBlockEntity pipe =
                    pipeEntity instanceof EnergyPipeBlockEntity energyPipe ? energyPipe : null;

            for (Direction direction : Direction.values()) {
                EnergyPipeSideMode mode = pipe == null
                        ? EnergyPipeSideMode.AUTO
                        : pipe.getSideMode(direction);

                if (!mode.isConnectionEnabled()) continue;

                BlockPos neighborPos = pipePos.relative(direction);
                if (!level.isLoaded(neighborPos)) continue;

                if (level.getBlockEntity(neighborPos) instanceof ServicePassThroughBlockEntity) {
                    ServicePassThroughTraversal.Exit exit =
                            ServicePassThroughTraversal.resolve(
                                    level, neighborPos, direction, ServiceConduitKind.ENERGY);
                    if (exit == null || !level.isLoaded(exit.pos())) continue;
                    neighborPos = exit.pos();
                }

                if (level.getBlockState(neighborPos).getBlock() instanceof EnergyPipeBlock) continue;

                BlockEntity blockEntity = level.getBlockEntity(neighborPos);
                if (blockEntity == null) continue;

                Direction machineSide = direction.getOpposite();
                EndpointKey key = new EndpointKey(neighborPos.asLong(), machineSide);
                if (!seenEndpoints.add(key)) continue;

                IEnergyStorage storage = blockEntity
                        .getCapability(ForgeCapabilities.ENERGY, machineSide)
                        .orElse(null);
                if (storage == null) continue;

                Endpoint endpoint = new Endpoint(key, storage);

                if (mode.allowsExtractionFromMachine() && storage.canExtract()) {
                    sources.add(endpoint);
                }
                if (mode.allowsInsertionIntoMachine() && storage.canReceive()) {
                    sinks.add(endpoint);
                }
            }
        }

        if (sources.isEmpty() || sinks.isEmpty()) return;

        int remainingNetworkBudget = component.transferLimit;
        int sinkOffset = Math.floorMod((int) level.getGameTime(), sinks.size());

        for (int i = 0; i < sinks.size() && remainingNetworkBudget > 0; i++) {
            Endpoint sink = sinks.get((i + sinkOffset) % sinks.size());

            int requested = sink.storage.receiveEnergy(remainingNetworkBudget, true);
            if (requested <= 0) continue;

            List<SourcePlan> plans = new ArrayList<>();
            int planned = 0;

            for (Endpoint source : sources) {
                if (planned >= requested) break;

                // Never pull energy out of the same machine that is currently
                // selected as the sink, even if that machine exposes both
                // receive and extract capabilities in AUTO mode.
                if (source.key.blockPos == sink.key.blockPos) continue;

                int canExtract = source.storage.extractEnergy(requested - planned, true);
                if (canExtract <= 0) continue;

                plans.add(new SourcePlan(source.storage, canExtract));
                planned += canExtract;
            }

            if (planned <= 0) continue;

            int sinkAccepts = sink.storage.receiveEnergy(planned, true);
            int target = Math.min(planned, sinkAccepts);
            if (target <= 0) continue;

            int extracted = 0;
            for (SourcePlan plan : plans) {
                if (extracted >= target) break;

                int wanted = Math.min(plan.amount, target - extracted);
                extracted += plan.storage.extractEnergy(wanted, false);
            }

            if (extracted <= 0) continue;

            int accepted = sink.storage.receiveEnergy(extracted, false);
            remainingNetworkBudget -= accepted;

            if (accepted < extracted) {
                // A foreign IEnergyStorage changed between simulation and
                // execution. Stop this network until the next server tick.
                break;
            }
        }
    }

    private static final class TickState {
        private long gameTime = Long.MIN_VALUE;
        private final LongOpenHashSet processed = new LongOpenHashSet();
    }

    private record Component(LongOpenHashSet pipes, int transferLimit) { }

    private record Endpoint(EndpointKey key, IEnergyStorage storage) { }

    private record EndpointKey(long blockPos, Direction side) { }

    private record SourcePlan(IEnergyStorage storage, int amount) { }
}
