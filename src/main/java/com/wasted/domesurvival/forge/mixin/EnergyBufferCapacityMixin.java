package com.wasted.domesurvival.forge.mixin;

import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.CapacityEnchantedEnergyBuffer;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockEntity;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferCapacity;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the Capacity I-IV state to all three survival energy buffers without
 * duplicating their large and otherwise identical FE implementation.
 */
@Mixin({
        EnergyBufferBlockEntity.class,
        TitanEnergyBufferBlockEntity.class,
        AdamantiumEnergyBufferBlockEntity.class
})
public abstract class EnergyBufferCapacityMixin implements CapacityEnchantedEnergyBuffer {
    @Shadow @Final private MachineEnergyStorage energyStorage;

    @Unique
    private int domesurvival$capacityEnchantLevel;

    @Override
    public int getCapacityEnchantLevel() {
        return domesurvival$capacityEnchantLevel;
    }

    @Override
    public void setCapacityEnchantLevel(int level) {
        int clamped = EnergyBufferCapacity.clampLevel(level);
        if (domesurvival$capacityEnchantLevel == clamped) return;

        domesurvival$capacityEnchantLevel = clamped;
        energyStorage.setCapacityInternal(EnergyBufferCapacity.applyMultiplier(domesurvival$getBaseCapacity(), clamped));
        ((BlockEntity) (Object) this).setChanged();
    }

    /**
     * Runs before the target load() restores Energy, so a charged enchanted
     * buffer is never temporarily clamped to its unenchanted base capacity.
     */
    @Inject(
            method = {
                    "load(Lnet/minecraft/nbt/CompoundTag;)V",
                    "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"
            },
            at = @At("HEAD"),
            remap = false
    )
    private void domesurvival$loadCapacityBeforeEnergy(CompoundTag tag, CallbackInfo ci) {
        domesurvival$capacityEnchantLevel = EnergyBufferCapacity.readLevel(tag);
        energyStorage.setCapacityInternal(EnergyBufferCapacity.applyMultiplier(
                domesurvival$getBaseCapacity(), domesurvival$capacityEnchantLevel));
    }

    @Inject(
            method = {
                    "saveAdditional(Lnet/minecraft/nbt/CompoundTag;)V",
                    "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V"
            },
            at = @At("TAIL"),
            remap = false
    )
    private void domesurvival$saveCapacity(CompoundTag tag, CallbackInfo ci) {
        EnergyBufferCapacity.writeLevel(tag, domesurvival$capacityEnchantLevel);
    }

    @Unique
    private int domesurvival$getBaseCapacity() {
        Object self = this;
        if (self instanceof TitanEnergyBufferBlockEntity) {
            return TitanEnergyBufferBlockEntity.ENERGY_CAPACITY;
        }
        if (self instanceof AdamantiumEnergyBufferBlockEntity) {
            return AdamantiumEnergyBufferBlockEntity.ENERGY_CAPACITY;
        }
        return EnergyBufferBlockEntity.ENERGY_CAPACITY;
    }
}
