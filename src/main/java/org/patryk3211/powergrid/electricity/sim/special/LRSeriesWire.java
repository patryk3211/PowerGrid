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
import org.patryk3211.powergrid.electricity.sim.node.ITimeAwareWire;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class LRSeriesWire extends AbstractElectricWire implements ISolverHook, IOuterHook, ITimeAwareWire {
    private double inductance;
    private double resistance;

    private double Ieq;
    private double I;
    private double Vprev;
    private double residualScale;

    public LRSeriesWire(double L, double R, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.inductance = L;
        this.resistance = R;

        var R_Inductor = (2 * inductance) / getDeltaTime();
        var G_I = 1 / R_Inductor;
        residualScale = 1 - G_I / (1 / resistance + G_I);
    }

    @Override
    public double conductance() {
        return 1 / (resistance + (2 * inductance) / getDeltaTime());
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
    public void preSolve() {
        Ieq = 0;
    }

    @Override
    public void postUpperSolve() {
        if(isConverged()) {
            Vprev = inductance * (current() - I) / getDeltaTime();
            I = current() * 0.99999;
        }
    }

    @Override
    public void startIteration() {
        if(inductance == 0) {
            Ieq = 0;
            return;
        }
        var G_I = getDeltaTime() / (2 * inductance);
        var V_Inductor = (inductance * (current() - I) / getDeltaTime());

        Ieq = (V_Inductor * 0.1f + Vprev * 0.9f * G_I + I) * residualScale;
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        if(node1 != null)
            residual.add(node1.getIndex(),  Ieq);
        if(node2 != null)
            residual.add(node2.getIndex(), -Ieq);
    }

    public void setLR(double L, double R) {
        var oldConductance = conductance();
        this.inductance = L;
        this.resistance = R;

        var R_Inductor = (2 * inductance) / 0.05;
        var G_I = 1 / R_Inductor;
        residualScale = 1 - G_I / (1 / resistance + G_I);
        if(network != null) {
            network.updateConductance(this, conductance() - oldConductance);
        }
    }

    public void setInductance(double L) {
        setLR(L, resistance);
    }

    public void setResistance(double R) {
        setLR(inductance, R);
    }
}
