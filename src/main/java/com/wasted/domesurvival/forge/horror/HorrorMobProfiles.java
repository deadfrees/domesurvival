package com.wasted.domesurvival.forge.horror;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class HorrorMobProfiles {
    private static final long ZOMBIE_SALT = 0x6A09E667F3BCC909L;
    private static final long GENERAL_SALT = 0xBB67AE8584CAA73BL;

    private HorrorMobProfiles() {
    }

    public enum ZombieGait {
        NORMAL_LIMP(-0.60D),
        LEFT_LEG_DRAG(-0.66D),
        RIGHT_LEG_DRAG(-0.66D),
        HUNCHED(-0.62D),
        UNSTABLE(-0.68D),
        BADLY_INJURED(-0.72D);

        private final double speedModifier;

        ZombieGait(double speedModifier) {
            this.speedModifier = speedModifier;
        }

        public double speedModifier() {
            return speedModifier;
        }
    }

    public static ZombieGait zombieGait(Entity entity) {
        ZombieGait[] values = ZombieGait.values();
        return values[variant(entity, ZOMBIE_SALT, values.length)];
    }

    public static int variant(Entity entity, long salt, int count) {
        if (count <= 1) {
            return 0;
        }

        long value = entity.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(entity.getUUID().getLeastSignificantBits(), 17)
                ^ salt;
        value = mix64(value);
        return (int) Math.floorMod(value, (long) count);
    }

    public static float phase(Entity entity, long salt) {
        int bucket = variant(entity, salt ^ GENERAL_SALT, 4096);
        return (float) (bucket * (Math.PI * 2.0D / 4096.0D));
    }

    public static boolean isNativeHorrorTarget(Entity entity) {
        EntityType<?> type = entity.getType();
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.SKELETON
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.CREEPER
                || type == EntityType.SPIDER
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.ENDERMAN;
    }

    public static double movementSpeedModifier(LivingEntity entity) {
        EntityType<?> type = entity.getType();

        if (type == EntityType.ZOMBIE) {
            return zombieGait(entity).speedModifier();
        }

        if (type == EntityType.HUSK) {
            return Math.min(-0.46D, zombieGait(entity).speedModifier() + 0.12D);
        }

        if (type == EntityType.DROWNED) {
            return -0.38D;
        }

        if (type == EntityType.SKELETON) {
            return -0.14D;
        }

        if (type == EntityType.WITHER_SKELETON) {
            return -0.08D;
        }

        if (type == EntityType.CREEPER) {
            return -0.15D;
        }

        if (type == EntityType.SPIDER
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.ENDERMAN) {
            return 0.0D;
        }

        return Double.NaN;
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }
}
