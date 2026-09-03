package com.wasted.domesurvival.forge.mixin;

import com.wasted.domesurvival.forge.client.horror.NativeHorrorAnimator;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonModel.class)
public abstract class HorrorSkeletonModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V",
            at = @At("TAIL")
    )
    private void domesurvival$nativeSkeletonAnimation(
            Mob entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        NativeHorrorAnimator.animateSkeleton(
                (HumanoidModel<?>)(Object)this,
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks
        );
    }
}
