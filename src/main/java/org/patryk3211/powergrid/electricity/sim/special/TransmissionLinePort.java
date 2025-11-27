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

public class TransmissionLinePort extends VoltageSourceCoupling {
    private final TransmissionLine line;
    TransmissionLinePort other;

    public TransmissionLinePort(IElectricNode node, float resistance, TransmissionLine line) {
        super(node, null, resistance);
        this.line = line;
        setVoltage(node.getVoltage());
    }

    public void preSolve() {
        var I = getCurrent() + other.getCurrent();
        var voltage = other.positive.getVoltage() + I * getResistance();
        setVoltage(getVoltage() * 0.5f + voltage * 0.5f);
    }
}
