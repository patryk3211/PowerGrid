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

import net.minecraft.core.BlockPos;
import org.patryk3211.powergrid.circuits.circuitboard.BakedCircuit;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface IElectricEntity {
    void buildCircuit(CircuitBuilder builder);

    class CircuitBuilder {
        private ElectricalNetwork network;
        private final BlockPos pos;
        private final List<IElectricNode> externalNodes;
        private final Collection<INode> internalNodes;
        private final Collection<AbstractElectricWire> wires;
        private boolean alterExternal = true;

        public CircuitBuilder(BlockPos pos, List<IElectricNode> externalNodes, Collection<INode> internalNodes, Collection<AbstractElectricWire> wires) {
            this.pos = pos;
            this.externalNodes = externalNodes;
            this.internalNodes = internalNodes;
            this.wires = wires;
        }

        public CircuitBuilder with(ElectricalNetwork network) {
            this.network = network;
            return this;
        }

        public CircuitBuilder alterExternal(boolean value) {
            alterExternal = false;
            return this;
        }

        public void clear() {
            if(network != null) {
                if(alterExternal)
                    externalNodes.forEach(network::removeNode);
                internalNodes.forEach(network::removeNode);
                wires.forEach(network::removeWire);
            }
            if(alterExternal)
                externalNodes.clear();
            internalNodes.clear();
            wires.clear();
        }

        /**
         * Add an external node to the circuit. The order in which these are added affects
         * node bindings for electric block terminal indices.
         */
        @Deprecated
        public FloatingNode addExternalNode() {
            if(!alterExternal)
                return null;
            int index = externalNodes.size();
            var node = new OwnedFloatingNode(new BlockWireEndpoint(pos, index));
            externalNodes.add(node);
            if(network != null)
                network.addNode(node);
            return node;
        }

        /**
         * Add a new internal node of the given class.
         * @param clazz Node class
         * @param params Constructor parameters
         * @return New internal node
         * @param <T> Node type
         */
        public <T extends INode> T addInternalNode(Class<T> clazz, Object... params) {
            try {
                var node = clazz.getConstructor(Arrays.stream(params).map(Object::getClass).toArray(len -> new Class<?>[len])).newInstance(params);
                add(node);
                return node;
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Add a new floating internal node.
         * @return Internal floating node
         */
        public FloatingNode addInternalNode() {
            var node = new FloatingNode();
            add(node);
            return node;
        }

        /**
         * Get external node at index.
         * @param index Index of node
         * @return External node
         */
        public FloatingNode terminalNode(int index) {
            return (FloatingNode) externalNodes.get(index);
        }

        /**
         * Set external node count. This should correspond to the value specified in block.
         * @param count Number of nodes
         */
        public void setTerminalCount(int count) {
            if(alterExternal) {
                var currentCount = externalNodes.size();
                if(currentCount != count) {
                    if(currentCount < count) {
                        for(int i = 0; i < count - currentCount; ++i) {
                            addExternalNode();
                        }
                    } else {
                        for(int i = 0; i < currentCount - count; ++i) {
                            var node = externalNodes.remove(externalNodes.size() - 1);
                            if(network != null)
                                network.removeNode(node);
                        }
                    }
                }
            }
        }

        /**
         * Set if external node should be present in the external node list.
         * This method allows for altering the external node structure even on circuit rebuilds.
         * @param index Index of node to alter
         * @param present Set if the node should exist
         */
        public void setExternalNode(int index, boolean present) {
            var node = externalNodes.get(index);
            if(node == null && present) {
                node = new FloatingNode();
                if(network != null)
                    network.addNode(node);
                externalNodes.set(index, node);
            } else if(node != null && !present) {
                if(network != null)
                    network.removeNode(node);
                externalNodes.set(index, null);
            }
        }

        /**
         * Connect two electric nodes with a simple wire.
         * @param resistance Initial resistance of wire
         * @param node1 First node
         * @param node2 Second node
         * @return Connection wire
         */
        public ElectricWire connect(float resistance, IElectricNode node1, IElectricNode node2) {
            var wire = new ElectricWire(resistance, node1, node2);
            add(wire);
            return wire;
        }

        /**
         * Connect two electric nodes with a switchable wire.
         * @param resistance Initial resistance of wire
         * @param node1 First node
         * @param node2 Second node
         * @param state Initial switch state
         * @return Connection wire
         */
        public SwitchedWire connectSwitch(float resistance, IElectricNode node1, IElectricNode node2, boolean state) {
            var wire = new SwitchedWire(resistance, node1, node2, state);
            add(wire);
            return wire;
        }

        public void add(AbstractElectricWire wire) {
            wires.add(wire);
            if(network != null)
                network.addWire(wire);
        }

        public void add(INode node) {
            internalNodes.add(node);
            if(network != null)
                network.addNode(node);
        }

        /**
         * Connect two electric nodes with a switchable wire with initial state set to true.
         * @param resistance Initial resistance of wire
         * @param node1 First node
         * @param node2 Second node
         * @return Connection wire
         */
        public SwitchedWire connectSwitch(float resistance, IElectricNode node1, IElectricNode node2) {
            return connectSwitch(resistance, node1, node2, true);
        }

        public TransformerCoupling couple(float ratio, float resistance, IElectricNode p1, IElectricNode s1) {
            var node = TransformerCoupling.create(ratio, resistance, p1, s1);
            add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, float resistance, IElectricNode p1, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, resistance, p1, s1, s2);
            add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, float resistance, IElectricNode p1, IElectricNode p2, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, resistance, p1, p2, s1, s2);
            add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, IElectricNode p1, IElectricNode s1) {
            var node = TransformerCoupling.create(ratio, p1, s1);
            add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, IElectricNode p1, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, p1, s1, s2);
            add(node);
            return node;
        }

        public TransformerCoupling couple(float ratio, IElectricNode p1, IElectricNode p2, IElectricNode s1, IElectricNode s2) {
            var node = TransformerCoupling.create(ratio, p1, p2, s1, s2);
            add(node);
            return node;
        }

        public void setTo(BakedCircuit circuit) {
            if(!alterExternal)
                throw new IllegalStateException("Alter external must be allowed for baked circuit");
            clear();
            for(var node : circuit.externalNodes) {
                externalNodes.add(node);
                if(network != null)
                    network.addNode(node);
            }
            for(var node : circuit.internalNodes) {
                internalNodes.add(node);
                if(network != null)
                    network.addNode(node);
            }
            for(var wire : circuit.wires) {
                wires.add(wire);
                if(network != null)
                    network.addWire(wire);
            }
        }
    }
}
