package com.wasted.domesurvival.forge.client;

import com.wasted.domesurvival.forge.item.BioModuleItem;
import com.wasted.domesurvival.forge.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import java.util.List;

/** Visual identities are independent of entity registry order and data-pack recipe order. */
@Mod.EventBusSubscriber(modid="domesurvival", bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class BioModuleModels {
    public static final ResourceLocation VARIANT = new ResourceLocation("domesurvival", "bio_variant");
    private static final List<String> SPECIES = List.of(
            "cow", "pig", "sheep", "chicken", "rabbit", "horse", "donkey", "llama", "goat",
            "camel", "wolf", "cat", "ocelot", "fox", "bee", "panda", "turtle", "axolotl",
            "frog", "mooshroom", "sniffer", "strider", "hoglin", "mule", "parrot", "polar_bear");

    private BioModuleModels() { }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.BIO_MODULE.get(), VARIANT,
                (stack, level, entity, seed) -> variant(stack)));
    }

    public static float variant(ItemStack stack) {
        ResourceLocation id = BioModuleItem.entityId(stack);
        if (id == null || !id.getNamespace().equals("minecraft")) return 0;
        int index = SPECIES.indexOf(id.getPath());
        // Exact binary fractions stay within the clamped item property range 0..1.
        return index < 0 ? 0 : (index + 1 + (BioModuleItem.hasDamagedGenome(stack) ? 26 : 0)) / 64F;
    }
}
