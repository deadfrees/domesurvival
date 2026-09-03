package com.wasted.domesurvival.forge.mixin;

import com.wasted.domesurvival.forge.client.horror.NativeHorrorAnimator;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public abstract class HorrorZombieModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V",
            at = @At("TAIL")
    )
    private void domesurvival$nativeZombieAnimation(
            Monster entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        NativeHorrorAnimator.animateZombie(
                (HumanoidModel<?>)(Object)this,
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks
        );
    }
}
