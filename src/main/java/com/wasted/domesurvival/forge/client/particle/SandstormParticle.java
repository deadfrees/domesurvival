package com.wasted.domesurvival.forge.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import javax.annotation.Nullable;

/** Fine orange sand mote with fast horizontal travel and small turbulent drift. */
public final class SandstormParticle extends TextureSheetParticle {
    private final double baseXSpeed;
    private final double baseZSpeed;

    private SandstormParticle(ClientLevel level,
                              double x, double y, double z,
                              double xd, double yd, double zd,
                              SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.baseXSpeed = xd;
        this.baseZSpeed = zd;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 0.985F;
        this.hasPhysics = false;
        this.lifetime = 24 + level.random.nextInt(20);
        this.quadSize = 0.075F + level.random.nextFloat() * 0.045F;
        this.alpha = 0.52F + level.random.nextFloat() * 0.26F;

        // Warm orange/gold variation without requiring multiple registered particle types.
        float warm = level.random.nextFloat();
        this.rCol = 0.92F + warm * 0.08F;
        this.gCol = 0.43F + warm * 0.24F;
        this.bCol = 0.08F + warm * 0.08F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }

        // Small oscillation gives the cloud a turbulent, wind-blown look instead of straight lines.
        double phase = (this.age * 0.47D) + (this.xo + this.zo) * 0.05D;
        this.xd += Math.sin(phase) * 0.006D;
        this.zd += Math.cos(phase * 0.83D) * 0.006D;
        this.yd += Math.sin(phase * 0.61D) * 0.0025D;

        // Keep the main motion strongly aligned to the wind even after turbulence is applied.
        this.xd += (baseXSpeed - this.xd) * 0.025D;
        this.zd += (baseZSpeed - this.zd) * 0.025D;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new SandstormParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
