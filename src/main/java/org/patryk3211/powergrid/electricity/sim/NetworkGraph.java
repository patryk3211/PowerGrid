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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkGraph {
    private static class Node {
        public final IElectricNode node;
        public final List<Node> connections;

        public Node(IElectricNode node) {
            this.node = node;
            this.connections = new ArrayList<>();
        }
    }

    private final Map<IElectricNode, Node> nodes = new HashMap<>();

    public void addNode(IElectricNode node) {
        if(nodes.containsKey(node))
            return;
        nodes.put(node, new Node(node));
    }

    public void removeNode(IElectricNode node) {
        var object = nodes.remove(node);
        if(object == null)
            return;
        for(var other : object.connections) {
            other.connections.remove(object);
        }
    }

    public void connect(IElectricNode node1, IElectricNode node2) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        object1.connections.add(object2);
        object2.connections.add(object1);
    }

    public void disconnect(IElectricNode node1, IElectricNode node2) {
        if(!nodes.containsKey(node1) || !nodes.containsKey(node2))
            return;

        var object1 = nodes.get(node1);
        var object2 = nodes.get(node2);

        object1.connections.remove(object2);
        object2.connections.remove(object1);
    }

    public int connectionCount(IElectricNode node) {
        if(!nodes.containsKey(node))
            return 0;
        return nodes.get(node).connections.size();
    }
}
