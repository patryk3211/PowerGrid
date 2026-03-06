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
import org.patryk3211.powergrid.electricity.sim.node.INetworkElement;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;

import java.util.Collection;
import java.util.List;

public abstract class AbstractElectricWire implements INetworkElement {
    protected IElectricNode node1;
    protected IElectricNode node2;

    protected ElectricalNetwork network;

    public AbstractElectricWire(IElectricNode node1, IElectricNode node2) {
        this.node1 = node1;
        this.node2 = node2;
    }

    public void valueChange(double x, double x0, int ticks) {
        if(network == null)
            return;
        if(x0 == 0 && Math.abs(x) > 0.05) {
            network.warmUp(1);
            return;
        }
        if(Math.abs((x - x0) / x0) > 0.2)
            network.warmUp(ticks);
    }

    protected void valueChange(double x, double x0) {
        valueChange(x, x0, 1);
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        this.network = network;
    }

    @Override
    public ElectricalNetwork getNetwork() {
        return network;
    }

    @Override
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

    public double potentialDifference() {
        if(node1 == null)
            return -node2.getVoltage();
        if(node2 == null)
            return node1.getVoltage();
        return node1.getVoltage() - node2.getVoltage();
    }

    public double current() {
        if(network == null)
            return 0;
        return potentialDifference() * conductance();
    }

    public double power() {
        return current() * potentialDifference();
    }

    public abstract double conductance();

    public void stamp(IAdmittanceAdder admittance, double change) {
        if(node1 != null && node2 != null) {
            var index1 = node1.getIndex();
            var index2 = node2.getIndex();
            admittance.add(index1, index1, change);
            admittance.add(index2, index2, change);
            admittance.add(index1, index2, -change);
            admittance.add(index2, index1, -change);
        } else {
            var index = node1 != null ? node1.getIndex() : node2.getIndex();
            admittance.add(index, index, change);
        }
    }

    public Collection<IElectricNode> coupledNodes() {
        if(node1 == null)
            return List.of(node2);
        if(node2 == null)
            return List.of(node1);
        return List.of(node1, node2);
    }

    public List<INode> affectedNodes() {
        if(node1 == null)
            return List.of(node2);
        if(node2 == null)
            return List.of(node1);
        return List.of(node1, node2);
    }

    public boolean isConverged() {
        if(network == null)
            return false;
        return network.isConverged();
    }

    public static double softDelta(double dX, double a) {
        return Math.min(a * Math.log1p(dX), dX);
    }
}
