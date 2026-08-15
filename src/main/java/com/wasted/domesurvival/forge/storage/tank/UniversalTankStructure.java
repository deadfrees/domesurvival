package com.wasted.domesurvival.forge.storage.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Event-driven V63 structure manager.
 *
 * <p>No scan runs every tick. A structure is recomputed only when a tank block
 * is placed or removed. The hard 12x12x12 bound caps all structural work at
 * 1,728 cells.</p>
 */
public final class UniversalTankStructure {
    public static final int MAX_AXIS_SIZE = 12;
    public static final int MAX_BLOCKS = MAX_AXIS_SIZE * MAX_AXIS_SIZE * MAX_AXIS_SIZE;

    private UniversalTankStructure() {
    }

    public static void tryMergeAround(ServerLevel level, BlockPos placedPos, @Nullable Player player) {
        BlockEntity placedEntity = level.getBlockEntity(placedPos);
        if (!(placedEntity instanceof UniversalTankBlockEntity placedTank)) return;

        LinkedHashMap<BlockPos, UniversalTankBlockEntity> masters = new LinkedHashMap<>();
        UniversalTankBlockEntity placedMaster = placedTank.getMasterEntity();
        masters.put(
                placedMaster == null ? placedTank.getBlockPos() : placedMaster.getBlockPos(),
                placedMaster == null ? placedTank : placedMaster
        );

        for (Direction direction : Direction.values()) {
            BlockEntity neighborEntity = level.getBlockEntity(placedPos.relative(direction));
            if (!(neighborEntity instanceof UniversalTankBlockEntity neighborTank)) continue;

            UniversalTankBlockEntity neighborMaster = neighborTank.getMasterEntity();
            if (neighborMaster == null) {
                notify(player, "message.domesurvival.universal_tank.unloaded");
                return;
            }
            masters.put(neighborMaster.getBlockPos(), neighborMaster);
        }

        if (masters.size() <= 1) {
            // This path is intended only for a freshly placed standalone block.
            // Never collapse an already-known structure if this helper is called defensively.
            if (placedTank.getStructureBlockCount() == 1) {
                applyComponent(
                        level,
                        Set.of(placedPos.immutable()),
                        placedPos,
                        ContentSnapshot.empty()
                );
            }
            return;
        }

        ContentSnapshot mergedContent = ContentSnapshot.empty();
        long mergedAmount = 0L;

        for (UniversalTankBlockEntity master : masters.values()) {
            ContentSnapshot snapshot = master.snapshotLocalContent();
            if (!mergedContent.compatibleWith(snapshot)) {
                notify(player, "message.domesurvival.universal_tank.incompatible");
                return;
            }

            if (mergedContent.kind() == UniversalTankContentKind.EMPTY
                    && snapshot.kind() != UniversalTankContentKind.EMPTY) {
                mergedContent = snapshot.copyWithAmount(snapshot.amount());
            }
            mergedAmount += snapshot.amount();
        }

        HashSet<BlockPos> members = new HashSet<>();
        for (UniversalTankBlockEntity master : masters.values()) {
            Set<BlockPos> masterMembers = membersForMaster(level, master);
            if (masterMembers == null) {
                notify(player, "message.domesurvival.universal_tank.unloaded");
                return;
            }
            members.addAll(masterMembers);
        }
        members.add(placedPos.immutable());

        Bounds bounds = Bounds.of(members);
        if (bounds == null
                || bounds.sizeX() > MAX_AXIS_SIZE
                || bounds.sizeY() > MAX_AXIS_SIZE
                || bounds.sizeZ() > MAX_AXIS_SIZE
                || members.size() > MAX_BLOCKS) {
            notify(player, "message.domesurvival.universal_tank.limit", MAX_AXIS_SIZE);
            return;
        }

        int capacity = Math.multiplyExact(members.size(), UniversalTankBlockEntity.CAPACITY_PER_BLOCK);
        if (mergedAmount > capacity) {
            // This should be impossible for a normal merge, but retain a fail-closed guard.
            notify(player, "message.domesurvival.universal_tank.overflow");
            return;
        }

        UniversalTankBlockEntity preferredMaster = masters.values().stream()
                .max(Comparator
                        .comparingInt(UniversalTankBlockEntity::getStructureBlockCount)
                        .thenComparing(tank -> !tank.getBlockPos().equals(placedPos)))
                .orElse(placedTank);

        ContentSnapshot finalContent = mergedContent.kind() == UniversalTankContentKind.EMPTY
                ? ContentSnapshot.empty()
                : mergedContent.copyWithAmount((int) mergedAmount);

        applyComponent(level, members, preferredMaster.getBlockPos(), finalContent);
    }

