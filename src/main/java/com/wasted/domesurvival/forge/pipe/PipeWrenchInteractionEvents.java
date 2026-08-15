package com.wasted.domesurvival.forge.pipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PipeWrenchInteractionEvents {
    private static final ResourceLocation MACHINE_WRENCH_ID =
            new ResourceLocation(DomeSurvival.MOD_ID, "machine_wrench");
    private static final Map<UUID, Long> LAST_LEFT_TOGGLE = new HashMap<>();

    private PipeWrenchInteractionEvents() { }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        ItemStack stack = player.getMainHandItem();
        if (!MACHINE_WRENCH_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) return;

        Direction side = PipeWrenchConnectionService.findOnlySameFamilySide(
                event.getLevel(), event.getPos(), event.getFace());
        if (side == null) return;

        // Cancel normal mining on both logical sides. Holding LMB can fire this
        // event repeatedly, so the server-side toggle is debounced.
        event.setCanceled(true);

        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        long previous = LAST_LEFT_TOGGLE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous < 5L) return;
        LAST_LEFT_TOGGLE.put(player.getUUID(), now);

        PipeWrenchConnectionService.toggle(level, event.getPos(), side, player);
    }
}
