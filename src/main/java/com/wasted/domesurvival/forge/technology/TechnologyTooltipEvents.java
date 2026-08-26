package com.wasted.domesurvival.forge.technology;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TechnologyTooltipEvents {
    private TechnologyTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        Optional<TechnologyRegistry.Technology> required = TechnologyRegistry.requiredFor(itemId);
        if (required.isEmpty() || TechnologyClientState.has(required.get().requiredFlag())) {
            return;
        }

        event.getToolTip().add(Component.literal("Технология ещё не изучена").withStyle(ChatFormatting.RED));
        event.getToolTip().add(Component.literal("Требуется: " + required.get().title()).withStyle(ChatFormatting.GRAY));
    }
}
