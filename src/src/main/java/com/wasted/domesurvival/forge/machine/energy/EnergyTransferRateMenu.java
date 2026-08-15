package com.wasted.domesurvival.forge.machine.energy;

/** Client-visible live FE throughput information for energy storage menus. */
public interface EnergyTransferRateMenu {
    int getInputPerTick();
    int getOutputPerTick();
    int getMaxInputPerTick();
    int getMaxOutputPerTick();
}
