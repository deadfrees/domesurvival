package com.wasted.domesurvival.forge.particle;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Client-rendered weather particle types. Kept data-free so they are cheap to spawn locally. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DomeSurvival.MOD_ID);

    /** Thin bright-green acid rain streak. overrideLimiter=true keeps it visible at the dome shell. */
    public static final RegistryObject<SimpleParticleType> ACID_RAIN_STREAK =
            PARTICLE_TYPES.register("acid_rain_streak", () -> new SimpleParticleType(true));

    /** Small orange airborne sand mote used by the custom sandstorm. */
    public static final RegistryObject<SimpleParticleType> SANDSTORM_MOTE =
            PARTICLE_TYPES.register("sandstorm_mote", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
