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
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class InductorWire extends AbstractElectricWire implements ISolverHook, IOuterHook {
    private double inductance;

    private double Ieq;
    private double I;

    public InductorWire(double inductance, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.inductance = inductance;
    }

    @Override
    public double conductance() {
        return 0.05 / (2 * inductance);
    }

    @Override
    public float current() {
        return (float) (super.current() + Ieq);
    }

    public void setCurrent(float current) {
        valueChange(current, I);
        if(Float.isFinite(current))
            I = current;
    }

    @Override
    public void postUpperSolve() {
        if(isConverged())
            I = current();
    }

    @Override
    public void startIteration() {
        var G = conductance();
        var V = inductance * (current() - I) / 0.05f;

        // Calculate current with a bit of leakage
        Ieq = (V * G + I) * 0.99999;
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        if(node1 != null)
            residual.add(node1.getIndex(),  Ieq);
        if(node2 != null)
            residual.add(node2.getIndex(), -Ieq);
    }

    public void setInductance(float inductance) {
        var oldConductance = conductance();
        this.inductance = inductance;
        if(network != null)
            network.updateConductance(this, conductance() - oldConductance);
    }
}
