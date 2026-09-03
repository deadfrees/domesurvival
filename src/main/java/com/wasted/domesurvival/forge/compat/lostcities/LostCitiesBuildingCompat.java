package com.wasted.domesurvival.forge.compat.lostcities;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.function.Function;

/** Optional, reflection-only access to Lost Cities building metadata. */
public final class LostCitiesBuildingCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Object api;
    private static volatile boolean reflectionWarningLogged;

    private LostCitiesBuildingCompat() {
    }

    public static void enqueueImc(InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded("lostcities")) {
            return;
        }
        InterModComms.sendTo(
                "lostcities",
                "getLostCities",
                () -> (Function<Object, Object>) value -> {
                    api = value;
                    LOGGER.info("Lost Cities building metadata connected for generated storage support");
                    return null;
                }
        );
    }

    @Nullable
    public static BuildingCell buildingAt(Level level, int chunkX, int chunkZ) {
        Object currentApi = api;
        if (currentApi == null) {
            return null;
        }
        try {
            Object info = invoke(currentApi, "getLostInfo", new Class<?>[]{Level.class}, level);
            if (info == null) return null;

            Object chunk = invoke(info, "getChunkInfo", new Class<?>[]{int.class, int.class}, chunkX, chunkZ);
            if (chunk == null || !((Boolean) invoke(chunk, "isCity", new Class<?>[0]))) {
                return null;
            }

            ResourceLocation buildingId = (ResourceLocation) invoke(chunk, "getBuildingId", new Class<?>[0]);
            String buildingType = (String) invoke(chunk, "getBuildingType", new Class<?>[0]);
            if (buildingId == null && (buildingType == null || buildingType.isBlank())) {
                return null; // street, park or another city cell without a building
            }

            int originChunkX = chunkX;
            int originChunkZ = chunkZ;
            int width = 1;
            int depth = 1;
            Object multi = invoke(chunk, "getMultiBuildingInfo", new Class<?>[0]);
            if (multi != null) {
                int offsetX = (Integer) invoke(multi, "offsetX", new Class<?>[0]);
                int offsetZ = (Integer) invoke(multi, "offsetZ", new Class<?>[0]);
                width = Math.max(1, (Integer) invoke(multi, "w", new Class<?>[0]));
                depth = Math.max(1, (Integer) invoke(multi, "h", new Class<?>[0]));
                originChunkX -= offsetX;
                originChunkZ -= offsetZ;
            }

            ResourceLocation categoryId = buildingId != null
                    ? buildingId
                    : new ResourceLocation("lostcities", sanitize(buildingType));
            String key = "lostcities:" + originChunkX + "," + originChunkZ
                    + ":" + width + "x" + depth + ":" + categoryId;
            return new BuildingCell(categoryId, key, originChunkX, originChunkZ, width, depth);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            if (!reflectionWarningLogged) {
                reflectionWarningLogged = true;
                LOGGER.warn("Lost Cities building metadata is unavailable; vanilla structure storage remains active",
                        exception);
            }
            return null;
        }
    }

    private static Object invoke(Object target, String name, Class<?>[] parameters, Object... arguments)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, parameters);
        return method.invoke(target, arguments);
    }

    private static String sanitize(String value) {
        String normalized = value == null ? "city_building"
                : value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        return normalized.isBlank() ? "city_building" : normalized;
    }

    public record BuildingCell(
            ResourceLocation buildingId,
            String key,
            int originChunkX,
            int originChunkZ,
            int width,
            int depth
    ) {
    }
}
