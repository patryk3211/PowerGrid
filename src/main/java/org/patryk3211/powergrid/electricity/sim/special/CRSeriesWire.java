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
import org.patryk3211.powergrid.electricity.sim.solver.IStaticResidual;

public class CRSeriesWire extends AbstractElectricWire implements IStaticResidual, IOuterHook, ITimeAwareWire {
    private double capacitance;
    private double resistance;

    private double Ieq;
    private double Iprev;
    private double V;

    public CRSeriesWire(double C, double R, IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
        this.capacitance = C;
        this.resistance = R;
    }

    @Override
    public boolean isSource() {
        return true;
    }

    public void setVoltage(float voltage) {
        valueChange(voltage, V);
        if(Float.isFinite(voltage)) {
            Iprev = 0;
            V = voltage;
        }
    }

    @Override
    public double potentialDifference() {
        if(network == null)
            return V;
        return super.potentialDifference();
    }

    public double capacitorVoltage() {
        return potentialDifference() - current() * resistance;
    }

    @Override
    public double conductance() {
        return 1 / (resistance + getDeltaTime() / (2 * capacitance));
    }

    @Override
    public double current() {
        return super.current() + Ieq;
    }

    @Override
    public void postUpperSolve() {
        if(isConverged()) {
            var Vcap = capacitorVoltage();
            Iprev = (Vcap - V) * 0.5 * capacitance / getDeltaTime();
            // Save voltage with a bit of leakage
            V = Vcap * 0.99999;
        }
    }

    @Override
    public void addStaticResidual(IResidualAdder residual) {
        if(capacitance == 0) {
            Ieq = 0;
            return;
        }
        var G_C = (2 * capacitance) / getDeltaTime();

        double residualScale = 1 - G_C / (1 / resistance + G_C);
        Ieq = (-G_C * V - Iprev) * residualScale;
        if(node1 != null)
            residual.add(node1.getIndex(), -Ieq);
        if(node2 != null)
            residual.add(node2.getIndex(),  Ieq);
    }

    public void setCR(double C, double R) {
        var oldConductance = conductance();
        this.capacitance = C;
        this.resistance = R;

        if(network != null) {
            network.updateConductance(this, conductance() - oldConductance);
        }
    }

    public void setCapacitance(double C) {
        setCR(C, resistance);
    }

    public void setResistance(double R) {
        setCR(capacitance, R);
    }

    @Override
    public String toString() {
        return String.format("CRWire(C=%g R=%g)", capacitance, resistance);
    }
}
