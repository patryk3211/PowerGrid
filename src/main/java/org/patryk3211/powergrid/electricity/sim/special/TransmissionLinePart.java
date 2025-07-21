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

import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.OwnedElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

public class TransmissionLinePart extends OwnedElectricWire {
    private TransmissionLine line;

    public TransmissionLinePart(double resistance, IElectricNode node1, IElectricNode node2, WireEntity owner, TransmissionLine line) {
        super(resistance, node1, node2, owner);
        this.line = line;
    }

    public void setLine(TransmissionLine line) {
        this.line = line;
    }

    // Transmission line part can NEVER be directly in a network.
    @Override
    public void setNetwork(ElectricalNetwork network) {
        throw new IllegalCallerException();
    }

    @Override
    public void remove() {
        line.removeSegment(this);
    }

    @Override
    public float potentialDifference() {
        return (float) (line.current() * getResistance());
    }

    @Override
    public float current() {
        return line.current();
    }
}
