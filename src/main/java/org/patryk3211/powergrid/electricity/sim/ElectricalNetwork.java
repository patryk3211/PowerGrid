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

import org.ejml.data.DMatrixRMaj;
import org.patryk3211.powergrid.electricity.sim.node.ICouplingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.electricity.sim.solver.BiCGSTABSolver;
import org.patryk3211.powergrid.electricity.sim.solver.ISolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ElectricalNetwork {
    private static final double PRECISION = 1e-6;

    private final Set<ElectricWire> wires = new HashSet<>();
    private final Set<ICouplingNode> couplings = new HashSet<>();
    private final ArrayList<INode> nodes = new ArrayList<>();

    private final ISolver solver;
    private boolean[] voltageSources;
    private DMatrixRMaj conductanceMatrix;
    private DMatrixRMaj AMatrix;
    private DMatrixRMaj currentMatrix;
    private int sourceCount;

    private boolean dirty;
    private boolean recalculating;

    public ElectricalNetwork() {
        solver = new BiCGSTABSolver(PRECISION);
        dirty = true;
        sourceCount = 0;
    }

    // Make sure all variables are completely rebuilt and repopulated.
    public void setDirty() {
        this.dirty = true;
    }

    public void addNode(INode node) {
        if(node instanceof IElectricNode enode)
            addNode(enode);
        else if(node instanceof ICouplingNode cnode)
            addNode(cnode);
        else
            throw new IllegalArgumentException("Unsupported node type provided");
    }

    public void addNode(IElectricNode node) {
        node.assignIndex(nodes.size());
        node.setNetwork(this);
        nodes.add(node);
        setDirty();

        if(node instanceof VoltageSourceNode)
            ++sourceCount;
    }

    public void addNodes(IElectricNode... nodes) {
        for(var node : nodes)
            addNode(node);
    }

    public void removeNode(INode node) {
        if(nodes.get(node.getIndex()) != node)
            // This node is not actually in this network.
            return;

        if(nodes.size() > 1) {
            // Move last node into the place of removed node to prevent holes in the array.
            var last = nodes.get(nodes.size() - 1);
            nodes.set(node.getIndex(), last);
            nodes.remove(nodes.size() - 1);
            last.assignIndex(node.getIndex());
        } else {
            // This is the only node so it's ok to just remove it.
            nodes.remove(node);
        }

        if(node instanceof ICouplingNode)
            couplings.remove(node);
        if(node instanceof VoltageSourceNode)
            --sourceCount;

        setDirty();
    }

    public void removeNode(int index) {
        if(nodes.size() <= index)
            return;

        removeNode(nodes.get(index));
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public void addWire(ElectricWire wire) {
        if((wire.node1 != null && !nodes.contains(wire.node1)) || (wire.node2 != null && !nodes.contains(wire.node2)))
            // If node of a wire is not null it must be in the network's node set.
            throw new IllegalArgumentException("Both nodes of a wire must be part of the network");
        wire.setNetwork(this);
        wires.add(wire);

        updateConductance(wire, wire.conductance());
    }

    public void updateConductance(ElectricWire wire, double change) {
        if(conductanceMatrix == null || dirty)
            return;

        if(wire.node1 != null && wire.node2 != null) {
            var index1 = wire.node1.getIndex();
            var index2 = wire.node2.getIndex();
            conductanceMatrix.add(index1, index1, change);
            conductanceMatrix.add(index2, index2, change);
            conductanceMatrix.add(index1, index2, -change);
            conductanceMatrix.add(index2, index1, -change);
            if(!voltageSources[index1]) {
                AMatrix.add(index1, index1, change);
                AMatrix.add(index2, index1, -change);
            } else {
                var U = ((VoltageSourceNode) nodes.get(index1)).getVoltage();
                currentMatrix.add(index1, 0, U * -change);
                currentMatrix.add(index2, 0, U * change);
            }
            if(!voltageSources[index2]) {
                AMatrix.add(index2, index2, change);
                AMatrix.add(index1, index2, -change);
            } else {
                var U = ((VoltageSourceNode) nodes.get(index2)).getVoltage();
                currentMatrix.add(index2, 0, U * -change);
                currentMatrix.add(index1, 0, U * change);
            }
        } else {
            var index = wire.node1 != null ? wire.node1.getIndex() : wire.node2.getIndex();
            conductanceMatrix.add(index, index, change);
            if(!voltageSources[index]) {
                AMatrix.add(index, index, change);
            } else {
                var U = ((VoltageSourceNode) nodes.get(index)).getVoltage();
                currentMatrix.add(index, 0, U * change);
            }
        }
    }

    public void removeWire(ElectricWire wire) {
        if(!wires.contains(wire))
            return;
        wires.remove(wire);

        updateConductance(wire, -wire.conductance());
    }

    public void updateResistance(ElectricWire wire, double oldResistance) {
        double change = wire.conductance();
        if(oldResistance != 0)
            change -= 1 / oldResistance;
        updateConductance(wire, change);
    }

    public void addNode(ICouplingNode coupling) {
        coupling.assignIndex(nodes.size());
        coupling.setNetwork(this);
        couplings.add(coupling);
        nodes.add(coupling);
        setDirty();
    }

    public void updateVoltage(VoltageSourceNode node, double oldVoltage) {
        if(conductanceMatrix == null || dirty)
            return;

        var diff = node.getVoltage() - oldVoltage;
        var index = node.getIndex();

        for(int i = 0; i < nodes.size(); ++i) {
            currentMatrix.add(i, 0, -diff * conductanceMatrix.get(i, index));
        }
    }

    private void populateConductanceMatrix() {
        conductanceMatrix.zero();
        for(var wire : wires) {
            var G = wire.conductance();
            if(wire.node1 != null && wire.node2 != null) {
                var index1 = wire.node1.getIndex();
                var index2 = wire.node2.getIndex();
                conductanceMatrix.add(index1, index1, G);
                conductanceMatrix.add(index2, index2, G);
                conductanceMatrix.add(index1, index2, -G);
                conductanceMatrix.add(index2, index1, -G);
            } else {
                var index = wire.node1 != null ? wire.node1.getIndex() : wire.node2.getIndex();
                conductanceMatrix.add(index, index, G);
            }
        }

        for(var node : couplings) {
            node.couple(conductanceMatrix);
        }

        AMatrix.setTo(conductanceMatrix);
    }

    private void populateCurrentMatrix() {
        currentMatrix.zero();
        for(int nodeIndex = 0; nodeIndex < nodes.size(); ++nodeIndex) {
            final var node = nodes.get(nodeIndex);
            if(node instanceof final VoltageSourceNode source) {
                var U = source.getVoltage();
                var index = node.getIndex();

                for(int i = 0; i < nodes.size(); ++i) {
                    currentMatrix.add(i, 0, -U * conductanceMatrix.get(i, index));
                    AMatrix.set(i, index, 0);
                }
                AMatrix.set(index, index, -1);
                voltageSources[nodeIndex] = true;
            } else {
                voltageSources[nodeIndex] = false;
            }
        }
    }

    public void merge(ElectricalNetwork other) {
        other.nodes.forEach(this::addNode);
        other.wires.forEach(this::addWire);
        // Make the other network empty.
        other.nodes.clear();
        other.wires.clear();
        other.couplings.clear();
    }

    public void calculate(boolean printResult, boolean printState) {
        if(sourceCount == 0) {
            for(var node : nodes) {
                if(node instanceof IElectricNode enode) {
                    enode.receiveResult(0);
                }
            }
            return;
        }

        var nodeCount = nodes.size();
        if(conductanceMatrix == null || dirty || conductanceMatrix.getNumRows() != nodeCount) {
            conductanceMatrix = new DMatrixRMaj(nodeCount, nodeCount);
            AMatrix = new DMatrixRMaj(nodeCount, nodeCount);
            currentMatrix = new DMatrixRMaj(nodeCount, 1);
            voltageSources = new boolean[nodeCount];
            solver.setStateSize(nodeCount);
            dirty = false;

            // Conductance and coupling matrices need to be fully rebuild only after a state size change,
            // individual resistance and coupling value changes are handled by `updateResistance()` and `updateCoupling()` respectively.
            populateConductanceMatrix();
            populateCurrentMatrix();
        }

        if(printState) {
            System.out.println(AMatrix);
            System.out.println(currentMatrix);
        }

        var result = solver.solve(AMatrix, currentMatrix);
        if(printResult) {
            System.out.println(result);
        }
        for(var node : nodes) {
            if(node instanceof IElectricNode enode) {
                float value = (float) result.get(node.getIndex(), 0);
                if(Float.isNaN(value)) {
                    if(!recalculating) {
                        // Try again.
                        solver.zero();
                        recalculating = true;
                        calculate();
                        recalculating = false;
                        break;
                    } else {
                        // Failed again
                        enode.receiveResult(0);
                    }
                } else {
                    enode.receiveResult(value);
                }
            }
        }
    }

    public void calculate() {
        calculate(false, false);
    }
}
