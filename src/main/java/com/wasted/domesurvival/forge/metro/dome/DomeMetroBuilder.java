package com.wasted.domesurvival.forge.metro.dome;

import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.metro.MetroRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Locale;

/**
 * Stage 4 V1.10 builder - exact Dome geometry, exact Dome blocks and exact airlock API.
 *
 * Layout rules:
 * - the public metro approach begins INSIDE the Dome, before the WEST shell;
 * - the Dome shell is opened only into a fully sealed glass transfer gallery;
 * - the descent starts outside the original Dome footprint;
 * - the descent is shallow, continuous and fully enclosed;
 * - all temporary/unfinished openings to the exterior are sealed;
 * - the two rail-tunnel stubs in the central station are capped until Stage 5.
 *
 * The placement scan stays bounded and rejects named/CustomNPC-like entities and
 * block entities in the full construction corridor. No global scans/chunk forcing.
 */
public final class DomeMetroBuilder {
    private static final int EDGE_SCAN_MIN = 10;
    private static final int EDGE_SCAN_MAX = 72;
    private static final int EDGE_SCAN_HALF_Z = 4;
    private static final int EDGE_GROUND_BAND = 10;
    private static final int EDGE_GLASS_THRESHOLD = 10;

    // Open approach inside the Dome, then a framed tunnel begins before the shell.
    private static final int APPROACH_LENGTH = 22;
    private static final int INNER_TUNNEL_LENGTH = 14;
    private static final int APPROACH_HALF_WIDTH = 4;

    // Outside the original shell: short sealed gallery, then a shallow stair tube.
    private static final int GALLERY_END_F = 8;
    private static final int STAIR_START_F = 9;
    private static final int STAIR_DROP = 9;
    private static final int STAIR_RUN_PER_DROP = 2;
    private static final int STAIR_END_F = STAIR_START_F + STAIR_DROP * STAIR_RUN_PER_DROP - 1;
    private static final int LOWER_VESTIBULE_START_F = STAIR_END_F + 1;
    private static final int LOWER_VESTIBULE_END_F = LOWER_VESTIBULE_START_F + 4;

    private static final int STATION_DROP = 9;
    private static final int STATION_FORWARD = 38;
    private static final int MIN_STATION_CLEARANCE = 12;

    private static final int PROTECTED_PENALTY = 1_000_000;
    private static final int BLOCK_ENTITY_PENALTY = 100_000;

    private DomeMetroBuilder() {
    }

    public static BuildResult build(ServerLevel level, BlockPos domeAnchor) {
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        Placement placement = chooseExactDomePlacement(level, spec);
        BlockPos entrance = placement.entrance();

        int stationFloorY = Math.max(
                level.getMinBuildHeight() + MIN_STATION_CLEARANCE,
                entrance.getY() - STATION_DROP
        );
        BlockPos stationCenter = at(
                entrance,
                placement.right(),
                placement.forward(),
                0,
                stationFloorY - entrance.getY(),
                STATION_FORWARD
        );

        // V1.10 uses the exact blocks already registered and used by DomeGenerationService.
        // No runtime scanning or guessed registry ids are involved.
        BlockState domeStructural = ModBlocks.DOME_FRAME.get().defaultBlockState();
        BlockState domeLamp = Blocks.SEA_LANTERN.defaultBlockState();

        // The route visibly begins inside the Dome. The original shell is opened
        // only after both sides of the opening already have sealed geometry.
        buildCleanInteriorApproach(level, placement);
        buildSealedInnerTunnel(level, placement);
        buildSealedTransferGallery(level, placement);
        buildShallowSealedStair(level, placement, stationFloorY);
        buildCentralHall(level, placement, stationCenter);
        buildBottomConnection(level, placement, stationFloorY, stationCenter);
        BlockPos trainConsole = buildTrain(level, placement, stationCenter);
        buildLighting(level, placement, stationCenter);
        buildSealedDomePortal(level, placement);

        // Make the metro use the same real unbreakable structural material that
        // already exists in this Dome. Decorative stairs/console/gate blocks stay
        // intact, while the structure is additionally protected from survival
        // breaking by DomeMetroProtectionEvents.
        reinforceBuiltMetro(level, placement, stationCenter, domeStructural);

        // Add visible fixtures in addition to hidden LIGHT blocks.
        installVisibleLighting(level, placement, stationCenter, domeLamp);

        // Install the project's real V56/V58 sliding gate and bind one real
        // control panel on each side. A gate integration failure is surfaced with
        // its exact reason instead of the old generic world-scan failure.
        // Finish the surrounding frame/clearance BEFORE the gate is installed.
        // V1.11 did this afterwards and cleared the 5x5 gate plane back to AIR.
        finishAirlockAndStairSeals(level, placement, -5, domeStructural);

        DomeMetroAirlock.InstallResult airlock = DomeMetroAirlock.install(
                level, placement.entrance(), placement.forward(), placement.right(), -5
        );
        if (!airlock.success()) {
            throw new IllegalStateException("Metro airlock install failed: " + airlock.detail());
        }

        return new BuildResult(entrance.immutable(), stationCenter.immutable(), trainConsole.immutable(), placement.forward());
    }

