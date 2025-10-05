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

import org.patryk3211.powergrid.electricity.sim.node.CouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IStaticResidual;

import java.util.Collection;
import java.util.List;

public class InductorCoupling extends CouplingNode implements IStaticResidual {
    private final IElectricNode node1;
    private final IElectricNode node2;

    private double inductance;
    private double Veq;
    private double currentInject;

    public InductorCoupling(double inductance, IElectricNode node1, IElectricNode node2) {
        this.node1 = node1;
        this.node2 = node2;
        this.inductance = inductance;
    }

    public double resistance() {
        // dt = 50ms (1 tick)
        return 2 * inductance / 0.05f;
    }

    public float current() {
        return (float) getStateValue();
    }

    public void setCurrent(float current) {
        currentInject = current - current();
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        var R = resistance();
        var I = current();
        var prevPotential = R * I + Veq;
        var prevCurrent = I + currentInject;
        currentInject = 0;

        // Calculate voltage with a bit of leakage
        Veq = (-R * prevCurrent - prevPotential) * 0.99999;

        residual.add(index, Veq);
    }

    @Override
    public void couple(IAdmittanceAdder admittance) {
        admittance.add(index, index, -resistance());
        admittance.add(index, node1.getIndex(),  1);
        admittance.add(index, node2.getIndex(), -1);
        admittance.add(node1.getIndex(), index,  1);
        admittance.add(node2.getIndex(), index, -1);
    }

    @Override
    public Collection<IElectricNode> coupledNodes() {
        return List.of();
    }
}
