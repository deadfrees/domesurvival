package com.wasted.domesurvival.forge.sound;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DomeSurvival.MOD_ID);

    public static final RegistryObject<SoundEvent> AIRLOCK_OPEN = register("airlock_open");
    public static final RegistryObject<SoundEvent> AIRLOCK_CLOSE = register("airlock_close");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DomeSurvival.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
