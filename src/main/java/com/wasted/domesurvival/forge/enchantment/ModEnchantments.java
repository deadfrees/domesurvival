package com.wasted.domesurvival.forge.enchantment;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, DomeSurvival.MOD_ID);

    public static final RegistryObject<Enchantment> CAPACITY = ENCHANTMENTS.register(
            "capacity",
            EnergyBufferCapacityEnchantment::new
    );

    private ModEnchantments() {
    }
}
