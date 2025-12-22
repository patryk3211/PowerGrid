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

public class GraphedElectricalNetwork extends ElectricalNetwork {
    private final NetworkGraph graph;

    public GraphedElectricalNetwork(boolean addGMin) {
        this(new NetworkGraph(), addGMin);
    }

    public GraphedElectricalNetwork(NetworkGraph graph, boolean addGMin) {
        super(addGMin);
        this.graph = graph;
    }

    protected GraphedElectricalNetwork(NetworkGraph graph, boolean addGMin, IMNA mna) {
        super(addGMin, mna);
        this.graph = graph;
    }

    @Override
    public void addNode(INode node) {
        super.addNode(node);
        if(node instanceof IElectricNode enode)
            graph.addNode(enode);
        if(node instanceof ICouplingNode coupling) {
            graph.couple(coupling);
            for(var coupled : coupling.coupledNodes()) {
                checkConnectivity(coupled, null);
                removeLeaf(coupled);
            }
        }
    }

    @Override
    public void removeNode(INode node) {
        super.removeNode(node);
        if(node instanceof IElectricNode enode) {
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
//            if(isLeaf(node)) {
//                checked.add(node);
//                // We must trace further
////                for(var node2 : nodes) {
////                    if(checked.contains(node2))
////                        continue;
////                    var subtrace = new HashSet<IElectricNode>();
////                    subtrace.add(node);
////                    if(circularCheck(node2, subtrace, null)) {
////                        checked.addAll(subtrace);
////                    }
////                }
//            } else {
            if (track != null)
                track.setValue(node);
//            }
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

    public void graphCheck() {
        var checked = new HashSet<IElectricNode>();
        for(var node : nodes) {
            if(!(node instanceof FloatingNode)) {
                // Only floating nodes are checked for continuity
                continue;
            }
        }
    }

//    @Override
//    protected boolean canOptimize(INode node) {
//        if(node instanceof IElectricNode enode) {
//            if(graph.hasCouplings(enode))
//                return false;
//            for(var wire : graph.getWires(enode)) {
//                if(wire instanceof SwitchedWire)
//                    return false;
//            }
//            return true;
//        }
//        return super.canOptimize(node);
//    }

    @Override
    public void addWire(AbstractElectricWire wire) {
        graph.connect(wire.node1, wire.node2, wire);
        super.addWire(wire);
        checkConnectivity(wire.node1, null);
        checkConnectivity(wire.node2, null);
    }

    @Override
    public void removeWire(AbstractElectricWire wire) {
        graph.disconnect(wire.node1, wire.node2, wire);
        super.removeWire(wire);
        checkConnectivity(wire.node1, null);
        checkConnectivity(wire.node2, null);
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
}
