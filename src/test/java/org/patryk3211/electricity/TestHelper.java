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

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.*;

public abstract class TestHelper {
    protected static class Network {
        public ElectricalNetwork network;

        public Network() {
            network = new ElectricalNetwork();
        }

        public FloatingNode N() {
            var node = new FloatingNode();
            network.addNode(node);
            return node;
        }

        public VoltageSourceNode V(float voltage) {
            var node = new VoltageSourceNode(voltage);
            network.addNode(node);
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
            network.calculate();
        }
    }
}
