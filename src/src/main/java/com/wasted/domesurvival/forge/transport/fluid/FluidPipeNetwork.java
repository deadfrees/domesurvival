package com.wasted.domesurvival.forge.transport.fluid;

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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class FluidPipeNetwork {
    private static final Map<ServerLevel, TickState> STATES = new WeakHashMap<>();

    private FluidPipeNetwork() {
    }

    public static void tick(ServerLevel level, BlockPos start) {
        TickState tickState = STATES.computeIfAbsent(level, ignored -> new TickState());
        long gameTime = level.getGameTime();

        if (tickState.gameTime != gameTime) {
            tickState.gameTime = gameTime;
            tickState.processed.clear();
        }

        long startKey = start.asLong();
        if (tickState.processed.contains(startKey)) return;

        Component component = collectComponent(level, start, tickState.processed);
        if (component.pipes.isEmpty() || component.transferLimit <= 0) return;

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
            if (!(state.getBlock() instanceof FluidPipeBlock pipeBlock)) {
                pipes.remove(packed);
                continue;
            }

            BlockEntity currentEntity = level.getBlockEntity(pos);
            FluidPipeBlockEntity currentPipe = currentEntity instanceof FluidPipeBlockEntity pipe ? pipe : null;

            processed.add(packed);
            transferLimit = Math.min(transferLimit, pipeBlock.tier().transferPerTick());

            for (Direction direction : Direction.values()) {
                if (currentPipe != null && !currentPipe.getSideMode(direction).isConnectionEnabled()) continue;

                BlockPos next = pos.relative(direction);
                if (!level.isLoaded(next)) continue;

                if (level.getBlockEntity(next) instanceof ServicePassThroughBlockEntity) {
                    ServicePassThroughTraversal.Exit exit =
                            ServicePassThroughTraversal.resolve(
                                    level, next, direction, ServiceConduitKind.FLUID);
                    if (exit == null || !level.isLoaded(exit.pos())) continue;
                    transferLimit = Math.min(transferLimit, exit.transferLimit());
                    next = exit.pos();
                }

                if (!(level.getBlockState(next).getBlock() instanceof FluidPipeBlock)) continue;

                BlockEntity nextEntity = level.getBlockEntity(next);
                if (nextEntity instanceof FluidPipeBlockEntity nextPipe
                        && !nextPipe.getSideMode(direction.getOpposite()).isConnectionEnabled()) {
                    continue;
                }

                long nextKey = next.asLong();
                if (!pipes.contains(nextKey)) queue.enqueue(nextKey);
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
            FluidPipeBlockEntity pipe = pipeEntity instanceof FluidPipeBlockEntity fluidPipe ? fluidPipe : null;

            for (Direction direction : Direction.values()) {
                FluidPipeSideMode mode = pipe == null ? FluidPipeSideMode.AUTO : pipe.getSideMode(direction);
                if (!mode.isConnectionEnabled()) continue;

                BlockPos neighborPos = pipePos.relative(direction);
                if (!level.isLoaded(neighborPos)) continue;

                if (level.getBlockEntity(neighborPos) instanceof ServicePassThroughBlockEntity) {
                    ServicePassThroughTraversal.Exit exit =
                            ServicePassThroughTraversal.resolve(
                                    level, neighborPos, direction, ServiceConduitKind.FLUID);
                    if (exit == null || !level.isLoaded(exit.pos())) continue;
                    neighborPos = exit.pos();
                }

                if (level.getBlockState(neighborPos).getBlock() instanceof FluidPipeBlock) continue;

                BlockEntity blockEntity = level.getBlockEntity(neighborPos);
                if (blockEntity == null) continue;

                Direction machineSide = direction.getOpposite();
                EndpointKey key = new EndpointKey(neighborPos.asLong(), machineSide);
                if (!seenEndpoints.add(key)) continue;

                IFluidHandler handler = blockEntity
                        .getCapability(ForgeCapabilities.FLUID_HANDLER, machineSide)
                        .orElse(null);
                if (handler == null) continue;

                Endpoint endpoint = new Endpoint(key, handler);
                if (mode.allowsDrainFromMachine()) sources.add(endpoint);
                if (mode.allowsFillIntoMachine()) sinks.add(endpoint);
            }
        }

        if (sources.isEmpty() || sinks.isEmpty()) return;

        int remainingBudget = component.transferLimit;
        int sourceOffset = Math.floorMod((int) level.getGameTime(), sources.size());
        int sinkOffset = Math.floorMod((int) (level.getGameTime() * 31L), sinks.size());

        for (int sourceIndex = 0; sourceIndex < sources.size() && remainingBudget > 0; sourceIndex++) {
            Endpoint source = sources.get((sourceIndex + sourceOffset) % sources.size());

            FluidStack offered = source.handler.drain(remainingBudget, IFluidHandler.FluidAction.SIMULATE);
            if (offered.isEmpty() || offered.getAmount() <= 0) continue;

            List<SinkPlan> plans = new ArrayList<>();
            int plannedTotal = 0;

            for (int sinkIndex = 0; sinkIndex < sinks.size() && plannedTotal < offered.getAmount(); sinkIndex++) {
                Endpoint sink = sinks.get((sinkIndex + sinkOffset) % sinks.size());
                if (sink.key.blockPos == source.key.blockPos) continue;

                FluidStack request = offered.copy();
                request.setAmount(offered.getAmount() - plannedTotal);

                int accepted = sink.handler.fill(request, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) continue;

                accepted = Math.min(accepted, request.getAmount());
                plans.add(new SinkPlan(sink.handler, accepted));
                plannedTotal += accepted;
            }

            if (plannedTotal <= 0) continue;

            FluidStack toDrain = offered.copy();
            toDrain.setAmount(plannedTotal);
            FluidStack drained = source.handler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty() || drained.getAmount() <= 0) continue;

            int remainingDrained = drained.getAmount();
            int filledTotal = 0;

            for (SinkPlan plan : plans) {
                if (remainingDrained <= 0) break;

                int amount = Math.min(plan.amount, remainingDrained);
                FluidStack portion = drained.copy();
                portion.setAmount(amount);

                int filled = plan.handler.fill(portion, IFluidHandler.FluidAction.EXECUTE);
                if (filled <= 0) continue;

                filled = Math.min(filled, amount);
                remainingDrained -= filled;
                filledTotal += filled;
            }

            remainingBudget -= filledTotal;

            // Forge fluid handlers are expected to honor their immediate simulation.
            // If a foreign handler changes between SIMULATE and EXECUTE, stop this
            // component for the tick rather than repeatedly draining from sources.
            if (remainingDrained > 0) break;
        }
    }

    private static final class TickState {
        private long gameTime = Long.MIN_VALUE;
        private final LongOpenHashSet processed = new LongOpenHashSet();
    }

    private record Component(LongOpenHashSet pipes, int transferLimit) {
    }

    private record Endpoint(EndpointKey key, IFluidHandler handler) {
    }

    private record EndpointKey(long blockPos, Direction side) {
    }

    private record SinkPlan(IFluidHandler handler, int amount) {
    }
}
