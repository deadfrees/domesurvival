package com.wasted.domesurvival.forge.machine.solar;

public enum SolarPanelState {
    NIGHT(0, "gui.domesurvival.solar_panel.status.night"),
    NO_SKY(1, "gui.domesurvival.solar_panel.status.no_sky"),
    BUFFER_FULL(2, "gui.domesurvival.solar_panel.status.buffer_full"),
    GENERATING(3, "gui.domesurvival.solar_panel.status.generating");

    private final int networkId;
    private final String translationKey;

    SolarPanelState(int networkId, String translationKey) {
        this.networkId = networkId;
        this.translationKey = translationKey;
    }

    public int networkId() {
        return networkId;
    }

    public String translationKey() {
        return translationKey;
    }

    public static SolarPanelState fromNetworkId(int networkId) {
        for (SolarPanelState value : values()) {
            if (value.networkId == networkId) {
                return value;
            }
        }
        return NIGHT;
    }
}
