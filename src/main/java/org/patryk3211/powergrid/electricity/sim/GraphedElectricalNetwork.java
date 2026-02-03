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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.commands.DebugCommand;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.sim.solver.IMNA;
import org.patryk3211.powergrid.electricity.sim.solver.IMatrixAccess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class GraphedElectricalNetwork extends ElectricalNetwork {
    private final NetworkGraph graph;
    private final Set<IElectricNode> deferredNodeCheck = new ReferenceOpenHashSet<>();
    private boolean preparing = false;

    public GraphedElectricalNetwork(boolean addGMin) {
        this(new NetworkGraph(), addGMin);
    }

    public GraphedElectricalNetwork(NetworkGraph graph, boolean addGMin) {
        super(addGMin);
        this.graph = graph;
    }

    public GraphedElectricalNetwork(NetworkGraph graph, boolean addGMin, Function<ElectricalNetwork, IMNA> mna) {
        super(addGMin, mna);
        this.graph = graph;
    }

    private void addToCheck(IElectricNode node) {
        if(preparing)
            return;
        deferredNodeCheck.add(node);
    }

    @Override
    public void addNode(INode node) {
        super.addNode(node);
        if(node instanceof IElectricNode enode) {
            graph.addNode(enode);
            addToCheck(enode);
        }
        if(node instanceof ICouplingNode coupling) {
            for(var coupled : coupling.coupledNodes()) {
                deferredNodeCheck.addAll(graph.getConnectedNodes(coupled));
                removeLeaf(coupled);
            }
            graph.couple(coupling);
        }
    }

    @Override
    public void removeNode(INode node) {
        super.removeNode(node);
        if(node instanceof IElectricNode enode) {
            deferredNodeCheck.remove(enode);
            var connections = graph.getConnectedLines(enode);
            if(!connections.isEmpty()) {
                PowerGrid.LOGGER.warn("Removed a node which had connections");
                for(var conn : connections) {
                    PowerGrid.LOGGER.warn(" - {}", conn);
                }
                PowerGrid.LOGGER.warn("Stack trace: ", new Throwable());
            }
            graph.removeNode(enode);
        }
        if(node instanceof ICouplingNode cnode)
            graph.decouple(cnode);
    }

    private boolean circularCheck(IElectricNode node, Set<IElectricNode> checked, @Nullable MutableObject<IElectricNode> track) {
        if(node == null) {
            if(track != null)
                track.setValue(null);
            return true;
        }
        if(checked.contains(node))
            return true;
        if(graph.isLeafEliminating(node)) {
            if(track != null)
                track.setValue(node);
            return true;
        }
        var nodes = graph.getConnectedNodes(node);
        if(node instanceof CurrentSourceNode) {
            if(track != null)
                track.setValue(node);
            return true;
        }
        if(nodes.size() <= 1) {
            // Leaf node, run circular check to find the tracked node.
            checked.add(node);
            if(nodes.size() == 1)
                circularCheck(nodes.get(0), checked, track);
            return false;
        } else if(nodes.size() >= 3) {
            // Connecting into a tie
            if (track != null)
                track.setValue(node);
            return true;
        } else { // nodes.size() == 2
            // Check both sides
            var failed = false;
            checked.add(node);
            for(var nextNode : nodes) {
                if(checked.contains(nextNode))
                    continue;
                if(!circularCheck(nextNode, checked, track))
                    failed = true;
            }
            return !failed;
        }
    }

    protected void checkConnectivity(IElectricNode node, Set<IElectricNode> outerChecked) {
        if(node == null)
            return;
        if(outerChecked == null)
            outerChecked = new HashSet<>();
        if(!outerChecked.add(node))
            return;
        var checked = new HashSet<IElectricNode>();
        if(isLeaf(node)) {
            // Trace the graph to check that the node is part of a circle
            var track = new MutableObject<IElectricNode>(null);
            if(circularCheck(node, checked, track)) {
                checked.add(node);
                for(var node2 : checked)
                    removeLeaf(node2);
                // Propagate update
                for(var connected : graph.getConnectedNodes(node)) {
                    checkConnectivity(connected, outerChecked);
                }
            } else {
                // Update tracked node
                for(var node2 : checked)
                    makeLeaf(node2, track.getValue());
            }
        } else {
            var track = new MutableObject<IElectricNode>(null);
            if(!circularCheck(node, checked, track)) {
                checked.add(node);
                for(var node2 : checked)
                    makeLeaf(node2, track.getValue());
                // Propagate update
                for(var connected : graph.getConnectedNodes(node)) {
                    checkConnectivity(connected, outerChecked);
                }
            }
        }
    }

    @Override
    public void addWire(AbstractElectricWire wire) {
        graph.connect(wire.node1, wire.node2, wire);
        super.addWire(wire);
        addToCheck(wire.node1);
        addToCheck(wire.node2);
    }

    @Override
    public void removeWire(AbstractElectricWire wire) {
        graph.disconnect(wire.node1, wire.node2, wire);
        super.removeWire(wire);
        addToCheck(wire.node1);
        addToCheck(wire.node2);
    }

    public Collection<AbstractElectricWire> findProblematicWires(IMatrixAccess residual, double threshold) {
        var nodes = findProblematicNodes(residual, threshold);
        var wires = new HashSet<AbstractElectricWire>();
        for(var node1 : nodes) {
            for(var node2 : nodes) {
                if(node1 == node2)
                    continue;
                if(node1 instanceof IElectricNode enode1 && node2 instanceof IElectricNode enode2)
                    wires.addAll(graph.getWires(enode1, enode2));
            }
        }
        return wires;
    }

    @Override
    public void convergenceProblems(double residual, IMatrixAccess ResidualVector) {
        DebugCommand.pushProblems(this, residual, findProblematicWires(ResidualVector, 1e-8f));
    }

    public NetworkGraph getGraph() {
        return graph;
    }

    @Override
    public void prepare(int multiTicks) {
        preparing = true;
        for(var node : deferredNodeCheck) {
            checkConnectivity(node, null);
        }
        deferredNodeCheck.clear();
        preparing = false;
        super.prepare(multiTicks);
    }

    @Override
    public void merge(ElectricalNetwork other) {
        super.merge(other);
        deferredNodeCheck.addAll(other.leafNodes.keySet());
    }

    @Override
    public void clear() {
        super.clear();
        deferredNodeCheck.clear();
    }
}
