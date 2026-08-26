package com.wasted.domesurvival.forge.technology;

import com.wasted.domesurvival.forge.network.ModNetwork;
import com.wasted.domesurvival.forge.network.TechnologySyncPacket;
import com.wasted.domesurvival.forge.quest.QuestProgressSavedData;
import com.wasted.domesurvival.forge.quest.QuestProgressService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class TechnologyUnlockService {
    private TechnologyUnlockService() {
    }

    public static boolean isUnlocked(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return isUnlocked(level, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isUnlocked(Level level, ResourceLocation itemId) {
        Optional<TechnologyRegistry.Technology> required = TechnologyRegistry.requiredFor(itemId);
        if (required.isEmpty()) {
            return true;
        }

        String flag = required.get().requiredFlag();
        if (level instanceof ServerLevel serverLevel) {
            return QuestProgressService.has(serverLevel, flag);
        }
        return level != null && level.isClientSide && TechnologyClientState.has(flag);
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendTo(player, new TechnologySyncPacket(
                QuestProgressSavedData.get(player.serverLevel()).sortedFlags()));
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    public static void onFlagChanged(ServerLevel level, String flag, boolean unlocked) {
        syncAll(level.getServer());
        if (!unlocked) {
            return;
        }

        for (TechnologyRegistry.Technology technology : TechnologyRegistry.technologiesForFlag(flag)) {
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("Изучена технология: " + technology.title())
                            .withStyle(ChatFormatting.AQUA),
                    false
            );
        }
    }
}
