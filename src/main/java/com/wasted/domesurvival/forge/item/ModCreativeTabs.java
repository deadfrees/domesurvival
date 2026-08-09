package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Comparator;

/**
 * Dedicated creative inventory tab for every item registered by DomeSurvival.
 *
 * <p>The contents are discovered from the final item registry by namespace instead of
 * being hard-coded. This means items added by either developer automatically appear in
 * this tab, even when they are registered from a different DeferredRegister class.</p>
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    private static final ResourceLocation TAB_ID = new ResourceLocation(DomeSurvival.MOD_ID, "items");
    private static final ResourceLocation PREFERRED_ICON =
            new ResourceLocation(DomeSurvival.MOD_ID, "reinforced_glass");

    private ModCreativeTabs() {
    }

    @SubscribeEvent
    public static void registerCreativeTab(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(
                TAB_ID,
                CreativeModeTab.builder()
                        .title(Component.literal("Dome Survival"))
                        .icon(ModCreativeTabs::createIcon)
                        .displayItems((parameters, output) -> ForgeRegistries.ITEMS.getValues().stream()
                                .filter(ModCreativeTabs::isDomeSurvivalItem)
                                .sorted(Comparator.comparing(ModCreativeTabs::registryPath))
                                .forEach(output::accept))
                        .build()
        ));
    }

    private static boolean isDomeSurvivalItem(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && DomeSurvival.MOD_ID.equals(id.getNamespace());
    }

    private static String registryPath(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? "" : id.getPath();
    }

    private static ItemStack createIcon() {
        Item preferred = ForgeRegistries.ITEMS.getValue(PREFERRED_ICON);
        return new ItemStack(preferred == null || preferred == Items.AIR ? Items.GLASS : preferred);
    }
}
