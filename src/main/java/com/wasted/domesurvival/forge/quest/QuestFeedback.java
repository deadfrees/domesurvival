package com.wasted.domesurvival.forge.quest;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Lightweight quest completion feedback.
 *
 * Runs only when an invisible FTB command reward is auto-claimed.
 * No ticking, polling, entity spawning or world scanning.
 */
public final class QuestFeedback {
    private QuestFeedback() {
    }

    public static int play(ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 1.0D;
        double z = player.getZ();

        switch (tier) {
            case NORMAL -> {
                player.playNotifySound(
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.55F,
                        1.18F
                );
                level.sendParticles(
                        ParticleTypes.FIREWORK,
                        x, y, z,
                        7,
                        0.45D, 0.55D, 0.45D,
                        0.025D
                );
            }
            case MILESTONE -> {
                player.playNotifySound(
                        SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS,
                        0.75F,
                        1.05F
                );
                level.sendParticles(
                        ParticleTypes.FIREWORK,
                        x, y, z,
                        18,
                        0.75D, 0.9D, 0.75D,
                        0.04D
                );
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        x, y + 0.25D, z,
                        6,
                        0.55D, 0.75D, 0.55D,
                        0.025D
                );
            }
            case CHAPTER -> {
                player.playNotifySound(
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        SoundSource.PLAYERS,
                        0.95F,
                        1.0F
                );
                level.sendParticles(
                        ParticleTypes.FIREWORK,
                        x, y + 0.2D, z,
                        34,
                        1.0D, 1.25D, 1.0D,
                        0.055D
                );
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        x, y + 0.5D, z,
                        12,
                        0.8D, 1.0D, 0.8D,
                        0.035D
                );
            }
        }

        return 1;
    }

    public enum Tier {
        NORMAL,
        MILESTONE,
        CHAPTER
    }
}
