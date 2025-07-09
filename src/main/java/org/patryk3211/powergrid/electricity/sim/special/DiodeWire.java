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

import org.ejml.data.DMatrixRMaj;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class DiodeWire extends AbstractElectricWire implements ISolverHook {
    private final float resistance;
    private double prevConductance;

    public DiodeWire(float resistance, IElectricNode cathode, IElectricNode anode) {
        super(cathode, anode);
        this.resistance = resistance;
        prevConductance = 0;
    }

    @Override
    public double conductance() {
        var anodePotential = node2.getVoltage() - node1.getVoltage();
        if(anodePotential == 0)
            return 0;

        var V = Math.abs(anodePotential);
        // Diode equation: I_S * exp(V_a / V_t) Here, V_t is constant at 25mV
        var Ia = Math.min(0.01f * (Math.exp(anodePotential / 0.025f)), V / resistance);
        return Ia / anodePotential;
    }

    @Override
    public void preSolve(DMatrixRMaj A, DMatrixRMaj x, DMatrixRMaj b) {
        var newConductance = conductance();
        network.updateConductance(this, newConductance - prevConductance);
        prevConductance = newConductance;
    }
}
