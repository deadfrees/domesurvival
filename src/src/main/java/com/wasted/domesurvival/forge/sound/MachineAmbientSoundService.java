package com.wasted.domesurvival.forge.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server-authoritative, low-overhead machine ambience.
 *
 * <p>The supplied loop assets are exactly four seconds long. We trigger one sound every
 * 80 active ticks instead of creating a sound every tick. This keeps network/audio work
 * tiny, starts immediately when a machine begins working, and stops naturally within at
 * most one four-second loop after the machine becomes inactive.</p>
 */
public final class MachineAmbientSoundService {
    public static final int LOOP_TICKS = 80;

    public enum MachineType {
        COAL_GENERATOR(MachineSoundEvents.COAL_GENERATOR_LOOP, 0.14F),
        WATER_PURIFIER(MachineSoundEvents.WATER_PURIFIER_LOOP, 0.10F),
        OXYGEN_ELECTROLYZER(MachineSoundEvents.OXYGEN_ELECTROLYZER_LOOP, 0.11F),
        OXYGEN_FILLER(MachineSoundEvents.OXYGEN_FILLER_LOOP, 0.09F),
        OXYGEN_COMPLEX(MachineSoundEvents.OXYGEN_COMPLEX_LOOP, 0.12F);

        private final net.minecraft.resources.ResourceLocation soundId;
        private final float volume;

        MachineType(net.minecraft.resources.ResourceLocation soundId, float volume) {
            this.soundId = soundId;
            this.volume = volume;
        }
    }

    private MachineAmbientSoundService() {
    }

    /**
     * @param previousTick transient per-BlockEntity counter; it intentionally is not saved to NBT
     * @return the counter value to store back on the BlockEntity
     */
    public static int tick(Level level, BlockPos pos, boolean active, int previousTick, MachineType type) {
        if (level.isClientSide || !active) {
            return 0;
        }

        int tick = previousTick + 1;
        if (tick == 1 || tick >= LOOP_TICKS) {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(type.soundId);
            if (sound != null) {
                level.playSound(null, pos, sound, SoundSource.BLOCKS, type.volume, 1.0F);
            }
            return 1;
        }
        return tick;
    }
}
