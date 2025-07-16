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
    private static final float I_LEAK = 1e-6f;

    private final float resistance;
    private final float biasVoltage;
    private double prevConductance;

    private double prevCurrent;
    private double r;

    public DiodeWire(float resistance, float biasVoltage, IElectricNode cathode, IElectricNode anode) {
        super(anode, cathode);
        this.resistance = resistance;
        this.biasVoltage = biasVoltage;
        prevConductance = 0;
        prevCurrent = 0;
    }

//    private double diodeCurrent(double V) {
//        var Ilin = V / resistance * 0.5;
//        var Ia = (Math.tanh((V - biasVoltage) / 0.1) + 1) * Ilin;
//        return Ia;
//    }

    @Override
    public double conductance() {
        var V = potentialDifference();
        if(V <= 0)
            return I_LEAK;
        if(V >= biasVoltage)
            return resistance;
        return (1 / resistance) * V / biasVoltage;

//        if(V <= 0)
//            return I_LEAK;

//        var Ia = diodeCurrent(V);
//        Ia = Ia * 0.75 + prevCurrent * 0.25;
//        prevCurrent = Ia;
//        return Ia / (V + biasVoltage) + I_LEAK;
    }

    @Override
    public void preSolve(DMatrixRMaj A, DMatrixRMaj x, DMatrixRMaj b) {
        var newConductance = conductance();
        network.updateConductance(this, newConductance - prevConductance);
        prevConductance = newConductance;
    }

//    @Override
//    public void iteration(DMatrixRMaj A, DMatrixRMaj x, DMatrixRMaj residual, DMatrixRMaj p) {
//        var V = x.get(node1.getIndex(), 0) - x.get(node2.getIndex(), 0);
//
//        var Ia = diodeCurrent(V);
//        var diff = Ia / (V + biasVoltage) - r;
//        p.add(node1.getIndex(), 0, -diff);
//        p.add(node2.getIndex(), 0, diff);
//        residual.add(node1.getIndex(), 0, -diff);
//        residual.add(node2.getIndex(), 0, diff);
//        r += diff;
//    }
}
