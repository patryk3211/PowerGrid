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
import org.patryk3211.powergrid.electricity.sim.node.INode;

public class GraphedElectricalNetwork extends ElectricalNetwork {
    private final NetworkGraph graph;

    public GraphedElectricalNetwork() {
        this(new NetworkGraph());
    }

    public GraphedElectricalNetwork(NetworkGraph graph) {
        this.graph = graph;
    }

    @Override
    public void removeNode(INode node) {
        super.removeNode(node);
        if(node instanceof IElectricNode enode)
            graph.removeNode(enode);
    }

    @Override
    public void addWire(ElectricWire wire) {
        super.addWire(wire);
        graph.connect(wire.node1, wire.node2);
    }

    @Override
    public void removeWire(ElectricWire wire) {
        super.removeWire(wire);
        graph.disconnect(wire.node1, wire.node2);
    }

    public NetworkGraph getGraph() {
        return graph;
    }
}
