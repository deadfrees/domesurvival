package com.wasted.domesurvival.forge.quest;

import com.wasted.domesurvival.core.dome.DomeBounds;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.core.dome.DomeZone;
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.oxygen.OxygenEquipment;
import com.wasted.domesurvival.forge.oxygen.OxygenEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Drives non-clickable advancement tasks from real server-observed actions.
 *
 * No world scan is performed. The 10-tick player check is O(1), and only a
 * handful of early campaign actions are evaluated.
 */
@Mod.EventBusSubscriber(modid = "domesurvival")
public final class QuestActionEvents {
    private static final DomeBounds START_DOME = new DomeBounds(DomeSpec.wastedV1());
    private static final Map<UUID, Integer> LOGIN_DELAY = new HashMap<>();
    private static final Map<UUID, Boolean> HAS_BEEN_OUTSIDE = new HashMap<>();
    private static final Map<UUID, Integer> SOLAR_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> SHADE_TICKS = new HashMap<>();

    // Chapter 2: all state is per-player and O(1); no world scans.
    private static final Map<UUID, Long> CH2_SORTIE_START = new HashMap<>();
    private static final Map<UUID, Double> CH2_SORTIE_MAX_RADIUS = new HashMap<>();
    private static final Map<UUID, Integer> CH2_VALID_RETURNS = new HashMap<>();
    private static final Map<UUID, Integer> CH2_TEMP_BLOCKS = new HashMap<>();

    private static final double CH2_NEAR_MIN_RADIUS = 60.0D; // ~10 blocks beyond R=50 shell
    private static final double CH2_NEAR_MAX_RADIUS = 70.0D; // ~20 blocks beyond shell
    private static final double CH2_OPERATION_MAX_RADIUS = 75.0D; // hard early-operation limit
    private static final long CH2_RETURN_MAX_TICKS = 360L; // ~18 seconds
    private static final long CH2_CONTROLLED_MIN_TICKS = 120L; // ~6 seconds

    // Chapter 3 settlement construction tracking. O(1), event-driven only.
    private static final Map<UUID, Integer> CH3_WORKSHOP_MASK = new HashMap<>();
    private static final Map<UUID, Integer> CH3_BEDS = new HashMap<>();
    private static final Map<UUID, Integer> CH3_LIGHTS = new HashMap<>();
    private static final Map<UUID, Integer> CH3_AIRLOCK_WORK_MASK = new HashMap<>();

    // Chapter 4 food-system tracking. Event-driven, no world scans.
    private static final Map<UUID, Integer> CH4_LIGHTS = new HashMap<>();

    // Chapter 6 power infrastructure. Event-driven, no world scans.
    private static final Map<UUID, Integer> CH6_POWER_LINKS = new HashMap<>();
    private static final Map<UUID, Integer> CH6_LAMPS = new HashMap<>();

    // Chapter 7 oxygen industry. Placement and sortie state stays O(1) per player.
    private static final Map<UUID, Integer> CH7_OXYGEN_PIPES = new HashMap<>();
    private static final Map<UUID, Long> CH7_SORTIE_START = new HashMap<>();
    private static final Map<UUID, Double> CH7_SORTIE_MAX_RADIUS = new HashMap<>();
    private static final int CH7_OXYGEN_LINE_BLOCKS = 6;
    private static final double CH7_SORTIE_MIN_RADIUS = 90.0D;
    private static final long CH7_SORTIE_MIN_TICKS = 400L;

    private QuestActionEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Let FTB Teams/FTB Quests finish their own login team synchronization first.
            LOGIN_DELAY.put(player.getUUID(), 40);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LOGIN_DELAY.remove(id);
        HAS_BEEN_OUTSIDE.remove(id);
        SOLAR_TICKS.remove(id);
        SHADE_TICKS.remove(id);
        CH2_SORTIE_START.remove(id);
        CH2_SORTIE_MAX_RADIUS.remove(id);
        CH2_VALID_RETURNS.remove(id);
        CH2_TEMP_BLOCKS.remove(id);
        CH3_WORKSHOP_MASK.remove(id);
        CH3_BEDS.remove(id);
        CH3_LIGHTS.remove(id);
        CH3_AIRLOCK_WORK_MASK.remove(id);
        CH4_LIGHTS.remove(id);
        CH6_POWER_LINKS.remove(id);
        CH6_LAMPS.remove(id);
        CH7_OXYGEN_PIPES.remove(id);
        CH7_SORTIE_START.remove(id);
        CH7_SORTIE_MAX_RADIUS.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID id = player.getUUID();

