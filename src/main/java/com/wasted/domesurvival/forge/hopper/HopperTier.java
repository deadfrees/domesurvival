package com.wasted.domesurvival.forge.hopper;

public enum HopperTier {
    COPPER(7),
    STEEL(9),
    DESH(11);

    private final int slots;

    HopperTier(int slots) {
        this.slots = slots;
    }

    public int slots() {
        return slots;
    }
}
