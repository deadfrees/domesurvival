package com.wasted.domesurvival.forge.itempipe;

import com.wasted.domesurvival.forge.machine.passthrough.ServiceConduitKind;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughBlockEntity;
import com.wasted.domesurvival.forge.machine.passthrough.ServicePassThroughTraversal;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import com.wasted.domesurvival.forge.DomeSurvival;

import java.util.*;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemPipeNetworkManager {
    private static final int MAX_NETWORK_PIPES = 4096;
    private static final int MAX_ACTIVE_VISUALS = 64;
    private static final int VISUAL_TICKS_PER_SEGMENT = 8;
    private static final Map<ServerLevel, LevelState> STATES = new WeakHashMap<>();

    private ItemPipeNetworkManager() { }

    public static void registerPipe(ItemPipeBlockEntity pipe) {
        if (!(pipe.getLevel() instanceof ServerLevel level)) return;
        LevelState state = STATES.computeIfAbsent(level, ignored -> new LevelState());
        state.knownPipes.add(pipe.getBlockPos().asLong());
        state.dirty = true;
    }

    public static void unregisterPipe(ItemPipeBlockEntity pipe) {
        if (!(pipe.getLevel() instanceof ServerLevel level)) return;
        LevelState state = STATES.computeIfAbsent(level, ignored -> new LevelState());
        state.knownPipes.remove(pipe.getBlockPos().asLong());
        state.dirty = true;
    }

    public static void markDirty(@org.jetbrains.annotations.Nullable Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        STATES.computeIfAbsent(serverLevel, ignored -> new LevelState()).dirty = true;
    }

    public static void markDirty(@org.jetbrains.annotations.Nullable LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel) {
            STATES.computeIfAbsent(serverLevel, ignored -> new LevelState()).dirty = true;
        }
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;

        LevelState state = STATES.computeIfAbsent(level, ignored -> new LevelState());
        if (state.dirty) rebuild(level, state);

        long gameTime = level.getGameTime();
        for (Network network : state.networks) {
            network.tick(level, gameTime);
        }
        tickTravelVisuals(level, state);
    }

    private static void rebuild(ServerLevel level, LevelState state) {
        state.dirty = false;
        state.networks.clear();

        LongOpenHashSet stale = new LongOpenHashSet();
        for (long packed : state.knownPipes) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof ItemPipeBlock)) {
                stale.add(packed);
            }
        }
        state.knownPipes.removeAll(stale);

        LongOpenHashSet unvisited = new LongOpenHashSet(state.knownPipes);
        while (!unvisited.isEmpty()) {
            long seed = unvisited.iterator().nextLong();
            Component component = collectComponent(level, BlockPos.of(seed), unvisited);
            if (!component.pipes.isEmpty()) state.networks.add(buildNetwork(level, component));
        }
    }

    private static Component collectComponent(ServerLevel level, BlockPos start, LongOpenHashSet unvisited) {
        LongOpenHashSet pipes = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(start.asLong());

        while (!queue.isEmpty() && pipes.size() < MAX_NETWORK_PIPES) {
            long packed = queue.dequeueLong();
            if (!pipes.add(packed)) continue;
            unvisited.remove(packed);

            BlockPos pos = BlockPos.of(packed);
            if (!(level.getBlockState(pos).getBlock() instanceof ItemPipeBlock)) {
                pipes.remove(packed);
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = resolvePipeNeighbor(level, pos, direction);
                if (next == null) continue;
                long nextKey = next.asLong();
                if (!pipes.contains(nextKey)) queue.enqueue(nextKey);
            }
        }
        return new Component(pipes);
    }

    private static Network buildNetwork(ServerLevel level, Component component) {
        List<Endpoint> inputs = new ArrayList<>();
        List<Endpoint> outputs = new ArrayList<>();

        int maxItems = Integer.MAX_VALUE;
        int cooldown = 1;
        boolean hasNormalTier = false;

        for (long packed : component.pipes) {
            BlockPos pipePos = BlockPos.of(packed);
            if (!(level.getBlockState(pipePos).getBlock() instanceof ItemPipeBlock pipeBlock)) continue;
            if (!(level.getBlockEntity(pipePos) instanceof ItemPipeBlockEntity pipeEntity)) continue;

            if (!pipeBlock.isFiltering()) {
                hasNormalTier = true;
                maxItems = Math.min(maxItems, pipeBlock.tier().itemsPerCycle());
                cooldown = Math.max(cooldown, pipeBlock.tier().cooldownTicks());
            }

            for (Direction direction : Direction.values()) {
                if (resolvePipeNeighbor(level, pipePos, direction) != null) continue;
                if (!ItemPipeBlock.hasObjectConnector(level, pipePos, direction)) continue;

                ItemConnectorMode mode = pipeEntity.getConnectorMode(direction);
                // Final GOTEICRAFT convention:
                // BLUE INPUT  = network -> attached object (sink).
                // ORANGE OUTPUT = attached object -> network (source).
                if (mode == ItemConnectorMode.OUTPUT) inputs.add(new Endpoint(pipePos, direction));
                if (mode == ItemConnectorMode.INPUT) outputs.add(new Endpoint(pipePos, direction));
            }
        }

        if (!hasNormalTier) {
            maxItems = ItemPipeTier.COPPER.itemsPerCycle();
            cooldown = ItemPipeTier.COPPER.cooldownTicks();
        }
        if (maxItems == Integer.MAX_VALUE) maxItems = ItemPipeTier.COPPER.itemsPerCycle();

        return new Network(component.pipes, inputs, outputs, maxItems, cooldown);
    }

    @org.jetbrains.annotations.Nullable
    private static BlockPos resolvePipeNeighbor(ServerLevel level, BlockPos pos, Direction direction) {
        BlockPos next = pos.relative(direction);
        if (!level.hasChunkAt(next)) return null;

        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() instanceof ItemPipeBlock
                && !currentState.getValue(ItemPipeBlock.propertyFor(direction))) {
            return null;
        }

        BlockState nextState = level.getBlockState(next);
        if (nextState.getBlock() instanceof ItemPipeBlock) {
            if (!nextState.getValue(ItemPipeBlock.propertyFor(direction.getOpposite()))) return null;
            return next;
        }

        if (level.getBlockEntity(next) instanceof ServicePassThroughBlockEntity pass
                && pass.getConduitKind() == ServiceConduitKind.ITEM
                && pass.isAxisCompatible(direction)) {
            ServicePassThroughTraversal.Exit exit = ServicePassThroughTraversal.resolve(
                    level, next, direction, ServiceConduitKind.ITEM
            );
            if (exit != null && level.hasChunkAt(exit.pos())
                    && level.getBlockState(exit.pos()).getBlock() instanceof ItemPipeBlock) {
                return exit.pos();
            }
        }
        return null;
    }

    @org.jetbrains.annotations.Nullable
    private static IItemHandler handler(ServerLevel level, Endpoint endpoint) {
        BlockPos targetPos = endpoint.pipePos.relative(endpoint.direction);
        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        if (blockEntity == null) return null;
        IItemHandler sided = blockEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER, endpoint.direction.getOpposite())
                .resolve().orElse(null);
        if (sided != null) return sided;
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .resolve().orElse(null);
    }

    private static final class Network {
        private final LongOpenHashSet pipes;
        private final List<Endpoint> inputs;
        private final List<Endpoint> outputs;
        private final int maxItemsPerCycle;
        private final int cooldownTicks;
        private final Map<RouteKey, List<RouteCandidate>> routeCache = new HashMap<>();
        private long nextTick;
        private int sourceCursor;

        private Network(LongOpenHashSet pipes, List<Endpoint> inputs, List<Endpoint> outputs,
                        int maxItemsPerCycle, int cooldownTicks) {
            this.pipes = pipes;
            this.inputs = inputs;
            this.outputs = outputs;
            this.maxItemsPerCycle = maxItemsPerCycle;
            this.cooldownTicks = cooldownTicks;
        }

        private void tick(ServerLevel level, long gameTime) {
            if (gameTime < nextTick || inputs.isEmpty() || outputs.isEmpty()) return;
            nextTick = gameTime + cooldownTicks;

            int remaining = maxItemsPerCycle;
            if (sourceCursor >= inputs.size()) sourceCursor = 0;

            for (int step = 0; step < inputs.size() && remaining > 0; step++) {
                int index = (sourceCursor + step) % inputs.size();
                Endpoint source = inputs.get(index);
                IItemHandler sourceHandler = handler(level, source);
                if (sourceHandler == null) continue;

                for (int slot = 0; slot < sourceHandler.getSlots() && remaining > 0; slot++) {
                    ItemStack simulated = sourceHandler.extractItem(slot, remaining, true);
                    if (simulated.isEmpty()) continue;

                    List<RouteCandidate> routes = routeCache.computeIfAbsent(
                            new RouteKey(source.pipePos.asLong(), simulated.getItem()),
                            ignored -> findRoutes(level, this, source, simulated)
                    );
                    if (routes.isEmpty()) continue;

                    TransferPlan plan = findAcceptingDestination(level, source, simulated, routes);
                    if (plan == null || plan.accepted <= 0) continue;

                    ItemStack extracted = sourceHandler.extractItem(slot, plan.accepted, false);
                    if (extracted.isEmpty()) continue;

                    IItemHandler sinkHandler = handler(level, plan.route.endpoint);
                    if (sinkHandler == null) {
                        returnRemainder(level, source, sourceHandler, extracted);
                        continue;
                    }

                    ItemStack remainder = insertAcross(sinkHandler, extracted, false);
                    int moved = extracted.getCount() - remainder.getCount();
                    if (!remainder.isEmpty()) {
                        returnRemainder(level, source, sourceHandler, remainder);
                    }

                    if (moved > 0) {
                        remaining -= moved;
                        queueTravelVisual(level, source, plan.route);
                    }
                }
            }
            sourceCursor = inputs.isEmpty() ? 0 : (sourceCursor + 1) % inputs.size();
        }
    }

    private static TransferPlan findAcceptingDestination(ServerLevel level, Endpoint source,
                                                           ItemStack stack, List<RouteCandidate> routes) {
        for (RouteCandidate route : routes) {
            if (route.endpoint.pipePos.equals(source.pipePos)
                    && route.endpoint.direction == source.direction) continue;

            IItemHandler target = handler(level, route.endpoint);
            if (target == null) continue;
            ItemStack remainder = insertAcross(target, stack, true);
            int accepted = stack.getCount() - remainder.getCount();
            if (accepted > 0) return new TransferPlan(route, accepted);
        }
        return null;
    }

    private static List<RouteCandidate> findRoutes(ServerLevel level, Network network,
                                                    Endpoint source, ItemStack stack) {
        List<RouteCandidate> result = new ArrayList<>();
        ArrayDeque<PathNode> queue = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet();

        queue.add(new PathNode(source.pipePos, List.of(source.pipePos)));
        visited.add(source.pipePos.asLong());

        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_PIPES) {
            PathNode node = queue.removeFirst();
            ItemPipeBlockEntity pipe = level.getBlockEntity(node.pos) instanceof ItemPipeBlockEntity p ? p : null;

            for (Endpoint output : network.outputs) {
                if (!output.pipePos.equals(node.pos)) continue;
                if (pipe != null && pipe.isFiltering() && !pipe.allowsFilterExit(output.direction, stack)) continue;
                result.add(new RouteCandidate(output, node.path));
            }

            for (Direction direction : Direction.values()) {
                if (pipe != null && pipe.isFiltering() && !pipe.allowsFilterExit(direction, stack)) continue;
                BlockPos next = resolvePipeNeighbor(level, node.pos, direction);
                if (next == null || !network.pipes.contains(next.asLong())) continue;
                if (!visited.add(next.asLong())) continue;

                List<BlockPos> nextPath = new ArrayList<>(node.path.size() + 1);
                nextPath.addAll(node.path);
                nextPath.add(next);
                queue.addLast(new PathNode(next, List.copyOf(nextPath)));
            }
        }
        return List.copyOf(result);
    }

    private static ItemStack insertAcross(IItemHandler target, ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < target.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = target.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    private static void returnRemainder(ServerLevel level, Endpoint source,
                                        IItemHandler sourceHandler, ItemStack remainder) {
        ItemStack left = insertAcross(sourceHandler, remainder, false);
        if (!left.isEmpty()) {
            ItemEntity entity = new ItemEntity(
                    level,
                    source.pipePos.getX() + 0.5D,
                    source.pipePos.getY() + 0.5D,
                    source.pipePos.getZ() + 0.5D,
                    left
            );
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

    private static void queueTravelVisual(ServerLevel level, Endpoint source, RouteCandidate route) {
        LevelState state = STATES.computeIfAbsent(level, ignored -> new LevelState());
        while (state.visuals.size() >= MAX_ACTIVE_VISUALS) {
            state.visuals.pollFirst();
        }

        List<Vec3> points = new ArrayList<>(route.path.size() + 2);
        points.add(facePoint(source.pipePos, source.direction));
        for (BlockPos pipePos : route.path) {
            Vec3 center = Vec3.atCenterOf(pipePos);
            if (points.isEmpty() || points.get(points.size() - 1).distanceToSqr(center) > 1.0E-6D) {
                points.add(center);
            }
        }
        points.add(facePoint(route.endpoint.pipePos, route.endpoint.direction));

        if (points.size() >= 2) {
            state.visuals.addLast(new TravelVisual(List.copyOf(points)));
        }
    }

    private static Vec3 facePoint(BlockPos pipePos, Direction direction) {
        Vec3 center = Vec3.atCenterOf(pipePos);
        return center.add(
                direction.getStepX() * 0.46D,
                direction.getStepY() * 0.46D,
                direction.getStepZ() * 0.46D
        );
    }

    private static void tickTravelVisuals(ServerLevel level, LevelState state) {
        Iterator<TravelVisual> iterator = state.visuals.iterator();
        while (iterator.hasNext()) {
            TravelVisual visual = iterator.next();
            int segmentCount = visual.points.size() - 1;
            if (segmentCount <= 0) {
                iterator.remove();
                continue;
            }

            double progress = visual.ageTicks / (double) VISUAL_TICKS_PER_SEGMENT;
            if (progress >= segmentCount) {
                iterator.remove();
                continue;
            }

            int segment = Math.min(segmentCount - 1, (int) Math.floor(progress));
            double local = progress - segment;
            Vec3 a = visual.points.get(segment);
            Vec3 b = visual.points.get(segment + 1);
            Vec3 p = a.lerp(b, local);

            level.sendParticles(
                    ItemPipeRegistry.PACKET_PARTICLE.get(),
                    p.x, p.y, p.z,
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
            visual.ageTicks++;
        }
    }

    private static final class LevelState {
        private final LongOpenHashSet knownPipes = new LongOpenHashSet();
        private final List<Network> networks = new ArrayList<>();
        private final ArrayDeque<TravelVisual> visuals = new ArrayDeque<>();
        private boolean dirty = true;
    }

    private record Component(LongOpenHashSet pipes) { }
    private record Endpoint(BlockPos pipePos, Direction direction) { }
    private record RouteKey(long sourcePipe, Item item) { }
    private record RouteCandidate(Endpoint endpoint, List<BlockPos> path) { }
    private record TransferPlan(RouteCandidate route, int accepted) { }
    private record PathNode(BlockPos pos, List<BlockPos> path) { }

    private static final class TravelVisual {
        private final List<Vec3> points;
        private int ageTicks;

        private TravelVisual(List<Vec3> points) {
            this.points = points;
        }
    }
}