        Integer delay = LOGIN_DELAY.get(id);
        if (delay != null) {
            if (delay > 0) {
                LOGIN_DELAY.put(id, delay - 1);
                return;
            }

            LOGIN_DELAY.remove(id);
            QuestGlobalSyncService.catchUp(player);
        }

        // 2 checks/sec. No need to evaluate atmosphere actions every tick.
        if ((player.tickCount % 10) != 0) {
            return;
        }

        if (!Level.OVERWORLD.equals(player.level().dimension())) {
            return;
        }

        if (!DomeSavedData.get(player.serverLevel()).isGenerated()) {
            return;
        }

        DomeZone zone = START_DOME.classify(player.getX(), player.getY(), player.getZ());
        boolean breathable = OxygenEnvironment.isBreathable(player);
        double horizontalRadius = horizontalRadius(player.getX(), player.getZ());
        boolean wasOutside = Boolean.TRUE.equals(HAS_BEEN_OUTSIDE.get(id));

        if (zone.isSafe()) {
            tryAction(player, QuestGlobalRegistry.Action.CH0_INTRO);
            tryAction(player, QuestGlobalRegistry.Action.CH0_BREATHABLE_DOME);

            if (Boolean.TRUE.equals(HAS_BEEN_OUTSIDE.get(id))) {
                tryAction(player, QuestGlobalRegistry.Action.CH0_RETURN_RULE);
                handleChapter2Return(player, id);
                handleChapter7Return(player, id);
                HAS_BEEN_OUTSIDE.put(id, false);
            }
        }

        if (zone == DomeZone.OUTSIDE && !breathable) {
            if (!wasOutside) {
                CH2_SORTIE_START.put(id, player.serverLevel().getGameTime());
                CH2_SORTIE_MAX_RADIUS.put(id, horizontalRadius);
            } else {
                CH2_SORTIE_MAX_RADIUS.merge(id, horizontalRadius, Math::max);
            }

            HAS_BEEN_OUTSIDE.put(id, true);
            tryAction(player, QuestGlobalRegistry.Action.CH0_NO_AIR_OUTSIDE);

            trackChapter7OxygenSortie(player, id, wasOutside, horizontalRadius);

            if (horizontalRadius <= CH2_OPERATION_MAX_RADIUS) {
                tryAction(player, QuestGlobalRegistry.Action.CH2_FIRST_EXIT);
            }

            if (horizontalRadius >= CH2_NEAR_MIN_RADIUS
                    && horizontalRadius <= CH2_NEAR_MAX_RADIUS) {
                tryAction(player, QuestGlobalRegistry.Action.CH2_NEAR_PERIMETER);
            }

            BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
            boolean openSky = player.serverLevel().canSeeSky(eyes);

            if (openSky
                    && player.serverLevel().isDay()
                    && !player.serverLevel().isRaining()
                    && !player.serverLevel().isThundering()) {
                int ticks = SOLAR_TICKS.merge(id, 10, Integer::sum);
                if (ticks >= 40) {
                    tryAction(player, QuestGlobalRegistry.Action.CH0_SOLAR);
                    SOLAR_TICKS.remove(id);
                }
            } else {
                SOLAR_TICKS.remove(id);
            }

            if (!openSky) {
                int ticks = SHADE_TICKS.merge(id, 10, Integer::sum);
                if (ticks >= 40) {
                    tryAction(player, QuestGlobalRegistry.Action.CH0_SHADE_NOT_AIR);
                    SHADE_TICKS.remove(id);
                }
            } else {
                SHADE_TICKS.remove(id);
            }
        } else {
            SOLAR_TICKS.remove(id);
            SHADE_TICKS.remove(id);
        }