    public static void beforeRemove(ServerLevel level, BlockPos removedPos) {
        BlockEntity removedEntity = level.getBlockEntity(removedPos);
        if (!(removedEntity instanceof UniversalTankBlockEntity removedTank)) return;

        UniversalTankBlockEntity oldMaster = removedTank.getMasterEntity();
        if (oldMaster == null) return;

        Set<BlockPos> oldMembers = membersForMaster(level, oldMaster);
        if (oldMembers == null || oldMembers.isEmpty()) return;

        ContentSnapshot stored = oldMaster.snapshotLocalContent();

        // Engineer's Wrench transfers up to one cell (4,000 mB) into the dropped
        // BlockItem. Subtract exactly that amount before redistributing the shared
        // storage so dismantling cannot duplicate fluid or oxygen.
        int portableAmount = Math.min(
                stored.amount(),
                removedTank.consumeEngineerWrenchExtraction()
        );
        if (portableAmount > 0) {
            stored = stored.copyWithAmount(stored.amount() - portableAmount);
        }

        HashSet<BlockPos> remaining = new HashSet<>(oldMembers);
        remaining.remove(removedPos);
        if (remaining.isEmpty()) return;

        List<Set<BlockPos>> components = splitConnected(remaining);
        components.sort((a, b) -> {
            boolean aHasOldMaster = a.contains(oldMaster.getBlockPos());
            boolean bHasOldMaster = b.contains(oldMaster.getBlockPos());
            if (aHasOldMaster != bHasOldMaster) return aHasOldMaster ? -1 : 1;
            return Integer.compare(b.size(), a.size());
        });

        int remainingAmount = stored.amount();
        for (Set<BlockPos> component : components) {
            int capacity = component.size() * UniversalTankBlockEntity.CAPACITY_PER_BLOCK;
            int portion = Math.min(remainingAmount, capacity);

            ContentSnapshot portionSnapshot = portion <= 0
                    ? ContentSnapshot.empty()
                    : stored.copyWithAmount(portion);

            BlockPos preferred = component.contains(oldMaster.getBlockPos())
                    ? oldMaster.getBlockPos()
                    : chooseStableMaster(component);

            applyComponent(level, component, preferred, portionSnapshot);
            remainingAmount -= portion;
        }
        // Ordinary survival mining intentionally drops a plain empty block.
        // Any amount that no longer fits after capacity shrinks is discarded.
        // Engineer-wrench dismantling has already removed its portable share above.
    }

