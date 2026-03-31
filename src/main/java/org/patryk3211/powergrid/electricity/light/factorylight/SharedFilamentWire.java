package org.patryk3211.powergrid.electricity.light.factorylight;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class SharedFilamentWire extends SwitchedWire {
    @Nullable
    private TopLevelSharedFilamentWire simWire;

    public SharedFilamentWire() {
        super(1, null, null, false);
    }

    public void setSimWire(TopLevelSharedFilamentWire wire) {
        if(simWire != wire) {
            if(simWire != null)
                simWire.updateConductance(-conductance());
            simWire = wire;
            if(wire != null)
                wire.updateConductance(conductance());
        }
    }

    @Override
    public void setResistance(double resistance) {
        double G0 = conductance();
        super.setResistance(resistance);
        if(simWire != null)
            simWire.updateConductance(conductance() - G0);
    }

    @Override
    public void setState(boolean state) {
        double G0 = conductance();
        super.setState(state);
        if(simWire != null)
            simWire.updateConductance(conductance() - G0);
    }

    @Override
    public double potentialDifference() {
        if(simWire == null)
            return 0;
        return simWire.potentialDifference();
    }

    @Override
    public double current() {
        // Avoid network check
        return potentialDifference() * conductance();
    }

    @Override
    public boolean isConverged() {
        if(simWire == null)
            return false;
        return simWire.isConverged();
    }
}
