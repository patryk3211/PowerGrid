package org.patryk3211.powergrid.electricity.sim.special;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.node.CurrentSourceWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;

public class SplitTransformerControllerWire extends CurrentSourceWire implements IOuterHook {
    public static final int AVG_SAMPLE_COUNT = 4;

    public SplitTransformerControllerWire secondary;
    private final double sinkConductance;
    private final double sourceConductance;

    public final double[] samples = new double[AVG_SAMPLE_COUNT];
    public int avgHead = 0;
    private boolean handled = false;

    public SplitTransformerControllerWire(IElectricNode positive, @Nullable IElectricNode negative, double sinkConductance, double sourceConductance) {
        super(positive, negative, sourceConductance);
        this.sinkConductance = sinkConductance;
        this.sourceConductance = sourceConductance;
    }

    @Override
    public void preSolve() {
        if(secondary == null)
            return;
        if(handled) {
            secondary.handled = false;
            handled = false;
        } else {
            secondary.handled = true;
            handled = true;
            handle(this, secondary, sourceConductance, sinkConductance);
        }
    }

    public static void handle(SplitTransformerControllerWire couplingI1, SplitTransformerControllerWire couplingI2, double sourceConductance, double sinkConductance) {
        if(couplingI1 == null)
            return;
        var I1Ok = couplingI1.isConverged() || couplingI1.network == null;
        var I2Ok = couplingI2.isConverged() || couplingI2.network == null;
        if(!I1Ok || !I2Ok)
            return;
        assert couplingI1.getNode2() != null && couplingI2.getNode2() != null;
        double V1 = couplingI1.getNetwork() != null
                ? couplingI1.potentialDifference()
                : couplingI1.getCurrent() / couplingI1.conductance();
        double V2 = couplingI2.getNetwork() != null
                ? couplingI2.potentialDifference()
                : couplingI2.getCurrent() / couplingI2.conductance();

        // No ratio multiplication since only 1:1 transformers are allowed here for now.
        var I1S = V1 * couplingI1.conductance() - couplingI1.getCurrent();
        var I2S = V2 * couplingI2.conductance() - couplingI2.getCurrent();

        boolean s1 = Math.signum(I1S) != Math.signum(V1), s2 = Math.signum(I2S) != Math.signum(V2);
        if(Math.abs(I1S) < 1e-7 && s1) s1 = false;
        if(Math.abs(I2S) < 1e-7 && s2) s2 = false;
        if(s1 && !s2) {
            // Sourced current.
            var Is = V1 * couplingI1.conductance() - couplingI1.getCurrent();
            V1 = Is / couplingI2.conductance();
            if(Math.abs(Is) > 1e-6) {
                couplingI1.setConductance(sourceConductance);
                couplingI2.setConductance(sinkConductance);
            }
        } else if(s2 && !s1) {
            // Sourced current.
            var Is = V2 * couplingI2.conductance() - couplingI2.getCurrent();
            V2 = Is / couplingI1.conductance();
            if(Math.abs(Is) > 1e-6) {
                couplingI1.setConductance(sinkConductance);
                couplingI2.setConductance(sourceConductance);
            }
        } else {
            couplingI1.setConductance(sinkConductance);
            couplingI2.setConductance(sinkConductance);
        }

        couplingI1.samples[couplingI1.avgHead] = V1;
        couplingI2.samples[couplingI2.avgHead] = V2;

        double V1a = 0, V2a = 0;
        couplingI1.avgHead = (couplingI1.avgHead + 1) % AVG_SAMPLE_COUNT;
        couplingI2.avgHead = (couplingI2.avgHead + 1) % AVG_SAMPLE_COUNT;
        for(int i = 0; i < AVG_SAMPLE_COUNT; ++i) {
            V1a += couplingI1.samples[i] * (1.0f / AVG_SAMPLE_COUNT);
            V2a += couplingI2.samples[i] * (1.0f / AVG_SAMPLE_COUNT);
        }

        couplingI1.setCurrent(V2a * couplingI1.conductance());
        couplingI2.setCurrent(V1a * couplingI2.conductance());
    }
}
