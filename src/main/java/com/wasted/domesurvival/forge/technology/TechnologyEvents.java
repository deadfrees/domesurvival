package com.wasted.domesurvival.forge.technology;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.bio.BioLootData;
import com.wasted.domesurvival.forge.bio.BioModuleData;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.network.BioModuleRegistrySyncPacket;
import com.wasted.domesurvival.forge.network.ModNetwork;
import com.wasted.domesurvival.forge.quest.QuestProgressService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
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
        if (QuestProgressService.has(player.serverLevel(), "GENETIC_SAMPLES_RECOVERED")) {
            QuestProgressService.set(player.serverLevel(), BioModuleData.IDENTIFICATION_FLAG,
                    "migration:genetic_samples_recovered");
        }
        if (QuestProgressService.has(player.serverLevel(), "FAUNA_RESTORATION_STARTED")) {
            player.server.getCommands().performPrefixedCommand(
                    player.server.createCommandSourceStack().withSuppressedOutput(),
                    "advancement grant "
                            + player.getScoreboardName()
                            + " only domesurvival:quest_actions/bioincubator_first_birth"
            );
        }
        TechnologyUnlockService.sync(player);
        ModNetwork.sendTo(player, new BioModuleRegistrySyncPacket(BioLootData.allSpecies()));
    }

    /**
     * FTB command rewards are not guaranteed to run immediately on every team
     * migration. Make the story condition authoritative in gameplay as well:
     * once a player actually recovers the three intact archive samples, the
     * database flag is restored even if the invisible quest reward was missed.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0
                || QuestProgressService.has(player.serverLevel(), BioModuleData.IDENTIFICATION_FLAG)) {
            return;
        }
        if (!hasItem(player, ModItems.CHICKEN_CRYOCAPSULE.get())
                || !hasItem(player, ModItems.SHEEP_CRYOCAPSULE.get())
                || !hasItem(player, ModItems.COW_CRYOCAPSULE.get())) {
            return;
        }
        QuestProgressService.set(player.serverLevel(), "GENETIC_SAMPLES_RECOVERED",
                "inventory:archive_samples");
        QuestProgressService.set(player.serverLevel(), BioModuleData.IDENTIFICATION_FLAG,
                "inventory:archive_samples");
    }

    private static boolean hasItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) return true;
        }
        return false;
    }
}
