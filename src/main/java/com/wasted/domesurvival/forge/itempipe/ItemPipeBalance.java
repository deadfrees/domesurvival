package com.wasted.domesurvival.forge.itempipe;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemPipeBalance extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final Map<ItemPipeTier, Settings> SETTINGS = new EnumMap<>(ItemPipeTier.class);

    static {
        resetDefaults();
    }

    public ItemPipeBalance() {
        super(GSON, "item_pipe_balance");
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ItemPipeBalance());
    }

    public static int itemsPerCycle(ItemPipeTier tier) {
        return SETTINGS.getOrDefault(tier, defaults(tier)).itemsPerCycle();
    }

    public static int cooldownTicks(ItemPipeTier tier) {
        return SETTINGS.getOrDefault(tier, defaults(tier)).cooldownTicks();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        resetDefaults();
        JsonElement element = objects.get(new ResourceLocation(DomeSurvival.MOD_ID, "default"));
        if (element == null || !element.isJsonObject()) return;

        JsonObject root = element.getAsJsonObject();
        for (ItemPipeTier tier : ItemPipeTier.values()) {
            if (!root.has(tier.id()) || !root.get(tier.id()).isJsonObject()) continue;
            JsonObject obj = root.getAsJsonObject(tier.id());
            int items = getPositive(obj, "items_per_cycle", tier.defaultItemsPerCycle(), 1, 64);
            int cooldown = getPositive(obj, "cooldown_ticks", tier.defaultCooldownTicks(), 1, 40);
            SETTINGS.put(tier, new Settings(items, cooldown));
        }
    }

    private static int getPositive(JsonObject obj, String key, int fallback, int min, int max) {
        if (!obj.has(key)) return fallback;
        try {
            return Math.max(min, Math.min(max, obj.get(key).getAsInt()));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static void resetDefaults() {
        SETTINGS.clear();
        for (ItemPipeTier tier : ItemPipeTier.values()) {
            SETTINGS.put(tier, defaults(tier));
        }
    }

    private static Settings defaults(ItemPipeTier tier) {
        return new Settings(tier.defaultItemsPerCycle(), tier.defaultCooldownTicks());
    }

    private record Settings(int itemsPerCycle, int cooldownTicks) { }
}