    /**
     * Pick the outermost dense WEST glass/window wall, not the first glass hit.
     * This keeps the outer-shell selection rule for internal Dome ribs being mistaken for the shell.
     */
    /**
     * V1.10 uses DomeSavedData/DomeSpec as the authoritative Dome geometry.
     * The metro is always attached to the WEST (left) edge of the starter Dome.
     * We only shift a little along Z when needed to avoid a named NPC or existing
     * machine/BlockEntity, but we never move the station to another side.
     */
    private static Placement chooseExactDomePlacement(ServerLevel level, DomeSpec spec) {
        int[] zOffsets = {0, 8, -8, 16, -16};
        Placement best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int zOffset : zOffsets) {
            int z = spec.centerZ() + zOffset;
            double dz = z - spec.centerZ();
            double radial = Math.sqrt(Math.max(1.0D,
                    (double) spec.surfaceRadius() * spec.surfaceRadius() - dz * dz));
            int shellX = spec.centerX() - (int) Math.round(radial);

            BlockPos entrance = new BlockPos(shellX, spec.baseY(), z);
            Placement candidate = new Placement(
                    entrance,
                    Direction.WEST,
                    Direction.NORTH,
                    ModBlocks.REINFORCED_GLASS.get().defaultBlockState()
            );

            int score = annexScore(level, candidate);
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
            if (score < PROTECTED_PENALTY) return candidate;
        }

