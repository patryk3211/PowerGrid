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

public class TransmissionLinePort extends VoltageSourceCoupling implements IOuterHook {
    private final TransmissionLine line;
    TransmissionLinePort other;
    public boolean solved = false;

    private float I, V;

    public TransmissionLinePort(IElectricNode node, float resistance, TransmissionLine line) {
        super(node, null, resistance);
        this.line = line;
        setVoltage(node.getVoltage());
    }

    @Override
    public void preSolve() {
        setVoltage(getVoltage() * 0.5f + V * 0.5f);
    }

    @Override
    public void postUpperSolve() {
        solved = true;
        if(other.solved) {
            solved = false;
            other.solved = false;

            other.I = I = getCurrent() + other.getCurrent();
            V = other.positive.getVoltage() + I * getResistance();
            other.V = positive.getVoltage() + I * getResistance();
        }
    }
}
