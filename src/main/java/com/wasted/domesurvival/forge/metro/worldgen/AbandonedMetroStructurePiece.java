package com.wasted.domesurvival.forge.metro.worldgen;

import com.wasted.domesurvival.forge.metro.MetroRegistry;
import com.wasted.domesurvival.forge.metro.MetroStationLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One logical metro station represented as a native StructurePiece.
 *
 * Minecraft invokes postProcess for every chunk intersecting this piece. Every block
 * write is gated by the chunk BoundingBox supplied by vanilla, so no far-chunk writes
 * occur during world generation.
 */
public final class AbandonedMetroStructurePiece extends StructurePiece {
    private static final int STATION_HALF_X = 16;
    private static final int STATION_NORTH = 10;
    private static final int STATION_SOUTH = 8;
    private static final int DESCENT_FLIGHT_LENGTH = 6;
    private static final int DESCENT_LANDING_LENGTH = 3;

    private static final String NBT_SURFACE_X = "SurfaceX";
    private static final String NBT_SURFACE_Y = "SurfaceY";
    private static final String NBT_SURFACE_Z = "SurfaceZ";
    private static final String NBT_DEPTH = "Depth";
    private static final String NBT_LAYOUT_SEED = "LayoutSeed";

    private final BlockPos surface;
    private final int depth;
    private final long layoutSeed;

    public AbandonedMetroStructurePiece(BlockPos surface, int depth, long layoutSeed) {
        super(MetroRegistry.ABANDONED_METRO_PIECE.get(), 0, createBoundingBox(surface, depth));
        this.surface = surface.immutable();
        this.depth = depth;
        this.layoutSeed = layoutSeed;
    }

