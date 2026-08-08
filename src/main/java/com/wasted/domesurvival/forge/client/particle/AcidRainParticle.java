package com.wasted.domesurvival.forge.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import javax.annotation.Nullable;

/**
 * Small, fast acid-rain streak. The custom texture is elongated, so a tiny quad still reads as rain
 * instead of the large square dust particles used by the earlier prototype.
 */
public final class AcidRainParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private AcidRainParticle(ClientLevel level,
                             double x, double y, double z,
                             double xd, double yd, double zd,
                             SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 0.99F;
        this.hasPhysics = true;
        this.lifetime = 12 + level.random.nextInt(9);
        this.quadSize = 0.14F + level.random.nextFloat() * 0.055F;
        this.alpha = 0.84F + level.random.nextFloat() * 0.14F;
        this.rCol = 0.70F;
        this.gCol = 1.00F;
        this.bCol = 0.16F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround) {
            this.remove();
        }
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
            return new AcidRainParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
