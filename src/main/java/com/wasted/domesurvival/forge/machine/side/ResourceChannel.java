package com.wasted.domesurvival.forge.machine.side;

/**
 * Resource groups that can be exposed through a configurable machine side.
 *
 * <p>Fluids are included now so future machines can use the same side-configuration
 * format without changing saved data, even though no DomeSurvival fluid machine exists yet.</p>
 */
public enum ResourceChannel {
    ITEM("item"),
    ENERGY("energy"),
    FLUID("fluid");

    private final String serializedName;

    ResourceChannel(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }
}
