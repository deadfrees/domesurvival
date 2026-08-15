package com.wasted.domesurvival.forge.progression;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Optional CustomNPCs integration.
 *
 * No compile-time CustomNPCs dependency is used. The integration activates
 * only in a normal Forge instance where the CustomNPCs classes are present.
 */
@Mod.EventBusSubscriber(modid = "domesurvival")
public final class JosephNpcCommands {
    private static final String NPC_TAG = "domesurvival_joseph_kupper";

    // Exact point supplied by the user from F3.
    private static final double X = -506.475D;
    private static final double Y = 62.000D;
    private static final double Z = -646.469D;

    // Minecraft yaw 0 = south.
    private static final float YAW = 0.0F;

    private JosephNpcCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dome")
                        .then(Commands.literal("npc")
                                .then(Commands.literal("joseph")
                                        .then(Commands.literal("status")
                                                .executes(ctx -> status(ctx.getSource())))
                                        .then(Commands.literal("spawn")
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> spawn(ctx.getSource())))
                                        .then(Commands.literal("remove")
                                                .requires(source -> source.hasPermission(2))
                                                .executes(ctx -> remove(ctx.getSource())))))
        );
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();

        source.sendSuccess(
                () -> Component.literal("Джозеф Куппер — Координатор купола")
                        .withStyle(ChatFormatting.GOLD),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Точка: X=" + X + " Y=" + Y + " Z=" + Z + " | направление: юг"
                ).withStyle(ChatFormatting.GRAY),
                false
        );

        if (!isCustomNpcsAvailable()) {
            source.sendFailure(Component.literal(
                    "CustomNPCs не загружен в этом запуске. В ForgeGradle runClient это ожидаемо."
            ));
            return 0;
        }

        List<LivingEntity> existing = findJoseph(level);
        if (existing.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("NPC пока не найден в загруженной области.")
                            .withStyle(ChatFormatting.YELLOW),
                    false
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("NPC найден: " + existing.size())
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int spawn(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();

        if (!isCustomNpcsAvailable()) {
            source.sendFailure(Component.literal(
                    "CustomNPCs не загружен. Эту команду нужно выполнять в обычном Forge-инстансе с CustomNPCs."
            ));
            return 0;
        }

        if (!findJoseph(level).isEmpty()) {
            source.sendFailure(Component.literal(
                    "Джозеф Куппер уже существует рядом с заданной точкой."
            ));
            return 0;
        }

        try {
            Class<?> apiClass = Class.forName("noppes.npcs.api.NpcAPI");

            Object api = apiClass.getMethod("Instance").invoke(null);
            if (api == null) {
                source.sendFailure(Component.literal("NpcAPI.Instance() вернул null."));
                return 0;
            }

            Method spawnNpc = apiClass.getMethod(
                    "spawnNPC",
                    Level.class,
                    int.class,
                    int.class,
                    int.class
            );

            // Integer spawn first, then exact center/rotation below.
            Object npc = spawnNpc.invoke(
                    api,
                    level,
                    (int) Math.floor(X),
                    (int) Math.floor(Y),
                    (int) Math.floor(Z)
            );

            if (npc == null) {
                source.sendFailure(Component.literal("CustomNPCs не создал NPC."));
                return 0;
            }

            configureNpc(npc);

            source.sendSuccess(
                    () -> Component.literal(
                            "Джозеф Куппер создан на базе. Теперь подключи к нему joseph_cooper_gui.js через Scripter."
                    ).withStyle(ChatFormatting.GREEN),
                    true
            );

            return 1;
        } catch (ReflectiveOperationException exception) {
            source.sendFailure(Component.literal(
                    "Не удалось вызвать API CustomNPCs: "
                            + exception.getClass().getSimpleName()
                            + ": "
                            + String.valueOf(exception.getMessage())
            ));
            return 0;
        }
    }

    private static void configureNpc(Object npc) throws ReflectiveOperationException {
        Class<?> npcClass = npc.getClass();

        invoke(npc, "setPosition",
                new Class<?>[]{double.class, double.class, double.class},
                X, Y, Z);

        invoke(npc, "setRotation",
                new Class<?>[]{float.class},
                YAW);

        invoke(npc, "setHome",
                new Class<?>[]{int.class, int.class, int.class},
                (int) Math.floor(X),
                (int) Math.floor(Y),
                (int) Math.floor(Z));

        invoke(npc, "addTag",
                new Class<?>[]{String.class},
                NPC_TAG);

        Object display = npcClass.getMethod("getDisplay").invoke(npc);
        invoke(display, "setName",
                new Class<?>[]{String.class},
                "Джозеф Куппер");
        invoke(display, "setTitle",
                new Class<?>[]{String.class},
                "Координатор купола");
        invoke(display, "setSkinTexture",
                new Class<?>[]{String.class},
                "domesurvival:textures/npc/joseph_cooper.png");
        invoke(display, "setShowName",
                new Class<?>[]{int.class},
                0);

        Object ai = npcClass.getMethod("getAi").invoke(npc);
        invoke(ai, "setMovingType",
                new Class<?>[]{int.class},
                0);
        invoke(ai, "setReturnsHome",
                new Class<?>[]{boolean.class},
                true);
        invoke(ai, "setRetaliateType",
                new Class<?>[]{int.class},
                3);
        invoke(ai, "setInteractWithNPCs",
                new Class<?>[]{boolean.class},
                false);
        invoke(ai, "setStandingType",
                new Class<?>[]{int.class},
                0);

        invoke(npc, "updateClient", new Class<?>[0]);
    }

    private static int remove(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        List<LivingEntity> found = findJoseph(level);

        if (found.isEmpty()) {
            source.sendFailure(Component.literal("Джозеф Куппер не найден рядом с заданной точкой."));
            return 0;
        }

        for (LivingEntity entity : found) {
            entity.discard();
        }

        source.sendSuccess(
                () -> Component.literal("Удалено NPC Джозеф Куппер: " + found.size())
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
        return found.size();
    }

    private static List<LivingEntity> findJoseph(ServerLevel level) {
        AABB area = new AABB(
                X - 8.0D, Y - 8.0D, Z - 8.0D,
                X + 8.0D, Y + 8.0D, Z + 8.0D
        );

        return level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.getTags().contains(NPC_TAG)
        );
    }

    private static boolean isCustomNpcsAvailable() {
        try {
            Class<?> apiClass = Class.forName("noppes.npcs.api.NpcAPI");
            Object result = apiClass.getMethod("IsAvailable").invoke(null);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static Object invoke(
            Object target,
            String method,
            Class<?>[] signature,
            Object... arguments
    ) throws ReflectiveOperationException {
        return target.getClass().getMethod(method, signature).invoke(target, arguments);
    }
}