    public AbandonedMetroStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(MetroRegistry.ABANDONED_METRO_PIECE.get(), tag);
        this.surface = new BlockPos(
                tag.getInt(NBT_SURFACE_X),
                tag.getInt(NBT_SURFACE_Y),
                tag.getInt(NBT_SURFACE_Z)
        );
        this.depth = tag.getInt(NBT_DEPTH);
        this.layoutSeed = tag.getLong(NBT_LAYOUT_SEED);
    }

    private static BoundingBox createBoundingBox(BlockPos surface, int depth) {
        int drops = depth - 2;
        int run = descentRunLength(drops);
        int centerZ = surface.getZ() + 4 + run + STATION_NORTH;
        int baseY = surface.getY() - depth;

        // Covers entrance, complete descent, 33-block station shell and both 10-block tunnels.
        return new BoundingBox(
                surface.getX() - STATION_HALF_X - 10,
                baseY,
                surface.getZ() - 4,
                surface.getX() + STATION_HALF_X + 10,
                surface.getY() + 5,
                centerZ + STATION_SOUTH
        );
    }

    private static int descentRunLength(int drops) {
        return drops + (drops / DESCENT_FLIGHT_LENGTH) * DESCENT_LANDING_LENGTH;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt(NBT_SURFACE_X, surface.getX());
        tag.putInt(NBT_SURFACE_Y, surface.getY());
        tag.putInt(NBT_SURFACE_Z, surface.getZ());
        tag.putInt(NBT_DEPTH, depth);
        tag.putLong(NBT_LAYOUT_SEED, layoutSeed);
    }

    @Override
    public void postProcess(WorldGenLevel level,
                            StructureManager structureManager,
                            ChunkGenerator chunkGenerator,
                            RandomSource random,
                            BoundingBox chunkBox,
                            ChunkPos chunkPos,
                            BlockPos pivot) {
        long chunkSeed = layoutSeed
                ^ ((long) chunkPos.x * 341873128712L)
                ^ ((long) chunkPos.z * 132897987541L);
        new Writer(level, chunkBox, RandomSource.create(chunkSeed), surface, depth).build();
    }

    private static final class Writer {
        private final WorldGenLevel level;
        private final BoundingBox chunkBox;
        private final RandomSource random;
        private final BlockPos surface;
        private final int depth;

        private Writer(WorldGenLevel level,
                       BoundingBox chunkBox,
                       RandomSource random,
                       BlockPos surface,
                       int depth) {
            this.level = level;
            this.chunkBox = chunkBox;
            this.random = random;
            this.surface = surface;
            this.depth = depth;
        }

        private void build() {
            int baseY = surface.getY() - depth;
            int drops = depth - 2;
            int stationNorthZ = surface.getZ() + 4 + descentRunLength(drops);
            BlockPos center = new BlockPos(surface.getX(), baseY, stationNorthZ + STATION_NORTH);
            BlockPos consoleAnchor = center.offset(-13, 2, -6);

            buildStationShell(center);
            buildVestibuleAndTechnical(center, consoleAnchor);
            buildPlatforms(center);
            buildTunnelSegments(center);
            buildStaticTrain(center, consoleAnchor);
            placeStage3Markers(consoleAnchor);
            buildDescent(surface, center, depth);
            buildEntrance(surface);
        }

        private void buildEntrance(BlockPos surface) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    set(surface.offset(x, -1, z), ruinStone());
                }
            }

            fill(surface.offset(-3, 0, -3), surface.offset(3, 5, 3), Blocks.AIR.defaultBlockState());
            for (int yy = 0; yy <= 4; yy++) {
                for (int x = -3; x <= 3; x++) {
                    set(surface.offset(x, yy, -3), ruinStone());
                    set(surface.offset(x, yy, 3), ruinStone());
                }
                for (int z = -2; z <= 2; z++) {
                    set(surface.offset(-3, yy, z), ruinStone());
                    set(surface.offset(3, yy, z), ruinStone());
                }
            }

            fill(surface.offset(-1, 0, 3), surface.offset(1, 3, 3), Blocks.AIR.defaultBlockState());
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if ((x + z) % 3 != 0) {
                        set(surface.offset(x, 5, z), Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
            }
            set(surface.offset(-2, 2, -3), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            set(surface.offset(2, 2, -3), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        }

        private static int descentRunLength(int drops) {
            return drops + (drops / DESCENT_FLIGHT_LENGTH) * DESCENT_LANDING_LENGTH;
        }

        private void buildDescent(BlockPos surface, BlockPos center, int depth) {
            int startY = surface.getY() - 1;
            int targetY = center.getY() + 1;
            int drops = depth - 2;
            int expectedNorthZ = center.getZ() - STATION_NORTH;

            BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.SOUTH);

            int z = surface.getZ() + 4;
            for (int i = 0; i <= drops; i++) {
                int y = startY - i;

                carveDescentSlice(surface.getX(), y, z);
                for (int x = -2; x <= 2; x++) {
                    set(new BlockPos(surface.getX() + x, y - 1, z), Blocks.STONE_BRICKS.defaultBlockState());
                    set(new BlockPos(surface.getX() + x, y, z), stair);
                }
                set(new BlockPos(surface.getX() - 3, y, z), Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
                set(new BlockPos(surface.getX() + 3, y, z), Blocks.CRACKED_STONE_BRICKS.defaultBlockState());

                if ((i + 1) % DESCENT_FLIGHT_LENGTH == 0 && i < drops) {
                    for (int landing = 1; landing <= DESCENT_LANDING_LENGTH; landing++) {
                        int landingZ = z + landing;
                        carveDescentSlice(surface.getX(), y, landingZ);
                        for (int x = -2; x <= 2; x++) {
                            set(new BlockPos(surface.getX() + x, y - 1, landingZ),
                                    Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                        set(new BlockPos(surface.getX() - 3, y, landingZ),
                                Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
                        set(new BlockPos(surface.getX() + 3, y, landingZ),
                                Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
                    }
                    z += DESCENT_LANDING_LENGTH;
                }
                z++;
            }

            // The final landing meets the vestibule opening cleanly.
            fill(new BlockPos(surface.getX() - 2, targetY, expectedNorthZ),
                    new BlockPos(surface.getX() + 2, targetY + 4, expectedNorthZ + 2),
                    Blocks.AIR.defaultBlockState());
            for (int x = -2; x <= 2; x++) {
                set(new BlockPos(surface.getX() + x, targetY - 1, expectedNorthZ + 1),
                        Blocks.DEEPSLATE_TILES.defaultBlockState());
                set(new BlockPos(surface.getX() + x, targetY - 1, expectedNorthZ + 2),
                        Blocks.DEEPSLATE_TILES.defaultBlockState());
            }
        }

        private void carveDescentSlice(int centerX, int y, int z) {
            fill(new BlockPos(centerX - 3, y, z),
                    new BlockPos(centerX + 3, y + 4, z),
                    Blocks.AIR.defaultBlockState());
        }

        private void buildStationShell(BlockPos center) {
            int minX = center.getX() - STATION_HALF_X;
            int maxX = center.getX() + STATION_HALF_X;
            int minZ = center.getZ() - STATION_NORTH;
            int maxZ = center.getZ() + STATION_SOUTH;
            int baseY = center.getY();

            fill(new BlockPos(minX + 1, baseY + 1, minZ + 1),
                    new BlockPos(maxX - 1, baseY + 9, maxZ - 1),
                    Blocks.AIR.defaultBlockState());

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(new BlockPos(x, baseY, z), Blocks.DEEPSLATE_TILES.defaultBlockState());
                    set(new BlockPos(x, baseY + 10, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                }
            }

            for (int y = baseY + 1; y <= baseY + 9; y++) {
                for (int x = minX; x <= maxX; x++) {
                    set(new BlockPos(x, y, minZ), wallStone());
                    set(new BlockPos(x, y, maxZ), wallStone());
                }
                for (int z = minZ + 1; z < maxZ; z++) {
                    set(new BlockPos(minX, y, z), wallStone());
                    set(new BlockPos(maxX, y, z), wallStone());
                }
            }

            // Access doorway from the stairs.
            fill(new BlockPos(center.getX() - 2, baseY + 1, minZ),
                    new BlockPos(center.getX() + 2, baseY + 5, minZ),
                    Blocks.AIR.defaultBlockState());

            // Divider between vestibule/technical zone and platform hall.
            int dividerZ = center.getZ() - 3;
            for (int x = minX + 1; x <= maxX - 1; x++) {
                for (int y = baseY + 1; y <= baseY + 7; y++) {
                    set(new BlockPos(x, y, dividerZ), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                }
            }
            fill(new BlockPos(center.getX() - 2, baseY + 1, dividerZ),
                    new BlockPos(center.getX() + 2, baseY + 5, dividerZ),
                    Blocks.AIR.defaultBlockState());
        }

        private void buildVestibuleAndTechnical(BlockPos center, BlockPos consoleAnchor) {
            int baseY = center.getY();
            int minX = center.getX() - STATION_HALF_X;
            int minZ = center.getZ() - STATION_NORTH;

            // Vestibule columns / ruined turnstile-like barriers.
            for (int x : new int[]{-6, 6}) {
                for (int y = baseY + 1; y <= baseY + 7; y++) {
                    set(new BlockPos(center.getX() + x, y, minZ + 4),
                            Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }
            }
            for (int x = -4; x <= 4; x += 2) {
                set(new BlockPos(center.getX() + x, baseY + 1, minZ + 6),
                        Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            }

            // Dedicated technical room in the north-west part of the station.
            int roomMinX = minX + 2;
            int roomMaxX = minX + 8;
            int roomMinZ = minZ + 2;
            int roomMaxZ = minZ + 6;
            for (int y = baseY + 1; y <= baseY + 6; y++) {
                for (int x = roomMinX; x <= roomMaxX; x++) {
                    set(new BlockPos(x, y, roomMinZ), wallStone());
                    set(new BlockPos(x, y, roomMaxZ), wallStone());
                }
                for (int z = roomMinZ; z <= roomMaxZ; z++) {
                    set(new BlockPos(roomMinX, y, z), wallStone());
                    set(new BlockPos(roomMaxX, y, z), wallStone());
                }
            }
            fill(new BlockPos(roomMaxX, baseY + 1, roomMinZ + 1),
                    new BlockPos(roomMaxX, baseY + 3, roomMinZ + 2),
                    Blocks.AIR.defaultBlockState());

            set(new BlockPos(roomMinX + 1, baseY + 1, roomMaxZ - 1), Blocks.POLISHED_ANDESITE.defaultBlockState());
            set(new BlockPos(roomMinX + 2, baseY + 1, roomMaxZ - 1), Blocks.POLISHED_ANDESITE.defaultBlockState());
            set(new BlockPos(roomMinX + 3, baseY + 1, roomMaxZ - 1), Blocks.GRAY_CONCRETE.defaultBlockState());
            set(consoleAnchor, MetroRegistry.METRO_RESTORATION_CONSOLE.get().defaultBlockState());
        }

        private void buildPlatforms(BlockPos center) {
            int minX = center.getX() - STATION_HALF_X + 2;
            int maxX = center.getX() + STATION_HALF_X - 2;
            int baseY = center.getY();

            for (int x = minX; x <= maxX; x++) {
                for (int z = center.getZ() - 2; z <= center.getZ(); z++) {
                    set(new BlockPos(x, baseY + 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                }
                for (int z = center.getZ() + 1; z <= center.getZ() + 3; z++) {
                    set(new BlockPos(x, baseY + 1, z), Blocks.GRAVEL.defaultBlockState());
                }
                for (int z = center.getZ() + 4; z <= center.getZ() + 6; z++) {
                    set(new BlockPos(x, baseY + 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                }
                set(new BlockPos(x, baseY + 2, center.getZ() + 1), Blocks.RAIL.defaultBlockState());
                set(new BlockPos(x, baseY + 2, center.getZ() + 3), Blocks.RAIL.defaultBlockState());
            }

            for (int x = center.getX() - 12; x <= center.getX() + 12; x += 6) {
                for (int y = baseY + 2; y <= baseY + 9; y++) {
                    set(new BlockPos(x, y, center.getZ() - 1), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    set(new BlockPos(x, y, center.getZ() + 7), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }
            }
        }

        private void buildTunnelSegments(BlockPos center) {
            int baseY = center.getY();
            int stationMinX = center.getX() - STATION_HALF_X;
            int stationMaxX = center.getX() + STATION_HALF_X;

            // Open the station end walls into the track tunnel.
            for (int x : new int[]{stationMinX, stationMaxX}) {
                fill(new BlockPos(x, baseY + 1, center.getZ()),
                        new BlockPos(x, baseY + 7, center.getZ() + 4),
                        Blocks.AIR.defaultBlockState());
            }

            buildTunnelSide(stationMinX - 10, stationMinX - 1, center);
            buildTunnelSide(stationMaxX + 1, stationMaxX + 10, center);
        }

        private void buildTunnelSide(int fromX, int toX, BlockPos center) {
            int minX = Math.min(fromX, toX);
            int maxX = Math.max(fromX, toX);
            int baseY = center.getY();
            int minZ = center.getZ();
            int maxZ = center.getZ() + 4;

            fill(new BlockPos(minX, baseY + 1, minZ + 1),
                    new BlockPos(maxX, baseY + 7, maxZ - 1),
                    Blocks.AIR.defaultBlockState());

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(new BlockPos(x, baseY, z), Blocks.DEEPSLATE_TILES.defaultBlockState());
                    set(new BlockPos(x, baseY + 8, z), wallStone());
                }
                for (int y = baseY + 1; y <= baseY + 7; y++) {
                    set(new BlockPos(x, y, minZ), wallStone());
                    set(new BlockPos(x, y, maxZ), wallStone());
                }
                set(new BlockPos(x, baseY + 1, center.getZ() + 1), Blocks.GRAVEL.defaultBlockState());
                set(new BlockPos(x, baseY + 1, center.getZ() + 3), Blocks.GRAVEL.defaultBlockState());
                set(new BlockPos(x, baseY + 2, center.getZ() + 1), Blocks.RAIL.defaultBlockState());
                set(new BlockPos(x, baseY + 2, center.getZ() + 3), Blocks.RAIL.defaultBlockState());
            }
        }

        private void buildStaticTrain(BlockPos center, BlockPos consoleAnchor) {
            int baseY = center.getY();
            int minX = center.getX() - 7;
            int maxX = center.getX() + 7;
            int minZ = center.getZ() + 1;
            int maxZ = center.getZ() + 3;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(new BlockPos(x, baseY + 2, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                    set(new BlockPos(x, baseY + 6, z), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                }
            }
            for (int y = baseY + 3; y <= baseY + 5; y++) {
                for (int x = minX; x <= maxX; x++) {
                    set(new BlockPos(x, y, minZ), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                    set(new BlockPos(x, y, maxZ), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                }
                set(new BlockPos(minX, y, center.getZ() + 2), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                set(new BlockPos(maxX, y, center.getZ() + 2), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }

            fill(new BlockPos(minX + 1, baseY + 3, center.getZ() + 2),
                    new BlockPos(maxX - 1, baseY + 5, center.getZ() + 2),
                    Blocks.AIR.defaultBlockState());

            for (int x = minX + 2; x <= maxX - 2; x += 3) {
                set(new BlockPos(x, baseY + 4, minZ), Blocks.GLASS_PANE.defaultBlockState());
                set(new BlockPos(x, baseY + 4, maxZ), Blocks.GLASS_PANE.defaultBlockState());
            }

            fill(new BlockPos(center.getX() - 1, baseY + 3, minZ),
                    new BlockPos(center.getX() + 1, baseY + 5, minZ),
                    Blocks.AIR.defaultBlockState());

            set(MetroStationLayout.at(consoleAnchor, MetroStationLayout.TRAIN_CONSOLE),
                    MetroRegistry.METRO_TRAIN_CONSOLE.get().defaultBlockState());
        }

        private void placeStage3Markers(BlockPos consoleAnchor) {
            for (BlockPos offset : MetroStationLayout.BROKEN_LIGHTS) {
                set(MetroStationLayout.at(consoleAnchor, offset), Blocks.GRAY_CONCRETE.defaultBlockState());
            }
            for (BlockPos offset : MetroStationLayout.DAMAGED_DISPLAYS) {
                set(MetroStationLayout.at(consoleAnchor, offset), Blocks.BLACK_CONCRETE.defaultBlockState());
            }
        }

        private BlockState ruinStone() {
            int roll = random.nextInt(5);
            if (roll == 0) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            if (roll == 1) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            return Blocks.STONE_BRICKS.defaultBlockState();
        }

        private BlockState wallStone() {
            return random.nextInt(6) == 0
                    ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                    : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }

        private void fill(BlockPos a, BlockPos b, BlockState state) {
            int minX = Math.min(a.getX(), b.getX());
            int maxX = Math.max(a.getX(), b.getX());
            int minY = Math.min(a.getY(), b.getY());
            int maxY = Math.max(a.getY(), b.getY());
            int minZ = Math.min(a.getZ(), b.getZ());
            int maxZ = Math.max(a.getZ(), b.getZ());
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        set(new BlockPos(x, y, z), state);
                    }
                }
            }
        }

        private void set(BlockPos pos, BlockState state) {
            if (!chunkBox.isInside(pos)) return;
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return;
            level.setBlock(pos, state, 2);
        }
    }
}
