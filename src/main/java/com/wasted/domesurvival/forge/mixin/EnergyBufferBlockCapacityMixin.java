package com.wasted.domesurvival.forge.mixin;

import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlock;
import com.wasted.domesurvival.forge.machine.energy.CapacityEnchantedEnergyBuffer;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlock;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferCapacity;
import com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Converts the persisted block-side Capacity level back into a normal ItemStack
 * enchantment on clone/dismantle paths used by the energy-buffer blocks.
 */
@Mixin({EnergyBufferBlock.class, TitanEnergyBufferBlock.class, AdamantiumEnergyBufferBlock.class})
public abstract class EnergyBufferBlockCapacityMixin {
    @Inject(method = "getCloneItemStack", at = @At("RETURN"))
    private void domesurvival$restoreCapacityEnchant(BlockState state, HitResult target,
                                                      BlockGetter level, BlockPos pos, Player player,
                                                      CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack == null || stack.isEmpty()) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CapacityEnchantedEnergyBuffer capacityBuffer) {
            EnergyBufferCapacity.applyToItem(stack, capacityBuffer.getCapacityEnchantLevel());
        }
    }
}
