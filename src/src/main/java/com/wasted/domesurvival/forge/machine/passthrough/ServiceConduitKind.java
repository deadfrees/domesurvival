package com.wasted.domesurvival.forge.machine.passthrough;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.oxygen.OxygenPipeBlock;
import com.wasted.domesurvival.forge.transport.energy.EnergyPipeBlock;
import com.wasted.domesurvival.forge.transport.fluid.FluidPipeBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;

public enum ServiceConduitKind {
    EMPTY,
    ENERGY,
    FLUID,
    OXYGEN,
    ITEM;

    public static final TagKey<Item> ENERGY_TAG = tag("service_pass_through/energy_conduits");
    public static final TagKey<Item> FLUID_TAG = tag("service_pass_through/fluid_conduits");
    public static final TagKey<Item> OXYGEN_TAG = tag("service_pass_through/oxygen_conduits");
    public static final TagKey<Item> ITEM_TAG = tag("service_pass_through/item_conduits");

    public static ServiceConduitKind detect(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return EMPTY;
        }

        if (blockItem.getBlock() instanceof EnergyPipeBlock) {
            return ENERGY;
        }
        if (blockItem.getBlock() instanceof FluidPipeBlock) {
            return FLUID;
        }
        if (blockItem.getBlock() instanceof OxygenPipeBlock) {
            return OXYGEN;
        }

        if (stack.is(ENERGY_TAG)) return ENERGY;
        if (stack.is(FLUID_TAG)) return FLUID;
        if (stack.is(OXYGEN_TAG)) return OXYGEN;
        if (stack.is(ITEM_TAG)) return ITEM;
        return EMPTY;
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(
                Registries.ITEM,
                new ResourceLocation(DomeSurvival.MOD_ID, path)
        );
    }
}
