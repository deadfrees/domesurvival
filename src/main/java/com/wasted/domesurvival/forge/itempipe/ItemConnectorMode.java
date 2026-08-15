package com.wasted.domesurvival.forge.itempipe;

public enum ItemConnectorMode {
    DISABLED("disabled"),
    INPUT("input"),
    OUTPUT("output");

    private final String id;

    ItemConnectorMode(String id) { this.id = id; }
    public String id() { return id; }

    public ItemConnectorMode next() {
        return switch (this) {
            case DISABLED -> INPUT;
            case INPUT -> OUTPUT;
            case OUTPUT -> DISABLED;
        };
    }

    public ItemConnectorMode previous() {
        return switch (this) {
            case DISABLED -> OUTPUT;
            case OUTPUT -> INPUT;
            case INPUT -> DISABLED;
        };
    }
}
