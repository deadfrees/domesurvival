package com.wasted.domesurvival.forge.client.weather;

import com.wasted.domesurvival.core.weather.SurfaceWeatherType;
import com.wasted.domesurvival.core.dome.DomeSpec;

/** Client-only mirror of the authoritative server surface-weather state. */
public final class ClientSurfaceWeatherState {
    private static volatile SurfaceWeatherType weather = SurfaceWeatherType.CLEAR;
    private static volatile boolean exposed;
    private static volatile boolean solarActive;
    private static volatile boolean solarExposed;
    private static volatile int secondsRemaining;
    private static volatile int domeCenterX = DomeSpec.wastedV1().centerX();
    private static volatile int domeBaseY = DomeSpec.wastedV1().baseY();
    private static volatile int domeCenterZ = DomeSpec.wastedV1().centerZ();

    private ClientSurfaceWeatherState() {
    }

    public static void update(SurfaceWeatherType newWeather,
                              boolean newExposed,
                              boolean newSolarActive,
                              boolean newSolarExposed,
                              int newSecondsRemaining,
                              int newDomeCenterX,
                              int newDomeBaseY,
                              int newDomeCenterZ) {
        weather = newWeather == null ? SurfaceWeatherType.CLEAR : newWeather;
        exposed = newExposed;
        solarActive = newSolarActive;
        solarExposed = newSolarExposed;
        secondsRemaining = Math.max(0, newSecondsRemaining);
        domeCenterX = newDomeCenterX;
        domeBaseY = newDomeBaseY;
        domeCenterZ = newDomeCenterZ;
    }

    public static void clear() {
        DomeSpec legacy = DomeSpec.wastedV1();
        update(SurfaceWeatherType.CLEAR, false, false, false, 0,
                legacy.centerX(), legacy.baseY(), legacy.centerZ());
    }

    public static SurfaceWeatherType weather() {
        return weather;
    }

    public static boolean exposed() {
        return exposed;
    }

    /** Clear daytime lethal-sun state, even when the local player is protected by the dome. */
    public static boolean solarActive() {
        return solarActive;
    }

    /** Directly exposed to the lethal sun (used for damage-adjacent visuals such as the heat vignette). */
    public static boolean solarExposed() {
        return solarExposed;
    }

    public static int secondsRemaining() {
        return secondsRemaining;
    }

    public static boolean weatherActive() {
        return weather != SurfaceWeatherType.CLEAR;
    }

    public static DomeSpec domeSpec() {
        return DomeSpec.wastedV1().at(domeCenterX, domeBaseY, domeCenterZ);
    }
}
