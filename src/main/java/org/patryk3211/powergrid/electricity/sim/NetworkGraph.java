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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePort;

import java.util.*;

public class NetworkGraph {
    private static class Node {
        public final IElectricNode node;
        public final Map<Node, List<AbstractElectricWire>> connections;
        public final Set<TransmissionLine> connectedLines;
        public final Set<ICouplingNode> couplings;
        public boolean isKept;

        public Node(IElectricNode node) {
            this.node = node;
            this.connections = new HashMap<>();
            this.connectedLines = new HashSet<>();
            this.couplings = new HashSet<>();
            isKept = false;
        }
    }

    private final Map<IElectricNode, Node> nodes = new HashMap<>();
    public IGraphModifyHooks hooks = null;

    public NetworkGraph() {
        // Ground
        addNode(null);
    }

    public void addNode(IElectricNode node) {
        if(nodes.containsKey(node))
            return;
        nodes.put(node, new Node(node));
        if(hooks != null)
            hooks.addNode(node);
    }

    public void removeNode(IElectricNode node) {
        var object = nodes.get(node);
        if(object == null)
            return;
        if(object.isKept) {
            object.isKept = false;
            return;
        }
        nodes.remove(node);
        for(var other : object.connections.keySet()) {
            other.connections.remove(object);
        }
        if(!object.couplings.isEmpty()) {
            ElectricalNetwork.LOGGER.warn("Electric node removed before it was fully decoupled, this can cause issues");
            for(var coupling : object.couplings) {
                var network = coupling.getNetwork();
                if(network != null)
                    network.removeNode(coupling);
            }
        }
        if(hooks != null)
            hooks.removeNode(node);
    }

    public void connect(IElectricNode node1, IElectricNode node2, @NotNull AbstractElectricWire wire) {
        if((node1 != null && !nodes.containsKey(node1)) || (node2 != null && !nodes.containsKey(node2)))
            return;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        var conns1 = object1.connections.computeIfAbsent(object2, key -> new ArrayList<>());
        if(!conns1.contains(wire)) conns1.add(wire);
        var conns2 = object2.connections.computeIfAbsent(object1, key -> new ArrayList<>());
        if(!conns2.contains(wire)) conns2.add(wire);
        if(hooks != null)
            hooks.addWire(wire);

        if(wire instanceof TransmissionLine line) {
            object1.connectedLines.add(line);
            object2.connectedLines.add(line);
            if(hooks != null)
                hooks.lineConnected(line);
        }
    }

    public boolean disconnect(IElectricNode node1, IElectricNode node2, @NotNull AbstractElectricWire wire) {
        if((node1 != null && !nodes.containsKey(node1)) || (node2 != null && !nodes.containsKey(node2)))
            return false;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        boolean removed = false;
        var conns1 = object1.connections.get(object2);
        if(conns1 != null) {
            removed |= conns1.remove(wire);
            if(conns1.isEmpty())
                object1.connections.remove(object2);
        }
        var conns2 = object2.connections.get(object1);
        if(conns2 != null) {
            removed |= conns2.remove(wire);
            if(conns2.isEmpty())
                object2.connections.remove(object1);
        }
        if(hooks != null)
            hooks.removeWire(wire);

        if(wire instanceof TransmissionLine line) {
            removed |= object1.connectedLines.remove(line);
            removed |= object2.connectedLines.remove(line);
            if(hooks != null)
                hooks.lineDisconnected(line);
        }
        return removed;
    }

    public void couple(ICouplingNode coupling) {
        for(var node : coupling.coupledNodes()) {
            var object = nodes.get(node);
            if(object == null)
                continue;
            object.couplings.add(coupling);
        }
    }

    public void decouple(ICouplingNode coupling) {
        for(var node : coupling.coupledNodes()) {
            var object = nodes.get(node);
            if(object == null)
                continue;
            object.couplings.remove(coupling);
        }
    }

    @NotNull
    public Collection<ICouplingNode> getCouplings(IElectricNode node) {
        if(node != null && !nodes.containsKey(node))
            return List.of();
        var object = nodes.get(node);
        return object.couplings;
    }

