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
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.ArrayList;
import java.util.List;

public abstract class CompoundWire extends AbstractElectricWire {
    private final List<AbstractElectricWire> wires = new ArrayList<>();
    private double conductance;

    public CompoundWire(IElectricNode node1, IElectricNode node2) {
        super(node1, node2);
    }

    protected <T extends AbstractElectricWire> T addInternalWire(T wire) {
        wires.add(wire);
        return wire;
    }

    protected CapacitorWire addInternalCapacitor(double C, IElectricNode node1, IElectricNode node2) {
        return addInternalWire(new CapacitorWire(C, node1, node2));
    }

    protected ElectricWire addInternalResistor(double R, IElectricNode node1, IElectricNode node2) {
        return addInternalWire(new ElectricWire(R, node1, node2));
    }

    protected ConductanceWire addDynamicWire(IElectricNode node1, IElectricNode node2) {
        return addInternalWire(new ConductanceWire(node1, node2));
    }

    @Override
    public double conductance() {
        return conductance;
    }

    public void setConductance(double conductance) {
        if(network != null) {
            network.updateConductance(this, conductance - this.conductance);
        }
        this.conductance = conductance;
    }

    @Override
    public void setNetwork(ElectricalNetwork network) {
        super.setNetwork(network);
        if(network != null) {
            wires.forEach(network::addWire);
        } else {
            wires.forEach(wire -> {
                wire.remove();
                wire.setNetwork(null);
            });
        }
    }

    @Override
    public void remove() {
        super.remove();
        wires.forEach(AbstractElectricWire::remove);
    }

    public static class ConductanceWire extends AbstractElectricWire {
        private double conductance;

        public ConductanceWire(IElectricNode node1, IElectricNode node2) {
            super(node1, node2);
        }

        @Override
        public double conductance() {
            return conductance;
        }

        public void setConductance(double conductance) {
            if(network != null) {
                network.updateConductance(this, conductance - this.conductance);
            }
            this.conductance = conductance;
        }

        @Override
        public String toString() {
            return String.format("ConductanceWire(G=%g)", conductance);
        }
    }
}
