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
    private final float biasVoltage;
    private double currentConductance;
    private double prevConductance;
    private double prevPotential;

    private double In;

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
    public float current() {
        return (float) (super.current() - Math.min(biasVoltage, potentialDifference()) * currentConductance);
    }

    @Override
    public float power() {
        return potentialDifference() * current();
    }

    @Override
    public void preSolve() {
        var V = super.potentialDifference();
        float step;
        if(V >= prevPotential) {
            step = 0.05f;
            V = (float) (prevPotential + step * Math.log(1 + (V - prevPotential) / step));
        }
        prevPotential = V;

        var maxConductance = 1 / resistance;
        var strength = 0.5 * (Math.tanh(10 * (V - biasVoltage - 0.3)) + 1);
        currentConductance = maxConductance * strength;
        In = -Math.min(biasVoltage, V) * currentConductance * 0.995;

        network.updateConductance(this, currentConductance - prevConductance);
        prevConductance = currentConductance;
    }

    @Override
    public void addResidual(DMatrixRMaj residual) {
        residual.add(node1.getIndex(), 0, -In);
        residual.add(node2.getIndex(), 0,  In);
    }
}
