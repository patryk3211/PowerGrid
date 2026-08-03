package org.patryk3211.powergrid.electricity.sim.special;

import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

public class FuseSwitchWire extends SwitchedWire implements IOuterHook {
    private final float maxCurrent;

    private boolean wasDisconnected = false;
    private boolean blown = false;

    public FuseSwitchWire(float resistance, IElectricNode node1, IElectricNode node2, float maxCurrent) {
        super(resistance, node1, node2);
        this.maxCurrent = maxCurrent;
    }

    public FuseSwitchWire(float resistance, IElectricNode node1, IElectricNode node2, boolean initialState, float maxCurrent) {
        super(resistance, node1, node2, initialState);
        this.maxCurrent = maxCurrent;
    }

    @Override
    public void preSolve() {
        if(isConverged()) {
            var I = Math.abs(current());
            if (getState()) {
                if (I > maxCurrent && !wasDisconnected) {
                    setState(false);
                    blown = true;
                }
                wasDisconnected = false;
            } else {
                wasDisconnected = true;
            }
        }
    }

    public boolean wasBlown() {
        boolean was = blown;
        blown = false;
        return was;
    }
}
