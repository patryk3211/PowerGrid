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
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.*;

public class NetworkGraph {
    private static class Node {
        public final IElectricNode node;
        public final Map<Node, List<AbstractElectricWire>> connections;
        public boolean isKept;

        public Node(IElectricNode node) {
            this.node = node;
            this.connections = new HashMap<>();
            isKept = false;
        }
    }

    private final Map<IElectricNode, Node> nodes = new HashMap<>();

    public void addNode(IElectricNode node) {
        if(nodes.containsKey(node))
            return;
        nodes.put(node, new Node(node));
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
    }

    public void connect(IElectricNode node1, IElectricNode node2, @NotNull AbstractElectricWire wire) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        var conns1 = object1.connections.computeIfAbsent(object2, key -> new ArrayList<>());
        if(!conns1.contains(wire)) conns1.add(wire);
        var conns2 = object2.connections.computeIfAbsent(object1, key -> new ArrayList<>());
        if(!conns2.contains(wire)) conns2.add(wire);
    }

    public void disconnect(IElectricNode node1, IElectricNode node2, @NotNull AbstractElectricWire wire) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        var conns1 = object1.connections.get(object2);
        if(conns1 != null) {
            conns1.remove(wire);
            if(conns1.isEmpty())
                object1.connections.remove(object2);
        }
        var conns2 = object2.connections.get(object1);
        if(conns2 != null) {
            conns2.remove(wire);
            if(conns2.isEmpty())
                object2.connections.remove(object1);
        }
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
        for(var otherNode : nodes.get(node).connections.keySet()) {
            eNodes.add(otherNode.node);
        }
        return eNodes;
    }

    public int connectionCount(IElectricNode node) {
        if(!nodes.containsKey(node))
            return 0;

        int size = 0;
        for(var list : nodes.get(node).connections.values())
            size += list.size();
        return size;
    }
}
