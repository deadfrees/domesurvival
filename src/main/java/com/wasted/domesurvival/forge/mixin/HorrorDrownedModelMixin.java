package com.wasted.domesurvival.forge.mixin;

import com.wasted.domesurvival.forge.client.horror.NativeHorrorAnimator;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedModel.class)
public abstract class HorrorDrownedModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/monster/Zombie;FFFFF)V",
            at = @At("TAIL")
    )
    private void domesurvival$nativeDrownedAnimation(
            Zombie entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        NativeHorrorAnimator.animateDrowned(
                (HumanoidModel<?>)(Object)this,
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks
        );
    }
}
