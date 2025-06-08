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
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkGraph {
    private static class Node {
        public final IElectricNode node;
        public boolean transmissionLinePoint;
        public final Map<Node, Connection> connections;
        public boolean isKept;

        public Node(IElectricNode node, boolean transmissionLinePoint) {
            this.node = node;
            this.connections = new HashMap<>();
            this.transmissionLinePoint = transmissionLinePoint;
            isKept = false;
        }
    }

    private record Connection(WireEntity entity, ElectricWire wire) { }

    private final Map<IElectricNode, Node> nodes = new HashMap<>();
    private final Map<IElectricNode, Integer> countOverrides = new HashMap<>();

    public void addNode(IElectricNode node, boolean transmissionLinePoint) {
        if(nodes.containsKey(node))
            return;
        nodes.put(node, new Node(node, transmissionLinePoint));
    }

    public void keepNode(IElectricNode node) {
        if(!nodes.containsKey(node))
            return;
        nodes.get(node).isKept = true;
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

    public void connect(IElectricNode node1, IElectricNode node2, WireEntity entity, @Nullable ElectricWire wire) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        var conn = new Connection(entity, wire);
        object1.connections.put(object2, conn);
        object2.connections.put(object1, conn);
    }

    public void disconnect(IElectricNode node1, IElectricNode node2) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        object1.connections.remove(object2);
        object2.connections.remove(object1);
    }

    public WireEntity getEntity(IElectricNode node1, IElectricNode node2) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return null;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);
        return object1.connections.get(object2).entity;
    }

    @Nullable
    public ElectricWire getWire(IElectricNode node1, IElectricNode node2) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return null;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);
        return object1.connections.get(object2).wire;
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
        if(countOverrides.containsKey(node))
            return countOverrides.get(node);
        if(!nodes.containsKey(node))
            return 0;
        return nodes.get(node).connections.size();
    }

    public boolean isTransmissionLinePoint(IElectricNode node) {
        if(!nodes.containsKey(node))
            return false;
        return nodes.get(node).transmissionLinePoint;
    }

    public void addCountOverrides(Map<IElectricNode, Integer> overrides) {
        countOverrides.putAll(overrides);
    }

    public void removeCountOverride(Map<IElectricNode, Integer> overrides) {
        for(var node : overrides.keySet()) {
            countOverrides.remove(node);
        }
    }
}