        // Dependency-only transitions: no manual checkbox.
        tryAction(player, QuestGlobalRegistry.Action.CH0_FINALE);
        tryAction(player, QuestGlobalRegistry.Action.CH1_INTRO);
        tryAction(player, QuestGlobalRegistry.Action.CH1_FINALE);
        tryAction(player, QuestGlobalRegistry.Action.CH2_INTRO);
        tryAction(player, QuestGlobalRegistry.Action.CH2_FINALE);
        tryAction(player, QuestGlobalRegistry.Action.CH3_INTRO);
        tryAction(player, QuestGlobalRegistry.Action.CH3_FINALE);
        tryAction(player, QuestGlobalRegistry.Action.CH4_INTRO);
        tryAction(player, QuestGlobalRegistry.Action.CH4_FINALE);
        tryAction(player, QuestGlobalRegistry.Action.CH6_INTRO);
        tryAction(player, QuestGlobalRegistry.Action.CH6_POWER_READY);
        tryAction(player, QuestGlobalRegistry.Action.CH6_FINALE);
        tryAction(player, QuestGlobalRegistry.Action.CH7_INTRO);
        tryAction(player, QuestGlobalRegistry.Action.CH7_FINALE);

        if (hasFullOxygenTank(player)) {
            tryAction(player, QuestGlobalRegistry.Action.CH7_TANK_FILLED);
        }

        retryChapter4Actions(player);

        // If a Chapter 3 construction threshold was reached on the same tick
        // as an FTB item prerequisite, retrying here avoids a timing race.
        retryChapter3ConstructionActions(player);

        // Chapter 1 readiness is a literal physical check at the airlock:
        // stand in the airlock with water container, food and light prepared.
        if (zone == DomeZone.AIRLOCK
                && hasAtLeast(player, "minecraft:bucket", 1)
                && hasAtLeast(player, "minecraft:bread", 6)
                && hasAtLeast(player, "minecraft:torch", 10)) {
            tryAction(player, QuestGlobalRegistry.Action.CH1_READINESS);
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !Level.OVERWORLD.equals(player.level().dimension())) {
            return;
        }

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(
                player.level().getBlockState(event.getPos()).getBlock()
        );
        if (id == null) {
            return;
        }

        String key = id.toString();
        String path = id.getPath();

