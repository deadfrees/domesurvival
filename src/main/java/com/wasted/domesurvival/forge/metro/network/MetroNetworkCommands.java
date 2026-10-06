package com.wasted.domesurvival.forge.metro.network;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.StringJoiner;

/** Stage-5 developer commands required by the metro specification. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MetroNetworkCommands {
    private MetroNetworkCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dome")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("metro")
                                .then(Commands.literal("routes")
                                        .executes(ctx -> routes(ctx.getSource())))
                                .then(Commands.literal("route")
                                        .then(Commands.argument("from", StringArgumentType.string())
                                                .then(Commands.argument("to", StringArgumentType.string())
                                                        .executes(ctx -> route(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to")
                                                        )))))
                                .then(Commands.literal("network")
                                        .then(Commands.literal("sync")
                                                .executes(ctx -> sync(ctx.getSource()))))
                        )
        );
    }

    private static int sync(CommandSourceStack source) {
        MetroNetworkService.SyncResult result = MetroNetworkService.synchronize(source.getServer());
        source.sendSuccess(() -> Component.literal(
                "\u0421\u0435\u0442\u044c \u043c\u0435\u0442\u0440\u043e \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u043d\u0430: "
                        + result.nodeCount() + " \u0443\u0437\u043b\u043e\u0432, "
                        + result.routeCount() + " \u043c\u0430\u0440\u0448\u0440\u0443\u0442\u043e\u0432."
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int routes(CommandSourceStack source) {
        MetroNetworkService.synchronize(source.getServer());
        MetroNetworkSavedData data = MetroNetworkSavedData.get(source.getServer());

        source.sendSuccess(() -> Component.literal(
                "\u0421\u0435\u0442\u044c \u043c\u0435\u0442\u0440\u043e: "
                        + data.nodeCount() + " \u0443\u0437\u043b\u043e\u0432, "
                        + data.routeCount() + " \u043c\u0430\u0440\u0448\u0440\u0443\u0442\u043e\u0432"
        ).withStyle(ChatFormatting.AQUA), false);

        for (MetroRoute route : data.routes()) {
            MetroNode first = data.node(route.nodeA());
            MetroNode second = data.node(route.nodeB());
            String firstName = first == null ? route.nodeA() : first.displayName();
            String secondName = second == null ? route.nodeB() : second.displayName();
            source.sendSuccess(() -> Component.literal(
                    firstName + " <-> " + secondName
                            + " | " + route.state()
                            + " | " + Math.round(route.distance()) + " blocks"
                            + " | " + (route.travelTimeTicks() / 20) + " s"
            ), false);
        }
        return data.routeCount();
    }

    private static int route(CommandSourceStack source, String fromToken, String toToken) {
        MetroNetworkService.synchronize(source.getServer());

        MetroNode from = MetroNetworkService.resolveNode(source.getServer(), fromToken);
        MetroNode to = MetroNetworkService.resolveNode(source.getServer(), toToken);
        if (from == null) {
            source.sendFailure(Component.literal("\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439 \u0443\u0437\u0435\u043b \u043c\u0435\u0442\u0440\u043e: " + fromToken));
            return 0;
        }
        if (to == null) {
            source.sendFailure(Component.literal("\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u044b\u0439 \u0443\u0437\u0435\u043b \u043c\u0435\u0442\u0440\u043e: " + toToken));
            return 0;
        }

        MetroNetworkService.PathResult path = MetroNetworkService.findPath(
                source.getServer(),
                from.nodeId(),
                to.nodeId()
        );
        if (path == null) {
            source.sendFailure(Component.literal(
                    "\u041c\u0430\u0440\u0448\u0440\u0443\u0442 \u043c\u0435\u0436\u0434\u0443 "
                            + from.displayName() + " \u0438 " + to.displayName() + " \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d."
            ));
            return 0;
        }

        StringJoiner chain = new StringJoiner(" -> ");
        for (MetroNode node : path.nodes()) chain.add(node.displayName());

        source.sendSuccess(() -> Component.literal(
                "\u041c\u0430\u0440\u0448\u0440\u0443\u0442: " + chain
                        + "\n\u0414\u0438\u0441\u0442\u0430\u043d\u0446\u0438\u044f: " + Math.round(path.distance()) + " blocks"
                        + "\n\u0420\u0430\u0441\u0447\u0451\u0442\u043d\u043e\u0435 \u0432\u0440\u0435\u043c\u044f: " + (path.travelTimeTicks() / 20) + " s"
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
