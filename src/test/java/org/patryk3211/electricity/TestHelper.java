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
package org.patryk3211.electricity;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.*;

import java.util.List;
import java.util.Random;

public abstract class TestHelper {
    public static class VoltageSourceNodePair extends FloatingNode {
        public VoltageSourceCoupling coupling;

        @Override
        public void setVoltage(float voltage) {
            coupling.setVoltage(voltage);
        }

        @Override
        public float getVoltage() {
            return coupling.getVoltage();
        }

        @Override
        public float getCurrent() {
            return -coupling.getCurrent();
        }
    }

    protected static class Network {
        public ElectricalNetwork network;

        public Network() {
            network = new ElectricalNetwork(false);
            network.warmUp(-1);
        }

        public Network(boolean addGMin) {
            network = new ElectricalNetwork(addGMin);
            network.warmUp(-1);
        }

        public FloatingNode N() {
            var node = new FloatingNode();
            network.addNode(node);
            return node;
        }

        public VoltageSourceNodePair V(float voltage) {
            var node = new VoltageSourceNodePair();
            var coupling = new VoltageSourceCoupling(node, null, 0, voltage);
            node.coupling = coupling;
            network.addNode(node);
            network.addNode(coupling);
            return node;
        }

        public VoltageSourceNodePair V(float voltage, float resistance) {
            var node = new VoltageSourceNodePair();
            var coupling = new VoltageSourceCoupling(node, null, resistance, voltage);
            node.coupling = coupling;
            network.addNode(node);
            network.addNode(coupling);
            return node;
        }

        public CurrentSourceNode C(float current) {
            var node = new CurrentSourceNode(current);
            network.addNode(node);
            return node;
        }

        public ElectricWire W(float R, IElectricNode N1, IElectricNode N2) {
            var wire = new ElectricWire(R, N1, N2);
            network.addWire(wire);
            return wire;
        }

        public TransformerCoupling TR(float ratio, IElectricNode P, IElectricNode S) {
            var node = TransformerCoupling.create(ratio, P, S);
            network.addNode(node);
            return node;
        }

        public TransformerCoupling TR(float ratio, float resistance, IElectricNode P, IElectricNode S) {
            var node = TransformerCoupling.create(ratio, resistance, P, S);
            network.addNode(node);
            return node;
        }

        public TransformerCoupling TR(float ratio, IElectricNode P, IElectricNode S1, IElectricNode S2) {
            var node = TransformerCoupling.create(ratio, P, S1, S2);
            network.addNode(node);
            return node;
        }

        public TransformerCoupling TR(float ratio, float resistance, IElectricNode P, IElectricNode S1, IElectricNode S2) {
            var node = TransformerCoupling.create(ratio, resistance, P, S1, S2);
            network.addNode(node);
            return node;
        }

        public TransformerCoupling TR(float ratio, IElectricNode P1, IElectricNode P2, IElectricNode S1, IElectricNode S2) {
            var node = TransformerCoupling.create(ratio, P1, P2, S1, S2);
            network.addNode(node);
            return node;
        }

        public TransformerCoupling TR(float ratio, float resistance, IElectricNode P1, IElectricNode P2, IElectricNode S1, IElectricNode S2) {
            var node = TransformerCoupling.create(ratio, resistance, P1, P2, S1, S2);
            network.addNode(node);
            return node;
        }

        public SwitchedWire SW(float R, IElectricNode N1, IElectricNode N2) {
            var wire = new SwitchedWire(R, N1, N2);
            network.addWire(wire);
            return wire;
        }

        public SwitchedWire SW(float R, IElectricNode N1, IElectricNode N2, boolean S) {
            var wire = new SwitchedWire(R, N1, N2, S);
            network.addWire(wire);
            return wire;
        }

        public void calculate() {
            network.calculate(1);
        }

        public void calculate(int multiTicks) {
            network.calculate(multiTicks);
        }
    }

    protected static FloatingNode[] buildGrid(Network Net, int size, float density, @Nullable List<ElectricWire> wiresOut) {
        var nodes = new FloatingNode[size * size];
        for(int i = 0; i < size * size; ++i) {
            nodes[i] = Net.N();
        }

        var random = new Random();

        float r = 1.0f;
        float dR = 1.0f / (size * size);
        for(int x = 0; x < size; ++x) {
            for(int y = 0; y < size; ++y) {
                var origin = nodes[x + y * size];
                if(x < size - 1) {
                    // Horizontal
                    if(random.nextFloat() < density) {
                        var wire = Net.W(r, origin, nodes[x + 1 + y * size]);
                        if (wiresOut != null)
                            wiresOut.add(wire);
                    }
                    r += dR;
                }
                if(y < size - 1) {
                    // Vertical
                    if(random.nextFloat() < density) {
                        var wire = Net.W(r, origin, nodes[x + (y + 1) * size]);
                        if (wiresOut != null)
                            wiresOut.add(wire);
                    }
                    r += dR;
                }
            }
        }

        return nodes;
    }

}
