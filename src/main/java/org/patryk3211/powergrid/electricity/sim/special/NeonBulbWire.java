/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.sim.special;

import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;

public class NeonBulbWire extends DynamicConductanceWire {
    private final float breakdownVoltage;
    private final float holdingVoltage;
    private final float holdingCurrent;
    private final float dischargeConductance;

    public boolean lit = false;
    private double I;

    public NeonBulbWire(float breakdownVoltage, float holdingVoltage, float holdingCurrent, float dischargeConductance, IElectricNode anode, IElectricNode cathode) {
        super(anode, cathode);
        this.breakdownVoltage = breakdownVoltage;
        this.holdingVoltage = holdingVoltage;
        this.holdingCurrent = holdingCurrent;
        this.dischargeConductance = dischargeConductance;
    }

    @Override
    protected double calculateConductance() {
        if(!lit) {
            this.I = 0;
            return ElectricalNetwork.G_MIN;
        }

        this.I = holdingVoltage * dischargeConductance * Math.signum(potentialDifference());
        return dischargeConductance;
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        residual.add(node1.getIndex(), -I);
        residual.add(node2.getIndex(),  I);
    }

    @Override
    public float current() {
        return (float) (super.current() - I);
    }

    @Override
    public void postUpperSolve() {
        // Update discharge state
        double V = Math.abs(potentialDifference()), I = Math.abs(current());
        if(!lit && V > breakdownVoltage) {
            lit = true;
        } else if(lit && I < holdingCurrent) {
            lit = false;
        }
    }
}