    public static boolean canBreakSafely(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof UniversalTankBlockEntity tank)) return true;

        UniversalTankBlockEntity master = tank.getMasterEntity();
        if (master == null) return false;

        int amount = master.getStoredAmount();
        int blocksAfter = Math.max(0, master.getStructureBlockCount() - 1);
        long capacityAfter = (long) blocksAfter * UniversalTankBlockEntity.CAPACITY_PER_BLOCK;
        return amount <= capacityAfter;
    }

    @Nullable
    private static Set<BlockPos> membersForMaster(ServerLevel level, UniversalTankBlockEntity master) {
        BlockPos min = master.getStructureMin();
        BlockPos max = master.getStructureMax();
        BlockPos masterPos = master.getBlockPos();

        HashSet<BlockPos> members = new HashSet<>(Math.max(4, master.getStructureBlockCount() * 2));

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.hasChunkAt(pos)) return null;

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof UniversalTankBlockEntity tank
                            && tank.getMasterPos().equals(masterPos)) {
                        members.add(pos.immutable());
                    }
                }
            }
        }

        if (members.isEmpty()) {
            members.add(masterPos.immutable());
        }
        return members;
    }

    private static List<Set<BlockPos>> splitConnected(Set<BlockPos> positions) {
        HashSet<BlockPos> unvisited = new HashSet<>(positions);
        ArrayList<Set<BlockPos>> components = new ArrayList<>();

        while (!unvisited.isEmpty()) {
            BlockPos seed = unvisited.iterator().next();
            HashSet<BlockPos> component = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(seed);
            unvisited.remove(seed);

            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();
                component.add(current);

                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (unvisited.remove(next)) {
                        queue.addLast(next);
                    }
                }
            }

            components.add(component);
        }

        return components;
    }

    private static void applyComponent(ServerLevel level, Set<BlockPos> members,
                                       BlockPos preferredMaster, ContentSnapshot content) {
        if (members == null || members.isEmpty()) return;

        Bounds bounds = Bounds.of(members);
        if (bounds == null) return;

        BlockPos masterPos = members.contains(preferredMaster)
                ? preferredMaster.immutable()
                : chooseStableMaster(members);

        boolean unified = isUnifiedRectangle(members, bounds);
        int blockCount = members.size();

        ArrayList<UniversalTankBlockEntity> blockEntities = new ArrayList<>(blockCount);
        for (BlockPos pos : members) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UniversalTankBlockEntity tank) {
                blockEntities.add(tank);
            }
        }

        // Clear old master copies before assigning the one authoritative new master.
        for (UniversalTankBlockEntity tank : blockEntities) {
            tank.clearLocalContent();
        }

        for (UniversalTankBlockEntity tank : blockEntities) {
            boolean metadataChanged = tank.applyStructureData(
                    masterPos,
                    bounds.min(),
                    bounds.max(),
                    blockCount,
                    unified
            );

            BlockState state = level.getBlockState(tank.getBlockPos());
            boolean stateChanged = state.getBlock() instanceof UniversalTankBlock
                    && state.getValue(UniversalTankBlock.FORMED) != unified;

            if (stateChanged) {
                level.setBlock(
                        tank.getBlockPos(),
                        state.setValue(UniversalTankBlock.FORMED, unified),
                        Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS
                );
            }

            if (metadataChanged || stateChanged) {
                tank.syncStructureStateToClient();
            } else {
                tank.setChanged();
            }
        }

        BlockEntity masterEntity = level.getBlockEntity(masterPos);
        if (masterEntity instanceof UniversalTankBlockEntity newMaster) {
            newMaster.setLocalContent(content);
            newMaster.markOxygenPortCacheDirty();
            UniversalTankEvents.trackMaster(level, masterPos);
            newMaster.syncStructureStateToClient();
        }
    }

    private static boolean isUnifiedRectangle(Set<BlockPos> members, Bounds bounds) {
        long rectangularVolume = (long) bounds.sizeX() * bounds.sizeY() * bounds.sizeZ();
        if (rectangularVolume != members.size()) return false;

        int largeAxes = 0;
        if (bounds.sizeX() >= 3) largeAxes++;
        if (bounds.sizeY() >= 3) largeAxes++;
        if (bounds.sizeZ() >= 3) largeAxes++;

        // "3x3 minimum" in any orientation: 3x3x1, 3x3x2 and 3x3x3 all qualify.
        return largeAxes >= 2;
    }

    private static BlockPos chooseStableMaster(Set<BlockPos> members) {
        return members.stream()
                .min(Comparator
                        .comparingInt((BlockPos pos) -> pos.getY())
                        .thenComparingInt(pos -> pos.getX())
                        .thenComparingInt(pos -> pos.getZ()))
                .orElseThrow()
                .immutable();
    }

    private static void notify(@Nullable Player player, String key, Object... args) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    public record ContentSnapshot(
            UniversalTankContentKind kind,
            FluidStack fluid,
            int amount
    ) {
        public ContentSnapshot {
            kind = kind == null ? UniversalTankContentKind.EMPTY : kind;
            fluid = fluid == null ? FluidStack.EMPTY : fluid.copy();
            amount = Math.max(0, amount);

            if (amount == 0 || kind == UniversalTankContentKind.EMPTY) {
                kind = UniversalTankContentKind.EMPTY;
                fluid = FluidStack.EMPTY;
                amount = 0;
            } else if (kind == UniversalTankContentKind.FLUID) {
                if (fluid.isEmpty()) {
                    kind = UniversalTankContentKind.EMPTY;
                    amount = 0;
                } else {
                    fluid.setAmount(amount);
                }
            } else {
                fluid = FluidStack.EMPTY;
            }
        }

        public static ContentSnapshot empty() {
            return new ContentSnapshot(UniversalTankContentKind.EMPTY, FluidStack.EMPTY, 0);
        }

        public boolean compatibleWith(ContentSnapshot other) {
            if (other == null
                    || kind == UniversalTankContentKind.EMPTY
                    || other.kind == UniversalTankContentKind.EMPTY) {
                return true;
            }
            if (kind != other.kind) return false;
            if (kind == UniversalTankContentKind.FLUID) {
                return !fluid.isEmpty()
                        && !other.fluid.isEmpty()
                        && fluid.isFluidEqual(other.fluid);
            }
            return true;
        }

        public ContentSnapshot copyWithAmount(int newAmount) {
            if (newAmount <= 0) return empty();
            if (kind == UniversalTankContentKind.FLUID) {
                FluidStack copy = fluid.copy();
                copy.setAmount(newAmount);
                return new ContentSnapshot(kind, copy, newAmount);
            }
            return new ContentSnapshot(kind, FluidStack.EMPTY, newAmount);
        }
    }

    private record Bounds(BlockPos min, BlockPos max) {
        @Nullable
        private static Bounds of(Set<BlockPos> members) {
            if (members == null || members.isEmpty()) return null;

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (BlockPos pos : members) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }

            return new Bounds(
                    new BlockPos(minX, minY, minZ),
                    new BlockPos(maxX, maxY, maxZ)
            );
        }

        private int sizeX() {
            return max.getX() - min.getX() + 1;
        }

        private int sizeY() {
            return max.getY() - min.getY() + 1;
        }

        private int sizeZ() {
            return max.getZ() - min.getZ() + 1;
        }
    }
}