        BlockEntity blockEntity = player.level().getBlockEntity(event.getPos());
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
                if (energy.getMaxEnergyStored() > 0) {
                    if (isEnergyGenerator(id)) {
                        tryAction(player, QuestGlobalRegistry.Action.CH6_GENERATOR_CHECKED);
                    }

                    if (isEnergyStorage(id)) {
                        tryAction(player, QuestGlobalRegistry.Action.CH6_STORAGE_CHECKED);
                    }
                }
            });

            if ("domesurvival:water_purifier".equals(key)
                    || "domesurvival:universal_tank".equals(key)) {
                blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(fluid -> {
                    if (containsPurifiedWater(fluid)) {
                        tryAction(player, QuestGlobalRegistry.Action.CH7_PURIFIED_WATER_READY);
                    }
                });
            }

            if ("domesurvival:oxygen_electrolyzer".equals(key)) {
                blockEntity.getCapability(ModCapabilities.OXYGEN).ifPresent(oxygen -> {
                    if (oxygen.getOxygenStored() > 0) {
                        tryAction(player, QuestGlobalRegistry.Action.CH7_OXYGEN_PRODUCED);
                    }
                });
            }
        }

        if ("domesurvival:airlock_control_panel".equals(key)) {
            // Deliberate pre-exit environmental check.
            tryAction(player, QuestGlobalRegistry.Action.CH0_WEATHER_CHECK);
            tryAction(player, QuestGlobalRegistry.Action.CH1_WEATHER_CHECK);
            tryAction(player, QuestGlobalRegistry.Action.CH1_AIRLOCK_WORKSPACE);
            tryAction(player, QuestGlobalRegistry.Action.CH2_PRECHECK);
            tryAction(player, QuestGlobalRegistry.Action.CH2_POSTCHECK);
            tryAction(player, QuestGlobalRegistry.Action.CH3_SETTLEMENT_CHECK);
            tryAction(player, QuestGlobalRegistry.Action.CH6_AIRLOCK_SEPARATION);
        } else if ("domesurvival:airlock_gate".equals(key)) {
            tryAction(player, QuestGlobalRegistry.Action.CH0_AIRLOCK_GATE);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !Level.OVERWORLD.equals(player.level().dimension())) {
            return;
        }

        BlockPos pos = event.getPos();
        DomeZone zone = START_DOME.classify(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(
                event.getPlacedBlock().getBlock()
        );
        if (blockId == null) {
            return;
        }

        String key = blockId.toString();
        String path = blockId.getPath();

        // Chapter 2: only the short external operation ring.
        if (zone == DomeZone.OUTSIDE) {
            double radius = horizontalRadius(
                    pos.getX() + 0.5D,
                    pos.getZ() + 0.5D
            );

            if (radius < 50.0D || radius > CH2_OPERATION_MAX_RADIUS) {
                return;
            }

            if ("minecraft:torch".equals(key)
                    || "minecraft:wall_torch".equals(key)) {
                tryAction(player, QuestGlobalRegistry.Action.CH2_TORCH_MARKER);
            }

            if ("minecraft:cobblestone".equals(key)
                    && !QuestGlobalSyncService.isGlobalCompleted(
                            player,
                            "706EFE49CEFC5DF3"
                    )) {
                int count = CH2_TEMP_BLOCKS.merge(
                        player.getUUID(),
                        1,
                        Integer::sum
                );
                if (count >= 5) {
                    tryAction(player, QuestGlobalRegistry.Action.CH2_TEMP_MARKER);
                }
            }
            return;
        }

        // Chapter 3 settlement blocks must be placed in breathable/safe Dome space.
        if (!zone.isSafe()) {
            return;
        }

        UUID playerId = player.getUUID();

        if (zone != DomeZone.AIRLOCK
                && ("minecraft:chest".equals(key)
                || "minecraft:barrel".equals(key))) {
            tryAction(player, QuestGlobalRegistry.Action.CH3_STORAGE_PLACED);
        }

        if (zone != DomeZone.AIRLOCK) {
            int workshopMask = CH3_WORKSHOP_MASK.getOrDefault(playerId, 0);

            if ("minecraft:crafting_table".equals(key)) {
                workshopMask |= 1;
            }
            if ("minecraft:stonecutter".equals(key)) {
                workshopMask |= 2;
            }

            if (workshopMask != 0) {
                CH3_WORKSHOP_MASK.put(playerId, workshopMask);
            }
            if ((workshopMask & 3) == 3) {
                tryAction(player, QuestGlobalRegistry.Action.CH3_WORKSHOP_PLACED);
            }

            if (path.contains("copper") && path.contains("furnace")) {
                tryAction(player, QuestGlobalRegistry.Action.CH3_COPPER_FURNACE);
            }

            if (path.endsWith("_bed")) {
                int beds = CH3_BEDS.merge(playerId, 1, Integer::sum);
                if (beds >= 3) {
                    tryAction(player, QuestGlobalRegistry.Action.CH3_BEDS_PLACED);
                }
            }

            if (isSettlementLight(key)) {
                int lights = CH3_LIGHTS.merge(playerId, 1, Integer::sum);
                if (lights >= 5) {
                    tryAction(player, QuestGlobalRegistry.Action.CH3_LIVING_LIGHTS);
                }
            }
        }

        // Chapter 4 food infrastructure.
        if (zone != DomeZone.AIRLOCK) {
            if ("minecraft:composter".equals(key)) {
                tryAction(player, QuestGlobalRegistry.Action.CH4_COMPOSTER_PLACED);
            }

            if (isSettlementLight(key)) {
                int lights = CH4_LIGHTS.merge(playerId, 1, Integer::sum);
                if (lights >= 5) {
                    tryAction(player, QuestGlobalRegistry.Action.CH4_FARM_LIGHTS);
                }
            }

            if ("minecraft:chest".equals(key) || "minecraft:barrel".equals(key)) {
                tryAction(player, QuestGlobalRegistry.Action.CH4_PANTRY_PLACED);
            }
        }

        // Chapter 6 power infrastructure. Only inside safe Dome space.
        if (zone != DomeZone.AIRLOCK) {
            if (isEnergyGenerator(blockId)) {
                tryAction(player, QuestGlobalRegistry.Action.CH6_GENERATOR_PLACED);
            }

            if (isEnergyLink(blockId)) {
                int links = CH6_POWER_LINKS.merge(playerId, 1, Integer::sum);
                if (links >= 4) {
                    tryAction(player, QuestGlobalRegistry.Action.CH6_POWER_LINE);
                }
            }

            if (isEnergyStorage(blockId)) {
                tryAction(player, QuestGlobalRegistry.Action.CH6_STORAGE_PLACED);
            }

            if ("minecraft:redstone_lamp".equals(key)) {
                int lamps = CH6_LAMPS.merge(playerId, 1, Integer::sum);
                if (lamps >= 5) {
                    tryAction(player, QuestGlobalRegistry.Action.CH6_LIGHTING_PLACED);
                }
            }

            if ("domesurvival:water_purifier".equals(key)) {
                tryAction(player, QuestGlobalRegistry.Action.CH7_PURIFIER_PLACED);
            }

            if ("domesurvival:oxygen_electrolyzer".equals(key)) {
                tryAction(player, QuestGlobalRegistry.Action.CH7_ELECTROLYZER_PLACED);
            }

            if (isOxygenLink(blockId)) {
                int pipes = CH7_OXYGEN_PIPES.merge(playerId, 1, Integer::sum);
                if (pipes >= CH7_OXYGEN_LINE_BLOCKS) {
                    tryAction(player, QuestGlobalRegistry.Action.CH7_OXYGEN_LINE);
                }
            }

            if ("domesurvival:oxygen_filler".equals(key)) {
                tryAction(player, QuestGlobalRegistry.Action.CH7_FILLER_PLACED);
            }
        }

        if (isNearInnerAirlockWorkZone(pos)) {
            int airlockMask = CH3_AIRLOCK_WORK_MASK.getOrDefault(playerId, 0);

            if ("minecraft:chest".equals(key) || "minecraft:barrel".equals(key)) {
                airlockMask |= 1;
            }
            if ("minecraft:crafting_table".equals(key)) {
                airlockMask |= 2;
            }

            if (airlockMask != 0) {
                CH3_AIRLOCK_WORK_MASK.put(playerId, airlockMask);
            }
            if ((airlockMask & 3) == 3) {
                tryAction(player, QuestGlobalRegistry.Action.CH3_AIRLOCK_WORKZONE);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !Level.OVERWORLD.equals(player.level().dimension())) {
            return;
        }

        DomeZone zone = START_DOME.classify(
                event.getPos().getX() + 0.5D,
                event.getPos().getY() + 0.5D,
                event.getPos().getZ() + 0.5D
        );
        if (!zone.isSafe() || zone == DomeZone.AIRLOCK) {
            return;
        }

        if (!(event.getState().getBlock() instanceof CropBlock crop)
                || !crop.isMaxAge(event.getState())) {
            return;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if (blockId == null) {
            return;
        }

        String key = blockId.toString();
        if ("minecraft:wheat".equals(key)) {
            tryAction(player, QuestGlobalRegistry.Action.CH4_FIRST_WHEAT_HARVEST);
        } else if ("minecraft:carrots".equals(key)) {
            tryAction(player, QuestGlobalRegistry.Action.CH4_FIRST_CARROT_HARVEST);
        } else if ("minecraft:potatoes".equals(key)) {
            tryAction(player, QuestGlobalRegistry.Action.CH4_FIRST_POTATO_HARVEST);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String name = event.getTarget().getName().getString()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (name.equals("joseph cooper")
                || name.equals("joseph")
                || name.equals("джозеф куппер")
                || name.equals("джозеф")) {
            tryAction(player, QuestGlobalRegistry.Action.CH0_JOSEPH_CONTACT);
        }

        if (name.equals("maneogflow")) {
            tryAction(player, QuestGlobalRegistry.Action.CH1_MANEOGFLOW);
        }

        if (name.equals("ivan")
                || name.equals("i van")
                || name.equals("иван")) {
            tryAction(player, QuestGlobalRegistry.Action.CH3_IVAN_CONTACT);
        }
    }

    private static boolean isEnergyGenerator(ResourceLocation id) {
        return isOurCanonicalGenerator(id)
                || isCompatibleEnergyGeneratorPath(id.getPath());
    }

    private static boolean isOurCanonicalGenerator(ResourceLocation id) {
        return "domesurvival".equals(id.getNamespace())
                && "coal_generator".equals(id.getPath());
    }

    private static boolean isCompatibleEnergyGeneratorPath(String path) {
        return path.contains("generator")
                || path.contains("dynamo")
                || path.contains("alternator");
    }

    private static boolean isEnergyLink(ResourceLocation id) {
        return isOurCanonicalEnergyLink(id)
                || isCompatibleEnergyLinkPath(id.getPath());
    }

    private static boolean isOurCanonicalEnergyLink(ResourceLocation id) {
        if (!"domesurvival".equals(id.getNamespace())) {
            return false;
        }

        return switch (id.getPath()) {
            case "basic_energy_pipe", "reinforced_energy_pipe", "high_voltage_energy_pipe" -> true;
            default -> false;
        };
    }

    private static boolean isCompatibleEnergyLinkPath(String path) {
        return path.contains("energy_cable")
                || path.contains("power_cable")
                || path.contains("energy_wire")
                || path.contains("power_wire")
                || path.contains("energy_conduit")
                || path.contains("power_conduit")
                || path.contains("energy_pipe")
                || path.contains("power_pipe")
                || path.endsWith("_cable")
                || path.endsWith("_wire")
                || path.endsWith("_conduit");
    }

    private static boolean isEnergyStorage(ResourceLocation id) {
        return isOurCanonicalEnergyStorage(id)
                || isCompatibleEnergyStoragePath(id.getPath());
    }

    private static boolean isOxygenLink(ResourceLocation id) {
        if (!"domesurvival".equals(id.getNamespace())) {
            return false;
        }

        return switch (id.getPath()) {
            case "oxygen_pipe", "reinforced_oxygen_pipe", "high_flow_oxygen_pipe" -> true;
            default -> false;
        };
    }

    private static boolean isOurCanonicalEnergyStorage(ResourceLocation id) {
        if (!"domesurvival".equals(id.getNamespace())) {
            return false;
        }

        return switch (id.getPath()) {
            case "energy_buffer", "energy_buffer_titan", "energy_buffer_adamantium", "energy_buffer_creative" -> true;
            default -> false;
        };
    }

    private static boolean isCompatibleEnergyStoragePath(String path) {
        return path.contains("energy_storage")
                || path.contains("power_storage")
                || path.contains("battery")
                || path.contains("capacitor")
                || path.contains("accumulator")
                || path.contains("energy_cell")
                || path.contains("power_cell")
                || path.contains("energy_block");
    }

    private static void retryChapter4Actions(ServerPlayer player) {
        UUID id = player.getUUID();

        if (CH4_LIGHTS.getOrDefault(id, 0) >= 5) {
            tryAction(player, QuestGlobalRegistry.Action.CH4_FARM_LIGHTS);
        }
    }

    private static void retryChapter3ConstructionActions(ServerPlayer player) {
        UUID id = player.getUUID();

        if ((CH3_WORKSHOP_MASK.getOrDefault(id, 0) & 3) == 3) {
            tryAction(player, QuestGlobalRegistry.Action.CH3_WORKSHOP_PLACED);
        }
        if (CH3_BEDS.getOrDefault(id, 0) >= 3) {
            tryAction(player, QuestGlobalRegistry.Action.CH3_BEDS_PLACED);
        }
        if (CH3_LIGHTS.getOrDefault(id, 0) >= 5) {
            tryAction(player, QuestGlobalRegistry.Action.CH3_LIVING_LIGHTS);
        }
        if ((CH3_AIRLOCK_WORK_MASK.getOrDefault(id, 0) & 3) == 3) {
            tryAction(player, QuestGlobalRegistry.Action.CH3_AIRLOCK_WORKZONE);
        }
    }

    private static boolean isSettlementLight(String blockId) {
        return "minecraft:torch".equals(blockId)
                || "minecraft:wall_torch".equals(blockId)
                || "minecraft:lantern".equals(blockId)
                || "minecraft:soul_lantern".equals(blockId);
    }

    private static boolean isNearInnerAirlockWorkZone(BlockPos pos) {
        int dx = pos.getX() - START_DOME.spec().airlockPanelX();
        int dz = pos.getZ() - START_DOME.spec().innerDomePanelZ();

        return Math.abs(dx) <= 7
                && Math.abs(dz) <= 7
                && pos.getY() >= START_DOME.spec().baseY() - 1
                && pos.getY() <= START_DOME.spec().airlockCeilingY() + 2;
    }

    private static void handleChapter2Return(ServerPlayer player, UUID id) {
        Long start = CH2_SORTIE_START.remove(id);
        Double maxRadius = CH2_SORTIE_MAX_RADIUS.remove(id);
        if (start == null || maxRadius == null) {
            return;
        }

        long duration = Math.max(0L, player.serverLevel().getGameTime() - start);
        boolean validShortSortie = duration <= CH2_RETURN_MAX_TICKS
                && maxRadius >= CH2_NEAR_MIN_RADIUS
                && maxRadius <= CH2_OPERATION_MAX_RADIUS;

        if (!validShortSortie) {
            return;
        }

        // Count valid returns even before the FTB technical reward is mirrored;
        // this makes the second sortie genuinely mean the second successful cycle.
        int validReturns = CH2_VALID_RETURNS.merge(id, 1, Integer::sum);

        tryAction(player, QuestGlobalRegistry.Action.CH2_FAST_RETURN);

        if (validReturns >= 2) {
            tryAction(player, QuestGlobalRegistry.Action.CH2_SECOND_SORTIE);
        }

        if (duration >= CH2_CONTROLLED_MIN_TICKS) {
            tryAction(player, QuestGlobalRegistry.Action.CH2_CONTROLLED_SORTIE);
        }

        if (hasAtLeast(player, "minecraft:sand", 20)) {
            tryAction(player, QuestGlobalRegistry.Action.CH2_SUPPLY_RETURN);
        }
    }

    private static void trackChapter7OxygenSortie(
            ServerPlayer player,
            UUID id,
            boolean wasOutside,
            double horizontalRadius
    ) {
        OxygenEquipment.TankView tank = OxygenEquipment.tank(player);
        boolean ready = OxygenEquipment.tankEquipmentReady(player, tank)
                && tank.oxygen() > 0
                && QuestGlobalSyncService.isGlobalCompleted(player, "478ADE8DCA760CBA");

        if (!ready) {
            CH7_SORTIE_START.remove(id);
            CH7_SORTIE_MAX_RADIUS.remove(id);
            return;
        }

        if (!wasOutside || !CH7_SORTIE_START.containsKey(id)) {
            CH7_SORTIE_START.put(id, player.serverLevel().getGameTime());
            CH7_SORTIE_MAX_RADIUS.put(id, horizontalRadius);
            return;
        }

        CH7_SORTIE_MAX_RADIUS.merge(id, horizontalRadius, Math::max);
    }

    private static void handleChapter7Return(ServerPlayer player, UUID id) {
        Long start = CH7_SORTIE_START.remove(id);
        Double maxRadius = CH7_SORTIE_MAX_RADIUS.remove(id);
        if (start == null || maxRadius == null) {
            return;
        }

        long duration = Math.max(0L, player.serverLevel().getGameTime() - start);
        if (duration >= CH7_SORTIE_MIN_TICKS && maxRadius >= CH7_SORTIE_MIN_RADIUS) {
            tryAction(player, QuestGlobalRegistry.Action.CH7_OXYGEN_SORTIE);
        }
    }

    private static boolean containsPurifiedWater(IFluidHandler fluid) {
        for (int tank = 0; tank < fluid.getTanks(); tank++) {
            var stack = fluid.getFluidInTank(tank);
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
            if (!stack.isEmpty()
                    && stack.getAmount() > 0
                    && fluidId != null
                    && "domesurvival:purified_water".equals(fluidId.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFullOxygenTank(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isFullOxygenTank(player.getInventory().getItem(slot))) {
                return true;
            }
        }

        OxygenEquipment.TankView equipped = OxygenEquipment.tank(player);
        return equipped != null && equipped.oxygen() >= equipped.capacity();
    }

    private static boolean isFullOxygenTank(ItemStack stack) {
        return stack.getItem() instanceof OxygenTankItem tank
                && tank.getOxygen(stack) >= tank.capacity();
    }

    private static double horizontalRadius(double x, double z) {
        double dx = x - START_DOME.spec().centerX();
        double dz = z - START_DOME.spec().centerZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean tryAction(ServerPlayer player, QuestGlobalRegistry.Action action) {
        var spec = QuestGlobalRegistry.auto(action).orElse(null);
        if (spec == null || QuestGlobalSyncService.isGlobalCompleted(player, spec.questId())) {
            return false;
        }

        for (String dependency : spec.dependencies()) {
            if (!QuestGlobalSyncService.isGlobalCompleted(player, dependency)) {
                return false;
            }
        }

        String advancementId = "domesurvival:quest_actions/"
                + action.name().toLowerCase(Locale.ROOT);

        int result = player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack().withSuppressedOutput(),
                "advancement grant "
                        + player.getScoreboardName()
                        + " only "
                        + advancementId
        );

        // FTB's AdvancementTask polls the actual advancement state and will
        // complete the quest automatically. A 0 result can simply mean this
        // advancement was already granted during a previous check.
        return result > 0;
    }

    private static boolean hasAtLeast(ServerPlayer player, String itemId, int required) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && itemId.equals(key.toString())) {
                count += stack.getCount();
                if (count >= required) {
                    return true;
                }
            }
        }
        return false;
    }
}
