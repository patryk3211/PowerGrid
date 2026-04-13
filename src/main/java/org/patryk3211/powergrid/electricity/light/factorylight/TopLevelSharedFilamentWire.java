package org.patryk3211.powergrid.electricity.light.factorylight;

import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

public class TopLevelSharedFilamentWire extends AbstractElectricWire implements IOuterHook {
    private double conductance;

    public TopLevelSharedFilamentWire(IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
    }

    @Override
    public double conductance() {
        return Math.max(conductance, ElectricalNetwork.G_MIN);
    }

    public void updateConductance(double change) {
        if(network != null)
            network.updateConductance(this, change);
        conductance += change;
    }
}
