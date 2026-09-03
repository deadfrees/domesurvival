package com.wasted.domesurvival.forge.mixin;

import com.wasted.domesurvival.forge.client.horror.NativeHorrorAnimator;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperModel.class)
public abstract class HorrorCreeperModelMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            at = @At("TAIL")
    )
    private void domesurvival$nativeCreeperAnimation(
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        NativeHorrorAnimator.animateCreeper(
                (CreeperModel<?>)(Object)this,
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks
        );
    }
}
