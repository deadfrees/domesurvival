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
    public static final RegistryObject<SoundEvent> ACID_RAIN_AMBIENCE = register("acid_rain_ambience");
    public static final RegistryObject<SoundEvent> SANDSTORM_WIND = register("sandstorm_wind");
    public static final RegistryObject<SoundEvent> SAND_SIEVE_PROCESS = register("sand_sieve_process");

    public static final RegistryObject<SoundEvent> MUSIC_MAIN_MENU = register("music_main_menu");
    public static final RegistryObject<SoundEvent> MUSIC_01 = register("music_01");
    public static final RegistryObject<SoundEvent> MUSIC_02 = register("music_02");
    public static final RegistryObject<SoundEvent> MUSIC_03 = register("music_03");
    public static final RegistryObject<SoundEvent> MUSIC_05 = register("music_05");
    public static final RegistryObject<SoundEvent> MUSIC_06 = register("music_06");
    public static final RegistryObject<SoundEvent> MUSIC_07 = register("music_07");
    public static final RegistryObject<SoundEvent> MUSIC_08 = register("music_08");
    public static final RegistryObject<SoundEvent> MUSIC_09 = register("music_09");
    public static final RegistryObject<SoundEvent> MUSIC_10 = register("music_10");
    public static final RegistryObject<SoundEvent> MUSIC_11 = register("music_11");
    public static final RegistryObject<SoundEvent> MUSIC_12 = register("music_12");
    public static final RegistryObject<SoundEvent> MUSIC_13 = register("music_13");
    public static final RegistryObject<SoundEvent> MUSIC_14 = register("music_14");
    public static final RegistryObject<SoundEvent> MUSIC_15 = register("music_15");
    public static final RegistryObject<SoundEvent> MUSIC_16 = register("music_16");
    public static final RegistryObject<SoundEvent> MUSIC_17 = register("music_17");
    public static final RegistryObject<SoundEvent> MUSIC_18 = register("music_18");
    public static final RegistryObject<SoundEvent> MUSIC_19 = register("music_19");
    public static final RegistryObject<SoundEvent> MUSIC_20 = register("music_20");
    public static final RegistryObject<SoundEvent> MUSIC_21 = register("music_21");
    public static final RegistryObject<SoundEvent> MUSIC_22 = register("music_22");
    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DomeSurvival.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
