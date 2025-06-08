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
package org.patryk3211.powergrid.electricity.sim;

import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.HashSet;
import java.util.Set;

// This is not realistic in any way, but it's good enough
// for this purpose. It even has "capacitance".
public class TransmissionLine extends ElectricWire {
    public final Set<WireEntity> holders = new HashSet<>();
    private double charge = 0;

    public TransmissionLine(double resistance, VoltageSourceNode node1, VoltageSourceNode node2) {
        super(resistance, node1, node2);
    }

    public TransmissionLine() {
        this(1, new VoltageSourceNode(), new VoltageSourceNode());
        // Hack to prevent exception.
        this.resistance = 0;
    }

    public void addHolder(WireEntity wire) {
        holders.add(wire);
        resistance += wire.getResistance();
    }

    public void merge(TransmissionLine line) {
        for(var holder : line.holders) {
            holder.setWire(this);
            addHolder(holder);
        }
        line.holders.clear();
    }

    @Override
    public void remove() {
        GlobalElectricNetworks.removeTransmissionLine(this);
//        var copyHolders = Set.copyOf(holders);
        holders.clear();
//        for(var holder : copyHolders) {
//            holder.setWire(null);
//        }
//        // This avoids unnecessary duplicate calls if a new transmission line is created.
//        for(var holder : copyHolders) {
//            if(holder.getWire() == null && !holder.isRemoved()) {
//                holder.makeWire();
//            }
//        }
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        throw new IllegalCallerException("Transmission line cannot be assigned to a network");
    }

    public void tick() {
        var deltaI = -(node1.getCurrent() + node2.getCurrent()) * 0.5f;
        charge += deltaI;

        ((VoltageSourceNode) node1).setVoltage((float) (charge * resistance));
        ((VoltageSourceNode) node2).setVoltage((float) (charge * resistance));
    }

    @Override
    public float potentialDifference() {
        var node1Voltage = -node1.getCurrent() * resistance;
        var node2Voltage = -node2.getCurrent() * resistance;
        return (float) (node1Voltage - node2Voltage);
    }
}
