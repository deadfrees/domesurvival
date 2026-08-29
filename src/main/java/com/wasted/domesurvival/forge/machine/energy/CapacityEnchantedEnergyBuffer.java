package com.wasted.domesurvival.forge.machine.energy;

/**
 * Small common contract injected into the three survival energy-buffer block
 * entities. Keeping the feature behind this interface avoids duplicating the
 * same capacity/NBT logic across every tier.
 */
public interface CapacityEnchantedEnergyBuffer {
    int getCapacityEnchantLevel();

    void setCapacityEnchantLevel(int level);
}
