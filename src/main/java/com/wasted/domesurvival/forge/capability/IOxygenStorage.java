package com.wasted.domesurvival.forge.capability;

/**
 * Minimal DomeSurvival oxygen capability used by machines.
 * Units are gameplay O2 units: the same scale used by player oxygen tanks.
 */
public interface IOxygenStorage {
    int receiveOxygen(int maxReceive, boolean simulate);
    int extractOxygen(int maxExtract, boolean simulate);
    int getOxygenStored();
    int getMaxOxygenStored();
    boolean canReceive();
    boolean canExtract();
}
