package org.patryk3211.powergrid.equipment.drill;

public interface PlayerDrillExtensions {
    void powerGrid$setMining(boolean value);
    boolean powerGrid$isMining();

    float powerGrid$drillSpeedMultiplier();
    void powerGrid$blockDrilled();
}
