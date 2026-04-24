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

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.createmod.catnip.data.Pair;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.commands.DebugCommand;
import org.patryk3211.powergrid.electricity.sim.node.CurrentSourceNode;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.solver.IMNA;
import org.patryk3211.powergrid.electricity.sim.solver.IMatrixAccess;
import org.patryk3211.powergrid.electricity.sim.special.SeriesWire;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class GraphedElectricalNetwork extends ElectricalNetwork {
    private final NetworkGraph graph;
    private final Set<IElectricNode> deferredNodeCheck = new ReferenceOpenHashSet<>();
    private final Map<ElectricWire, SeriesWire> seriesWires = new Reference2ReferenceOpenHashMap<>();

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
                for(var wire : graph.getWires(coupled)) {
                    var series = seriesWires.get(wire);
                    if(series == null)
                        continue;
                    dissolveSeriesWire(series, null);
                    addToCheck(coupled);
                }
            }
            graph.couple(coupling);
        }
    }

    @Override
    public void removeNode(INode node) {
        if(node instanceof IElectricNode enode) {
            for(var wire : graph.getWires(enode)) {
                var series = seriesWires.get(wire);
                if(series == null)
                    continue;
                dissolveSeriesWire(series, null);
            }
        }
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

    protected void collectSeries(List<ElectricWire> collection, boolean atEnd, IElectricNode node, AbstractElectricWire wire) {
        if(graph.connectionCount(node) != 2)
            return;
        var wires = graph.getWires(node);
        if(wires.size() != 2)
            return;
        var wire1 = wires.get(0);
        var wire2 = wires.get(1);
        AbstractElectricWire nextWire;
        if(wire == wire1) {
            nextWire = wire2;
        } else if(wire == wire2) {
            nextWire = wire1;
        } else {
            return;
        }
        if(nextWire.getNode1() == null || nextWire.getNode2() == null)
            return;
        if(nextWire instanceof ElectricWire simple) {
            if(collection.contains(simple))
                return;
            var series = seriesWires.get(simple);
            if(series != null)
                dissolveSeriesWire(series, null);
            if(atEnd) {
                collection.add(simple);
            } else {
                collection.add(0, simple);
            }
        } else if(nextWire instanceof SeriesWire series) {
            seriesWires.entrySet().removeIf(entry -> entry.getValue() == series);
            super.removeWire(series);
            if(atEnd) {
                if(nextWire.getNode1() == node) {
                    // Append to end and series wires start here
                    for(int i = 0; i < series.wires.size(); ++i) {
                        var simple = series.wires.get(i);
                        if(collection.contains(simple))
                            return;
                        collection.add(simple);
                    }
                } else {
                    // Append to end but flip series wire order
                    for(int i = series.wires.size() - 1; i >= 0; --i) {
                        var simple = series.wires.get(i);
                        if(collection.contains(simple))
                            return;
                        collection.add(simple);
                    }
                }
            } else {
                if(nextWire.getNode2() == node) {
                    // Append to start and series wire ends here
                    for(int i = 0; i < series.wires.size(); ++i) {
                        var simple = series.wires.get(i);
                        if(collection.contains(simple))
                            return;
                        collection.add(i, simple);
                    }
                } else {
                    // Append to start but flip series wire order
                    for(int i = series.wires.size() - 1; i >= 0; --i) {
                        var simple = series.wires.get(i);
                        if(collection.contains(simple))
                            return;
                        collection.add(simple);
                    }
                }
            }
        } else {
            return;
        }
        assert nextWire.getNode1() == node || nextWire.getNode2() == node : "Next wire isn't connected to node";
        if(nextWire.getNode1() == node) {
            collectSeries(collection, atEnd, nextWire.getNode2(), nextWire);
        } else {
            collectSeries(collection, atEnd, nextWire.getNode1(), nextWire);
        }
    }

    private IElectricNode findEndNode(ElectricWire endWire, ElectricWire secondWire) {
        // Make sure at least one node is actually shared.
        assert endWire.getNode1() == secondWire.getNode1() || endWire.getNode1() == secondWire.getNode2() ||
                endWire.getNode2() == secondWire.getNode1() || endWire.getNode2() == secondWire.getNode2()
                : "End wire and previous wire don't share a node";
        if(endWire.getNode1() == secondWire.getNode1() || endWire.getNode1() == secondWire.getNode2()) {
            // First node is shared
            return endWire.getNode2();
        } else {
            // Second node is shared
            return endWire.getNode1();
        }
    }

    protected Set<IElectricNode> checkSeries(IElectricNode node) {
        var seriesWires = new ArrayList<ElectricWire>();
        if(graph.connectionCount(node) != 2)
            return null;
        var wires = graph.getWires(node);
        if(wires.size() != 2)
            return null;
        var wire1 = wires.get(0);
        var wire2 = wires.get(1);
        // TODO: Wire cannot be a series wire since those are not stored in the graph, those check might be redundant.
        if(wire1 instanceof ElectricWire simple) {
            collectSeries(seriesWires, false, node, simple);
        } else if(wire1 instanceof SeriesWire series) {
            collectSeries(seriesWires, false, node, series);
        }
        if(wire2 instanceof ElectricWire simple) {
            collectSeries(seriesWires, true, node, simple);
        } else if(wire2 instanceof SeriesWire series) {
            collectSeries(seriesWires, true, node, series);
        }
        if(seriesWires.size() <= 1)
            return null;
        var first = seriesWires.get(0);
        var second = seriesWires.get(1);
        var node1 = findEndNode(first, second);

        var last = seriesWires.get(seriesWires.size() - 1);
        var previous = seriesWires.get(seriesWires.size() - 2);
        var node2 = findEndNode(last, previous);
        if(node1 == node2)
            return null;

        var affected = new ReferenceOpenHashSet<IElectricNode>();
        var seriesWire = new SeriesWire(node1, node2, seriesWires);
        seriesWires.forEach(part -> {
            // Make sure the graph is not affected.
            super.removeWire(part);
            this.seriesWires.put(part, seriesWire);
            var partNode1 = part.getNode1();
            if(partNode1 != node1 && partNode1 != node2) {
                seriesWire.nodes.add(partNode1);
                internalRemoveNode(partNode1);
                partNode1.setNetwork(this);
                partNode1.assignIndex(-1);
            }
            var partNode2 = part.getNode2();
            if(partNode2 != node1 && partNode2 != node2) {
                seriesWire.nodes.add(partNode2);
                internalRemoveNode(partNode2);
                partNode2.setNetwork(this);
                partNode2.assignIndex(-1);
            }
            part.setNetwork(this);
            affected.add(part.getNode1());
            affected.add(part.getNode2());
        });
        super.addWire(seriesWire);
        return affected;
    }

    @Override
    public void addWire(AbstractElectricWire wire) {
        dissolveSeriesWire(wire.getNode1());
        dissolveSeriesWire(wire.getNode2());
        graph.connect(wire.node1, wire.node2, wire);
        super.addWire(wire);
        addToCheck(wire.node1);
        addToCheck(wire.node2);
    }

    private void dissolveSeriesWire(SeriesWire series, @Nullable AbstractElectricWire except) {
        super.removeWire(series);
        seriesWires.entrySet().removeIf(entry -> entry.getValue() == series);
        series.nodes.forEach(node -> {
            super.addNode(node);
            addToCheck(node);
        });
        series.wires.forEach(part -> {
            if(part != except) {
                super.addWire(part);
            }
        });
    }

    private void dissolveSeriesWire(IElectricNode node) {
        if(node == null)
            return;
        var wires = graph.getWires(node);
        if(wires.size() != 2)
            return;
        var series = seriesWires.get(wires.get(0));
        if(series != null)
            dissolveSeriesWire(series, null);
    }

    @Override
    public void removeWire(AbstractElectricWire wire) {
        var series = seriesWires.get(wire);
        if(series != null) {
            dissolveSeriesWire(series, wire);
        } else {
            super.removeWire(wire);
        }
        graph.disconnect(wire.node1, wire.node2, wire);
        addToCheck(wire.node1);
        addToCheck(wire.node2);
    }

    @Override
    protected Double tryGetValue(INode node) {
        var val = super.tryGetValue(node);
        if(val == null && node.getIndex() == -1)
            return node.getSavedValue();
        return val;
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

    public Collection<Pair<AbstractElectricWire, Double>> findTopProblematicWires(IMatrixAccess residual, int count, double threshold) {
        var nodes = findProblematicNodes(residual, threshold);
        var wires = new Object2DoubleArrayMap<AbstractElectricWire>();
        for(var node1 : nodes) {
            for(var node2 : nodes) {
                if(node1 == node2)
                    continue;
                double score = Math.max(residual.get(node1.getIndex(), 0), residual.get(node2.getIndex(), 0));
                if(node1 instanceof IElectricNode enode1 && node2 instanceof IElectricNode enode2) {
                    for(var wire : graph.getWires(enode1, enode2)) {
                        wires.compute(wire, (key, value) -> value == null || score > value ? score : value);
                    }
                }
            }
        }
        return wires.object2DoubleEntrySet().stream()
                .sorted(Comparator
                        .comparingDouble((ToDoubleFunction<Object2DoubleMap.Entry<AbstractElectricWire>>) Object2DoubleMap.Entry::getDoubleValue)
                        .reversed())
                .limit(count)
                .map(entry -> Pair.of(entry.getKey(), entry.getDoubleValue()))
                .toList();
    }

    @Override
    public void convergenceProblems(double residual, IMatrixAccess ResidualVector) {
        DebugCommand.pushProblems(this, residual, findTopProblematicWires(ResidualVector, 5, 1e-8));
    }

    public NetworkGraph getGraph() {
        return graph;
    }

    @Override
    public void prepare(int multiTicks) {
        preparing = true;
        var cleanNodes = new HashSet<IElectricNode>();
        var changed = true;
        while(changed) {
            if(deferredNodeCheck.isEmpty())
                break;
            var iter = deferredNodeCheck.iterator();
            IElectricNode node;
            boolean added;
            do {
                node = iter.next();
            } while(!(added = cleanNodes.add(node)) && iter.hasNext());
            if(!added)
                break;

            changed = false;
            var affected = checkSeries(node);
            if(affected != null) {
                changed = true;
                affected.forEach(node1 -> {
                    if(node1.getIndex() == -1 && !isLeaf(node1))
                        deferredNodeCheck.remove(node1);
                    else deferredNodeCheck.add(node1);
                });
            }
        }
        for(var node : deferredNodeCheck) {
            checkConnectivity(node, null);
        }
        deferredNodeCheck.clear();
        preparing = false;
        super.prepare(multiTicks);
    }

    @Override
    public void merge(ElectricalNetwork other) {
        if(other instanceof GraphedElectricalNetwork graphed) {
            while(!graphed.seriesWires.isEmpty()) {
                var wire = graphed.seriesWires.values().iterator().next();
                graphed.dissolveSeriesWire(wire, null);
            }
        }
        while(!seriesWires.isEmpty()) {
            var wire = seriesWires.values().iterator().next();
            dissolveSeriesWire(wire, null);
        }
        super.merge(other);
        deferredNodeCheck.addAll(other.leafNodes.keySet());
    }

    @Override
    public void clear() {
        super.clear();
        seriesWires.clear();
        deferredNodeCheck.clear();
    }
}
