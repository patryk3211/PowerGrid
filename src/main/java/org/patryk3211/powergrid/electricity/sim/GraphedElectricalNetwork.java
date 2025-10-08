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

import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;

public class GraphedElectricalNetwork extends OptimizingElectricalNetwork {
    private final NetworkGraph graph;

    public GraphedElectricalNetwork(boolean addGMin) {
        this(new NetworkGraph(), addGMin);
    }

    public GraphedElectricalNetwork(NetworkGraph graph, boolean addGMin) {
        super(addGMin);
        this.graph = graph;
    }

    @Override
    public void addNode(INode node) {
        super.addNode(node);
        if(node instanceof IElectricNode enode)
            graph.addNode(enode);
        if(node instanceof ICouplingNode coupling)
            graph.couple(coupling);
    }

    @Override
    public void removeNode(INode node) {
        super.removeNode(node);
        if(node instanceof IElectricNode enode) {
            if(!graph.getConnectedLines(enode).isEmpty())
                PowerGrid.LOGGER.warn("Removed a node which had connections", new Throwable());
            graph.removeNode(enode);
        }
        if(node instanceof ICouplingNode cnode)
            graph.decouple(cnode);
    }

    @Override
    public void addWire(AbstractElectricWire wire) {
        graph.connect(wire.node1, wire.node2, wire);
        super.addWire(wire);
    }

    @Override
    public void removeWire(AbstractElectricWire wire) {
        graph.disconnect(wire.node1, wire.node2, wire);
        super.removeWire(wire);
    }

    public NetworkGraph getGraph() {
        return graph;
    }
}