        if (best != null && bestScore < PROTECTED_PENALTY) return best;
        throw new IllegalStateException("WEST Dome metro corridor is blocked by an NPC or existing machine");
    }

    private static EdgeHit findOutermostWestShell(ServerLevel level, BlockPos domeAnchor, int zOffset) {
        int targetZ = domeAnchor.getZ() + zOffset;
        EdgeHit best = null;

        for (int distance = EDGE_SCAN_MIN; distance <= EDGE_SCAN_MAX; distance++) {
            int x = domeAnchor.getX() - distance;
            int groundY = findLocalGroundY(level, new BlockPos(x + 4, domeAnchor.getY(), targetZ), domeAnchor.getY());
            int hits = 0;
            BlockState representative = Blocks.GLASS.defaultBlockState();
            BlockPos representativePos = null;

            // Require a dense wall close to local ground. Roof glass and internal
            // high ribs no longer qualify by themselves.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -EDGE_SCAN_HALF_Z; dz <= EDGE_SCAN_HALF_Z; dz++) {
                    for (int y = groundY + 1; y <= groundY + EDGE_GROUND_BAND; y++) {
                        BlockPos pos = new BlockPos(x + dx, y, targetZ + dz);
                        BlockState state = level.getBlockState(pos);
                        if (isDomeShell(state)) {
                            hits++;
                            representative = fullGlassOrDefault(state);
                            if (representativePos == null
                                    || Math.abs(dx) + Math.abs(dz) < Math.abs(representativePos.getX() - x)
                                    + Math.abs(representativePos.getZ() - targetZ)) {
                                representativePos = pos;
                            }
                        }
                    }
                }
            }

            if (hits >= EDGE_GLASS_THRESHOLD) {
                BlockPos shell = representativePos == null
                        ? new BlockPos(x, groundY + 1, targetZ)
                        : new BlockPos(x, groundY + 1, targetZ);
                // Keep scanning: the farthest qualifying wall is the exterior shell.
                best = new EdgeHit(shell, representative, groundY, distance);
            }
        }
        return best;
    }

    /** Find terrain/floor near spawn height while ignoring the glass roof of the Dome. */
    private static int findLocalGroundY(ServerLevel level, BlockPos column, int preferredY) {
        int top = Math.min(level.getMaxBuildHeight() - 4, preferredY + 10);
        int bottom = Math.max(level.getMinBuildHeight() + 2, preferredY - 12);
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || isDomeShell(state) || !state.getFluidState().isEmpty()) continue;
            BlockState a1 = level.getBlockState(pos.above());
            BlockState a2 = level.getBlockState(pos.above(2));
            if (!a1.blocksMotion() && !a2.blocksMotion()) return y;
        }
        return preferredY - 1;
    }

    private static boolean isDomeShell(BlockState state) {
        if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE) || state.is(Blocks.TINTED_GLASS)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        return path.contains("glass") || path.contains("window");
    }

    private static BlockState fullGlassOrDefault(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        if (path.contains("pane")) return Blocks.GLASS.defaultBlockState();
        return state;
    }

    /** Bounded safety check; protects named/CustomNPC entities and machines. */
    private static int annexScore(ServerLevel level, Placement p) {
        // Include the interior approach/tunnel as well as the complete external
        // gallery, stair and lower vestibule in the bounded safety box.
        BlockPos a = at(p.entrance(), p.right(), p.forward(), -7, -STATION_DROP - 3, -APPROACH_LENGTH - 2);
        BlockPos b = at(p.entrance(), p.right(), p.forward(), 7, 10, LOWER_VESTIBULE_END_F + 3);
        AABB box = new AABB(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1
        );

        int score = 0;
        for (Entity entity : level.getEntities((Entity) null, box, e -> !(e instanceof Player))) {
            if (isProtectedEntity(entity)) return PROTECTED_PENALTY;
            score += 25;
        }

        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.ceil(box.maxX) - 1;
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.ceil(box.maxY) - 1;
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.ceil(box.maxZ) - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) != null) score += BLOCK_ENTITY_PENALTY;
                }
            }
        }
        return score;
    }

    private static boolean isProtectedEntity(Entity entity) {
        if (entity.hasCustomName()) return true;
        String className = entity.getClass().getName().toLowerCase(Locale.ROOT);
        return className.contains("customnpc") || className.contains("customnpcs") || className.contains("npc");
    }

    /**
     * Open approach in the Dome interior. This remains visually part of the Dome,
     * so only terrain/vegetation is cleared and a clean floor is laid. The actual
     * enclosed metro tunnel begins farther forward, still inside the Dome shell.
     */
    private static void buildCleanInteriorApproach(ServerLevel level, Placement p) {
        BlockPos o = p.entrance();
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState edge = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState accent = Blocks.CYAN_CONCRETE.defaultBlockState();

        for (int f = -APPROACH_LENGTH; f < -INNER_TUNNEL_LENGTH; f++) {
            for (int r = -APPROACH_HALF_WIDTH; r <= APPROACH_HALF_WIDTH; r++) {
                for (int u = 0; u <= 5; u++) {
                    BlockPos pos = at(o, p.right(), p.forward(), r, u, f);
                    BlockState state = level.getBlockState(pos);
                    if (level.getBlockEntity(pos) == null && isNaturalObstruction(state)) {
                        set(level, pos, Blocks.AIR.defaultBlockState());
                    }
                }

                BlockPos floorPos = at(o, p.right(), p.forward(), r, -1, f);
                if (level.getBlockEntity(floorPos) == null) {
                    BlockState current = level.getBlockState(floorPos);
                    if (current.isAir() || isNaturalObstruction(current)) set(level, floorPos, floor);
                }
                BlockPos support = floorPos.below();
                if (level.getBlockEntity(support) == null && level.getBlockState(support).isAir()) {
                    set(level, support, edge);
                }
            }
            BlockPos center = at(o, p.right(), p.forward(), 0, -1, f);
            if (level.getBlockEntity(center) == null) set(level, center, accent);
        }
    }

    private static boolean isNaturalObstruction(BlockState state) {
        if (state.isAir()) return false;
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"minecraft".equals(key.getNamespace())) return false;
        String path = key.getPath().toLowerCase(Locale.ROOT);

        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.GRAVEL) || state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.CALCITE)) {
            return true;
        }

        return path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_leaves")
                || path.contains("grass") || path.contains("fern") || path.contains("flower")
                || path.contains("bush") || path.contains("cactus") || path.contains("roots");
    }

    /**
     * A seven-block-wide enclosed tunnel begins while the player is still inside
     * the original Dome. Its mouth is open to the Dome interior, but every external
     * face is solid; therefore the path reaches the shell before any descent starts.
     */
    private static void buildSealedInnerTunnel(ServerLevel level, Placement p) {
        BlockPos o = p.entrance();
        BlockState wall = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState trim = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState accent = Blocks.CYAN_CONCRETE.defaultBlockState();

        for (int f = -INNER_TUNNEL_LENGTH; f <= -1; f++) {
            // Controlled corridor volume. The placement check has already rejected
            // block entities/NPCs in this bounded region.
            fillLocal(level, o, p, -3, 0, f, 3, 5, f, Blocks.AIR.defaultBlockState());
            fillLocal(level, o, p, -3, -1, f, 3, -1, f, floor);
            set(level, at(o, p.right(), p.forward(), 0, -1, f), accent);

            for (int y = -1; y <= 6; y++) {
                set(level, at(o, p.right(), p.forward(), -4, y, f), (f % 4 == 0) ? trim : wall);
                set(level, at(o, p.right(), p.forward(), 4, y, f), (f % 4 == 0) ? trim : wall);
            }
            for (int r = -4; r <= 4; r++) {
                set(level, at(o, p.right(), p.forward(), r, 6, f), (f % 4 == 0) ? trim : wall);
            }
            if (f % 4 == 0) set(level, at(o, p.right(), p.forward(), 0, 5, f), light(13));
        }

        // Frame the mouth that opens to the Dome interior. It is intentionally not
        // capped: this is the public entrance, still safely inside the Dome.
        int mouthF = -INNER_TUNNEL_LENGTH;
        for (int y = 0; y <= 6; y++) {
            set(level, at(o, p.right(), p.forward(), -4, y, mouthF), trim);
            set(level, at(o, p.right(), p.forward(), 4, y, mouthF), trim);
        }
        for (int r = -4; r <= 4; r++) set(level, at(o, p.right(), p.forward(), r, 6, mouthF), trim);
    }

    /**
     * Fully sealed glass transfer gallery OUTSIDE the old Dome footprint. The old
     * shell is never opened into raw outside air: floor, sides and roof exist first.
     */
    private static void buildSealedTransferGallery(ServerLevel level, Placement p) {
        BlockPos o = p.entrance();
        BlockState glass = p.shellGlass();
        BlockState frame = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState accent = Blocks.CYAN_CONCRETE.defaultBlockState();

        for (int f = 0; f <= GALLERY_END_F; f++) {
            // Clear the whole usable gallery, including any terrain that protrudes
            // through the future extension.
            fillLocal(level, o, p, -3, 0, f, 3, 5, f, Blocks.AIR.defaultBlockState());
            fillLocal(level, o, p, -3, -1, f, 3, -1, f, floor);
            set(level, at(o, p.right(), p.forward(), 0, -1, f), accent);

            for (int y = -1; y <= 6; y++) {
                set(level, at(o, p.right(), p.forward(), -4, y, f), (f % 4 == 0) ? frame : glass);
                set(level, at(o, p.right(), p.forward(), 4, y, f), (f % 4 == 0) ? frame : glass);
            }
            for (int r = -4; r <= 4; r++) {
                set(level, at(o, p.right(), p.forward(), r, 6, f), (f % 4 == 0) ? frame : glass);
            }
            if (f % 4 == 0) set(level, at(o, p.right(), p.forward(), 0, 5, f), light(13));
        }

        // End wall between the level gallery and stair tube. Only the framed
        // five-wide doorway remains open, so there is no exposed side gap.
        int endF = GALLERY_END_F;
        for (int r = -4; r <= 4; r++) {
            for (int y = 0; y <= 5; y++) {
                boolean doorway = Math.abs(r) <= 2 && y <= 4;
                if (!doorway) set(level, at(o, p.right(), p.forward(), r, y, endF), frame);
            }
        }
        set(level, at(o, p.right(), p.forward(), -4, 3, endF), accent);
        set(level, at(o, p.right(), p.forward(), 4, 3, endF), accent);
    }

    /**
     * Open the original Dome shell only inside the already-sealed transfer volume.
     * This removes the old glass wall across the passage without creating a hole to
     * the exterior. A strong frame makes the old shell -> new annex merge explicit.
     */
    private static void buildSealedDomePortal(ServerLevel level, Placement p) {
        BlockPos o = p.entrance();
        BlockState frame = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState accent = Blocks.CYAN_CONCRETE.defaultBlockState();

        fillLocal(level, o, p, -3, 0, -1, 3, 5, 1, Blocks.AIR.defaultBlockState());
        for (int f = -1; f <= 1; f++) {
            for (int y = -1; y <= 6; y++) {
                set(level, at(o, p.right(), p.forward(), -4, y, f), frame);
                set(level, at(o, p.right(), p.forward(), 4, y, f), frame);
            }
            for (int r = -4; r <= 4; r++) set(level, at(o, p.right(), p.forward(), r, 6, f), frame);
        }
        set(level, at(o, p.right(), p.forward(), -4, 3, 0), accent);
        set(level, at(o, p.right(), p.forward(), 4, 3, 0), accent);
        set(level, at(o, p.right(), p.forward(), 0, 5, 0), light(15));
    }

    /**
     * Shallow continuous descent. Every one-block drop uses a real stair and is
     * followed by one flat tread, so the run is twice as long as the drop. This
     * eliminates the old cliff-like descent. The complete tube is opaque and sealed.
     */
    private static void buildShallowSealedStair(ServerLevel level, Placement p, int stationFloorY) {
        BlockPos o = p.entrance();
        BlockState stair = Blocks.POLISHED_ANDESITE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, p.forward().getOpposite());
        BlockState tread = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState frame = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState accent = Blocks.CYAN_CONCRETE.defaultBlockState();

        /*
         * V1.12.7 changes ONLY the walking geometry of the original V1.12 stair.
         *
         * Old V1.12 order:
         *   stair at the upper Y -> full tread at the same Y -> next level
         * That made the full block visually sit in front of / over the stair.
         *
         * Correct shallow pair:
         *   flat tread at current Y -> stair one block LOWER -> next flat level
         *
         * Everything else (walls, roof, lighting, station, gate, glass) stays V1.12.
         */
        int runIndex = 0;
        for (int drop = 0; drop < STAIR_DROP; drop++) {
            int upperFloorY = -1 - drop;

            for (int within = 0; within < STAIR_RUN_PER_DROP; within++) {
                int f = STAIR_START_F + runIndex;
                boolean stairSection = within == STAIR_RUN_PER_DROP - 1;
                int surfaceY = stairSection ? upperFloorY - 1 : upperFloorY;

                // Support follows the actual walking surface, so it never replaces
                // or visually covers the stair block.
                fillLocal(level, o, p, -3, surfaceY - 2, f, 3, surfaceY - 1, f, wall);

                // Keep the original V1.12 headroom relative to each walking section.
                fillLocal(level, o, p, -2, surfaceY + 1, f, 2, surfaceY + 5, f, Blocks.AIR.defaultBlockState());

                for (int r = -2; r <= 2; r++) {
                    set(level, at(o, p.right(), p.forward(), r, surfaceY, f),
                            stairSection ? stair : tread);
                }

                // Original V1.12 enclosure follows the actual surface height.
                for (int y = surfaceY - 1; y <= surfaceY + 6; y++) {
                    set(level, at(o, p.right(), p.forward(), -3, y, f), wall);
                    set(level, at(o, p.right(), p.forward(), 3, y, f), wall);
                }
                for (int r = -3; r <= 3; r++) {
                    set(level, at(o, p.right(), p.forward(), r, surfaceY + 6, f), frame);
                }

                if (runIndex % 4 == 0) {
                    set(level, at(o, p.right(), p.forward(), -3, surfaceY + 3, f), accent);
                    set(level, at(o, p.right(), p.forward(), 3, surfaceY + 3, f), accent);
                    set(level, at(o, p.right(), p.forward(), 0, surfaceY + 5, f), light(11));
                }
                runIndex++;
            }
        }

        int targetLocalY = stationFloorY - o.getY();
        int finalY = Math.max(targetLocalY, -STAIR_DROP);
        for (int f = LOWER_VESTIBULE_START_F; f <= LOWER_VESTIBULE_END_F; f++) {
            fillLocal(level, o, p, -3, finalY - 2, f, 3, finalY - 1, f, wall);
            fillLocal(level, o, p, -2, finalY + 1, f, 2, finalY + 5, f, Blocks.AIR.defaultBlockState());
            fillLocal(level, o, p, -2, finalY, f, 2, finalY, f, tread);
            for (int y = finalY - 1; y <= finalY + 6; y++) {
                set(level, at(o, p.right(), p.forward(), -3, y, f), wall);
                set(level, at(o, p.right(), p.forward(), 3, y, f), wall);
            }
            for (int r = -3; r <= 3; r++) {
                set(level, at(o, p.right(), p.forward(), r, finalY + 6, f), frame);
            }
        }
        set(level, at(o, p.right(), p.forward(), 0, finalY + 5, LOWER_VESTIBULE_START_F + 2), light(13));
    }

    /**
     * Final cleanup around the metro airlock and upper stair head.
     *
     * The real project gate is 5x5, while the corridor shell is wider/taller. To
     * avoid visible side/top gaps around the gate, we explicitly frame the portal
     * and clear stray ceiling blocks above the first stair section.
     */
    private static void finishAirlockAndStairSeals(ServerLevel level,
                                                   Placement p,
                                                   int gateForwardOffset,
                                                   BlockState frameState) {
        BlockPos o = p.entrance();
        BlockState stairWall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState roof = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState accent = Blocks.CYAN_CONCRETE.defaultBlockState();

        // Frame the real 5x5 gate into the full tunnel aperture.
        for (int y = -1; y <= 6; y++) {
            set(level, at(o, p.right(), p.forward(), -3, y, gateForwardOffset), frameState);
            set(level, at(o, p.right(), p.forward(), 3, y, gateForwardOffset), frameState);
        }
        for (int r = -3; r <= 3; r++) {
            set(level, at(o, p.right(), p.forward(), r, 5, gateForwardOffset), frameState);
            set(level, at(o, p.right(), p.forward(), r, 6, gateForwardOffset), frameState);
        }

        // Keep the passage around the gate clean on both sides.
        for (int f = gateForwardOffset - 1; f <= gateForwardOffset + 1; f++) {
            fillLocal(level, o, p, -2, 0, f, 2, 4, f, Blocks.AIR.defaultBlockState());
            fillLocal(level, o, p, -2, -1, f, 2, -1, f, floor);
            set(level, at(o, p.right(), p.forward(), 0, -1, f), accent);
        }

        // Clean the top of the stair head so no blocks hang above the steps,
        // while preserving a sealed corridor shell.
        for (int f = GALLERY_END_F - 1; f <= STAIR_START_F + 1; f++) {
            fillLocal(level, o, p, -2, 0, f, 2, 5, f, Blocks.AIR.defaultBlockState());
            fillLocal(level, o, p, -2, -1, f, 2, -1, f, floor);
            for (int y = -1; y <= 6; y++) {
                set(level, at(o, p.right(), p.forward(), -3, y, f), stairWall);
                set(level, at(o, p.right(), p.forward(), 3, y, f), stairWall);
            }
            for (int r = -3; r <= 3; r++) {
                set(level, at(o, p.right(), p.forward(), r, 6, f), roof);
            }
        }
        set(level, at(o, p.right(), p.forward(), 0, 5, GALLERY_END_F), light(13));
        set(level, at(o, p.right(), p.forward(), 0, 5, STAIR_START_F + 1), light(12));
    }

    private static void buildCentralHall(ServerLevel level, Placement p, BlockPos center) {
        int y = center.getY();
        fillAround(level, center, p, -19, 1, -9, 19, 8, 8, Blocks.AIR.defaultBlockState());
        fillAround(level, center, p, -20, 0, -10, 20, 0, 9, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fillAround(level, center, p, -20, 9, -10, 20, 9, 9, Blocks.POLISHED_DEEPSLATE.defaultBlockState());

        for (int r = -20; r <= 20; r++) {
            for (int yy = 1; yy <= 8; yy++) {
                set(level, around(center, p, r, yy, -10), wallState(r, y + yy));
                set(level, around(center, p, r, yy, 9), wallState(r, y + yy));
            }
        }
        for (int f = -10; f <= 9; f++) {
            for (int yy = 1; yy <= 8; yy++) {
                set(level, around(center, p, -20, yy, f), wallState(f, y + yy));
                set(level, around(center, p, 20, yy, f), wallState(f, y + yy));
            }
        }

        int partitionF = -4;
        for (int r = -19; r <= 19; r++) {
            for (int yy = 1; yy <= 6; yy++) set(level, around(center, p, r, yy, partitionF), Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
        fillAround(level, center, p, -4, 1, partitionF, 4, 5, partitionF, Blocks.AIR.defaultBlockState());
        for (int r = -8; r <= 8; r++) set(level, around(center, p, r, 4, partitionF), Blocks.CYAN_CONCRETE.defaultBlockState());

        for (int r = -18; r <= 18; r++) {
            fillAround(level, center, p, r, 1, -3, r, 1, -1, Blocks.SMOOTH_STONE.defaultBlockState());
            fillAround(level, center, p, r, 1, 0, r, 1, 4, Blocks.GRAVEL.defaultBlockState());
            fillAround(level, center, p, r, 1, 5, r, 1, 7, Blocks.SMOOTH_STONE.defaultBlockState());
            set(level, around(center, p, r, 2, 1), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
            set(level, around(center, p, r, 2, 3), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
        }

        for (int r = -16; r <= 16; r += 8) {
            for (int yy = 2; yy <= 8; yy++) {
                set(level, around(center, p, r, yy, -2), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                set(level, around(center, p, r, yy, 6), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
            set(level, around(center, p, r, 5, -2), Blocks.CYAN_CONCRETE.defaultBlockState());
            set(level, around(center, p, r, 5, 6), Blocks.CYAN_CONCRETE.defaultBlockState());
        }

        buildTunnel(level, p, center, -32, -21);
        buildTunnel(level, p, center, 21, 32);
        sealTunnelEnd(level, p, center, -33);
        sealTunnelEnd(level, p, center, 33);
        fillAround(level, center, p, -20, 1, -1, -20, 7, 5, Blocks.AIR.defaultBlockState());
        fillAround(level, center, p, 20, 1, -1, 20, 7, 5, Blocks.AIR.defaultBlockState());
    }

    private static void buildBottomConnection(ServerLevel level, Placement p, int stationFloorY, BlockPos center) {
        BlockPos o = p.entrance();
        int localY = stationFloorY - o.getY();
        int hallNearForward = STATION_FORWARD - 10;
        int fromF = LOWER_VESTIBULE_END_F;
        int toF = hallNearForward;
        int minF = Math.min(fromF, toF);
        int maxF = Math.max(fromF, toF);
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState roof = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState floor = Blocks.DEEPSLATE_TILES.defaultBlockState();

        for (int f = minF; f <= maxF; f++) {
            fillLocal(level, o, p, -2, localY + 1, f, 2, localY + 5, f, Blocks.AIR.defaultBlockState());
            fillLocal(level, o, p, -2, localY, f, 2, localY, f, floor);
            for (int y = localY; y <= localY + 6; y++) {
                set(level, at(o, p.right(), p.forward(), -3, y, f), wall);
                set(level, at(o, p.right(), p.forward(), 3, y, f), wall);
            }
            for (int r = -3; r <= 3; r++) set(level, at(o, p.right(), p.forward(), r, localY + 6, f), roof);
            if ((f - minF) % 4 == 0) set(level, at(o, p.right(), p.forward(), 0, localY + 5, f), light(11));
        }

        // Open only the intentional doorway into the already sealed central hall.
        fillAround(level, center, p, -2, 1, -10, 2, 5, -10, Blocks.AIR.defaultBlockState());
    }

    private static int forwardDistance(BlockPos origin, Direction forward, BlockPos target) {
        return switch (forward) {
            case NORTH -> origin.getZ() - target.getZ();
            case SOUTH -> target.getZ() - origin.getZ();
            case WEST -> origin.getX() - target.getX();
            case EAST -> target.getX() - origin.getX();
            default -> 0;
        };
    }

    private static void buildTunnel(ServerLevel level, Placement p, BlockPos center, int fromR, int toR) {
        int minR = Math.min(fromR, toR);
        int maxR = Math.max(fromR, toR);
        for (int r = minR; r <= maxR; r++) {
            fillAround(level, center, p, r, 1, 0, r, 7, 4, Blocks.AIR.defaultBlockState());
            fillAround(level, center, p, r, 0, -1, r, 0, 5, Blocks.DEEPSLATE_TILES.defaultBlockState());
            fillAround(level, center, p, r, 8, -1, r, 8, 5, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            for (int yy = 1; yy <= 7; yy++) {
                set(level, around(center, p, r, yy, -1), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                set(level, around(center, p, r, yy, 5), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
            fillAround(level, center, p, r, 1, 0, r, 1, 4, Blocks.GRAVEL.defaultBlockState());
            set(level, around(center, p, r, 2, 1), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
            set(level, around(center, p, r, 2, 3), Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState());
        }
    }

    /** Stage 5 will connect these stubs. Until then they must never expose outside. */
    private static void sealTunnelEnd(ServerLevel level, Placement p, BlockPos center, int r) {
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState frame = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState accent = Blocks.CYAN_CONCRETE.defaultBlockState();
        fillAround(level, center, p, r, 1, -1, r, 7, 5, wall);
        fillAround(level, center, p, r, 0, -1, r, 0, 5, frame);
        fillAround(level, center, p, r, 8, -1, r, 8, 5, frame);
        set(level, around(center, p, r, 4, 2), accent);

        // V1.8: never replace a cap wall block with a LIGHT block. LIGHT is
        // transparent/non-solid and was the last one-block exterior hole on each
        // rail stub. Put the light one block INSIDE the sealed tunnel instead.
        int insideR = r > 0 ? r - 1 : r + 1;
        set(level, around(center, p, insideR, 7, 2), light(10));
    }

    private static BlockPos buildTrain(ServerLevel level, Placement p, BlockPos center) {
        for (int r = -8; r <= 8; r++) {
            for (int f = 1; f <= 3; f++) {
                set(level, around(center, p, r, 2, f), Blocks.POLISHED_ANDESITE.defaultBlockState());
                set(level, around(center, p, r, 6, f), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }
        }
        for (int r = -8; r <= 8; r++) {
            for (int yy = 3; yy <= 5; yy++) {
                set(level, around(center, p, r, yy, 1), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                set(level, around(center, p, r, yy, 3), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }
        }
        for (int yy = 3; yy <= 5; yy++) {
            for (int f = 1; f <= 3; f++) {
                set(level, around(center, p, -8, yy, f), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                set(level, around(center, p, 8, yy, f), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }
        }

        fillAround(level, center, p, -7, 3, 2, 7, 5, 2, Blocks.AIR.defaultBlockState());
        for (int r = -6; r <= 6; r += 4) {
            set(level, around(center, p, r, 4, 1), Blocks.GLASS_PANE.defaultBlockState());
            set(level, around(center, p, r, 4, 3), Blocks.GLASS_PANE.defaultBlockState());
        }
        fillAround(level, center, p, -1, 3, 1, 1, 5, 1, Blocks.AIR.defaultBlockState());

        // Keep the original in-train service console for visual continuity.
        BlockPos trainConsole = around(center, p, 6, 3, 2);
        set(level, trainConsole, MetroRegistry.METRO_TRAIN_CONSOLE.get().defaultBlockState());
        set(level, around(center, p, 6, 4, 2), Blocks.CYAN_CONCRETE.defaultBlockState());

        // Stage 7.1: add a clearly accessible passenger console on the near
        // platform. It sits on the authored smooth-stone strip (F=-2), outside
        // the train body and away from the R=0/8 structural pillars.
        BlockPos passengerConsole = around(center, p, 6, 2, -2);
        set(level, passengerConsole, MetroRegistry.METRO_TRAIN_CONSOLE.get().defaultBlockState());

        // Persist the accessible passenger console as the canonical position for
        // newly built worlds. Older worlds are migrated by MetroNetworkService.
        return passengerConsole;
    }

    private static void buildLighting(ServerLevel level, Placement p, BlockPos center) {
        for (int r = -16; r <= 16; r += 8) {
            set(level, around(center, p, r, 8, -2), light(15));
            set(level, around(center, p, r, 8, 6), light(15));
        }
        for (int f = -8; f <= -5; f += 3) set(level, around(center, p, 0, 7, f), light(15));
    }

    /**
     * Find the actual unbreakable structural block already used by this Dome.
     * Bedrock/barrier/debug blocks and gate/panel blocks are excluded. The scan
     * is bounded and only runs once when the central metro is constructed.
     */
    private static BlockState detectDomeStructuralBlock(ServerLevel level, BlockPos domeAnchor, Placement placement) {
        java.util.Map<Block, Integer> counts = new java.util.HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // First inspect the actual outer shell selected for the metro. Blocks that
        // physically frame the Dome glass are the best visual/material match even
        // when their unbreakability is provided by Dome protection logic rather
        // than destroySpeed == -1.
        BlockPos edge = placement.entrance();
        for (int x = edge.getX() - 10; x <= edge.getX() + 10; x++) {
            for (int z = edge.getZ() - 10; z <= edge.getZ() + 10; z++) {
                for (int y = Math.max(level.getMinBuildHeight() + 2, edge.getY() - 3);
                     y <= Math.min(level.getMaxBuildHeight() - 3, edge.getY() + 12); y++) {
                    pos.set(x, y, z);
                    scoreStructuralCandidate(level, pos, counts, true);
                }
            }
        }

        // Broader bounded Dome scan is only a fallback/tie breaker.
        for (int x = domeAnchor.getX() - 48; x <= domeAnchor.getX() + 48; x++) {
            for (int z = domeAnchor.getZ() - 48; z <= domeAnchor.getZ() + 48; z++) {
                for (int y = Math.max(level.getMinBuildHeight() + 2, domeAnchor.getY() - 8);
                     y <= Math.min(level.getMaxBuildHeight() - 3, domeAnchor.getY() + 16); y++) {
                    pos.set(x, y, z);
                    scoreStructuralCandidate(level, pos, counts, false);
                }
            }
        }

        Block best = null;
        int bestScore = Integer.MIN_VALUE;
        for (var e : counts.entrySet()) {
            if (e.getValue() > bestScore) {
                best = e.getKey();
                bestScore = e.getValue();
            }
        }

        // Never abort the entire station because a particular Dome build protects
        // its walls through events instead of negative hardness. The metro region
        // itself is protected by DomeMetroProtectionEvents, so this fallback is
        // still non-breakable in survival while keeping construction recoverable.
        return best == null ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : best.defaultBlockState();
    }

    private static void scoreStructuralCandidate(ServerLevel level, BlockPos pos,
                                                 java.util.Map<Block, Integer> counts,
                                                 boolean edgePass) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.blocksMotion() || isDomeShell(state)) return;
        if (level.getBlockEntity(pos) != null) return;

        Block block = state.getBlock();
        if (block == Blocks.BEDROCK || block == Blocks.BARRIER
                || block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK || block == Blocks.STRUCTURE_BLOCK
                || block == Blocks.JIGSAW || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK
                || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL
                || block == Blocks.CLAY || block == Blocks.WATER || block == Blocks.LAVA) return;

        var key = BuiltInRegistries.BLOCK.getKey(block);
        String ns = key.getNamespace().toLowerCase(Locale.ROOT);
        String path = key.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("gate") || path.contains("door") || path.contains("panel")
                || path.contains("console") || path.contains("controller")
                || path.contains("lamp") || path.contains("light") || path.contains("window")
                || path.contains("stairs") || path.contains("slab") || path.contains("fence")) return;

        int weight = edgePass ? 4 : 1;
        if ("domesurvival".equals(ns)) weight += 10;
        if (path.contains("dome") || path.contains("reinforced") || path.contains("struct")
                || path.contains("wall") || path.contains("frame") || path.contains("hull")) weight += 8;
        if (state.getDestroySpeed(level, pos) < 0.0F) weight += 18;

        // Strongly prefer blocks directly touching the glass/window shell.
        for (Direction d : Direction.values()) {
            if (isDomeShell(level.getBlockState(pos.relative(d)))) {
                weight += 24;
                break;
            }
        }
        counts.merge(block, weight, Integer::sum);
    }

    /**
     * Reuse a visible light-emitting block from the Dome when possible. If the
     * Dome uses invisible/technical lighting only, sea lantern is a safe visible
     * fallback; hidden LIGHT blocks remain in place too.
     */
    private static BlockState detectDomeLamp(ServerLevel level, BlockPos domeAnchor) {
        java.util.Map<Block, Integer> counts = new java.util.HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = domeAnchor.getX() - 40; x <= domeAnchor.getX() + 40; x++) {
            for (int z = domeAnchor.getZ() - 40; z <= domeAnchor.getZ() + 40; z++) {
                for (int y = Math.max(level.getMinBuildHeight() + 2, domeAnchor.getY() - 6);
                     y <= Math.min(level.getMaxBuildHeight() - 3, domeAnchor.getY() + 14); y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getLightEmission() <= 0 || !state.blocksMotion()) continue;
                    if (state.is(Blocks.LIGHT) || level.getBlockEntity(pos) != null) continue;
                    var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    String path = key.getPath().toLowerCase(Locale.ROOT);
                    if (path.contains("fire") || path.contains("lava")) continue;
                    int weight = 1 + ("domesurvival".equals(key.getNamespace()) ? 6 : 0);
                    if (path.contains("lamp") || path.contains("light") || path.contains("ceiling")) weight += 4;
                    counts.merge(state.getBlock(), weight, Integer::sum);
                }
            }
        }
        Block best = null;
        int score = Integer.MIN_VALUE;
        for (var e : counts.entrySet()) {
            if (e.getValue() > score) {
                best = e.getKey();
                score = e.getValue();
            }
        }
        return best == null ? Blocks.SEA_LANTERN.defaultBlockState() : best.defaultBlockState();
    }

    private static boolean isBuilderStructural(BlockState state) {
        return state.is(Blocks.SMOOTH_STONE)
                || state.is(Blocks.POLISHED_ANDESITE)
                || state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.POLISHED_DEEPSLATE)
                || state.is(Blocks.DEEPSLATE_TILES);
    }

    /**
     * Replace the builder's temporary vanilla structural palette with the real
     * unbreakable Dome material. Only known structural blocks are replaced, so
     * stairs, windows, lights, consoles, train details and airlock hardware survive.
     */
    private static void reinforceBuiltMetro(ServerLevel level, Placement p, BlockPos stationCenter, BlockState domeStructural) {
        BlockPos o = p.entrance();
        int minLocalY = stationCenter.getY() - o.getY() - 3;
        for (int f = -INNER_TUNNEL_LENGTH - 2; f <= STATION_FORWARD + 2; f++) {
            for (int r = -5; r <= 5; r++) {
                for (int u = minLocalY; u <= 8; u++) {
                    BlockPos pos = at(o, p.right(), p.forward(), r, u, f);
                    BlockState state = level.getBlockState(pos);
                    if (isBuilderStructural(state) && level.getBlockEntity(pos) == null) {
                        set(level, pos, domeStructural);
                    }
                }
            }
        }

        for (int r = -34; r <= 34; r++) {
            for (int f = -11; f <= 10; f++) {
                for (int u = -2; u <= 10; u++) {
                    BlockPos pos = around(stationCenter, p, r, u, f);
                    BlockState state = level.getBlockState(pos);
                    if (isBuilderStructural(state) && level.getBlockEntity(pos) == null) {
                        set(level, pos, domeStructural);
                    }
                }
            }
        }
    }

    /** Visible, regular lighting throughout the approach, stair, hall and rail stubs. */
    private static void installVisibleLighting(ServerLevel level, Placement p, BlockPos center, BlockState lamp) {
        BlockPos o = p.entrance();

        // Corridor fixtures are flush in the side walls: no hanging blocks over the path.
        for (int f = -INNER_TUNNEL_LENGTH + 2; f <= GALLERY_END_F; f += 4) {
            set(level, at(o, p.right(), p.forward(), -4, 3, f), lamp);
            set(level, at(o, p.right(), p.forward(), 4, 3, f), lamp);
        }

        // Stair lighting is wall-mounted, keeping the full headroom clear.
        for (int f = STAIR_START_F; f <= STAIR_END_F; f += 4) {
            int run = f - STAIR_START_F;
            int drop = Math.min(STAIR_DROP - 1, run / STAIR_RUN_PER_DROP);
            int floorY = -1 - drop;
            set(level, at(o, p.right(), p.forward(), -3, floorY + 3, f), lamp);
            set(level, at(o, p.right(), p.forward(), 3, floorY + 3, f), lamp);
        }

        int lowerY = center.getY() - o.getY();
        for (int f = LOWER_VESTIBULE_START_F; f <= STATION_FORWARD - 10; f += 4) {
            set(level, at(o, p.right(), p.forward(), -3, lowerY + 3, f), lamp);
            set(level, at(o, p.right(), p.forward(), 3, lowerY + 3, f), lamp);
        }

        for (int r = -16; r <= 16; r += 8) {
            set(level, around(center, p, r, 8, -7), lamp);
            set(level, around(center, p, r, 8, 7), lamp);
        }
        for (int r = -30; r <= -22; r += 4) set(level, around(center, p, r, 7, 2), lamp);
        for (int r = 22; r <= 30; r += 4) set(level, around(center, p, r, 7, 2), lamp);
    }

    private static BlockState wallState(int coordinate, int y) {
        if (y % 5 == 0) return Blocks.CYAN_CONCRETE.defaultBlockState();
        return (coordinate & 3) == 0
                ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    private static BlockState light(int level) {
        return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level);
    }

    private static BlockPos at(BlockPos origin, Direction right, Direction forward, int r, int up, int f) {
        return origin.relative(right, r).relative(forward, f).above(up);
    }

    private static BlockPos around(BlockPos center, Placement p, int r, int up, int f) {
        return at(center, p.right(), p.forward(), r, up, f);
    }

    private static void fillLocal(ServerLevel level, BlockPos origin, Placement p,
                                  int r1, int u1, int f1, int r2, int u2, int f2, BlockState state) {
        fillOriented(level, origin, p.right(), p.forward(), r1, u1, f1, r2, u2, f2, state);
    }

    private static void fillAround(ServerLevel level, BlockPos center, Placement p,
                                   int r1, int u1, int f1, int r2, int u2, int f2, BlockState state) {
        fillOriented(level, center, p.right(), p.forward(), r1, u1, f1, r2, u2, f2, state);
    }

    private static void fillOriented(ServerLevel level, BlockPos origin, Direction right, Direction forward,
                                     int r1, int u1, int f1, int r2, int u2, int f2, BlockState state) {
        int minR = Math.min(r1, r2), maxR = Math.max(r1, r2);
        int minU = Math.min(u1, u2), maxU = Math.max(u1, u2);
        int minF = Math.min(f1, f2), maxF = Math.max(f1, f2);
        for (int r = minR; r <= maxR; r++) {
            for (int u = minU; u <= maxU; u++) {
                for (int f = minF; f <= maxF; f++) set(level, at(origin, right, forward, r, u, f), state);
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return;
        level.setBlock(pos, state, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
    }

    private record Placement(BlockPos entrance, Direction forward, Direction right, BlockState shellGlass) {
    }

    private record EdgeHit(BlockPos shellPos, BlockState shellState, int groundY, int distance) {
    }

    public record BuildResult(BlockPos entrance, BlockPos stationAnchor, BlockPos trainConsolePos, Direction forward) {
    }
}
