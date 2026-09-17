package com.wasted.domesurvival.forge.capability;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Typed process-gas storage used by DomeSurvival machines.
 *
 * <p>This is intentionally separate from {@link IOxygenStorage}: life-support
 * consumers keep requesting the oxygen capability and therefore can never
 * consume technological gases such as mineral gas.</p>
 */
public interface IGasStorage {
    int receiveGas(ResourceLocation gas, int maxReceive, boolean simulate);
    int extractGas(ResourceLocation gas, int maxExtract, boolean simulate);

    @Nullable ResourceLocation getGasType();
    int getGasStored();
    int getMaxGasStored();

    boolean canReceiveGas(ResourceLocation gas);
    boolean canExtractGas(ResourceLocation gas);
}
