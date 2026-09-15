package com.wasted.domesurvival.forge.itempipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.wasted.domesurvival.forge.itempipe.ItemPipeNetworkManager.*;

/** Owns real in-flight stacks, independent of transient network rebuilds and chunk BEs. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID)
public final class ItemPipeTransitData extends SavedData {
    private static final String NAME = "domesurvival_item_pipe_transit_v1";
    private static final int MAX_PENDING = 1024;
    private static final int MAX_PER_SOURCE = 4;
    private final Map<UUID, Transit> pending = new LinkedHashMap<>();

    public static ItemPipeTransitData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ItemPipeTransitData::load, ItemPipeTransitData::new, NAME);
    }

    public int pendingCount() { return pending.size(); }
    public int pendingItems() { return pending.values().stream().mapToInt(t -> t.stack.getCount()).sum(); }

    boolean canEnqueue(Endpoint source) {
        return pending.size() < MAX_PENDING
                && pending.values().stream().filter(t -> t.source.equals(source)).limit(MAX_PER_SOURCE).count() < MAX_PER_SOURCE;
    }

    boolean enqueue(ServerLevel level, Endpoint source, Endpoint target, List<BlockPos> path, ItemStack stack) {
        if (stack.isEmpty() || path.isEmpty() || path.size() > 4096 || !canEnqueue(source)) return false;
        double speed = Double.POSITIVE_INFINITY;
        for (BlockPos pos : path) {
            if (level.getBlockState(pos).getBlock() instanceof ItemPipeBlock pipe && !pipe.isFiltering()) {
                speed = Math.min(speed, pipe.tier().travelSpeed());
            }
        }
        if (!Double.isFinite(speed)) speed = ItemPipeTier.COPPER.travelSpeed();
        Transit transit = new Transit(UUID.randomUUID(), source, target, path, stack.copy(), speed);
        pending.put(transit.id, transit);
        setDirty();
        ItemPipeVisualNetwork.send(level, transit.message(level));
        return true;
    }

    void tick(ServerLevel level, long revision) {
        long now = level.getGameTime();
        for (Iterator<Transit> iterator = pending.values().iterator(); iterator.hasNext();) {
            Transit transit = iterator.next();
            if (!transit.moving && transit.elapsed < transit.duration && now < transit.retryAt && transit.revision == revision) continue;
            if (transit.revision != revision || (!transit.moving && transit.elapsed < transit.duration) || now % 20 == 0) {
                // Never force-load a chunk or interpret an unloaded destination as destroyed.
                boolean loaded = level.hasChunkAt(transit.source.pipePos().relative(transit.source.direction()))
                        && level.hasChunkAt(transit.target.pipePos().relative(transit.target.direction()))
                        && routeChunksLoaded(level, transit.path);
                if (!loaded) {
                    if (transit.moving) {
                        transit.moving = false;
                        ItemPipeVisualNetwork.send(level, transit.message(level));
                    }
                    transit.revision = revision;
                    transit.retryAt = now + 20;
                    continue;
                }
                if (!validRoute(level, transit)) {
                    // Refund a broken/disabled route; if the source refuses insertion, drop
                    // the owned remainder at its loaded source pipe, exactly once.
                    returnRemainder(level, transit.source, handler(level, transit.source), transit.stack);
                    iterator.remove(); setDirty();
                    ItemPipeVisualNetwork.remove(level, transit.id);
                    continue;
                }
                transit.revision = revision;
                if (!transit.moving && transit.elapsed < transit.duration) {
                    transit.moving = true;
                    ItemPipeVisualNetwork.send(level, transit.message(level));
                }
            }
            if (transit.elapsed < transit.duration) {
                transit.elapsed++;
                setDirty();
            }
            if (transit.elapsed < transit.duration || now < transit.retryAt) continue;
            var sink = handler(level, transit.target);
            if (sink != null) transit.stack = insertAcross(sink, transit.stack, false);
            setDirty();
            if (transit.stack.isEmpty()) {
                iterator.remove();
                ItemPipeVisualNetwork.remove(level, transit.id);
            } else {
                // Full destination: keep the actual stack visible at the outlet. Four
                // outstanding packets per source bound both extraction and storage.
                transit.moving = false;
                transit.retryAt = now + 20;
                ItemPipeVisualNetwork.send(level, transit.message(level));
            }
        }
    }

    private static boolean validRoute(ServerLevel level, Transit transit) {
        for (int i = 0; i < transit.path.size(); i++) {
            BlockPos pos = transit.path.get(i);
            if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) return false;
            Direction exit;
            if (i + 1 < transit.path.size()) {
                BlockPos next = transit.path.get(i + 1);
                exit = Direction.getNearest(next.getX()-pos.getX(), next.getY()-pos.getY(), next.getZ()-pos.getZ());
                if (!next.equals(resolvePipeNeighbor(level, pos, exit))) return false;
            } else {
                exit = transit.target.direction();
                if (pipe.getConnectorMode(exit) != ItemConnectorMode.INPUT || !ItemPipeBlock.hasObjectConnector(level,pos,exit)) return false;
            }
            if (pipe.isFiltering() && !pipe.allowsFilterExit(exit, transit.stack)) return false;
        }
        return true;
    }

    private static boolean routeChunksLoaded(ServerLevel level, List<BlockPos> path) {
        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);
            if (!level.hasChunkAt(pos)) return false;
            if (i + 1 == path.size()) continue;
            BlockPos next = path.get(i + 1);
            // Service pass-through blocks are not graph vertices. Check the intervening
            // chunks too, so unloading a wall segment pauses instead of refunding cargo.
            int distance = pos.distManhattan(next);
            Direction step = Direction.getNearest(next.getX()-pos.getX(), next.getY()-pos.getY(), next.getZ()-pos.getZ());
            for (int offset = 1; offset < distance; offset++) {
                if (!level.hasChunkAt(pos.relative(step, offset))) return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void watch(ChunkWatchEvent.Watch event) {
        for (Transit transit : get(event.getLevel()).pending.values()) {
            if (transit.path.stream().anyMatch(p -> (p.getX() >> 4) == event.getPos().x && (p.getZ() >> 4) == event.getPos().z)) {
                ItemPipeVisualNetwork.sendTo(event.getPlayer(), transit.message(event.getLevel()));
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Transit t : pending.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", t.id);
            entry.putLong("Source", t.source.pipePos().asLong()); entry.putInt("SourceSide", t.source.direction().ordinal());
            entry.putLong("Target", t.target.pipePos().asLong()); entry.putInt("TargetSide", t.target.direction().ordinal());
            entry.putLongArray("Path", t.path.stream().mapToLong(BlockPos::asLong).toArray());
            entry.put("Stack", t.stack.save(new CompoundTag()));
            entry.putDouble("Speed", t.speed); entry.putInt("Elapsed", t.elapsed);
            list.add(entry);
        }
        tag.put("Packets", list);
        return tag;
    }

    public static ItemPipeTransitData load(CompoundTag tag) {
        ItemPipeTransitData data = new ItemPipeTransitData();
        ListTag list = tag.getList("Packets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ItemStack stack = ItemStack.of(e.getCompound("Stack"));
            long[] path = e.getLongArray("Path");
            if (stack.isEmpty() || path.length == 0 || path.length > 4096) continue;
            double speed = e.getDouble("Speed");
            if (!Double.isFinite(speed) || speed < .01 || speed > .25) speed = .04;
            Transit transit = new Transit(e.hasUUID("Id") ? e.getUUID("Id") : UUID.randomUUID(),
                    new Endpoint(BlockPos.of(e.getLong("Source")), Direction.from3DDataValue(e.getInt("SourceSide"))),
                    new Endpoint(BlockPos.of(e.getLong("Target")), Direction.from3DDataValue(e.getInt("TargetSide"))),
                    Arrays.stream(path).mapToObj(BlockPos::of).toList(), stack, speed);
            transit.elapsed = Math.max(0, Math.min(transit.duration, e.getInt("Elapsed")));
            transit.moving = false; // Validated and resynchronized on the first loaded tick.
            data.pending.put(transit.id, transit);
        }
        return data;
    }

    private static final class Transit {
        final UUID id;
        final Endpoint source, target;
        final List<BlockPos> path;
        final List<Vec3> points;
        final double speed;
        final int duration;
        ItemStack stack;
        int elapsed;
        boolean moving = true;
        long revision = Long.MIN_VALUE, retryAt;

        Transit(UUID id, Endpoint source, Endpoint target, List<BlockPos> path, ItemStack stack, double speed) {
            this.id=id; this.source=source; this.target=target; this.path=List.copyOf(path); this.stack=stack; this.speed=speed;
            List<Vec3> points = new ArrayList<>(); points.add(facePoint(source.pipePos(),source.direction()));
            for (BlockPos pos : path) points.add(Vec3.atCenterOf(pos));
            points.add(facePoint(target.pipePos(),target.direction())); this.points=List.copyOf(points);
            double length=0; for (int i=1;i<points.size();i++) length+=points.get(i-1).distanceTo(points.get(i));
            duration=Math.max(1,(int)Math.ceil(length/speed));
        }

        ItemPipeVisualNetwork.Travel message(ServerLevel level) {
            return new ItemPipeVisualNetwork.Travel(id, level.dimension().location(), points, stack.copy(), duration, elapsed, level.getGameTime(), moving);
        }
    }
}
