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
import com.wasted.domesurvival.forge.environment.SurfaceExposure;
import com.wasted.domesurvival.forge.environment.SurfaceHazardEnvironment;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.oxygen.OxygenEnvironment;
import com.wasted.domesurvival.forge.oxygen.OxygenEquipment;
import com.wasted.domesurvival.forge.oxygen.OxygenService;
import com.wasted.domesurvival.forge.oxygen.PlayerOxygenData;
import com.wasted.domesurvival.forge.weather.SurfaceWeatherService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
                .then(Commands.literal("survival")
                        .then(Commands.literal("status").executes(ctx -> survivalStatus(ctx.getSource())))
                        .then(Commands.literal("reset").executes(ctx -> survivalReset(ctx.getSource())))
                        .then(Commands.literal("kit").executes(ctx -> survivalKit(ctx.getSource())))
                        .then(Commands.literal("oxygen")
                                .then(Commands.literal("full").executes(ctx -> oxygenFull(ctx.getSource())))
                                .then(Commands.literal("empty").executes(ctx -> oxygenEmpty(ctx.getSource()))))
                        .then(Commands.literal("tank")
                                .then(Commands.literal("full").executes(ctx -> tankFull(ctx.getSource())))
                                .then(Commands.literal("empty").executes(ctx -> tankEmpty(ctx.getSource()))))
                        .then(Commands.literal("day").executes(ctx -> survivalDay(ctx.getSource())))
                        .then(Commands.literal("night").executes(ctx -> survivalNight(ctx.getSource())))
                        .then(Commands.literal("weather")
                                .then(Commands.literal("clear").executes(ctx -> survivalWeatherClear(ctx.getSource())))
                                .then(Commands.literal("rain").executes(ctx -> survivalWeatherRain(ctx.getSource())))
                                .then(Commands.literal("thunder").executes(ctx -> survivalWeatherThunder(ctx.getSource())))
                                .then(Commands.literal("sandstorm").executes(ctx -> survivalWeatherSandstorm(ctx.getSource())))))
                .then(Commands.literal("airlock")
                        .then(Commands.literal("status").executes(ctx -> airlockStatus(ctx.getSource())))
                        .then(Commands.literal("inner").executes(ctx -> toggleAirlock(ctx.getSource(), AirlockDoor.INNER)))
                        .then(Commands.literal("outer").executes(ctx -> toggleAirlock(ctx.getSource(), AirlockDoor.OUTER)))
                        .then(Commands.literal("reset").executes(ctx -> resetAirlock(ctx.getSource()))))
                .then(Commands.literal("weather")
                        .then(Commands.literal("status").executes(ctx -> weatherStatus(ctx.getSource())))
                        .then(Commands.literal("sandstorm")
                                .then(Commands.literal("start").executes(ctx -> startSandstorm(ctx.getSource())))
                                .then(Commands.literal("stop").executes(ctx -> stopSandstorm(ctx.getSource()))))));
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
                    "Dome current generation started: " + DomeGenerationService.total() + " block operations queued."), true);
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
                    "Dome upgrade to current structure started: " + DomeGenerationService.total() + " block operations queued."), true);
            case ALREADY_RUNNING -> source.sendFailure(Component.literal("Dome generation/update is already running."));
            case NOT_GENERATED -> source.sendFailure(Component.literal("No generated dome found. Use /dome generate."));
            case UP_TO_DATE -> source.sendSuccess(() -> Component.literal("Dome structure is already current."), false);
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
        AirlockState airlock = AirlockService.state(level);
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
        source.sendSuccess(() -> AirlockService.statusComponent(state), false);
        return 1;
    }

    private static int toggleAirlock(CommandSourceStack source, AirlockDoor door) {
        AirlockTransition result = AirlockService.toggle(source.getLevel(), door);
        if (result.allowed()) {
            source.sendSuccess(() -> AirlockService.doorStatusComponent(door, result.state()), true);
            return 1;
        }
        source.sendFailure(AirlockService.interlockComponent(result.message()));
        return 0;
    }


    private static int survivalStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        DomeBounds bounds = new DomeBounds(DomeSpec.wastedV1());
        DomeZone zone = bounds.classify(player.getX(), player.getY(), player.getZ());
        boolean breathable = OxygenEnvironment.isBreathable(player);
        boolean mask = OxygenEquipment.hasMask(player);
        OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
        boolean tankReady = OxygenEquipment.tankEquipmentReady(player, tank);

        int reserve = PlayerOxygenData.oxygen(player);
        int reserveMax = PlayerOxygenData.maxOxygen(player);
        String tankText = tank == null
                ? "none"
                : tank.oxygen() + "/" + tank.capacity();

        BlockPos eye = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        boolean sky = level.canSeeSky(eye);
        SurfaceExposure exposure = SurfaceHazardEnvironment.exposure(player);
        boolean weatherExposed = SurfaceHazardEnvironment.directlyExposedToWeather(player);

        source.sendSuccess(() -> Component.literal(
                "SURVIVAL TEST @ "
                        + String.format("%.1f %.1f %.1f", player.getX(), player.getY(), player.getZ())
        ), false);

        source.sendSuccess(() -> Component.literal(
                "zone=" + zone
                        + ", domeGenerated=" + DomeSavedData.get(level).isGenerated()
                        + ", breathable=" + breathable
                        + ", canSeeSky=" + sky
        ), false);

        source.sendSuccess(() -> Component.literal(
                "oxygenReserve=" + reserve + "/" + reserveMax
                        + ", mask=" + mask
                        + ", tank=" + tankText
                        + ", tankReady=" + tankReady
        ), false);

        source.sendSuccess(() -> Component.literal(
                "weather=" + SurfaceWeatherService.currentWeather(level)
                        + ", exposure=" + exposure
                        + ", directlyWeatherExposed=" + weatherExposed
                        + ", day=" + level.isDay()
                        + ", raining=" + level.isRaining()
                        + ", thundering=" + level.isThundering()
        ), false);

        source.sendSuccess(() -> Component.literal(
                "health=" + String.format("%.1f/%.1f", player.getHealth(), player.getMaxHealth())
                        + " | mask+tank protects oxygen ONLY; it does not block sun/acid rain/sandstorm damage."
        ), false);

        OxygenService.forceSync(player);
        return 1;
    }

    private static int survivalReset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        PlayerOxygenData.reset(player);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        OxygenService.forceSync(player);

        source.sendSuccess(() -> Component.literal(
                "Survival test reset: lungs full, health full, food restored."
        ), false);
        return 1;
    }

    private static int survivalKit(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        ItemStack mask = new ItemStack(ModItems.OXYGEN_MASK.get());
        ItemStack tank = new ItemStack(ModItems.SMALL_OXYGEN_TANK.get());

        boolean maskAdded = player.addItem(mask);
        boolean tankAdded = player.addItem(tank);

        if (!maskAdded) {
            player.drop(mask, false);
        }
        if (!tankAdded) {
            player.drop(tank, false);
        }

        source.sendSuccess(() -> Component.literal(
                "Survival test kit issued: oxygen mask + full small oxygen tank. Equip mask in HEAD and tank in CHEST."
        ), false);
        return 1;
    }

    private static int oxygenFull(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerOxygenData.reset(player);
        OxygenService.forceSync(player);
        source.sendSuccess(() -> Component.literal("Emergency oxygen reserve set to FULL."), false);
        return 1;
    }

    private static int oxygenEmpty(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerOxygenData.set(player, 0, 0);
        OxygenService.forceSync(player);
        source.sendSuccess(() -> Component.literal(
                "Emergency oxygen reserve set to EMPTY. Suffocation begins on the next oxygen simulation update if no source is available."
        ), false);
        return 1;
    }

    private static int tankFull(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
        if (tank == null) {
            source.sendFailure(Component.literal("No DomeSurvival oxygen tank is equipped in the CHEST slot."));
            return 0;
        }

        tank.setOxygen(tank.capacity());
        OxygenService.forceSync(player);
        source.sendSuccess(() -> Component.literal(
                "Equipped oxygen tank filled: " + tank.capacity() + "/" + tank.capacity()
        ), false);
        return 1;
    }

    private static int tankEmpty(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
        if (tank == null) {
            source.sendFailure(Component.literal("No DomeSurvival oxygen tank is equipped in the CHEST slot."));
            return 0;
        }

        tank.setOxygen(0);
        OxygenService.forceSync(player);
        source.sendSuccess(() -> Component.literal("Equipped oxygen tank set to EMPTY."), false);
        return 1;
    }

    private static int survivalDay(CommandSourceStack source) {
        source.getLevel().setDayTime(1000L);
        source.sendSuccess(() -> Component.literal("Survival test time set to DAY."), false);
        return 1;
    }

    private static int survivalNight(CommandSourceStack source) {
        source.getLevel().setDayTime(13000L);
        source.sendSuccess(() -> Component.literal("Survival test time set to NIGHT."), false);
        return 1;
    }

    private static int survivalWeatherClear(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        SurfaceWeatherService.stopSandstorm(level);
        level.setWeatherParameters(6000, 0, false, false);
        source.sendSuccess(() -> Component.literal("Survival test weather set to CLEAR."), false);
        return 1;
    }

    private static int survivalWeatherRain(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        SurfaceWeatherService.stopSandstorm(level);
        level.setWeatherParameters(0, 6000, true, false);
        source.sendSuccess(() -> Component.literal("Survival test weather set to ACID RAIN."), false);
        return 1;
    }

    private static int survivalWeatherThunder(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        SurfaceWeatherService.stopSandstorm(level);
        level.setWeatherParameters(0, 6000, true, true);
        source.sendSuccess(() -> Component.literal("Survival test weather set to ACID THUNDERSTORM."), false);
        return 1;
    }

    private static int survivalWeatherSandstorm(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        level.setWeatherParameters(6000, 0, false, false);
        int seconds = SurfaceWeatherService.startSandstorm(level, 300);
        if (seconds <= 0) {
            source.sendFailure(Component.literal("Sandstorm test can only run in the overworld."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Survival test weather set to SANDSTORM for 300 seconds."), false);
        return 1;
    }

    private static int weatherStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        source.sendSuccess(() -> Component.literal(
                "Surface weather: " + SurfaceWeatherService.currentWeather(level)
                        + ", sandstormRemaining=" + SurfaceWeatherService.sandstormSecondsRemaining(level) + "s"
                        + ", nextSandstorm=" + SurfaceWeatherService.sandstormCooldownSeconds(level) + "s"), false);
        return 1;
    }

    private static int startSandstorm(CommandSourceStack source) {
        int duration = SurfaceWeatherService.startSandstorm(source.getLevel());
        if (duration <= 0) {
            source.sendFailure(Component.literal("Sandstorms can only be started in the overworld."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Sandstorm started for " + duration + " seconds."), true);
        return 1;
    }

    private static int stopSandstorm(CommandSourceStack source) {
        SurfaceWeatherService.stopSandstorm(source.getLevel());
        source.sendSuccess(() -> Component.literal("Sandstorm stopped."), true);
        return 1;
    }

    private static int resetAirlock(CommandSourceStack source) {
        AirlockService.reset(source.getLevel());
        source.sendSuccess(() -> AirlockService.statusComponent(AirlockService.state(source.getLevel())), true);
        return 1;
    }
}
