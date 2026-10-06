package com.wasted.domesurvival.forge.metro.worldgen;

import com.mojang.serialization.Codec;
import com.wasted.domesurvival.forge.metro.MetroRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Native multi-chunk Stage-2 metro structure.
 *
 * Unlike the old placed Feature, this is scheduled by Minecraft as a real Structure.
 * Its StructurePiece is processed once per intersecting chunk, so it never writes to
 * a far chunk that is still at an incompatible world-generation status.
 */
public final class AbandonedMetroStructure extends Structure {
    public static final Codec<AbandonedMetroStructure> CODEC = simpleCodec(AbandonedMetroStructure::new);

    public AbandonedMetroStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int surfaceX = context.chunkPos().getMinBlockX() + 8;
        int surfaceZ = context.chunkPos().getMinBlockZ() + 8;
        int surfaceY = context.chunkGenerator().getFirstFreeHeight(
                surfaceX,
                surfaceZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );

        if (surfaceY + 6 >= context.heightAccessor().getMaxBuildHeight()) {
            return Optional.empty();
        }

        NoiseColumn column = context.chunkGenerator().getBaseColumn(
                surfaceX,
                surfaceZ,
                context.heightAccessor(),
                context.randomState()
        );
        BlockState surfaceState = column.getBlock(surfaceY - 1);
        if (!surfaceState.getFluidState().isEmpty()) {
            return Optional.empty();
        }

        int depth = 18 + context.random().nextInt(3);
        int baseY = surfaceY - depth;
        if (baseY < context.heightAccessor().getMinBuildHeight() + 6) {
            return Optional.empty();
        }

        BlockPos surface = new BlockPos(surfaceX, surfaceY, surfaceZ);
        long layoutSeed = context.seed()
                ^ ((long) surfaceX * 341873128712L)
                ^ ((long) surfaceZ * 132897987541L);

        return Optional.of(new GenerationStub(
                surface,
                builder -> builder.addPiece(new AbandonedMetroStructurePiece(surface, depth, layoutSeed))
        ));
    }

    @Override
    public StructureType<?> type() {
        return MetroRegistry.ABANDONED_METRO_STRUCTURE.get();
    }
}
