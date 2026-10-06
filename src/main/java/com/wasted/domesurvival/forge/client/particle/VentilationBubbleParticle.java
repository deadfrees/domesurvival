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
 * A light plume of oxygen vapour, emitted locally from the filler's diffuser.
 */
public final class VentilationBubbleParticle extends TextureSheetParticle {
    private static final float START_SIZE = 0.07F;
    private static final float END_SIZE = 0.38F;
    private static final float MAX_ALPHA = 0.24F;
    private float previousAlpha;

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
        this.hasPhysics = true;
        this.lifetime = 42 + random.nextInt(18);
        this.quadSize = START_SIZE;
        this.alpha = 0.0F;
        this.rCol = 0.78F;
        this.gCol = 0.80F;
        this.bCol = 0.81F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        previousAlpha = alpha;
        super.tick();
        if (!isAlive()) return;

        float progress = Math.min(1.0F, (float) age / (float) lifetime);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        this.quadSize = START_SIZE + (END_SIZE - START_SIZE) * eased;

        float fadeIn = Math.min(1.0F, progress / 0.16F);
        float fadeOut = 1.0F - Math.max(0.0F, (progress - 0.58F) / 0.42F);
        this.alpha = MAX_ALPHA * fadeIn * fadeOut;

        // Very small drift prevents the pulse from looking like a static GUI decal in-world.
        this.xd *= 0.99D;
        this.zd *= 0.99D;
        this.yd = Math.min(0.024D, this.yd + 0.00015D);
        this.setSpriteFromAge(sprites);
    }

    @Override public float getQuadSize(float partialTick) {
        float t=Math.min(1,(age+partialTick)/(float)lifetime);
        return START_SIZE+(END_SIZE-START_SIZE)*(1-(1-t)*(1-t));
    }
    @Override public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer,net.minecraft.client.Camera camera,float partialTick) {
        float current=alpha;alpha=previousAlpha+(current-previousAlpha)*partialTick;
        super.render(buffer,camera,partialTick);alpha=current;
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
