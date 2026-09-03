package com.wasted.domesurvival.forge.progression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.environment.LastWorldWorldState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;

/**
 * Optional CustomNPCs integration.
 *
 * No compile-time CustomNPCs dependency is used. The integration activates
 * only in a normal Forge instance where the CustomNPCs classes are present.
 */
@Mod.EventBusSubscriber(modid = "domesurvival")
public final class JosephNpcCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NPC_TAG = "domesurvival_joseph_kupper";

    public enum EnsureResult {
        CREATED,
        ALREADY_PRESENT,
        SCRIPT_PENDING,
        API_UNAVAILABLE,
        FAILED
    }

    public record StarterNpcRestore(
            EnsureResult joseph,
            EnsureResult securityOfficer,
            EnsureResult expeditionSoldier
    ) {
        public boolean complete() {
            return ready(joseph) && ready(securityOfficer) && ready(expeditionSoldier);
        }

        private static boolean ready(EnsureResult result) {
            return result == EnsureResult.CREATED || result == EnsureResult.ALREADY_PRESENT;
        }
    }

    private record AmbientNpc(
            String tag,
            String name,
            String title,
            String skin,
            String script,
            double offsetX,
            double offsetZ
    ) {
    }

    private static final AmbientNpc SECURITY_OFFICER = new AmbientNpc(
            "domesurvival_ambient_security",
            "iVan",
            "Служба безопасности купола",
            "domesurvival:textures/npc/dome_security_officer.png",
            "ambient_security_officer.js",
            -28.938D,
            -23.466D
    );
    private static final AmbientNpc EXPEDITION_SOLDIER = new AmbientNpc(
            "domesurvival_ambient_expedition",
            "maneogflow",
            "Экспедиционный корпус",
            "domesurvival:textures/npc/expedition_soldier.png",
            "ambient_expedition_soldier.js",
            -2.950D,
            44.412D
    );

    // Exact point supplied by the user from F3.
    private static final double OFFSET_X = -0.475D;
    private static final double OFFSET_Z = -5.469D;

    // Minecraft yaw 0 = south.
    private static final float YAW = 0.0F;
    private static int maintenanceTicks;
    private static boolean maintenanceComplete;

    private JosephNpcCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || maintenanceComplete || ++maintenanceTicks < 100) {
            return;
        }
        maintenanceTicks = 0;
        ServerLevel level = event.getServer().overworld();
        if (!LastWorldWorldState.isLastWorld(level) || !DomeSavedData.get(level).isGenerated()) {
            return;
        }

        StarterNpcRestore result = ensureStarterNpcs(level);
        maintenanceComplete = result.complete();
        if (maintenanceComplete) {
            LOGGER.info("Starter dome NPCs repaired and scripts attached: {}", result);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        maintenanceTicks = 0;
        maintenanceComplete = false;
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
        NpcPosition position = position(level);

        source.sendSuccess(
                () -> Component.literal("Джозеф Куппер — Координатор купола")
                        .withStyle(ChatFormatting.GOLD),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Точка: X=" + position.x() + " Y=" + position.y() + " Z=" + position.z() + " | направление: юг"
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
        EnsureResult result = ensurePresent(source.getServer().overworld());
        if (result == EnsureResult.CREATED) {
            source.sendSuccess(() -> Component.literal(
                    "Джозеф Куппер создан на базе, его сценарий подключён автоматически."
            ).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        if (result == EnsureResult.ALREADY_PRESENT) {
            source.sendFailure(Component.literal("Джозеф Куппер уже существует рядом с заданной точкой."));
        } else if (result == EnsureResult.API_UNAVAILABLE) {
            source.sendFailure(Component.literal("CustomNPCs не загружен или его API ещё не готов."));
        } else {
            source.sendFailure(Component.literal("CustomNPCs не смог создать Джозефа. Подробности записаны в latest.log."));
        }
        return 0;
    }

    /** Ensures the story NPC exists at the local position of the movable dome. */
    public static EnsureResult ensurePresent(ServerLevel level) {
        if (!isCustomNpcsAvailable()) {
            return EnsureResult.API_UNAVAILABLE;
        }

        NpcPosition position = position(level);
        try {
            List<LivingEntity> candidates = findNpcInDome(level, NPC_TAG, "Джозеф");
            boolean created = candidates.isEmpty();
            Object npc;
            if (created) {
                npc = spawnNpc(level, position);
                if (npc == null) {
                    return EnsureResult.FAILED;
                }
            } else {
                LivingEntity keeper = nearest(candidates, position);
                candidates.stream().filter(entity -> entity != keeper).forEach(LivingEntity::discard);
                npc = keeper;
            }

            configureNpc(npc, position);
            boolean attached = tryAttachDefaultScript(npc);
            if (!attached) {
                return EnsureResult.SCRIPT_PENDING;
            }
            return created ? EnsureResult.CREATED : EnsureResult.ALREADY_PRESENT;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Could not restore Joseph at the portable dome", exception);
            return EnsureResult.FAILED;
        }
    }

    /** Restores every NPC that belongs to the authored starter dome. */
    public static StarterNpcRestore ensureStarterNpcs(ServerLevel level) {
        return new StarterNpcRestore(
                ensurePresent(level),
                ensureAmbient(level, SECURITY_OFFICER),
                ensureAmbient(level, EXPEDITION_SOLDIER)
        );
    }

    private static EnsureResult ensureAmbient(ServerLevel level, AmbientNpc definition) {
        if (!isCustomNpcsAvailable()) {
            return EnsureResult.API_UNAVAILABLE;
        }

        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        NpcPosition position = new NpcPosition(
                spec.centerX() + definition.offsetX(),
                spec.baseY(),
                spec.centerZ() + definition.offsetZ()
        );
        try {
            List<LivingEntity> candidates = findNpcInDome(
                    level, definition.tag(), definition.name()
            );
            boolean created = candidates.isEmpty();
            Object npc;
            if (created) {
                npc = spawnNpc(level, position);
                if (npc == null) {
                    return EnsureResult.FAILED;
                }
            } else {
                LivingEntity keeper = nearest(candidates, position);
                candidates.stream().filter(entity -> entity != keeper).forEach(LivingEntity::discard);
                npc = keeper;
            }
            configureAmbientNpc(npc, position, definition);
            boolean attached = tryAttachScript(npc, definition.script());
            if (!attached) {
                return EnsureResult.SCRIPT_PENDING;
            }
            return created ? EnsureResult.CREATED : EnsureResult.ALREADY_PRESENT;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Could not restore starter NPC {}", definition.name(), exception);
            return EnsureResult.FAILED;
        }
    }

    private static void configureNpc(Object npc, NpcPosition position) throws ReflectiveOperationException {
        Object apiNpc = apiNpc(npc);
        Class<?> npcClass = apiNpc.getClass();

        invoke(apiNpc, "setPosition",
                new Class<?>[]{double.class, double.class, double.class},
                position.x(), position.y(), position.z());

        invoke(apiNpc, "setRotation",
                new Class<?>[]{float.class},
                YAW);

        invoke(apiNpc, "setHome",
                new Class<?>[]{int.class, int.class, int.class},
                (int) Math.floor(position.x()),
                (int) Math.floor(position.y()),
                (int) Math.floor(position.z()));

        invoke(apiNpc, "addTag",
                new Class<?>[]{String.class},
                NPC_TAG);

        Object display = npcClass.getMethod("getDisplay").invoke(apiNpc);
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

        Object ai = npcClass.getMethod("getAi").invoke(apiNpc);
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

        invoke(apiNpc, "updateClient", new Class<?>[0]);
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
        return findNpcInDome(level, NPC_TAG, "Джозеф");
    }

    private static List<LivingEntity> findNpcInDome(
            ServerLevel level,
            String tag,
            String expectedName
    ) {
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        double radius = spec.surfaceRadius() + 8.0D;
        AABB area = new AABB(
                spec.centerX() - radius, spec.foundationMinY() - 4, spec.centerZ() - radius,
                spec.centerX() + radius, spec.topY() + 8, spec.centerZ() + radius
        );
        return level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> matchesNpc(entity, tag, expectedName)
        );
    }

    private static boolean matchesNpc(LivingEntity entity, String tag, String expectedName) {
        if (entity.getTags().contains(tag)) {
            return true;
        }
        String name = entity.getName().getString().toLowerCase(Locale.ROOT);
        String expected = expectedName.toLowerCase(Locale.ROOT);
        return name.contains(expected)
                || (tag.equals(NPC_TAG) && (name.contains("joseph") || name.contains("куппер")));
    }

    private static LivingEntity nearest(List<LivingEntity> entities, NpcPosition position) {
        return entities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(
                        position.x(), position.y(), position.z()
                )))
                .orElseThrow();
    }

    private static List<LivingEntity> findNpc(
            ServerLevel level,
            NpcPosition position,
            String tag,
            String expectedName
    ) {
        AABB area = new AABB(
                position.x() - 8.0D, position.y() - 8.0D, position.z() - 8.0D,
                position.x() + 8.0D, position.y() + 8.0D, position.z() + 8.0D
        );

        return level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> {
                    if (entity.getTags().contains(tag)) {
                        return true;
                    }
                    String name = entity.getName().getString().toLowerCase(Locale.ROOT);
                    String expected = expectedName.toLowerCase(Locale.ROOT);
                    return name.contains(expected)
                            || (tag.equals(NPC_TAG)
                            && (name.contains("joseph") || name.contains("куппер")));
                }
        );
    }

    private static NpcPosition position(ServerLevel level) {
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        return new NpcPosition(
                spec.centerX() + OFFSET_X,
                spec.baseY(),
                spec.centerZ() + OFFSET_Z
        );
    }

    private record NpcPosition(double x, double y, double z) {
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

    private static boolean tryAttachDefaultScript(Object npc) {
        return tryAttachScript(npc, "joseph_cooper_gui.js");
    }

    private static boolean tryAttachScript(Object npc, String scriptFile) {
        try {
            Class<?> command = Class.forName(
                    "com.wasted.domesurvival.forge.integration.customnpcs.JosephScriptCommand"
            );
            Object attached = command.getMethod("attachScript", Object.class, String.class)
                    .invoke(null, npc, scriptFile);
            if (!Boolean.TRUE.equals(attached)) {
                LOGGER.warn("NPC was restored, but {} was not ready to attach", scriptFile);
            }
            return Boolean.TRUE.equals(attached);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("NPC was restored without automatic script attachment: {}", scriptFile, exception);
            return false;
        }
    }

    private static Object spawnNpc(ServerLevel level, NpcPosition position)
            throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("noppes.npcs.api.NpcAPI");
        Object api = apiClass.getMethod("Instance").invoke(null);
        if (api == null) {
            return null;
        }
        Method spawnNpc = apiClass.getMethod(
                "spawnNPC", Level.class, int.class, int.class, int.class
        );
        return spawnNpc.invoke(
                api,
                level,
                (int) Math.floor(position.x()),
                (int) Math.floor(position.y()),
                (int) Math.floor(position.z())
        );
    }

    private static void configureAmbientNpc(
            Object npc,
            NpcPosition position,
            AmbientNpc definition
    ) throws ReflectiveOperationException {
        Object apiNpc = apiNpc(npc);
        Class<?> npcClass = apiNpc.getClass();
        invoke(apiNpc, "setPosition",
                new Class<?>[]{double.class, double.class, double.class},
                position.x(), position.y(), position.z());
        invoke(apiNpc, "setRotation", new Class<?>[]{float.class}, YAW);
        invoke(apiNpc, "setHome",
                new Class<?>[]{int.class, int.class, int.class},
                (int) Math.floor(position.x()),
                (int) Math.floor(position.y()),
                (int) Math.floor(position.z()));
        invoke(apiNpc, "addTag", new Class<?>[]{String.class}, definition.tag());

        Object display = npcClass.getMethod("getDisplay").invoke(apiNpc);
        invoke(display, "setName", new Class<?>[]{String.class}, definition.name());
        invoke(display, "setTitle", new Class<?>[]{String.class}, definition.title());
        invoke(display, "setSkinTexture", new Class<?>[]{String.class}, definition.skin());
        invoke(display, "setShowName", new Class<?>[]{int.class}, 0);

        Object ai = npcClass.getMethod("getAi").invoke(apiNpc);
        invoke(ai, "setMovingType", new Class<?>[]{int.class}, 0);
        invoke(ai, "setReturnsHome", new Class<?>[]{boolean.class}, false);
        invoke(ai, "setRetaliateType", new Class<?>[]{int.class}, 3);
        invoke(ai, "setInteractWithNPCs", new Class<?>[]{boolean.class}, false);
        invoke(apiNpc, "updateClient", new Class<?>[0]);
    }

    /** Converts a raw Minecraft CustomNPC entity to its stable API wrapper. */
    private static Object apiNpc(Object npc) throws ReflectiveOperationException {
        if (!(npc instanceof Entity entity)) {
            return npc;
        }
        Class<?> apiClass = Class.forName("noppes.npcs.api.NpcAPI");
        Object api = apiClass.getMethod("Instance").invoke(null);
        if (api == null) {
            throw new IllegalStateException("NpcAPI.Instance() returned null");
        }
        return apiClass.getMethod("getIEntity", Entity.class).invoke(api, entity);
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
