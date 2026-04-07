package org.patryk3211.powergrid.kinetics.plotter;

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

public class PlotterWire extends ElectricWire {
    public double[] samples;
    private int currentTick;

    public PlotterWire(double resistance, IElectricNode node1, IElectricNode node2) {
        super(resistance, node1, node2);
    }

    @Override
    public void prepare(int multiTicks) {
        super.prepare(multiTicks);
        if(multiTicks <= 1) {
            samples = null;
        } else if(samples == null || samples.length != multiTicks) {
            samples = new double[multiTicks];
        }
        currentTick = 0;
    }

    @Override
    public void postMicroTick() {
        super.postMicroTick();
        samples[currentTick++] = potentialDifference();
    }
}
