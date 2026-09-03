package com.wasted.domesurvival.forge.loot;

import com.mojang.serialization.Codec;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    DomeSurvival.MOD_ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> BIO_MODULE =
            SERIALIZERS.register("bio_module", () -> BioModuleLootModifier.CODEC);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> TECHNOLOGY_SALVAGE =
            SERIALIZERS.register("technology_salvage", () -> TechnologySalvageLootModifier.CODEC);

    private ModLootModifiers() { }
}
