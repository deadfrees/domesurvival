package com.wasted.domesurvival.core.weather;

/** Server-authoritative environmental weather mode used by surface hazards and client visuals. */
public enum SurfaceWeatherType {
    CLEAR,
    ACID_RAIN,
    ACID_THUNDERSTORM,
    SANDSTORM;

    public boolean isAcidPrecipitation() {
        return this == ACID_RAIN || this == ACID_THUNDERSTORM;
    }

    public boolean obscuresSun() {
        return this != CLEAR;
    }
}
