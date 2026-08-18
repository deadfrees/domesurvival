package com.wasted.domesurvival.forge.registry;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.entity.MemoryPaintingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DomeSurvival.MOD_ID);

    public static final RegistryObject<EntityType<MemoryPaintingEntity>> MEMORY_PAINTING =
            ENTITY_TYPES.register(
                    "memory_painting_entity",
                    () -> EntityType.Builder
                            .<MemoryPaintingEntity>of(MemoryPaintingEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(10)
                            .updateInterval(Integer.MAX_VALUE)
                            .build(DomeSurvival.MOD_ID + ":memory_painting_entity")
            );

    private ModEntityTypes() {
    }
}
