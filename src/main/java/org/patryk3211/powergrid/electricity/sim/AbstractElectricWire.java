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

public abstract class AbstractElectricWire {
    protected IElectricNode node1;
    protected IElectricNode node2;

    protected ElectricalNetwork network;

    public AbstractElectricWire(IElectricNode node1, IElectricNode node2) {
        this.node1 = node1;
        this.node2 = node2;
    }

    public void setNetwork(ElectricalNetwork network) {
        this.network = network;
    }

    public ElectricalNetwork getNetwork() {
        return network;
    }

    public void remove() {
        if(network != null)
            network.removeWire(this);
    }

    public void setNode1(IElectricNode node1) {
        if(network != null) {
            var network = this.network;
            network.removeWire(this);
            this.node1 = node1;
            network.addWire(this);
        } else {
            this.node1 = node1;
        }
    }

    public void setNode2(IElectricNode node2) {
        if(network != null) {
            var network = this.network;
            network.removeWire(this);
            this.node2 = node2;
            network.addWire(this);
        } else {
            this.node2 = node2;
        }
    }

    public void flipNodes() {
        // Node order doesn't matter in the conductance matrix so no additional updates are required.
        var b = node2;
        node2 = node1;
        node1 = b;
    }

    public IElectricNode getNode1() {
        return node1;
    }

    public IElectricNode getNode2() {
        return node2;
    }

    public float potentialDifference() {
        if(node1 == null)
            return -node2.getVoltage();
        if(node2 == null)
            return node1.getVoltage();
        return node1.getVoltage() - node2.getVoltage();
    }

    public float current() {
        if(network == null)
            return 0;
        return (float) (potentialDifference() * conductance());
    }

    public float power() {
        var I = current();
        var G = conductance();
        if(I == 0 || G == 0)
            return 0;
        return (float) (I * I / G);
    }

    public abstract double conductance();
}
