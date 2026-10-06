package com.wasted.domesurvival.forge.metro;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.metro.dome.DomeMetroSavedData;
import com.wasted.domesurvival.forge.metro.dome.DomeMetroService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MetroCommands {
    private MetroCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dome")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("metro")
                                .then(Commands.literal("list")
                                        .executes(ctx -> list(ctx.getSource())))
                                .then(Commands.literal("inspect")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> inspect(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")
                                                ))))
                                .then(Commands.literal("restore")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> forceRestore(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")
                                                ))))
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> reset(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")
                                                ))))
                                .then(Commands.literal("dome")
                                        .then(Commands.literal("unlock")
                                                .executes(ctx -> unlockDomeMetro(ctx.getSource())))
                                        .then(Commands.literal("status")
                                                .executes(ctx -> domeStatus(ctx.getSource()))))
                        )
        );
    }

    private static int list(CommandSourceStack source) {
        MetroStationSavedData data = MetroStationSavedData.get(source.getServer());
        source.sendSuccess(() -> Component.literal("Metro stations: " + data.size())
                .withStyle(ChatFormatting.AQUA), false);
        for (MetroStationRecord record : data.all()) {
            source.sendSuccess(() -> Component.literal(
                    record.stationName() + " | " + record.stationId()
                            + " | " + record.state()
                            + " | level=" + (record.state() == StationState.RESTORED ? "2" : "-")
                            + " | discovered=" + record.discovered()
                            + " | " + record.dimension()
                            + " @ " + record.anchor().toShortString()
            ), false);
        }
        return data.size();
    }

    private static int inspect(CommandSourceStack source, String rawId) {
        UUID id = parseId(source, rawId);
        if (id == null) return 0;
        MetroStationRecord record = MetroStationSavedData.get(source.getServer()).get(id);
        if (record == null) {
            source.sendFailure(Component.literal("Unknown metro station: " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                record.stationName() + "\n"
                        + "id=" + record.stationId() + "\n"
                        + "dimension=" + record.dimension() + "\n"
                        + "anchor=" + record.anchor().toShortString() + "\n"
                        + "state=" + record.state() + "\n"
                        + "level=" + (record.state() == StationState.RESTORED ? "LEVEL 2 / METRO HUB" : "UNRESTORED") + "\n"
                        + "discovered=" + record.discovered() + "\n"
                        + "restored=" + record.restored() + "\n"
                        + "restoredBy=" + (record.restoredBy() == null ? "<none>" : record.restoredBy())
        ), false);
        return 1;
    }

    private static int forceRestore(CommandSourceStack source, String rawId) {
        UUID id = parseId(source, rawId);
        if (id == null) return 0;
        MetroStationRecord record = MetroStationSavedData.get(source.getServer()).get(id);
        if (record == null) {
            source.sendFailure(Component.literal("Unknown metro station: " + id));
            return 0;
        }

        ServerLevel level = resolveLevel(source, record);
        if (level == null) return 0;
        UUID actor = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
        if (!MetroRestorationService.forceRestore(level, id, actor)) {
            source.sendFailure(Component.literal("Could not restore metro station: " + id));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Metro station restored: " + id)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, String rawId) {
        UUID id = parseId(source, rawId);
        if (id == null) return 0;
        MetroStationRecord record = MetroStationSavedData.get(source.getServer()).get(id);
        if (record == null) {
            source.sendFailure(Component.literal("Unknown metro station: " + id));
            return 0;
        }

        ServerLevel level = resolveLevel(source, record);
        if (level == null) return 0;
        if (!MetroRestorationService.reset(level, id)) {
            source.sendFailure(Component.literal("Could not reset metro station: " + id));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Metro station reset to ABANDONED: " + id)
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int unlockDomeMetro(CommandSourceStack source) {
        UUID actor = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
        DomeMetroService.unlock(source.getServer(), actor);
        DomeMetroSavedData data = DomeMetroSavedData.get(source.getServer());
        if (!data.isBuilt()) {
            source.sendFailure(text("\u041c\u0435\u0442\u0440\u043e \u041a\u0443\u043f\u043e\u043b\u0430 \u0440\u0430\u0437\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043d\u043e, \u043d\u043e \u0441\u0442\u0430\u043d\u0446\u0438\u044e \u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u0441\u0442\u0440\u043e\u0438\u0442\u044c."));
            return 0;
        }
        source.sendSuccess(() -> text("\u041c\u0435\u0442\u0440\u043e \u041a\u0443\u043f\u043e\u043b\u0430: \u041e\u0422\u041a\u0420\u042b\u0422\u041e").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int domeStatus(CommandSourceStack source) {
        DomeMetroSavedData data = DomeMetroSavedData.get(source.getServer());
        source.sendSuccess(() -> text("\u0426\u0435\u043d\u0442\u0440\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0442\u0430\u043d\u0446\u0438\u044f \u041a\u0443\u043f\u043e\u043b\u0430"), false);
        source.sendSuccess(() -> Component.literal(
                "DOME_METRO_UNLOCKED=" + data.unlocked() + "\n"
                        + "DomeMetroVersion=" + data.domeMetroVersion() + "\n"
                        + "node=" + DomeMetroService.DOME_NODE_ID + "\n"
                        + "dimension=" + data.dimension() + "\n"
                        + "domeAnchor=" + pos(data.domeAnchor()) + "\n"
                        + "stationAnchor=" + pos(data.stationAnchor()) + "\n"
                        + "trainConsole=" + pos(data.trainConsolePos())
        ), false);
        return data.isBuilt() ? 1 : 0;
    }

    private static String pos(BlockPos pos) {
        return pos == null ? "<none>" : pos.toShortString();
    }

    private static MutableComponent text(String value) {
        return Component.literal(value);
    }

    private static ServerLevel resolveLevel(CommandSourceStack source, MetroStationRecord record) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(record.dimension());
        if (dimensionId == null) {
            source.sendFailure(Component.literal("Invalid station dimension: " + record.dimension()));
            return null;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = source.getServer().getLevel(dimension);
        if (level == null) {
            source.sendFailure(Component.literal("Station dimension is not available: " + record.dimension()));
        }
        return level;
    }

    private static UUID parseId(CommandSourceStack source, String rawId) {
        try {
            return UUID.fromString(rawId);
        }
        catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("Invalid station UUID: " + rawId));
            return null;
        }
    }
}
