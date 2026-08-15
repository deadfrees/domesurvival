package com.wasted.domesurvival.forge.sound;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Registers the low-volume machine work loops without touching the older ModSounds registry. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MachineSoundEvents {
    public static final ResourceLocation COAL_GENERATOR_LOOP = id("machine.coal_generator.loop");
    public static final ResourceLocation WATER_PURIFIER_LOOP = id("machine.water_purifier.loop");
    public static final ResourceLocation OXYGEN_ELECTROLYZER_LOOP = id("machine.electrolyzer.loop");
    public static final ResourceLocation OXYGEN_FILLER_LOOP = id("machine.oxygen_filler.loop");
    public static final ResourceLocation OXYGEN_COMPLEX_LOOP = id("machine.oxygen_complex.loop");

    private MachineSoundEvents() {
    }

    @SubscribeEvent
    public static void registerSounds(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.SOUND_EVENTS, helper -> {
            helper.register(COAL_GENERATOR_LOOP, SoundEvent.createVariableRangeEvent(COAL_GENERATOR_LOOP));
            helper.register(WATER_PURIFIER_LOOP, SoundEvent.createVariableRangeEvent(WATER_PURIFIER_LOOP));
            helper.register(OXYGEN_ELECTROLYZER_LOOP, SoundEvent.createVariableRangeEvent(OXYGEN_ELECTROLYZER_LOOP));
            helper.register(OXYGEN_FILLER_LOOP, SoundEvent.createVariableRangeEvent(OXYGEN_FILLER_LOOP));
            helper.register(OXYGEN_COMPLEX_LOOP, SoundEvent.createVariableRangeEvent(OXYGEN_COMPLEX_LOOP));
        });
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(DomeSurvival.MOD_ID, path);
    }
}
