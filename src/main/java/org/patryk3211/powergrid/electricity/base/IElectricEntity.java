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
package org.patryk3211.powergrid.electricity.base;

import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.node.TransformerCoupling;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface IElectricEntity {
    void buildCircuit(CircuitBuilder builder);

    class CircuitBuilder {
        private ElectricalNetwork network;
        private final List<IElectricNode> externalNodes;
        private final Collection<INode> internalNodes;
        private final Collection<ElectricWire> wires;

        public CircuitBuilder(List<IElectricNode> externalNodes, Collection<INode> internalNodes, Collection<ElectricWire> wires) {
            this.externalNodes = externalNodes;
            this.internalNodes = internalNodes;
            this.wires = wires;
        }

        public CircuitBuilder with(ElectricalNetwork network) {
            this.network = network;
            return this;
        }

        public void clear() {
            if(network != null) {
                externalNodes.forEach(network::removeNode);
                internalNodes.forEach(network::removeNode);
                wires.forEach(network::removeWire);
            }
            externalNodes.clear();
            internalNodes.clear();
            wires.clear();
        }

        /**
         * Add an external node to the circuit. The order in which these are added affects
         * node bindings for electric block terminal indices.
         */
        public FloatingNode addExternalNode() {
            var node = new FloatingNode();
            externalNodes.add(node);
            if(network != null)
                network.addNode(node);
            return node;
        }

        public <T extends INode> T addInternalNode(Class<T> clazz, Object... params) {
            try {
                var node = clazz.getConstructor(Arrays.stream(params).map(Object::getClass).toArray(len -> new Class<?>[len])).newInstance(params);
                internalNodes.add(node);
                if(network != null)
                    network.addNode(node);
                return node;
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public FloatingNode addInternalNode() {
            var node = new FloatingNode();
            internalNodes.add(node);
            if(network != null)
                network.addNode(node);
            return node;
        }

        public FloatingNode getExternalNode(int index) {
            return (FloatingNode) externalNodes.get(index);
        }

        public ElectricWire connect(float resistance, IElectricNode node1, IElectricNode node2) {
            var wire = new ElectricWire(resistance, node1, node2);
            wires.add(wire);
            if(network != null)
                network.addWire(wire);
            return wire;
        }

        public SwitchedWire connectSwitch(float resistance, IElectricNode node1, IElectricNode node2, boolean state) {
            var wire = new SwitchedWire(resistance, node1, node2, state);
            wires.add(wire);
            if(network != null)
                network.addWire(wire);
            return wire;
        }

        public SwitchedWire connectSwitch(float resistance, IElectricNode node1, IElectricNode node2) {
            return connectSwitch(resistance, node1, node2, true);
        }

        public TransformerCoupling couple(float ratio, float resistance, IElectricNode p1, IElectricNode s1) {
            var node = TransformerCoupling.create(ratio, resistance, p1, s1);
            internalNodes.add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, float resistance, IElectricNode p1, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, resistance, p1, s1, s2);
            internalNodes.add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, float resistance, IElectricNode p1, IElectricNode p2, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, resistance, p1, p2, s1, s2);
            internalNodes.add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, IElectricNode p1, IElectricNode s1) {
            var node = TransformerCoupling.create(ratio, p1, s1);
            internalNodes.add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, IElectricNode p1, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, p1, s1, s2);
            internalNodes.add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, IElectricNode p1, IElectricNode p2, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, p1, p2, s1, s2);
            internalNodes.add(node);
            return node;
        }
    }
}