    @Nullable
    public AbstractElectricWire getFirstWire(IElectricNode node1, IElectricNode node2) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return null;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        var conn = object1.connections.get(object2);
        return conn == null ? null : conn.isEmpty() ? null : conn.get(0);
    }

    public List<AbstractElectricWire> getWires(IElectricNode node) {
        if(!nodes.containsKey(node))
            return List.of();

        var object1 = nodes.get(node);
        var allWires = new ArrayList<AbstractElectricWire>();
        for(var wires : object1.connections.values()) {
            allWires.addAll(wires);
        }

        return allWires;
    }

    public Collection<AbstractElectricWire> getWires(IElectricNode node1, IElectricNode node2) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return List.of();

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        var conn = object1.connections.get(object2);
        return conn == null ? List.of() : conn;
    }

    @NotNull
    public List<IElectricNode> getConnectedNodes(IElectricNode node) {
        if(!nodes.containsKey(node))
            return List.of();
        var eNodes = new ArrayList<IElectricNode>();
        var object = nodes.get(node);
        for(var otherNode : object.connections.keySet()) {
            if(eNodes.contains(otherNode.node))
                continue;
            eNodes.add(otherNode.node);
        }
        for(var coupling : object.couplings) {
            for(var otherNode : coupling.coupledNodes()) {
                if(otherNode == node)
                    continue;
                if(eNodes.contains(otherNode))
                    continue;
                eNodes.add(otherNode);
            }
        }
        return eNodes;
    }

    public List<INode> matrixConnectedNodes(IElectricNode node, @NotNull ElectricalNetwork network) {
        if(!nodes.containsKey(node))
            return List.of();
        var aNodes = new ArrayList<INode>();
        var object = nodes.get(node);
        for(var otherNode : object.connections.keySet()) {
            if(aNodes.contains(otherNode.node) || otherNode.node.getNetwork() != network)
                continue;
            aNodes.add(otherNode.node);
        }
        for(var coupling : object.couplings) {
            if(aNodes.contains(coupling) || coupling.getNetwork() != network)
                continue;
            aNodes.add(coupling);
        }
        return aNodes;
    }

    public int connectionCount(IElectricNode node) {
        if(!nodes.containsKey(node))
            return 0;

        var object = nodes.get(node);
        int size = (int) object.couplings.stream().filter(cnode -> !(cnode instanceof TransmissionLinePort)).count();
        for(var list : object.connections.values())
            size += list.size();
        return size;
    }

    public boolean hasComplexConnections(IElectricNode node) {
        if(!nodes.containsKey(node))
            return false;
        var object = nodes.get(node);
        if(!object.couplings.isEmpty())
            return true;
        for(var wires : object.connections.values()) {
            for(var wire : wires) {
                if(!(wire instanceof ElectricWire))
                    return true;
            }
        }
        return false;
    }

    public boolean hasCouplings(IElectricNode node) {
        if(!nodes.containsKey(node))
            return false;
        var object = nodes.get(node);
        return !object.couplings.isEmpty();
    }

    public boolean isLeafEliminating(IElectricNode node) {
        if(!nodes.containsKey(node))
            return false;
        var object = nodes.get(node);
        if(!object.couplings.isEmpty())
            return true;
        for(var wires : object.connections.values()) {
            for(var wire : wires) {
                if(wire.node1 == null || wire.node2 == null)
                    return true;
                if(wire.isSource())
                    return true;
            }
        }
        return false;
    }

    @NotNull
    public Collection<TransmissionLine> getConnectedLines(IElectricNode node) {
        if(!nodes.containsKey(node))
            return List.of();
        return nodes.get(node).connectedLines;
    }

    @NotNull
    public Collection<IElectricNode> getTransmissionLineConnectedNodes(IElectricNode node) {
        if(!nodes.containsKey(node))
            return List.of();
        var object = nodes.get(node);
        var connected = new HashSet<IElectricNode>();
        for(var line : object.connectedLines) {
            if(line.getNode1() == node) {
                connected.add(line.getNode2());
            } else if(line.getNode2() == node) {
                connected.add(line.getNode1());
            } else {
                PowerGrid.LOGGER.warn("Neither end of line {} is connected to node {}", line, node);
            }
        }
        return connected;
    }

    public int transmissionLineCount(IElectricNode node) {
        if(!nodes.containsKey(node))
            return 0;
        var object = nodes.get(node);
        return object.connectedLines.size();
    }

    public interface IGraphModifyHooks {
        default void addNode(IElectricNode node) { }
        default void removeNode(IElectricNode node) { }
        default void addWire(AbstractElectricWire wire) { }
        default void removeWire(AbstractElectricWire wire) { }
        default void lineConnected(TransmissionLine line) { }
        default void lineDisconnected(TransmissionLine line) { }
    }
}
