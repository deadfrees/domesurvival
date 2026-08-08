package com.wasted.domesurvival.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.wasted.domesurvival.core.airlock.AirlockDoor;
import com.wasted.domesurvival.core.airlock.AirlockState;
import com.wasted.domesurvival.core.airlock.AirlockTransition;
import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.forge.airlock.AirlockService;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.dome.DomeGenerationService;
import com.wasted.domesurvival.forge.dome.DomePreview;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class DomeCommands {
    private DomeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dome")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("preview").executes(ctx -> preview(ctx.getSource())))
                .then(Commands.literal("generate").executes(ctx -> generate(ctx.getSource())))
                .then(Commands.literal("upgrade").executes(ctx -> upgrade(ctx.getSource())))
                .then(Commands.literal("check").executes(ctx -> check(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("airlock")
                        .then(Commands.literal("status").executes(ctx -> airlockStatus(ctx.getSource())))
                        .then(Commands.literal("inner").executes(ctx -> toggleAirlock(ctx.getSource(), AirlockDoor.INNER)))
                        .then(Commands.literal("outer").executes(ctx -> toggleAirlock(ctx.getSource(), AirlockDoor.OUTER)))
                        .then(Commands.literal("reset").executes(ctx -> resetAirlock(ctx.getSource())))));
    }

    private static int preview(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int particles = DomePreview.show(level);
        DomeSpec spec = DomeSpec.wastedV1();
        source.sendSuccess(() -> Component.literal(
                "Dome preview: center=" + spec.centerX() + "," + spec.baseY() + "," + spec.centerZ()
                        + " R=" + spec.surfaceRadius() + ", airlockX=" + spec.airlockCenterX()
                        + ", particles=" + particles), false);
        return 1;
    }

    private static int generate(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        DomeGenerationService.StartResult result = DomeGenerationService.startGenerate(level);
        switch (result) {
            case STARTED -> source.sendSuccess(() -> Component.literal(
                    "Dome V2 generation started: " + DomeGenerationService.total() + " block operations queued."), true);
            case ALREADY_RUNNING -> source.sendFailure(Component.literal("Dome generation/update is already running."));
            case ALREADY_GENERATED -> source.sendFailure(Component.literal("Dome already exists. Use /dome upgrade."));
            default -> source.sendFailure(Component.literal("Unable to start dome generation: " + result));
        }
        return result == DomeGenerationService.StartResult.STARTED ? 1 : 0;
    }

    private static int upgrade(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        DomeGenerationService.StartResult result = DomeGenerationService.startUpgrade(level);
        switch (result) {
            case STARTED -> source.sendSuccess(() -> Component.literal(
                    "Dome upgrade to V2 started: " + DomeGenerationService.total() + " block operations queued."), true);
            case ALREADY_RUNNING -> source.sendFailure(Component.literal("Dome generation/update is already running."));
            case NOT_GENERATED -> source.sendFailure(Component.literal("No generated dome found. Use /dome generate."));
            case UP_TO_DATE -> source.sendSuccess(() -> Component.literal("Dome structure is already V2/current."), false);
            default -> source.sendFailure(Component.literal("Unable to start dome upgrade: " + result));
        }
        return result == DomeGenerationService.StartResult.STARTED ? 1 : 0;
    }

    private static int check(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        DomeBounds bounds = new DomeBounds(DomeSpec.wastedV1());
        DomeZone zone = bounds.classify(pos.x, pos.y, pos.z);
        boolean safe = zone == DomeZone.AIRLOCK ? AirlockService.isBreathable(level) : zone.isSafe();
        source.sendSuccess(() -> Component.literal(String.format(
                "Dome zone @ %.1f %.1f %.1f: %s, breathable=%s",
                pos.x, pos.y, pos.z, zone.name(), safe)), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        DomeSavedData data = DomeSavedData.get(level);
        DomeSpec spec = DomeSpec.wastedV1();
        AirlockState airlock = data.airlockState();
        source.sendSuccess(() -> Component.literal(
                "Dome status: generated=" + data.isGenerated()
                        + ", structureVersion=" + data.structureVersion()
                        + "/" + DomeGenerationService.CURRENT_STRUCTURE_VERSION
                        + ", running=" + DomeGenerationService.isRunning()
                        + ", operation=" + DomeGenerationService.operationName()
                        + ", progress=" + DomeGenerationService.placed() + "/" + DomeGenerationService.total()
                        + ", airlockPressure=" + airlock.pressure()
                        + ", innerOpen=" + airlock.innerOpen()
                        + ", outerOpen=" + airlock.outerOpen()
                        + ", airlock=" + spec.airlockCenterX() + "," + spec.baseY() + "," + spec.airlockStartZ()
                        + ".." + spec.airlockEndZ()), false);
        return 1;
    }

    private static int airlockStatus(CommandSourceStack source) {
        AirlockState state = AirlockService.state(source.getLevel());
        source.sendSuccess(() -> Component.literal("Airlock: " + AirlockService.localizedState(state)), false);
        return 1;
    }

    private static int toggleAirlock(CommandSourceStack source, AirlockDoor door) {
        AirlockTransition result = AirlockService.toggle(source.getLevel(), door);
        if (result.allowed()) {
            source.sendSuccess(() -> Component.literal("Airlock: " + AirlockService.localizedState(result.state())), true);
            return 1;
        }
        source.sendFailure(Component.literal("Airlock interlock: " + result.message()));
        return 0;
    }

    private static int resetAirlock(CommandSourceStack source) {
        AirlockService.reset(source.getLevel());
        source.sendSuccess(() -> Component.literal("Airlock reset: both shutters closed, chamber pressurized."), true);
        return 1;
    }
}
