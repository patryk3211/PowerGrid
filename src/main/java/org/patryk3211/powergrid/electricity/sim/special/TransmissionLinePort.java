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
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;
import org.patryk3211.powergrid.electricity.sim.solver.IOuterHook;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.ISolverHook;

public class TransmissionLinePort extends VoltageSourceCoupling implements IOuterHook, ISolverHook {
    private final TransmissionLine line;
    public TransmissionLinePort other;
    public boolean solved = false;

    float I, V;
    private float Ieq;

    public TransmissionLinePort(IElectricNode node, float resistance, TransmissionLine line) {
        super(node, null, resistance);
        this.line = line;
        V = node.getVoltage();
    }

    @Override
    public void preSolve() {
        setVoltage(V);
    }

    @Override
    public void postUpperSolve() {
        solved = true;
        if(other.solved) {
            solved = false;
            other.solved = false;

            I = getCurrent();
            other.I = other.getCurrent();
            var I = this.I + other.I;

            V = other.positive.getVoltage() + I * getResistance();
            other.V = positive.getVoltage() + I * getResistance();
        }
    }

    @Override
    public void startIteration() {
        var I = -(getCurrent() - this.I) * getResistance();
        Ieq = Ieq * 0.5f + I * 0.5f;
    }

    @Override
    public void addResidual(IResidualAdder residual) {
        residual.add(index, Ieq);
    }

    public TransmissionLinePort getOther() {
        return other;
    }

    public TransmissionLine getLine() {
        return line;
    }
}
