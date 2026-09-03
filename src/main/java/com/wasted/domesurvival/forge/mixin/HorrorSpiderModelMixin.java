package com.wasted.domesurvival.forge.mixin;

import com.wasted.domesurvival.forge.client.horror.NativeHorrorAnimator;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiderModel.class)
public abstract class HorrorSpiderModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            at = @At("TAIL")
    )
    private void domesurvival$nativeSpiderAnimation(
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        NativeHorrorAnimator.animateSpider(
                (SpiderModel<?>)(Object)this,
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks
        );
    }
}
