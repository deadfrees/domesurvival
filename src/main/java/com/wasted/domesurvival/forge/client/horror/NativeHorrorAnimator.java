package com.wasted.domesurvival.forge.client.horror;

import com.wasted.domesurvival.forge.horror.HorrorMobProfiles;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;

public final class NativeHorrorAnimator {
    private static final float PI = (float) Math.PI;

    private NativeHorrorAnimator() {
    }

    public static void animateZombie(
            HumanoidModel<?> model,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        EntityType<?> type = entity.getType();
        if (type != EntityType.ZOMBIE && type != EntityType.HUSK) {
            return;
        }

        HorrorMobProfiles.ZombieGait gait = HorrorMobProfiles.zombieGait(entity);
        float phase = HorrorMobProfiles.phase(entity, 0xD1B54A32D192ED03L);
        float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float cycle = limbSwing * 0.6662F;
        float lowHealth = 1.0F - Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
        float attack = Mth.clamp(model.attackTime, 0.0F, 1.0F);

        float torsoPitch = 0.10F + 0.04F * lowHealth;
        float torsoRoll = 0.0F;
        float leftLegScale = 0.62F;
        float rightLegScale = 0.68F;
        float leftLegBias = 0.0F;
        float rightLegBias = 0.0F;

        switch (gait) {
            case NORMAL_LIMP -> {
                torsoRoll = 0.015F;
                leftLegScale = 0.95F;
                rightLegScale = 1.25F;
            }
            case LEFT_LEG_DRAG -> {
                torsoRoll = -0.030F;
                leftLegScale = 0.22F;
                rightLegScale = 1.32F;
                leftLegBias = 0.10F;
            }
            case RIGHT_LEG_DRAG -> {
                torsoRoll = 0.030F;
                leftLegScale = 1.32F;
                rightLegScale = 0.22F;
                rightLegBias = 0.10F;
            }
            case HUNCHED -> {
                torsoPitch = 0.18F + 0.04F * lowHealth;
                torsoRoll = -0.010F;
                leftLegScale = 0.92F;
                rightLegScale = 1.05F;
            }
            case UNSTABLE -> {
                torsoRoll = Mth.sin(ageInTicks * 0.080F + phase) * 0.026F;
                torsoPitch = 0.13F + Mth.sin(ageInTicks * 0.050F + phase) * 0.018F;
                leftLegScale = 0.82F;
                rightLegScale = 1.18F;
            }
            case BADLY_INJURED -> {
                torsoPitch = 0.22F + 0.05F * lowHealth;
                torsoRoll = -0.040F;
                leftLegScale = 0.16F;
                rightLegScale = 0.95F;
                leftLegBias = 0.14F;
            }
        }

        if (type == EntityType.HUSK) {
            torsoRoll += Mth.sin(ageInTicks * 0.32F + phase) * 0.010F;
        }

        model.body.xRot = torsoPitch;
        model.body.zRot = torsoRoll;

        model.leftLeg.xRot = Mth.cos(cycle) * leftLegScale * move + leftLegBias;
        model.rightLeg.xRot = Mth.cos(cycle + PI) * rightLegScale * move + rightLegBias;
        model.leftLeg.yRot = 0.0F;
        model.rightLeg.yRot = 0.0F;
        model.leftLeg.zRot = 0.0F;
        model.rightLeg.zRot = 0.0F;

        model.head.xRot += 0.025F + Mth.sin(ageInTicks * 0.028F + phase) * 0.010F + lowHealth * 0.010F;
        model.head.zRot = 0.0F;

        if (attack > 0.001F) {
            float lunge = Mth.sin(attack * PI);
            model.body.xRot -= lunge * 0.12F;
            model.rightArm.xRot = -1.20F + lunge * 0.18F;
            model.leftArm.xRot = -1.08F + lunge * 0.10F;
            model.rightArm.yRot = -0.06F;
            model.leftArm.yRot = 0.06F;
            model.rightArm.zRot = 0.02F;
            model.leftArm.zRot = -0.02F;
        } else {
            float deadSwing = Mth.sin(cycle * 0.50F) * 0.07F * move;
            model.rightArm.xRot = -0.30F - deadSwing;
            model.leftArm.xRot = -0.24F + deadSwing * 0.60F;
            model.rightArm.yRot = 0.0F;
            model.leftArm.yRot = 0.0F;
            model.rightArm.zRot = 0.02F;
            model.leftArm.zRot = -0.02F;
        }

        applyDamageWeight(model, entity);
        model.hat.copyFrom(model.head);
    }

    public static void animateDrowned(
            HumanoidModel<?> model,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        if (entity.getType() != EntityType.DROWNED) {
            return;
        }

        float phase = HorrorMobProfiles.phase(entity, 0x94D049BB133111EBL);
        float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);

        if (entity.isInWaterOrBubble()) {
            model.body.zRot += Mth.sin(ageInTicks * 0.040F + phase) * 0.010F;
            model.head.zRot = 0.0F;
            model.hat.copyFrom(model.head);
            return;
        }

