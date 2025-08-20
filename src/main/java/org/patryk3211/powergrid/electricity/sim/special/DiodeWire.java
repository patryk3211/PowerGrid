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

import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class DiodeWire extends AbstractElectricWire implements ISolverHook {
    private static final float I_LEAK = 1e-6f;

    private final float resistance;
    private final float biasVoltage;
    private double currentConductance;
    private double prevConductance;

    public DiodeWire(float resistance, float biasVoltage, IElectricNode cathode, IElectricNode anode) {
        super(anode, cathode);
        this.resistance = resistance;
        this.biasVoltage = biasVoltage;
        prevConductance = 0;
    }

    @Override
    public double conductance() {
        return currentConductance;
    }

    @Override
    public void preSolve() {
        var V = potentialDifference();
        var maxConductance = 1 / resistance;
        var strength = 0.5 * (Math.tanh(2 * (V - biasVoltage)) + 1);
        currentConductance = maxConductance * strength;

        network.updateConductance(this, currentConductance - prevConductance);
        prevConductance = currentConductance;
    }
}
