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

import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;

public class ColdCathodeRegulatorTubeWire extends NeonBulbWire {
    public ColdCathodeRegulatorTubeWire(float breakdownVoltage, float holdingVoltage, float holdingCurrent, float dischargeConductance, IElectricNode anode, IElectricNode cathode) {
        super(breakdownVoltage, holdingVoltage, holdingCurrent, dischargeConductance, anode, cathode);
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        if(isLit()) {
            I = holdingVoltage * dischargeConductance;
            residual.add(node1.getIndex(),  I);
            residual.add(node2.getIndex(), -I);
        } else {
            I = 0;
        }
    }

    @Override
    public void preSolve() {
        if(!isConverged())
            return;
        // Update discharge state
        double V = potentialDifference(), I = current();
        if(litTicks <= 0 && V > breakdownVoltage) {
            updateConductance(dischargeConductance);
            litTicks = 2;
        } else if(litTicks > 0 && I < holdingCurrent) {
            if(--litTicks <= 0) {
                updateConductance(OFF_CONDUCTANCE);
                litTicks = 0;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("RegulatorTube(Vb=%g Vh=%g Ih=%g G=%g)", breakdownVoltage, holdingVoltage, holdingCurrent, dischargeConductance);
    }
}
