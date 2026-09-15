package com.wasted.domesurvival.forge.itempipe;

public enum ItemPipeTier {
    COPPER("copper", 1, 40, 0.04),
    STEEL("steel", 2, 30, 0.06),
    DESH("desh", 4, 20, 0.08),
    FILTERING("filtering", 1, 40, 0.04);

    private final String id;
    private final int defaultItemsPerCycle;
    private final int defaultCooldownTicks;
    private final double defaultTravelSpeed;

    ItemPipeTier(String id, int defaultItemsPerCycle, int defaultCooldownTicks, double defaultTravelSpeed) {
        this.id = id;
        this.defaultItemsPerCycle = defaultItemsPerCycle;
        this.defaultCooldownTicks = defaultCooldownTicks;
        this.defaultTravelSpeed = defaultTravelSpeed;
    }

    public String id() { return id; }
    public int defaultItemsPerCycle() { return defaultItemsPerCycle; }
    public int defaultCooldownTicks() { return defaultCooldownTicks; }
    public double defaultTravelSpeed() { return defaultTravelSpeed; }
    public double travelSpeed() { return ItemPipeBalance.travelSpeed(this); }

    public int itemsPerCycle() {
        return ItemPipeBalance.itemsPerCycle(this);
    }

    public int cooldownTicks() {
        return ItemPipeBalance.cooldownTicks(this);
    }
}
