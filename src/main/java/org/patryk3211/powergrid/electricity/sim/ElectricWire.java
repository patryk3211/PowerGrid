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

import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

public class ElectricWire {
    protected double resistance;
    public final IElectricNode node1;
    public final IElectricNode node2;

    protected ElectricalNetwork network;

    public ElectricWire(double resistance, IElectricNode node1, IElectricNode node2) {
        if(resistance == 0)
            throw new IllegalStateException("Wire resistance must not be zero");
        this.resistance = resistance;
        this.node1 = node1;
        this.node2 = node2;
    }

    public void setResistance(double resistance) {
        if(resistance == 0)
            throw new IllegalStateException("Wire resistance must not be zero");
        double old = this.resistance;
        this.resistance = resistance;
        if(network != null)
            this.network.updateResistance(this, old);
    }

    public double getResistance() {
        return resistance;
    }

    public void setNetwork(ElectricalNetwork network) {
        this.network = network;
    }

    public void remove() {
        if(network != null)
            network.removeWire(this);
    }

    public float potentialDifference() {
        if(node1 == null)
            return -node2.getVoltage();
        if(node2 == null)
            return node1.getVoltage();
        return node1.getVoltage() - node2.getVoltage();
    }

    public float current() {
        return (float) (potentialDifference() / resistance);
    }

    public float power() {
        var I = current();
        return (float) (I * I * resistance);
    }

    public double conductance() {
        if(resistance == 0)
            throw new IllegalStateException("Wire resistance must not be zero");
        return 1 / resistance;
    }
}
