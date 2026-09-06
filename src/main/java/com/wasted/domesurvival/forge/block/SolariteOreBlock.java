package com.wasted.domesurvival.forge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

public class SolariteOreBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final DustParticleOptions GOLD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.0F);

    public SolariteOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        activate(state, level, pos);
        super.attack(state, level, pos, player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        activate(state, level, pos);
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        activate(state, level, pos);
        super.stepOn(level, pos, state, entity);
    }

    private static void activate(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide) {
            spawnGoldenParticles(level, pos);
            return;
        }

        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        }

        // Vanilla-redstone-style delayed shutoff: 30 game ticks = 1.5 seconds.
        level.scheduleTick(pos, state.getBlock(), 30);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            spawnGoldenParticles(level, pos);
        }
    }

    private static void spawnGoldenParticles(Level level, BlockPos pos) {
        RandomSource random = level.random;

        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);

            if (level.getBlockState(adjacent).isSolidRender(level, adjacent)) {
                continue;
            }

            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double offset = 0.0625D;

            switch (direction) {
                case DOWN -> y = pos.getY() - offset;
                case UP -> y = pos.getY() + 1.0D + offset;
                case NORTH -> z = pos.getZ() - offset;
                case SOUTH -> z = pos.getZ() + 1.0D + offset;
                case WEST -> x = pos.getX() - offset;
                case EAST -> x = pos.getX() + 1.0D + offset;
            }

            level.addParticle(GOLD_DUST, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}