package com.wasted.domesurvival.forge.client.particle;

import com.wasted.domesurvival.forge.machine.sieve.SandSieveBlock;
import com.wasted.domesurvival.forge.machine.sieve.SandSieveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Client ticker only: small grains fall through the fixed mesh and vanish on impact. */
public final class SandSieveParticles {
    private SandSieveParticles() { }

    public static void tick(Level level, BlockPos pos, BlockState state, SandSieveBlockEntity sieve) {
        if (!(level instanceof ClientLevel clientLevel) || !state.getValue(SandSieveBlock.ACTIVE)
                || level.getGameTime() % 2L != 0L) return;
        Minecraft minecraft = Minecraft.getInstance();
        var camera = minecraft.getCameraEntity();
        ParticleStatus setting = minecraft.options.particles().get();
        if (camera == null || setting == ParticleStatus.MINIMAL
                || camera.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 1024D) return;

        int count = setting == ParticleStatus.DECREASED ? 1 : 3;
        for (int i = 0; i < count; i++) {
            minecraft.particleEngine.add(new SandGrainParticle(clientLevel,
                    pos.getX() + 0.27D + level.random.nextDouble() * 0.46D,
                    pos.getY() + 0.62D,
                    pos.getZ() + 0.27D + level.random.nextDouble() * 0.46D));
        }
    }

    private static final class SandGrainParticle extends TerrainParticle {
        private SandGrainParticle(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z, 0, 0, 0, Blocks.SAND.defaultBlockState());
            quadSize = 0.022F + random.nextFloat() * 0.01F;
            setSize(0.025F, 0.025F);
            xd = (random.nextDouble() - 0.5D) * 0.008D;
            yd = -0.025D;
            zd = (random.nextDouble() - 0.5D) * 0.008D;
            gravity = 0.32F;
            lifetime = 16;
            rCol = gCol = bCol = 0.9F;
        }

        @Override
        public void tick() {
            super.tick();
            if (onGround) remove();
        }
    }
}
