package com.wasted.domesurvival.core.oxygen;

/** Active breathing source reported by the authoritative server. */
public enum OxygenSource {
    ENVIRONMENT(0),
    TANK(1),
    RESERVE(2);

    private final int networkId;

    OxygenSource(int networkId) {
        this.networkId = networkId;
    }

    public int networkId() {
        return networkId;
    }

    public static OxygenSource fromNetworkId(int id) {
        for (OxygenSource source : values()) {
            if (source.networkId == id) {
                return source;
            }
        }
        return RESERVE;
    }
}
