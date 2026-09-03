package com.wasted.domesurvival.forge.client.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Client-only streamed music instance with deterministic fade-in/fade-out.
 */
public final class DomeMusicSoundInstance extends AbstractTickableSoundInstance {
    private float targetVolume;
    private float fadeStep;
    private boolean stopAfterFade;

    public DomeMusicSoundInstance(SoundEvent event, float initialVolume, float targetVolume, int fadeTicks, boolean loop) {
        super(event, SoundSource.MUSIC, RandomSource.create());
        this.volume = Math.max(0.0F, initialVolume);
        this.targetVolume = Math.max(0.0F, targetVolume);
        this.fadeStep = calculateStep(this.volume, this.targetVolume, fadeTicks);
        this.looping = loop;
        this.delay = 0;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    public void fadeTo(float newTargetVolume, int fadeTicks, boolean stopAfterFade) {
        this.targetVolume = Math.max(0.0F, newTargetVolume);
        this.fadeStep = calculateStep(this.volume, this.targetVolume, fadeTicks);
        this.stopAfterFade = stopAfterFade;

        if (fadeTicks <= 0) {
            this.volume = this.targetVolume;
            if (this.stopAfterFade && this.volume <= 0.0001F) {
                stop();
            }
        }
    }

    @Override
    public void tick() {
        if (isStopped()) {
            return;
        }

        if (Math.abs(volume - targetVolume) <= 0.0001F) {
            volume = targetVolume;
            if (stopAfterFade && volume <= 0.0001F) {
                stop();
            }
            return;
        }

        if (volume < targetVolume) {
            volume = Math.min(targetVolume, volume + fadeStep);
        } else {
            volume = Math.max(targetVolume, volume - fadeStep);
        }

        if (stopAfterFade && volume <= 0.0001F) {
            volume = 0.0F;
            stop();
        }
    }

    private static float calculateStep(float from, float to, int fadeTicks) {
        if (fadeTicks <= 0) {
            return Math.abs(to - from);
        }
        return Math.max(0.0001F, Math.abs(to - from) / (float) fadeTicks);
    }
}
