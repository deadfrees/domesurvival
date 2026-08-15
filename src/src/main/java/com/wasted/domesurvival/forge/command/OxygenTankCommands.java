package com.wasted.domesurvival.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OxygenTankCommands {
    private OxygenTankCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dome")
                .then(Commands.literal("oxygen")
                        .then(Commands.literal("tank")
                                .then(Commands.literal("empty")
                                        .executes(ctx -> emptyPlayerTanks(ctx.getSource()))))
                        .then(Commands.literal("empty_tanks")
                                .executes(ctx -> emptyPlayerTanks(ctx.getSource())))));
    }

    private static int emptyPlayerTanks(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Command can only be used by a player."));
            return 0;
        }

        final int[] changed = {0};

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            changed[0] += drainStack(inventory.getItem(i));
        }

        CuriosApi.getCuriosInventory(player).ifPresent(curios -> {
            for (var stacksHandler : curios.getCurios().values()) {
                var stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    changed[0] += drainStack(stacks.getStackInSlot(i));
                }
            }
        });

        final int emptied = changed[0];
        if (emptied > 0) {
            source.sendSuccess(
                    () -> Component.literal("Oxygen emptied in tanks: " + emptied),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("No oxygen tanks found to empty."),
                    false
            );
        }

        return emptied;
    }

    private static int drainStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof OxygenTankItem tank)) {
            return 0;
        }

        if (tank.getOxygen(stack) <= 0) {
            return 0;
        }

        tank.setOxygen(stack, 0);
        return 1;
    }
}
