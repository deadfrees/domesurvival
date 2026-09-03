package com.wasted.domesurvival.forge.client.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/** Lightweight JEI view of a process whose actual logic remains in its block entity. */
public record DomeMachineRecipe(
        ResourceLocation id,
        Layout layout,
        List<List<ItemStack>> itemInputs,
        List<FluidStack> fluidInputs,
        List<List<ItemStack>> itemOutputs,
        List<FluidStack> fluidOutputs,
        Component note,
        List<Component> outputNotes,
        int processTicks,
        int energyPerTick
) {
    public enum Layout {
        COKE_OVEN,
        SHAFT_FURNACE,
        WATER_PURIFIER,
        OXYGEN_ELECTROLYZER,
        OXYGEN_FILLER,
        BIO_REPAIR,
        BIO_INCUBATION,
        SAND_SIEVE
    }
}
