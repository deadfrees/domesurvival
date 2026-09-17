package com.wasted.domesurvival.forge.gas;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;

/** Stable ids for DomeSurvival's typed gas transport. */
public final class ModGases {
    public static final ResourceLocation OXYGEN =
            ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, "oxygen");
    public static final ResourceLocation MINERAL_GAS =
            ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, "mineral_gas");

    private ModGases() {
    }
}
