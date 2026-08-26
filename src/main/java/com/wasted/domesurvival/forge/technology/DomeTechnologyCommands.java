package com.wasted.domesurvival.forge.technology;

import com.mojang.brigadier.CommandDispatcher;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.quest.QuestProgressSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DomeTechnologyCommands {
    private DomeTechnologyCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dometech")
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("inspect").executes(ctx -> inspect(ctx.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        QuestProgressSavedData progress = QuestProgressSavedData.get(source.getLevel());
        long unlocked = TechnologyRegistry.all().stream()
                .filter(technology -> progress.hasFlag(technology.requiredFlag()))
                .count();
        source.sendSuccess(() -> Component.literal(
                "Изучено технологий: " + unlocked + " / " + TechnologyRegistry.all().size()
        ).withStyle(ChatFormatting.AQUA), false);
        return (int) unlocked;
    }

    private static int inspect(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Возьмите предмет в основную руку."));
            return 0;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Optional<TechnologyRegistry.Technology> required = TechnologyRegistry.requiredFor(itemId);
        if (required.isEmpty()) {
            source.sendSuccess(() -> Component.literal(itemId + ": доступен без исследования")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }

        boolean unlocked = TechnologyUnlockService.isUnlocked(player.level(), itemId);
        TechnologyRegistry.Technology technology = required.get();
        ChatFormatting color = unlocked ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal(
                itemId + ": " + (unlocked ? "изучено" : "заблокировано")
                        + " — " + technology.title()
                        + " [" + technology.requiredFlag() + "]"
        ).withStyle(color), false);
        return unlocked ? 1 : 0;
    }
}