        float cycle = limbSwing * 0.78F + phase;
        float lowHealth = 1.0F - Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);

        model.body.xRot = 0.16F + lowHealth * 0.04F;
        model.body.zRot = 0.016F + Mth.sin(ageInTicks * 0.042F + phase) * 0.010F;

        model.leftLeg.xRot = Mth.cos(cycle) * 0.44F * move + 0.02F;
        model.rightLeg.xRot = Mth.cos(cycle + PI) * 0.54F * move;
        model.leftLeg.zRot = 0.0F;
        model.rightLeg.zRot = 0.0F;

        model.head.xRot += 0.035F;
        model.head.zRot = 0.0F;

        float armSwing = Mth.sin(cycle * 0.45F) * 0.05F * move;
        model.rightArm.xRot = -0.22F - armSwing;
        model.leftArm.xRot = -0.16F + armSwing * 0.50F;
        model.rightArm.zRot = 0.01F;
        model.leftArm.zRot = -0.01F;

        applyDamageWeight(model, entity);
        model.hat.copyFrom(model.head);
    }

    public static void animateSkeleton(
            HumanoidModel<?> model,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        EntityType<?> type = entity.getType();
        if (type != EntityType.SKELETON && type != EntityType.WITHER_SKELETON) {
            return;
        }

        float phase = HorrorMobProfiles.phase(entity, 0x2545F4914F6CDD1DL);
        float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float cycle = limbSwing * 0.82F + phase;
        float attack = Mth.clamp(model.attackTime, 0.0F, 1.0F);

        model.head.zRot = 0.0F;

        if (type == EntityType.WITHER_SKELETON) {
            model.body.xRot = 0.060F;
            model.body.zRot = Mth.sin(ageInTicks * 0.030F + phase) * 0.008F;
            model.leftLeg.xRot = Mth.cos(cycle) * 0.52F * move;
            model.rightLeg.xRot = Mth.cos(cycle + PI) * 0.52F * move;

            if (attack > 0.001F) {
                float strike = Mth.sin(attack * PI);
                model.body.xRot -= strike * 0.08F;
                model.rightArm.xRot -= strike * 0.22F;
            }
        } else {
            model.body.xRot = 0.020F;
            model.body.zRot = Mth.sin(ageInTicks * 0.028F + phase) * 0.006F;
            model.leftLeg.xRot = Mth.cos(cycle) * 0.38F * move;
            model.rightLeg.xRot = Mth.cos(cycle + PI) * 0.38F * move;
            model.rightArm.zRot += Mth.sin(ageInTicks * 0.035F + phase) * 0.006F;
            model.leftArm.zRot -= Mth.sin(ageInTicks * 0.034F + phase) * 0.006F;
        }

        if (entity.hurtTime > 0) {
            float shake = Mth.sin(entity.hurtTime * 1.6F) * 0.018F;
            model.body.zRot += shake;
        }

        model.hat.copyFrom(model.head);
    }

    public static void animateEnderman(
            HumanoidModel<?> model,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        if (entity.getType() != EntityType.ENDERMAN) {
            return;
        }

        float phase = HorrorMobProfiles.phase(entity, 0x369DEA0F31A53F85L);
        float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        boolean aggressive = entity instanceof Mob mob && mob.isAggressive();

        model.head.zRot = 0.0F;

        if (!aggressive) {
            model.body.xRot = 0.010F;
            model.body.zRot = Mth.sin(ageInTicks * 0.012F + phase) * 0.004F;

            float longStep = Mth.cos(limbSwing * 0.52F + phase) * 0.24F * move;
            model.leftLeg.xRot = longStep;
            model.rightLeg.xRot = -longStep;

            model.leftArm.xRot = -0.04F;
            model.rightArm.xRot = -0.04F;
        } else {
            model.body.xRot = 0.08F;
            model.body.zRot = Mth.sin(ageInTicks * 0.060F + phase) * 0.008F;

            float step = Mth.cos(limbSwing * 0.78F + phase) * 0.42F * move;
            model.leftLeg.xRot = step;
            model.rightLeg.xRot = -step;

            model.leftArm.xRot = -0.18F;
            model.rightArm.xRot = -0.18F;
            model.leftArm.zRot = -0.02F;
            model.rightArm.zRot = 0.02F;
            model.head.xRot -= 0.02F;
        }

        model.hat.copyFrom(model.head);
    }

    public static void animateSpider(
            SpiderModel<?> model,
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        EntityType<?> type = entity.getType();
        if (type != EntityType.SPIDER && type != EntityType.CAVE_SPIDER) {
            return;
        }

        ModelPart root = model.root();
        ModelPart head = root.getChild("head");
        ModelPart body0 = root.getChild("body0");
        ModelPart body1 = root.getChild("body1");

        ModelPart rightHind = root.getChild("right_hind_leg");
        ModelPart leftHind = root.getChild("left_hind_leg");
        ModelPart rightMidHind = root.getChild("right_middle_hind_leg");
        ModelPart leftMidHind = root.getChild("left_middle_hind_leg");
        ModelPart rightMidFront = root.getChild("right_middle_front_leg");
        ModelPart leftMidFront = root.getChild("left_middle_front_leg");
        ModelPart rightFront = root.getChild("right_front_leg");
        ModelPart leftFront = root.getChild("left_front_leg");

        float phase = HorrorMobProfiles.phase(entity, 0xDB4F0B9175AE2165L);
        float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float speed = type == EntityType.CAVE_SPIDER ? 1.10F : 0.92F;
        float cycle = limbSwing * speed + phase;

        body0.xRot += 0.030F + Mth.sin(ageInTicks * 0.040F + phase) * 0.010F;
        body1.xRot -= 0.010F;
        body0.zRot += Mth.sin(ageInTicks * 0.028F + phase) * 0.010F;
        head.xRot += 0.020F + move * 0.020F;

        ModelPart[] legs = {
                rightHind, leftHind,
                rightMidHind, leftMidHind,
                rightMidFront, leftMidFront,
                rightFront, leftFront
        };

        float[] offsets = {
                0.0F, PI,
                PI * 0.50F, PI * 1.50F,
                PI * 1.50F, PI * 0.50F,
                PI, 0.0F
        };

        for (int i = 0; i < legs.length; i++) {
            float local = cycle + offsets[i];
            float lift = Math.max(0.0F, Mth.sin(local));
            float idle = Mth.sin(ageInTicks * (0.018F + i * 0.001F) + phase + i) * 0.004F;

            legs[i].xRot += lift * (0.07F + 0.018F * move) * move + idle;
            legs[i].yRot += Mth.cos(local) * 0.025F * move;
        }

        if (type == EntityType.CAVE_SPIDER) {
            head.zRot += Mth.sin(ageInTicks * 0.18F + phase) * 0.006F;
        }

        if (entity instanceof LivingEntity living && living.hurtTime > 0) {
            body0.zRot += Mth.sin(living.hurtTime * 1.0F) * 0.022F;
        }

        float attack = Mth.clamp(model.attackTime, 0.0F, 1.0F);
        if (attack > 0.001F) {
            float pounce = Mth.sin(attack * PI);
            body0.xRot -= pounce * 0.08F;
            head.xRot -= pounce * 0.06F;
        }
    }

    public static void animateCreeper(
            CreeperModel<?> model,
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        if (entity.getType() != EntityType.CREEPER) {
            return;
        }

        ModelPart root = model.root();
        ModelPart head = root.getChild("head");
        ModelPart body = root.getChild("body");
        ModelPart rightHind = root.getChild("right_hind_leg");
        ModelPart leftHind = root.getChild("left_hind_leg");
        ModelPart rightFront = root.getChild("right_front_leg");
        ModelPart leftFront = root.getChild("left_front_leg");

        float phase = HorrorMobProfiles.phase(entity, 0xA4093822299F31D0L);
        float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float cycle = limbSwing * 0.76F + phase;
        float stride = 0.44F * move;

        rightHind.xRot = Mth.cos(cycle) * stride;
        leftFront.xRot = Mth.cos(cycle) * stride;
        leftHind.xRot = Mth.cos(cycle + PI) * stride;
        rightFront.xRot = Mth.cos(cycle + PI) * stride;

        float weight = Mth.sin(cycle) * 0.014F * move;
        body.xRot = 0.050F + move * 0.018F;
        body.zRot = weight + Mth.sin(ageInTicks * 0.020F + phase) * 0.004F;
        head.xRot += 0.020F;
        head.zRot = 0.0F;

        if (entity instanceof Creeper creeper) {
            float swell = Mth.clamp(creeper.getSwelling(0.0F), 0.0F, 1.0F);
            if (swell > 0.0F) {
                float tension = Mth.sin(ageInTicks * (0.18F + swell * 0.25F) + phase)
                        * 0.012F * swell;
                body.xRot += swell * 0.035F;
                body.zRot += tension;
            }
        }

        if (entity instanceof LivingEntity living && living.hurtTime > 0) {
            body.zRot += Mth.sin(living.hurtTime * 1.0F) * 0.020F;
        }
    }

    private static void applyDamageWeight(HumanoidModel<?> model, LivingEntity entity) {
        if (entity.hurtTime > 0) {
            float recoil = Mth.sin(entity.hurtTime * 1.0F) * 0.025F;
            model.body.zRot += recoil;
            model.rightArm.zRot += recoil * 0.25F;
            model.leftArm.zRot -= recoil * 0.25F;
        }

        if (entity.isDeadOrDying()) {
            float collapse = Mth.clamp(entity.deathTime / 20.0F, 0.0F, 1.0F);
            model.body.xRot += collapse * 0.08F;
            model.leftArm.xRot += collapse * 0.10F;
            model.rightArm.xRot -= collapse * 0.08F;
        }
    }
}
