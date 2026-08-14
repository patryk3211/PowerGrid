package org.patryk3211.powergrid.electricity.light.bulb;

import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public interface IFixtureEntity {
    SwitchedWire getFilament();

    void setPowerLevel(int bulbPower);
    int getPowerLevel();
}
