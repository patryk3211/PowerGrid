package org.patryk3211.powergrid.electricity.sim.special;

import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

public class RelaySwitchWire extends SwitchedWire implements IOuterHook {
    private final ElectricWire coilWire;
    private final float onCurrent;
    private final float offCurrent;
    private final boolean normallyClosed;
    private final boolean polarized;

    private boolean switched = false;

    public RelaySwitchWire(float resistance, IElectricNode node1, IElectricNode node2, ElectricWire coilWire, float onCurrent, float offCurrent, boolean normallyClosed) {
        super(resistance, node1, node2);
        this.coilWire = coilWire;
        this.onCurrent = onCurrent;
        this.offCurrent = offCurrent;
        this.normallyClosed = normallyClosed;
        this.polarized = false;
    }

    public RelaySwitchWire(float resistance, IElectricNode node1, IElectricNode node2, boolean initialState, ElectricWire coilWire, float onCurrent, float offCurrent, boolean normallyClosed, boolean polarized) {
        super(resistance, node1, node2, initialState);
        this.coilWire = coilWire;
        this.onCurrent = onCurrent;
        this.offCurrent = offCurrent;
        this.normallyClosed = normallyClosed;
        this.polarized = polarized;
    }

    @Override
    public void preSolve() {
        if(coilWire.isConverged()) {
            if (polarized) {
                var I = coilWire.current();
                PowerGrid.LOGGER.info("{}A", I);
                if ((getState() == normallyClosed) && I > onCurrent) { // Forward
                    setState(!normallyClosed);
                    switched = true;
                } else if ((getState() != normallyClosed) && I < -onCurrent) { // Reverse
                    setState(normallyClosed);
                    switched = true;
                }
            } else {
                var I = Math.abs(coilWire.current());
                if ((getState() != normallyClosed) && I < offCurrent) {
                    setState(normallyClosed);
                    switched = true;
                } else if ((getState() == normallyClosed) && I > onCurrent) {
                    setState(!normallyClosed);
                    switched = true;
                }
            }
        }
    }

    public boolean wasSwitched() {
        boolean was = switched;
        switched = false;
        return was;
    }
}
