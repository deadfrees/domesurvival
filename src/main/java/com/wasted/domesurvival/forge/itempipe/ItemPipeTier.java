package com.wasted.domesurvival.forge.itempipe;

public enum ItemPipeTier {
    COPPER("copper", 4, 40),
    STEEL("steel", 16, 20),
    DESH("desh", 32, 10),
    FILTERING("filtering", 16, 20);

    private final String id;
    private final int defaultItemsPerCycle;
    private final int defaultCooldownTicks;

    ItemPipeTier(String id, int defaultItemsPerCycle, int defaultCooldownTicks) {
        this.id = id;
        this.defaultItemsPerCycle = defaultItemsPerCycle;
        this.defaultCooldownTicks = defaultCooldownTicks;
    }

    public String id() { return id; }
    public int defaultItemsPerCycle() { return defaultItemsPerCycle; }
    public int defaultCooldownTicks() { return defaultCooldownTicks; }

    public int itemsPerCycle() {
        return ItemPipeBalance.itemsPerCycle(this);
    }

    public int cooldownTicks() {
        return ItemPipeBalance.cooldownTicks(this);
    }
}
