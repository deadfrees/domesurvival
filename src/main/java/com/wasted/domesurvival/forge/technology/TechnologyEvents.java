package com.wasted.domesurvival.forge.technology;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechnologyEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TechnologyEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int migrated = TechnologyProgressMigration.apply(player);
        if (migrated > 0) {
            LOGGER.info("[DomeTechnology] Migrated {} flags for {}", migrated, player.getScoreboardName());
        }
        TechnologyUnlockService.sync(player);
    }
}
