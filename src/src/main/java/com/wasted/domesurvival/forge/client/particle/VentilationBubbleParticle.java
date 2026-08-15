package com.wasted.domesurvival.forge.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * A restrained oxygen-vent pulse: a translucent membrane that grows from the top of the machine
 * and dissolves as it rises. It is intentionally client-local to avoid particle network traffic.
 */
public final class VentilationBubbleParticle extends TextureSheetParticle {
    private static final float START_SIZE = 0.16F;
    private static final float END_SIZE = 0.82F;
    private static final float MAX_ALPHA = 0.34F;

    private final SpriteSet sprites;

    private VentilationBubbleParticle(ClientLevel level,
                                      double x, double y, double z,
                                      double xd, double yd, double zd,
                                      SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 0.985F;
        this.hasPhysics = false;
        this.lifetime = 28;
        this.quadSize = START_SIZE;
        this.alpha = 0.0F;
        this.rCol = 0.70F;
        this.gCol = 0.90F;
        this.bCol = 0.98F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) return;

        float progress = Math.min(1.0F, (float) age / (float) lifetime);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        this.quadSize = START_SIZE + (END_SIZE - START_SIZE) * eased;

        float fadeIn = Math.min(1.0F, progress / 0.16F);
        float fadeOut = 1.0F - Math.max(0.0F, (progress - 0.58F) / 0.42F);
        this.alpha = MAX_ALPHA * fadeIn * fadeOut;

        // Very small drift prevents the pulse from looking like a static GUI decal in-world.
        this.xd *= 0.96D;
        this.zd *= 0.96D;
        this.yd = Math.min(0.022D, this.yd + 0.00035D);
        this.setSpriteFromAge(sprites);
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
            return new VentilationBubbleParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
